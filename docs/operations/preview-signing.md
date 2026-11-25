# Persistent PocketForge Preview signing

The installed initial app uses package ID `com.pocketforge.app.debug`. The new development build uses `com.pocketforge.app.preview` and appears as **PocketForge Preview**. It installs alongside the existing app; it does not replace or delete the old app's local records. Because Android app data is isolated by package ID, link the project folder again inside Preview.

## One-time repository setup

The Preview build must use a private, persistent signing key so future CI APKs can update the same Preview installation. Never commit the keystore or passwords to this public repository.

Add these four repository Actions secrets under **Settings → Secrets and variables → Actions**:

- `POCKETFORGE_DEBUG_KEYSTORE_BASE64`: the complete base64 content from the private signing bundle.
- `POCKETFORGE_DEBUG_KEYSTORE_PASSWORD`: the keystore password.
- `POCKETFORGE_DEBUG_KEY_ALIAS`: the key alias.
- `POCKETFORGE_DEBUG_KEY_PASSWORD`: the private key password.

The private bundle supplied separately contains the generated keystore material and these values. Copy each value into the matching secret field. Keep one secure offline backup of the bundle; never commit it, add it to an issue, or paste it into chat or a pull request.

After adding the secrets, run **Actions → PocketForge CI pipeline → Run workflow** on the feature branch. The pipeline materializes the keystore in the runner's temporary directory with restrictive permissions, passes credentials only to the Gradle signing step, and removes the keystore after APK assembly. The APK verification step prints the signing certificate SHA-256 fingerprint so it can be checked across builds. If the four secrets are absent, CI deliberately falls back to the runner's temporary debug key for compilation and emulator tests only; it skips the persistent-key gate, APK provenance attestation and APK/SBOM upload. This fallback is not an installable update artifact. Trusted same-repository PR/push builds also enforce the approved certificate fingerprint `9d89c5b33b9db756b60084228bb9589812c71fd5f7562e30a7d4ab3906d06d5f`; a mismatch fails before artifact attestation or upload. CI determines fork status from the GitHub event's explicit head repository and base repository names (not assumed runner environment variables). Same-repository PRs must receive the configured repository secrets; genuine fork PRs may compile with a temporary debug key, but their APK and provenance are not uploaded.

## Fork builds

GitHub does not expose repository secrets to workflows triggered from forks. Fork PR builds may therefore use the runner's temporary default debug key and are marked as unsuitable for in-place updates. Do not install such an artifact over a Preview build signed with the persistent key.

## Signing boundaries

- This key signs only the `.preview` development variant.
- The production release variant continues to use its separate `android-release` environment and release keystore secrets.
- Do not reuse the Preview keystore for production releases.
- Replacing the Preview keystore later changes the signing identity and prevents in-place updates of the existing Preview package.
