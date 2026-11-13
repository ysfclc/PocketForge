# Acceptance ledger

| Gate | Status | Evidence required |
|---|---|---|
| Product, architecture, security docs | Authored | repository review |
| Seven agent roles and ten skills | Authored | `.agent/` inventory |
| Git-native direct commit pipeline | Authored; YAML parsed locally | `.github/workflows/pipeline.yml` |
| Action SHA pins and per-job permissions | Static verification passed in local mirror | `scripts/verify-foundation.py` |
| Rust three Android ABIs | Pending GitHub runner | cargo-ndk logs and three JNI library outputs |
| Android debug APK | Pending GitHub runner | APK artifact, apksigner verification |
| actionlint and foundation verification | Pending GitHub runner | security workflow run |
| SBOM and provenance attestation | Pending GitHub runner | artifact and attestation |
| iOS contract-only scaffold | Authored; intentionally gated | no Xcode build job |
| Protected release signing | Not configured/verified | approved Environment run |
| Physical Redmi device install | Not tested | Phase 1 device report |

Never mark pending/not-tested as passed without command, run URL, revision and result.

## GitHub runner allocation observation — 2026-11-14

The Git-native workflows were triggered on commit `df1c4591a7bc4a6ecfa6fb5c9728930adbf60b1f`. GitHub created both the pipeline and security jobs, but they completed in approximately two seconds with `runner_id: 0`, empty step lists and no job logs. No workflow step ran, so this does not establish a source, Rust or Gradle failure and does not count as a pass. A successful runner allocation and full run are still required.

- Pipeline: https://github.com/ysfclc/PocketForge/actions/runs/43527587860
- Security: https://github.com/ysfclc/PocketForge/actions/runs/43527587819

Earlier runner-allocation failures are historical only. Current acceptance is measured against the Git-native pipeline, not the removed ZIP-ingestion workflow.
