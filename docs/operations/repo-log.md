# Repository log — 2026-11-14

## Keystone foundation

Created product, architecture, data-model and FFI contracts; 15 ADRs; seven agent roles; ten skills; security and operations documentation; Rust/Android scaffolds; iOS contract-only scaffold; and pinned GitHub Actions workflows.

## Delivery model correction

The user confirmed that PocketForge is developed directly in `ysfclc/PocketForge`. ZIP handoff, ZIP ingestion, secondary repository provisioning and the `REPO_CREATE_TOKEN` setup were not wanted. The repository was corrected accordingly:

- `.github/workflows/pipeline.yml` builds the exact GitHub commit: foundation checks → Rust Android ABI cross-build/tests → Android debug APK → APK verification → CycloneDX SBOM → build provenance → Actions artifact.
- `.github/workflows/android.yml` is manual release-only and protected by the `android-release` Environment.
- `.github/workflows/security.yml` validates action syntax and repository contracts, then runs pinned Cargo security scanners.
- ZIP validator/publisher scripts, `incoming/`, archive manifest and ZIP-specific skill were removed/replaced.
- README, SETUP, scope, secrets policy, threat model, phase protocol, release process and acceptance ledger now describe Git-native delivery.
- ADR-0015 records the decision and supersedes the old archive-based direction.

## First native product slice

Commit `c57dc23` replaces the placeholder text-only Android screen with a first interactive workspace:

- Local project list and Project Passport creation.
- Android Storage Access Framework folder linking with persistable URI permission.
- Mission creation, optional project association and local Draft / Ready state.
- Workspace, Missions and Factory navigation.
- Factory screen reports the native bridge status and explicitly shows unverified build gates as pending.
- `WorkspaceStore` persists project and mission records in app-private SharedPreferences. This is a starter local schema; it is not the final database or full Passport model.

This slice is authored, not yet build- or device-verified. No agent execution, project compilation, GitHub/Memo connector or real build artifact is claimed.

## Verification status

A local mirror of the original foundation layout passed `scripts/verify-foundation.py`; PyYAML parsed all three workflow files; action SHA pins and explicit per-job permissions passed the static verifier. Rust, Android SDK/Gradle, actionlint and physical-device checks remain unverified. The latest pipeline run for `c57dc23` (43651244224) and security run (43651244177) each terminated in about two seconds with `runner_id: 0`, empty steps and no runner assignment. The pipeline produced zero artifacts. No build gate is marked passed until a runner executes the full pipeline.
