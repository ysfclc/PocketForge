# Release process

1. Merge reviewed changes to protected `main`.
2. Confirm the exact commit has passing direct-CI, Rust, Android, QA and security evidence.
3. Configure the GitHub Environment `android-release` with required reviewers and the four Android signing secrets.
4. Manually dispatch `.github/workflows/android.yml` from `main`.
5. Approve the Environment gate. Only then does the job materialize the base64 keystore to a permission-restricted temporary file.
6. Build and sign the APK, verify its signature, and remove the temporary keystore in an always-run cleanup step.
7. Generate the SBOM, attest the APK provenance and upload the signed APK plus SBOM as workflow artifacts.
8. Verify the artifact hash and attestation before distribution.

Never release an unreviewed commit or distribute the debug APK as a production release. Signing material must never enter agent context, source control, logs or artifacts.
