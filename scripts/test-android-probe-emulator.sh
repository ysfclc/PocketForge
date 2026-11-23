#!/usr/bin/env bash
set -euo pipefail

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:?ANDROID_SDK_ROOT or ANDROID_HOME is required}}"
SDKMANAGER="$(command -v sdkmanager || printf '%s' "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager")"
AVDMANAGER="$(command -v avdmanager || printf '%s' "$SDK_ROOT/cmdline-tools/latest/bin/avdmanager")"
EMULATOR="$SDK_ROOT/emulator/emulator"
AVD_NAME="pocketforge-art-probe-api35"
IMAGE="system-images;android-35;google_apis;x86_64"
LOG_FILE="${RUNNER_TEMP:-/tmp}/pocketforge-emulator.log"

"$SDKMANAGER" --install "emulator" "$IMAGE"
printf 'no\n' | "$AVDMANAGER" create avd --force --name "$AVD_NAME" --package "$IMAGE" --device pixel_2

"$EMULATOR" -avd "$AVD_NAME" -no-window -no-audio -no-boot-anim -no-snapshot -wipe-data \
  -gpu swiftshader -accel auto -memory 2048 -cores 2 > "$LOG_FILE" 2>&1 &
EMULATOR_PID=$!
cleanup() {
  adb emu kill >/dev/null 2>&1 || true
  kill "$EMULATOR_PID" >/dev/null 2>&1 || true
}
trap cleanup EXIT

export ANDROID_SERIAL=emulator-5554
timeout 120 adb wait-for-device
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
