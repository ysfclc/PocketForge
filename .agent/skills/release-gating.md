# Skill: release-gating

## When to invoke
Release or irreversible publication requested.

## Preconditions
Protected ref, reviewers, secrets and QA/security evidence configured.

## Exact procedure
Verify protected ref; require QA/security; obtain approval; sign once; verify signature; attest; publish immutable artifact.

## Verification
No secret before approval; signed hash matches attested hash.

## Failure modes
Missing any prerequisite blocks release.
