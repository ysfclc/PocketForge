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
./scripts/build-rust.sh
(cd android && gradle --no-daemon assembleDebug)
```

The second command cross-compiles the Rust FFI library for all supported Android ABIs. The third assembles the debug APK. The equivalent one-command helper is `./scripts/build-android.sh debug`.

CI builds the exact GitHub commit, compiles the Rust FFI library for all supported Android ABIs, runs Rust tests/lints, assembles and verifies the debug APK, then uploads the APK and SBOM with build provenance.

CI cross-compilation does not prove on-device compilation. Phase 1 measures RAM, storage, thermal throttling, cancellation, process-death recovery and build duration on supported devices.


## Imported-project preflight

PocketForge's Android app can inspect a linked local project through its persisted Storage Access Framework grant. The **Assess build** action is a bounded, read-only structural scan; it identifies common Gradle/Android/Rust markers and reports missing wrapper/build files. It does not execute the project's Gradle wrapper or scripts. Do not treat this assessment as proof that the project can compile.

The on-device execution environment is a separate Phase 1 deliverable. It must provision or explicitly discover a compatible JDK, Gradle distribution, Android SDK/build-tools and any required NDK/toolchains; enforce workspace-scoped file access; support cancellation and process-death recovery; capture real stdout/stderr and exit codes; and verify APK package metadata and signatures before exposing an artifact. A GitHub Actions build is optional and does not satisfy the local-build requirement.
