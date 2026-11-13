# Skill: android-signing

## When to invoke
APK needs debug/release signature.

## Preconditions
Signing mode explicit; release Environment approval configured.

## Exact procedure
Debug uses disposable key. Release uses protected Environment; decode key to temp with restrictive permissions; sign; verify; delete temp key.

## Verification
Signatures verify; release secrets absent from logs/artifacts.

## Failure modes
Missing secret/verification failure blocks release.
