# Threat model

Assets: source, project context, connector credentials, signing keys, CI tokens, build outputs and provenance.

Hostile ZIP: traversal, absolute paths, links, zip bombs and duplicates. Enforce compressed/expanded/count caps, reject links/special files, normalize, allowlist, isolate extraction and test adversarial fixtures.
Malicious build scripts: treat projects as untrusted; ephemeral CI; no release secrets in build jobs; minimal permissions.
Dependency/action substitution: SHA-pinned actions, Cargo.lock, cargo-deny/audit, Gradle verification and SBOM.
Signing leak: protected Environment, single job, temp file, cleanup, no agent access.
Compromised runner: least privilege, short-lived tokens, no persistent self-hosted runner for untrusted code.
Artifact substitution: SHA-256 and provenance.
Malicious agent/connector: capability grants, resource scopes, approvals, event audit and revocation.

Residual: GitHub-hosted runners are not a formal sandbox for hostile build scripts. Never expose release secrets to ZIP-controlled code.
