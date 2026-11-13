# Supply chain

Pin all GitHub Actions by full commit SHA. Commit Cargo.lock; run cargo-deny and cargo-audit. Pin Gradle dependencies and add dependency verification metadata before production release. Generate CycloneDX SBOM and GitHub build provenance for APKs. Use dependency review and secret scanning where supported. No untrusted build runs with release secrets or broad write permissions. Caches never contain credentials.
