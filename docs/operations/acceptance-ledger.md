# Acceptance ledger

| Gate | Status | Evidence required |
|---|---|---|
| Product, architecture, security docs | Authored | repository review |
| Seven agent roles and ten skills | Authored | `.agent/` inventory |
| Rust three Android ABIs | Pending CI | cargo-ndk logs and three JNI library outputs |
| Android debug APK | Pending CI | APK artifact, apksigner verification |
| iOS contract-only scaffold | Authored; intentionally gated | no Xcode build job |
| Actions SHA pins and per-job permissions | Static review required | workflow scan |
| actionlint and foundation verification | Pending CI | security workflow run |
| SBOM and provenance attestation | Pending CI | artifact and attestation |
| Protected release signing | Not configured/verified | approved Environment run |
| Physical Redmi device install | Not tested | Phase 1 device report |

Never mark pending/not-tested as passed without command, run URL, revision and result.

## Historical CI observation — 2026-11-13

The initial Keystone workflows created Rust, Android and supply-chain runs, but jobs ended before runner allocation (runner ID 0; no steps). This was an infrastructure-level failure and did not prove or disprove the source build. The ZIP-ingestion workflow was subsequently removed. Current acceptance must be measured against the Git-native pipeline in `.github/workflows/pipeline.yml`, not those historical runs.

Historical runs:
- Supply chain: https://github.com/ysfclc/PocketForge/actions/runs/43526175094
- Rust: https://github.com/ysfclc/PocketForge/actions/runs/43526175087
- Android: https://github.com/ysfclc/PocketForge/actions/runs/43526174937
