# Secrets policy

CI tokens live only in GitHub Actions secrets. Release signing values live only in protected Environment android-release. Local connector credentials use Android Keystore-backed encrypted storage.

Names: REPO_CREATE_TOKEN; ANDROID_KEYSTORE_BASE64; ANDROID_KEYSTORE_PASSWORD; ANDROID_KEY_ALIAS; ANDROID_KEY_PASSWORD.

Pass secrets only to the step that needs them. Never echo, commit, cache, artifact or send to agents. Decode keystore to a restrictive temp file, use and delete in same job. Rotate immediately after suspected exposure and at least annually. Revoke obsolete tokens.
