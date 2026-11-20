# Acceptance ledger

| Gate | Status | Evidence required |
|---|---|---|
| Product, architecture, security docs | Authored | repository review |
| Seven agent roles and ten skills | Authored | `.agent/` inventory |
| Git-native direct commit pipeline | Authored; YAML parsed locally | `.github/workflows/pipeline.yml` |
| Action SHA pins and per-job permissions | Static verification passed in local mirror | `scripts/verify-foundation.py` |
| Native workspace / Project Passport / mission queue | Authored in `c57dc23`; Kotlin string interpolation corrected in `7e181f4`; build and device verification pending | APK build, persistence/import checks and target-device report |
| GitHub repository connection and browser | Authored on the current feature branch: repository verification, pinned default-branch SHA, directory/text reads, per-repository AES-GCM Android Keystore token vault; build and device verification pending | Android build; public/private repository connection; encrypted token persistence/restart; pinned-revision directory navigation and text preview on Redmi device |
| GitHub single-file branch/commit/PR flow | Authored in source: branch from pinned commit; blob-SHA stale-write check; diff preview; explicit commit and separate PR approval; no merge operation | Android build; private repo token permissions; branch SHA/base verification; stale-file rejection; commit and PR URLs; no mutation without approval |
| Advanced GitHub operations (multi-file patches, history/checks, artifact retrieval, mission integration) | Not implemented | scoped token grants, branch creation, base-revision checks, reviewed diff, explicit commit/PR approval and resulting GitHub evidence |
| Rust three Android ABIs | Pending GitHub runner | cargo-ndk logs and three JNI library outputs |
| Android debug APK | Pending GitHub runner | APK artifact, apksigner verification |
| actionlint and foundation verification | Pending GitHub runner | security workflow run |
| SBOM and provenance attestation | Pending GitHub runner | artifact and attestation |
| iOS contract-only scaffold | Authored; intentionally gated | no Xcode build job |
| Protected release signing | Not configured/verified | approved Environment run |
| Physical Redmi device install | Not tested | Phase 1 device report |
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

The latest pipeline run has zero artifacts. The repository-side workflow configuration has been narrowed as a cause; the remaining checks are GitHub account/repository Actions policy, hosted-runner entitlement/usage, or GitHub-side runner provisioning. Those account-level settings are not exposed by the connected repository API. A successful runner allocation and full run are still required to produce the APK and close Phase 0.
