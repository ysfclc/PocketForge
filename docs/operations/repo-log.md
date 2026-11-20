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

Commit `7e181f4` corrected Kotlin string interpolation and dialog newline escapes found during source review. The source is now checked for these accidental escape patterns by the repository contract verifier.

Commit `66c1254` pins all GitHub-hosted jobs to `ubuntu-24.04` rather than the floating `ubuntu-latest` label as a runner-allocation diagnostic. Both labels produce the same pre-runner failure. The documentation-only follow-up commit `093a254` also retriggered both workflows and reproduced it (pipeline run 43651709151; security run 43651709132).

This slice is authored, not yet build- or device-verified. No agent execution, project compilation, GitHub/Memo connector or real build artifact is claimed.

## Verification status

A local mirror of the original foundation layout passed `scripts/verify-foundation.py`; PyYAML parsed all three workflow files; action SHA pins and explicit per-job permissions passed the static verifier. Rust, Android SDK/Gradle, actionlint and physical-device checks remain unverified. The latest explicit `ubuntu-24.04` pipeline run (43651673973) and security run (43651673846) each failed before runner assignment with `runner_id: 0`, empty steps and no runner name. Job-log retrieval returns `BlobNotFound`; the pipeline produced zero artifacts. The same pre-runner failure also occurred on `ubuntu-latest`. Account-level Actions settings and billing/runner entitlements are not available through the connected repository API. No build gate is marked passed until a runner executes the full pipeline.

## 2026-11-20 — Mission execution constraints locked

- Added [mission execution invariants](../architecture/mission-execution-invariants.md) defining outcome-driven missions, provider/tool separation, one bounded durable mission loop, scoped callable capabilities, result verification and safe recovery.
- Updated `AGENTS.md`, `PRINCIPLES.md`, the architecture overview, product vision and Phase 2 roadmap to make the constraints mandatory.
- This is a documentation/architecture constraint update only. The current Rust core remains a foundation scaffold and no agent execution, capability gateway or build artifact is claimed as implemented.
- The user-approved product boundary remains PocketForge as a specialized Android development factory, independent from Memo.
- Draft review is open as [PR #2](https://github.com/ysfclc/PocketForge/pull/2); it is not merged.

## 2026-11-20 — Direct GitHub mode confirmed as first-class

- The user confirmed that PocketForge must support direct work against connected GitHub repositories as well as local Android project folders.
- Local and GitHub targets share the same mission engine; they are project-source adapters, not separate AI workflows.
- GitHub mode covers scoped repository inspection, branches, diffs, approved commits/PRs, checks and artifact retrieval. GitHub Actions is a separate optional remote execution capability.
- Local compilation, signing and installation remain independent of GitHub accounts and hosted-runner availability.
- Updated the vision, scope, roadmap, architecture, mission invariants and README. This is a documentation/contract update, not an implemented GitHub connector.


## 2026-11-20 — First direct GitHub project workflow authored

- Added an Android GitHub REST adapter to verify a repository, capture its default-branch commit SHA, browse directory contents and read text files at the pinned revision.
- Added repository-scoped GitHub token storage using AES-GCM with a non-exportable Android Keystore key. The encrypted preference key is derived from a SHA-256 digest of the repository identity; tokens are not stored in project records.
- Extended the local workspace project schema compatibly with source kind, repository identity, default branch and pinned revision. Existing records without these fields continue to load as local projects.
- Added a controlled single-file remote change path: create a working branch from the pinned commit, compare the current blob SHA to the reviewed base, show a line-based diff, require an explicit commit action, and require a separate explicit action to create a pull request. No merge action exists.
- GitHub API mutations require a repository-scoped token; editing requires Contents read/write and PR creation additionally requires Pull requests read/write. Public repository browsing remains possible without a token.
- Implementation is authored but not build/device verified. GitHub Actions still fails before runner allocation (runner_id 0, zero steps, no artifacts), so no APK or Kotlin compilation result is claimed.

- Extended `scripts/verify-foundation.py` to require the native GitHub connect/browser/edit/PR flow, pinned revision model, stale-blob guard, strict UTF-8 handling, repository-scoped Android Keystore credential vault and INTERNET permission.
- The latest CI pipeline run (44712322777) and security run (44712322822) still terminate before runner assignment (runner_id 0, zero steps, no artifacts). The new Kotlin source has not yet been compiled by CI.
