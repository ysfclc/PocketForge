#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
: "${ANDROID_NDK_HOME:=${ANDROID_NDK_ROOT:-}}"
test -n "$ANDROID_NDK_HOME" || { echo "ANDROID_NDK_HOME required" >&2; exit 2; }
export ANDROID_NDK_HOME
cd "$ROOT/core"
rustup toolchain install 1.82.0 --profile minimal --component rustfmt --component clippy
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android --toolchain 1.82.0
cargo +1.82.0 install cargo-ndk --version 3.5.4 --locked
for abi in arm64-v8a armeabi-v7a x86_64; do
  cargo +1.82.0 ndk -t "$abi" -o "$ROOT/android/app/src/main/jniLibs" build --release --locked -p pocketforge-ffi
done
