# Direct commit intake and review

## When to invoke
Whenever a new phase, patch, agent contribution or external code change is proposed for PocketForge.

## Preconditions
- The canonical repository and base commit are known.
- The change has an explicit objective and acceptance criteria.
- The contributor is working on a branch; protected main is not edited directly.

## Procedure
1. Identify the exact base and head commit SHAs.
2. Review the full changed-file list, including hidden files, generated files and workflow changes.
3. Reject unexpected binaries, credentials, signing material, build outputs and unrelated generated content.
4. Inspect scripts, Gradle plugins, Cargo build scripts and workflow changes as executable/untrusted input.
5. Check path ownership and CODEOWNERS requirements.
6. Run foundation verification, tests, Rust cross-compilation and Android assembly as applicable.
7. Record decisions and evidence in the PR and acceptance ledger.
8. Merge only after required human review and passing checks.

## Verification
The PR diff is limited to the declared scope; no secrets or generated artifacts are committed; applicable CI checks pass; acceptance evidence points to the exact commit.

## Failure modes
- Unknown base/head: stop and refresh branch state.
- Secret detected: stop, revoke/rotate it, and perform incident review.
- Unexpected workflow or build-script changes: request security review.
- Missing acceptance evidence: keep the PR unmerged.
