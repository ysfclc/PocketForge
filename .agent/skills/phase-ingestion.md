# Skill: phase-ingestion

## When to invoke
A phase ZIP is submitted.

## Preconditions
Archive path known; untrusted-input boundary active.

## Exact procedure
Hash archive; validate limits; extract safely; normalize root; strip VCS; generate provenance; publish content-addressed branch/PR; build exact commit.

## Verification
PR contains archive hash, run ID, timestamp, phase and normalized tree hash.

## Failure modes
Fail closed; remove staging and report safe diagnostic.
