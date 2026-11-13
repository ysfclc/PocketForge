# Local development

## Prerequisites

- JDK 17
- Android SDK platform 35 and build-tools 35.0.0
- Android NDK 27.2.12479018
- Rust 1.82.0 with rustfmt and clippy
- cargo-ndk 3.5.4
- Gradle 8.7

The repository's small `android/gradlew` launcher uses `GRADLE_HOME` or a system Gradle installation. CI installs Gradle 8.7 explicitly with the pinned Gradle setup action.

## Commands

From repository root:

```bash
./scripts/verify-foundation.sh
./scripts/build-android.sh debug
```

The Android helper cross-compiles the Rust FFI library for all supported Android ABIs, then assembles the debug APK. To compile only the Rust libraries, run `./scripts/build-rust.sh`.

CI builds the exact GitHub commit, compiles the Rust FFI library for all supported Android ABIs, runs Rust tests/lints, assembles and verifies the debug APK, then uploads the APK and SBOM with build provenance.

CI cross-compilation does not prove on-device compilation. Phase 1 measures RAM, storage, thermal throttling, cancellation, process-death recovery and build duration on supported devices.
