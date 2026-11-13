# Direct commit intake and review

## When to invoke
Whenever a new phase, patch, agent contribution or external code change is proposed for PocketForge.

## Preconditions
- Canonical repository and base commit are known.
- The change has an explicit objective and acceptance criteria.
- Work is on a branch; protected main is not edited directly.

## Procedure
1. Identify exact base and head commit SHAs.
2. Review all changed files, including hidden/generated files and workflow changes.
3. Reject unexpected binaries, credentials, signing material and build outputs.
4. Inspect scripts, Gradle plugins, Cargo build scripts and workflows as untrusted executable input.
5. Check path ownership and CODEOWNERS requirements.
6. Run foundation verification, tests, Rust cross-compilation and Android assembly as applicable.
7. Record decisions and evidence in the PR and acceptance ledger.
8. Merge only after human review and passing required checks.

## Verification
The diff matches declared scope; no secrets or generated build outputs are committed; applicable CI checks pass; evidence identifies the exact commit.

## Failure modes
- Unknown base/head: stop and refresh branch state.
- Secret detected: stop, revoke/rotate it and perform incident review.
- Unexpected workflow/build-script changes: request security review.
- Missing acceptance evidence: keep the PR unmerged.
