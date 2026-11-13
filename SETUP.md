# Keystone setup

This supplied repository is the control repository. Its permanent workflow accepts a ZIP, validates it, provisions/reuses a phase target repository, publishes a provenance-bearing branch and PR, then builds an APK where manifests exist.

1. Confirm PocketForge is private, Actions enabled and main protected with PR reviews, required checks, CODEOWNERS review and no force pushes.
2. Create a dedicated fine-grained GitHub token with minimum repository creation/content/PR permissions. Add it as REPO_CREATE_TOKEN in Actions secrets. Classic repo scope is a broad compatibility fallback only.
3. Later, create Environment android-release with required reviewers and add ANDROID_KEYSTORE_BASE64, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS and ANDROID_KEY_PASSWORD as Environment secrets only.
4. Name phase ZIPs project-phase-NN-description.zip and place under incoming/. Run workflow_dispatch with dry_run=true first, inspect validation, then run false. The workflow provisions/reuses a target, publishes a deterministic phase branch and PR, builds and uploads artifacts.

No unreviewed ZIP is release-signed and no ZIP content is pushed directly to a target default branch.
