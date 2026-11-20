# Acceptance ledger

| Gate | Status | Evidence required |
|---|---|---|
| Product, architecture, security docs | Authored | repository review |
| Seven agent roles and ten skills | Authored | `.agent/` inventory |
| Git-native direct commit pipeline | Authored; YAML parsed locally | `.github/workflows/pipeline.yml` |
| Action SHA pins and per-job permissions | Static verification passed in local mirror | `scripts/verify-foundation.py` |
| Native workspace / Project Passport / mission queue | Current PR-head source builds in CI run #109; first UI launch observed by user; improved folder inspection is compiled but not yet device-accepted | Run #109, commit `c0b829c`; verify folder permission survives restart and browse/open behavior on target phone |
| GitHub repository connection and browser | Current PR-head source builds in CI run #109; URL/owner-repo input, immediate project handoff and strict immutable commit pinning are compiled; not yet device-accepted | Run #109, commit `c0b829c`; verify public URL, private token, pinned SHA, directory navigation and text preview on device |
| GitHub single-file branch/commit/PR flow | Source compiled in current PR-head CI run #109; still not device-tested | Device evidence required for branch SHA/base verification, stale-file rejection, commit and PR URLs, and no mutation without approval |
| Advanced GitHub operations (multi-file patches, history/checks, artifact retrieval, mission integration) | Not implemented | scoped token grants, branch creation, base-revision checks, reviewed diff, explicit commit/PR approval and resulting GitHub evidence |
| Rust three Android ABIs | Passed in current CI run #109 | `cargo-ndk` built arm64-v8a, armeabi-v7a and x86_64 in run 44793160312 |
| Android debug APK | Passed in current PR-head CI run #109 | Artifact 14657897652; package `com.pocketforge.app.debug`, version `0.1.0-keystone`, min API 26, target API 35; APK v2 signature verified |
| actionlint and foundation verification | Passed on current PR head in security run #122 | `actionlint` and `scripts/verify-foundation.sh` succeeded in run 44793160820 |
| SBOM and provenance attestation | Passed in current CI run #109 | SBOM artifact 14657571912; APK SHA-256 `fe90729305a10ee1e9c8bfdc3e4507bdccb8bc100ba6fc149cd18ee59da7846b`; provenance attestation emitted and uploaded by run 44793160312 |
| iOS contract-only scaffold | Authored; intentionally gated | no Xcode build job |
| Protected release signing | Not configured/verified | approved Environment run |
| Physical device acceptance | Initial APK launch observed by user; revised onboarding not yet tested | On device: import a folder, inspect/browse/open; connect public GitHub URL, inspect pinned SHA and browse; separately test private token and reviewed write approval |
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


## Verified first-use revision — 2026-11-20

- Current PR head: `c0b829cff3500c54903c047cc8375bed323e3dcb`.
- Successful Android pipeline #109: https://github.com/ysfclc/PocketForge/actions/runs/44793160312.
- Rust cross-compilation for arm64-v8a, armeabi-v7a and x86_64 passed; Rust tests and Clippy passed; Android Gradle assembly and APK signature verification passed.
- APK artifact ID 14657897652. Package `com.pocketforge.app.debug`, version `0.1.0-keystone`, min API 26, target API 35.
- APK SHA-256: `fe90729305a10ee1e9c8bfdc3e4507bdccb8bc100ba6fc149cd18ee59da7846b`. This matches the SHA-256 in the GitHub build provenance attestation.
- SBOM artifact ID 14657571912.
- Security run #122: actionlint, the expanded foundation verifier, `cargo deny check` (advisories, bans, licenses and sources all OK) and `cargo audit` passed. The overall workflow still reports failure because the separate Dependency Review action says Dependency Graph is disabled for this repository. Enable it under GitHub repository Settings → Security → Code security and analysis → Dependency graph to clear that integration-level block.
- This verifies PocketForge's own debug package and static/source security checks. It does not prove real-device behavior of folder/GitHub flows, local compilation of imported projects, or agent execution.
- Physical device acceptance remains pending. The next acceptance pass must exercise local folder selection/permission persistence/inspection/browsing and public/private GitHub connect/pinned browsing on the target phone.
