# PocketForge repository setup

PocketForge is developed directly in this repository. Do not create a control repository, upload phase ZIPs, configure a repository-provisioning token or copy source into another repository.

## 1. Repository settings

1. Keep `ysfclc/PocketForge` as the canonical source repository.
2. Enable GitHub Actions.
3. Protect `main`: require pull requests, require the relevant CI checks, require CODEOWNERS review for protected paths, and disallow force pushes.
4. Keep the repository private unless an explicit product decision changes its visibility.

## 2. CI requirements

The direct-commit pipeline needs no repository-creation token and no application secrets. It uses the GitHub Actions runner to install Java, Android SDK/NDK and Rust toolchains, then builds the APK from the checked-out commit.

The workflow is `.github/workflows/pipeline.yml`. It runs on pull requests, pushes to `main`, and manual dispatch. The debug APK and CycloneDX SBOM are downloadable from the completed workflow run's Artifacts section.

## 3. Optional release signing

Only when a release is intentionally requested:

1. Create the GitHub Environment named `android-release`.
2. Configure required reviewers for that Environment.
3. Add `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD` as Environment secrets.
4. Dispatch `.github/workflows/android.yml` from `main` and approve the Environment gate.

Never put signing material in repository files, build logs, artifacts or ordinary repository secrets. Debug builds use the Android debug signing configuration.

## 4. Local development

Install Rust 1.82.0, cargo-ndk 3.5.4, Android NDK 27.2.12479018, JDK 17, Android platform 35 and the Gradle version declared by the wrapper. Then run the three quickstart commands in README.md.

## 5. Normal phase workflow

1. Create a feature branch from current `main`.
2. Update the relevant source, tests, documentation and decision records in the same change.
3. Run foundation verification and the applicable Rust/Android checks locally.
4. Open a pull request. Review the diff, security implications and CI evidence.
5. Merge only after required checks pass.
6. The pipeline builds the exact merged commit and retains the APK/SBOM as workflow artifacts.

No manual archive transfer, secondary repository, or inferred publishing step exists.
