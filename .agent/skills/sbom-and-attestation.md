# Skill: sbom-and-attestation

## When to invoke
APK artifact built.

## Preconditions
Artifact hash and workflow identity available.

## Exact procedure
Generate CycloneDX SBOM; calculate SHA-256; attest APK provenance; upload immutable artifacts.

## Verification
SBOM and attestation refer to same source revision and artifact.

## Failure modes
Attestation failure makes artifact non-release-eligible.
