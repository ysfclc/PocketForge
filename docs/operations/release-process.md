# Release process

Merge reviewed changes to protected main. Confirm QA/security evidence. Dispatch Android release from protected main, approve android-release Environment, materialize signing key only inside job, build/sign, verify signature, delete temp key, generate SBOM/provenance and upload immutable APK. Verify hash/attestation before distribution. Never release an unreviewed ZIP branch or debug APK.
