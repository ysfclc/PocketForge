# Acceptance ledger

| Gate | Status | Evidence required |
|---|---|---|
| Product, architecture, security docs | Authored | repository review |
| Seven agent roles and ten skills | Authored | `.agent/` inventory |
| Git-native direct commit pipeline | Authored; YAML parsed locally | `.github/workflows/pipeline.yml` |
| Action SHA pins and per-job permissions | Static verification passed in local mirror | `scripts/verify-foundation.py` |
| Native workspace / Project Passport / mission queue | Android build passed in CI run #87; initial UI launched by user; broader persistence and import acceptance remains pending | Run #87; follow-up folder-import inspection change awaits build/device verification |
| GitHub repository connection and browser | Initial connector compiled in CI run #87; user reported first-pass connection difficulty. URL normalization and direct post-connect project handoff are now authored; latest UX revision awaits build/device verification | Run #87 for the prior source; next revision must verify pasted public URL, private token, pinned revision, directory navigation and text preview |
| GitHub single-file branch/commit/PR flow | Prior implementation compiled in CI run #87; still not device-tested | Device evidence required for branch SHA/base verification, stale-file rejection, commit and PR URLs, and no mutation without approval |
| Advanced GitHub operations (multi-file patches, history/checks, artifact retrieval, mission integration) | Not implemented | scoped token grants, branch creation, base-revision checks, reviewed diff, explicit commit/PR approval and resulting GitHub evidence |
| Rust three Android ABIs | Passed in CI run #87 | `cargo-ndk` built arm64-v8a, armeabi-v7a and x86_64 in run 44784314374 |
| Android debug APK | Passed in CI run #87 | Artifact 14653222608; package `com.pocketforge.app.debug`, version `0.1.0-keystone`; APK v2 signature verified |
| actionlint and foundation verification | Passed on commit `3d92e1d` in the security workflow's static job | `actionlint` and `scripts/verify-foundation.sh` steps succeeded in run 44784314265; Cargo scanner install and dependency review were separate, not yet green |
| SBOM and provenance attestation | Passed in CI run #87 | SBOM artifact 14654355782; provenance attestation 70253439; APK SHA-256 `b2cead4187ac591c76e8d971a44576da4e9934ad24efde331bb37a7249fc79ac` |
| iOS contract-only scaffold | Authored; intentionally gated | no Xcode build job |
| Protected release signing | Not configured/verified | approved Environment run |
| Physical device acceptance | Initial APK launch observed by user; detailed acceptance remains pending | Test GitHub connect/browse/write approvals, folder link/restart/browse, persistence, and target-device stability on the user's Android device |
| Gradle dependency verification metadata | Deferred Phase 1 gate | committed verification-metadata.xml and strict CI mode |

Never mark pending/not-tested as passed without command, run URL, revision and result.

## GitHub runner allocation observation — 2026-11-14

The Git-native workflows are registered and triggered on main commits. However, all build jobs tested so far terminate before runner allocation: `runner_id: 0`, empty runner name, zero steps and no logs. Downloading the job logs returns GitHub's `BlobNotFound`, confirming that no runner log stream was created. No source/build step ran, so this does not establish a Kotlin, Rust, Gradle or actionlint failure.

Both runner labels have now been tested:
- `ubuntu-latest`: earlier runs fail before runner assignment.
- `ubuntu-24.04`: the explicit-label diagnostic also fails before runner assignment.

Explicit-label diagnostic runs for source commit `66c1254`:
- Direct CI pipeline: https://github.com/ysfclc/PocketForge/actions/runs/43651673973
- Supply-chain/actionlint checks: https://github.com/ysfclc/PocketForge/actions/runs/43651673846

The subsequent documentation commit `093a254` triggered the same two workflows and reproduced the failure:
- Direct CI pipeline: https://github.com/ysfclc/PocketForge/actions/runs/43651709151
- Supply-chain/actionlint checks: https://github.com/ysfclc/PocketForge/actions/runs/43651709132

This earlier runner-allocation diagnosis was superseded on 2026-11-20: after the repository became public, run #87 allocated a hosted runner and completed the full Rust, Android APK, signature, SBOM and provenance pipeline successfully.


## GitHub connector CI attempt — 2026-11-20

The new connector source and static contract checks were committed to the feature branch. The latest pipeline run (44712322777) and security run (44712322822) still fail before runner allocation: runner_id 0, empty runner name, zero executed steps and no artifacts. These runs therefore do not compile or validate the new Kotlin implementation. Build and Redmi device acceptance remain pending.


## Verified PocketForge build — 2026-11-20

- Successful CI run: https://github.com/ysfclc/PocketForge/actions/runs/44784314374
- Source commit: `3d92e1d2463dbd60101d9be2232a582c82e94dd0`.
- Rust cross-compilation for arm64-v8a, armeabi-v7a and x86_64 passed; Rust tests and Clippy passed.
- Android debug APK assembly and `apksigner verify` passed. Package metadata: `com.pocketforge.app.debug`, version `0.1.0-keystone`, min API 26, target API 35.
- APK artifact: `pocketforge-debug-81463e9decd62968bbe432f47dca1e4cde2a7464` (artifact ID 14653222608).
- APK SHA-256: `b2cead4187ac591c76e8d971a44576da4e9934ad24efde331bb37a7249fc79ac`.
- SBOM artifact ID 14654355782. Build provenance attestation: https://github.com/ysfclc/PocketForge/attestations/70253439.
- This verifies PocketForge's own Android debug package. It does not verify local compilation of imported projects or mission/agent execution.
- The user launched the initial APK and reported two usability defects: GitHub connection did not complete from the first UI attempt, and folder import produced no useful visible outcome. The next feature revision adds URL normalization, a connected-project handoff, persisted folder-permission validation, SAF folder inspection and browsing, and an accurate Factory status. That follow-up revision still requires CI and physical-device acceptance.
