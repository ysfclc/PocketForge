# Secrets policy

CI receives only the minimum GitHub token permissions declared by each workflow job. Release signing values live only in the protected GitHub Environment `android-release`. Local connector credentials will use Android Keystore-backed encrypted storage when that capability is implemented.

Names: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.

Pass secrets only to the step that needs them. Never echo, commit, cache, artifact or send secrets to agents. Decode the release keystore to a restrictive temporary file, use it and delete it in the same job. Rotate immediately after suspected exposure and review rotation at least annually. Revoke obsolete tokens. There is no `REPO_CREATE_TOKEN` because PocketForge does not provision secondary repositories.
