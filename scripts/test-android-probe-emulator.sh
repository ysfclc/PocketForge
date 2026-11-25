#!/usr/bin/env bash
set -euo pipefail

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:?ANDROID_SDK_ROOT or ANDROID_HOME is required}}"
SDKMANAGER="$(command -v sdkmanager || printf '%s' "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager")"
AVDMANAGER="$(command -v avdmanager || printf '%s' "$SDK_ROOT/cmdline-tools/latest/bin/avdmanager")"
EMULATOR="$SDK_ROOT/emulator/emulator"
AVD_NAME="pocketforge-art-probe-api35"
export ANDROID_AVD_HOME="${RUNNER_TEMP:-/tmp}/pocketforge-avd-home"
mkdir -p "$ANDROID_AVD_HOME"
IMAGE="system-images;android-35;google_apis;x86_64"
LOG_FILE="${RUNNER_TEMP:-/tmp}/pocketforge-emulator.log"

yes | "$SDKMANAGER" --licenses >/dev/null || true
"$SDKMANAGER" --install "emulator" "$IMAGE"
printf 'no\n' | "$AVDMANAGER" create avd --force --name "$AVD_NAME" --package "$IMAGE" --device pixel_2 --path "$ANDROID_AVD_HOME/$AVD_NAME.avd"
"$AVDMANAGER" list avd
if [[ ! -s "$ANDROID_AVD_HOME/$AVD_NAME.ini" ]]; then
  find "$ANDROID_AVD_HOME" -maxdepth 2 -type f -print || true
  echo "avdmanager did not create the expected AVD configuration."
  exit 1
fi

# GitHub-hosted Linux runners expose /dev/kvm, but the runner user is not
# necessarily a member of the kvm group. Apply GitHub's documented udev rule
# on this disposable runner, then fail closed unless KVM is actually usable.
if [[ ! -e /dev/kvm ]]; then
  echo "KVM device /dev/kvm is missing; hardware-accelerated emulator tests cannot run."
  exit 1
fi
echo 'KERNEL=="kvm", GROUP="kvm", MODE="0666", OPTIONS+="static_node=kvm"' |
  sudo tee /etc/udev/rules.d/99-kvm4all.rules >/dev/null
sudo udevadm control --reload-rules
sudo udevadm trigger --name-match=kvm
if [[ ! -r /dev/kvm || ! -w /dev/kvm ]]; then
  ls -l /dev/kvm || true
  id
  echo "The runner still cannot read and write /dev/kvm after applying the hosted-runner rule."
  exit 1
fi
ACCEL_CHECK="$("$EMULATOR" -accel-check 2>&1)"
printf '%s\n' "$ACCEL_CHECK"
if ! grep -Eqi 'KVM.*installed and usable' <<<"$ACCEL_CHECK"; then
  echo "Android Emulator did not confirm usable KVM acceleration."
  exit 1
fi

"$EMULATOR" -avd "$AVD_NAME" -no-window -no-audio -no-boot-anim -no-snapshot -wipe-data \
  -gpu swiftshader -accel auto -memory 2048 -cores 2 -no-metrics -no-snapshot-load -no-snapshot-save \
  -camera-back none > "$LOG_FILE" 2>&1 &
EMULATOR_PID=$!
cleanup() {
  adb emu kill >/dev/null 2>&1 || true
  kill "$EMULATOR_PID" >/dev/null 2>&1 || true
}
trap cleanup EXIT

export ANDROID_SERIAL=emulator-5554
if ! timeout 240 adb wait-for-device; then
  cat "$LOG_FILE" || true
  ps -ef | grep -E "[e]mulator|[q]emu" || true
  echo "Android emulator did not become visible to adb."
  exit 1
fi
BOOTED=false
for attempt in $(seq 1 180); do
  if [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; then
    BOOTED=true
    break
  fi
  sleep 2
done
if [[ "$BOOTED" != true ]]; then
  cat "$LOG_FILE" || true
  adb logcat -d -t 300 || true
  echo "Android emulator did not finish booting."
  exit 1
fi

adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

set +e
(
  cd android
  gradle --no-daemon --stacktrace connectedDebugAndroidTest
)
RESULT=$?
set -e
if [[ "$RESULT" -ne 0 ]]; then
  adb logcat -d -t 500 || true
  cat "$LOG_FILE" || true
  exit "$RESULT"
fi
