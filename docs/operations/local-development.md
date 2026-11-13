# Local development

Prerequisites: JDK 17, Android SDK platform/build-tools 35, Android NDK 27.2.12479018, Rust 1.82.0, cargo-ndk 3.5.4 and Gradle 8.7. The gradlew launcher delegates to GRADLE_HOME or system Gradle; CI downloads 8.7.

Commands: scripts/verify-foundation.sh; scripts/build-rust.sh; scripts/build-android.sh debug.

CI cross-compilation does not prove on-device compilation. Phase 1 measures RAM, storage, thermal throttling, cancellation, process death recovery and build duration on supported devices.
