# Skill: dependency-pinning

## When to invoke
Third-party action/library introduced.

## Preconditions
Source/license/maintenance checked.

## Exact procedure
Pin Actions by full SHA; commit Cargo.lock; screen cargo-deny/audit; pin Gradle and verify checksums before release.

## Verification
No floating Actions; lockfiles/verification reviewed.

## Failure modes
Block unknown provenance or unresolved critical vulnerability.
