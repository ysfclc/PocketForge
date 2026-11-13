# Acceptance ledger

| Gate | Status | Evidence |
|---|---|---|
| Product/docs, 7 agents, 10 skills | Authored; static inventory passed locally | verify-foundation.sh |
| Rust three ABIs | Pending CI | cargo-ndk logs and .so checks |
| Android debug APK | Pending CI | APK artifact and apksigner |
| iOS gated scaffold | Authored | no Xcode job |
| Action SHA pins/per-job permissions | Static check passed locally | workflow scan |
| actionlint | Pending CI | security workflow |
| ZIP valid/traversal/symlink/secret fixtures | Passed locally | validator fixture runs |
| Idempotent publication | Authored; integration pending | repeat workflow run |
| Repository provisioning/PR | Requires REPO_CREATE_TOKEN | run URL and PR |
| Release signing/attestation | Requires protected Environment and secrets | signed APK and provenance |
| Physical Redmi device | Not tested | Phase 1 device report |

Never mark pending/not-tested as passed without command, run URL, revision and result.
