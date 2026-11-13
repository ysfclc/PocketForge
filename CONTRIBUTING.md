# Contributing

1. Read PRINCIPLES.md, AGENTS.md and relevant ADRs.
2. Use a focused branch and identify the mission/issue.
3. Never commit credentials, signing material, SDKs or generated build outputs.
4. Update ADRs for consequential contract/dependency changes.
5. Run scripts/verify-foundation.sh, Rust checks and Android debug assembly where available.
6. Include exact commands, results and limitations.
7. Complete the PR security checklist.

Build artifacts belong in CI artifacts, not source control. Never self-approve a protected release environment.
