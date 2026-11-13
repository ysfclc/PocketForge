# Repository log — 2026-11-14

## Keystone foundation

Created product, architecture, data-model and FFI contracts; 15 ADRs; seven agent roles; ten skills; security and operations documentation; Rust/Android scaffolds; iOS contract-only scaffold; and pinned GitHub Actions workflows.

## Delivery model correction

The user confirmed that PocketForge is developed directly in `ysfclc/PocketForge`. ZIP handoff, ZIP ingestion, secondary repository provisioning and the `REPO_CREATE_TOKEN` setup were not wanted. The repository was corrected accordingly:

- `.github/workflows/pipeline.yml` builds the exact GitHub commit: foundation checks → Rust Android ABI cross-build/tests → Android debug APK → APK verification → CycloneDX SBOM → build provenance → Actions artifact.
- `.github/workflows/android.yml` is manual release-only and protected by the `android-release` Environment.
- ZIP validator/publisher scripts, `incoming/`, archive manifest and ZIP-specific skill were removed/replaced.
- README, SETUP, scope, secrets policy, threat model, phase protocol, release process and acceptance ledger now describe Git-native delivery.
- ADR-0015 records the decision and supersedes the old archive-based direction.

## Verification status

A local mirror of the final foundation layout passed `scripts/verify-foundation.py`; PyYAML parsed all three workflow files; action SHA pins and explicit per-job permissions passed the static verifier. Rust, Android SDK/Gradle, actionlint and physical-device checks remain unverified. GitHub Actions jobs currently fail before runner allocation (`runner_id: 0`, no steps); see the acceptance ledger. No build gate is marked passed until a runner executes the full pipeline.
