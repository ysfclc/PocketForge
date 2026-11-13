# Git-native phase protocol

## Source of truth

The canonical project state is the Git history of `ysfclc/PocketForge`. A phase is a coherent set of commits and pull requests, not an uploaded archive. There is no ZIP drop zone, archive parser, secondary target repository or automatic repository provisioning.

## Change lifecycle

1. Start from the latest `main` and create a focused feature branch.
2. Read `FRESH_START.md`, the relevant product/architecture docs, ADRs and agent routing before editing.
3. State the phase objective, constraints, acceptance criteria and affected modules in the PR description.
4. Keep changes reviewable. Update ADRs when changing consequential decisions.
5. Run `scripts/verify-foundation.sh`, Rust tests/cross-builds and Android build checks as applicable.
6. Open a PR and wait for all required checks. Review security-sensitive changes and generated artifacts.
7. Merge to `main` only after review and passing checks.
8. The permanent pipeline builds the exact commit and uploads the debug APK and SBOM to the Actions run.

## Provenance and idempotency

Git commit SHA is the immutable source identity. The workflow checks out `github.sha`, records that revision in the Actions run, and produces an APK from that tree. Re-running a workflow for the same commit may create a new run/artifact, but it does not create another source commit or mutate repository contents.

## External inputs

Treat code, dependencies, model outputs, agent patches and generated scripts as untrusted until reviewed. Do not use archive ingestion as a privileged path. Secret and signing rules are defined in `docs/security/`.
