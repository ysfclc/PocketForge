# Git-native phase delivery

## When to invoke
At the start of every PocketForge phase and whenever a substantial feature or foundation change is prepared.

## Preconditions
- Current `main` SHA is known and the worktree/branch is based on it.
- Phase objective, scope boundaries and acceptance criteria are written.
- Relevant product, architecture, ADR and security documents have been read.

## Procedure
1. Create a focused branch from current main.
2. Record phase objective, constraints, deliverables and acceptance evidence in the PR description.
3. Implement in small commits; keep code, tests and docs aligned.
4. Update ADRs for consequential changes and preserve the original product intent.
5. Run foundation verification and all affected test/build commands.
6. Open a PR and review the complete diff, including workflows and executable build scripts.
7. Resolve all CI failures; do not bypass required checks.
8. Merge only after human review and acceptance evidence are complete.
9. Update the repo log and acceptance ledger with exact commit and workflow references.

## Verification
PR targets current main, scope is coherent, checks pass on the proposed head SHA, no secrets or build outputs are committed, and the ledger records evidence.

## Failure modes
- Stale base: rebase/update from main and rerun checks.
- Unclear acceptance: stop and refine the phase contract.
- CI unavailable: report infrastructure status accurately; do not mark gates passed.
- Security-sensitive changes: request security-agent review before merge.
