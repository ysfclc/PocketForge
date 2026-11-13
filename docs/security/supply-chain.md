# Supply-chain policy

- Pin every GitHub Action to a full commit SHA; update pins through reviewed PRs.
- Commit Cargo.lock and use locked Cargo operations.
- Run pinned cargo-deny and cargo-audit versions in security CI.
- Review Gradle plugin and repository changes. **Strict Gradle dependency verification metadata is a Phase 1 gate**: generate and commit `android/gradle/verification-metadata.xml`, then enforce strict verification in CI before introducing external product dependencies. It is not claimed as enabled in the Keystone scaffold.
- Generate a CycloneDX SBOM for every distributable APK.
- Attest APK build provenance against the exact Git commit and workflow identity.
- Treat agent-produced code, Gradle plugins, Cargo build scripts and shell scripts as untrusted until reviewed.
- Use ephemeral GitHub-hosted runners for builds; never expose release secrets to pull-request jobs.
- Keep release signing isolated in the protected android-release Environment.
- Do not ingest source through archives or provision secondary repositories. Git commits and reviewed pull requests are the only source path.
