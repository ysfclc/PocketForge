# Acceptance ledger

| Gate | Status | Evidence required |
|---|---|---|
| Product, architecture, security docs | Authored | repository review |
| Seven agent roles and ten skills | Authored | `.agent/` inventory |
| Git-native direct commit pipeline | Authored; YAML parsed locally | `.github/workflows/pipeline.yml` |
| Action SHA pins and per-job permissions | Static verification passed in local mirror | `scripts/verify-foundation.py` |
| Native workspace / Project Passport / mission queue | Authored in `c57dc23`; build and device verification pending | APK build, persistence/import checks and target-device report |
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

The Git-native workflows are correctly registered and triggered on main commits. The latest runs were created but each build job terminated before runner allocation (`runner_id: 0`, empty steps, no runner name or group). No source/build step ran, so this does not establish a Rust, Gradle or actionlint failure and does not count as a pass.

- Direct CI pipeline for workspace commit `c57dc23`: https://github.com/ysfclc/PocketForge/actions/runs/43651244224
- Supply-chain/actionlint checks for workspace commit `c57dc23`: https://github.com/ysfclc/PocketForge/actions/runs/43651244177

The pipeline run has zero artifacts. A successful runner allocation and full run are still required to produce the APK artifact and close Phase 0. The repository previously passed a local-mirror static contract check and PyYAML parsing for all three workflows; actionlint remains pending because GitHub did not execute the job.
