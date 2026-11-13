# Repository log — 2026-11-14

## Keystone foundation

Created product, architecture, data-model and FFI contracts; 14 ADRs; seven agent roles; ten skills; security and operations documentation; Rust/Android scaffolds; iOS contract-only scaffold; and pinned GitHub Actions workflows.

## Delivery model correction

The user confirmed that PocketForge is developed directly in `ysfclc/PocketForge`. ZIP handoff, ZIP ingestion, secondary repository provisioning and the `REPO_CREATE_TOKEN` setup were not wanted. The repository was corrected accordingly:

- `.github/workflows/pipeline.yml` now builds the exact GitHub commit: foundation checks → Rust Android ABI cross-build/tests → Android debug APK → APK verification → CycloneDX SBOM → build provenance → Actions artifact.
- `.github/workflows/android.yml` is now manual release-only and protected by the `android-release` Environment.
- ZIP validator/publisher scripts, `incoming/`, archive manifest and ZIP-specific skill were removed/replaced.
- README, SETUP, threat model, phase protocol, release process and acceptance ledger now describe Git-native delivery.

## Verification status

The foundation inventory script passed in the available local scaffold copy before the direct-delivery conversion. Rust, Android SDK/Gradle, actionlint and physical-device checks are not installed in this container and remain unverified here. GitHub Actions is the authoritative build environment. Historical runner-allocation failures are documented in the acceptance ledger; the new pipeline requires a fresh successful run before any build gate is marked passed.
