# Skill: secret-handling

## When to invoke
A credential is needed.

## Preconditions
Credential is stored in GitHub secret/Environment or OS secure store.

## Exact procedure
Use only named secret; pass to one step; never echo/commit/cache/artifact/agent prompt; rotate on exposure; delete temp material.

## Verification
Secret scan and workflow review show no persistence.

## Failure modes
Exposure means revoke/rotate first.
