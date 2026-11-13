# Threat model

## Assets
Source and build scripts, project context, connector credentials, signing keys, CI tokens, build outputs and provenance attestations.

## Threats and controls

- **Untrusted commits and agent patches:** review exact base/head SHAs and the full diff; require PR review; treat generated scripts, Gradle plugins, Cargo build scripts and workflow changes as executable input.
- **Malicious build scripts or dependencies:** use ephemeral GitHub-hosted runners, least-privilege job permissions, lockfiles, dependency screening and no release secrets in ordinary CI.
- **Dependency/action substitution:** pin GitHub Actions by full commit SHA; keep Cargo.lock; run cargo-deny and cargo-audit; enable Gradle dependency verification; generate an SBOM.
- **Signing-key disclosure:** protected `android-release` Environment, required reviewers, one gated job, temporary keystore file and cleanup; agents never receive signing credentials.
- **Compromised CI runner:** avoid persistent self-hosted runners for untrusted changes; minimize token scopes; attest build provenance; treat runner compromise as possible.
- **Artifact substitution:** bind provenance to the exact workflow commit and APK subject; retain the SBOM and Actions artifact together.
- **Malicious agent/connector:** capability grants, narrow resource scopes, explicit approvals for source mutation/push/signing, event audit and revocation.

## Delivery boundary

PocketForge is developed directly in its canonical GitHub repository. Git commits and reviewed pull requests are the only source-ingestion path. There is no ZIP extraction or automatic repository provisioning path.

## Residual risk

GitHub-hosted runners are not a formal sandbox for hostile build scripts. Never expose release secrets to unreviewed pull-request code. Do not merge a change merely because an agent claims it is safe.
