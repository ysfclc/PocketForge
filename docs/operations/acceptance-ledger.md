# Acceptance ledger

| Gate | Status | Evidence required |
|---|---|---|
| Product, architecture, security docs | Authored | repository review |
| Seven agent roles and ten skills | Authored | `.agent/` inventory |
| Git-native direct commit pipeline | Authored; YAML parsed locally | `.github/workflows/pipeline.yml` |
| Action SHA pins and per-job permissions | Static verification passed in local mirror | `scripts/verify-foundation.py` |
| Native workspace / Project Passport / mission queue | Latest PR-head source builds in CI run #150; first UI launch observed by user; current folder inspection and preflight compiled but not yet device-accepted | Run #150, commit `018b029`; verify folder permission survives restart, nested Android build-root detection and browse/open behavior on target phone |
| Imported-project build preflight | Source and Android CI compile verified in run #150; target-device SAF acceptance pending | `BuildPreflight.kt` performs a bounded, read-only SAF scan for nested Gradle project roots, wrapper/settings/modules, Android manifests/plugin markers and Cargo manifests; verify a real Android project report on device. This is not a build runner and executes no project code |
| On-device build execution architecture | Specified; feasibility spike and implementation not started | `docs/architecture/local-build-execution.md`; prove JDK/Gradle/SDK execution, lifecycle, resource safety and signed APK acceptance on Android before selecting the full runner implementation |
| GitHub repository connection and browser | Latest PR-head source builds in CI run #150; URL/owner-repo input, immediate project handoff and strict immutable commit pinning are compiled; not yet device-accepted | Run #150, commit `018b029`; verify public URL, private token, pinned SHA, directory navigation and text preview on device |
| GitHub single-file branch/commit/PR flow | Source compiled in PR-head CI run #150; still not device-tested | Device evidence required for branch SHA/base verification, stale-file rejection, commit and PR URLs, and no mutation without approval |
| Advanced GitHub operations (multi-file patches, history/checks, artifact retrieval, mission integration) | Not implemented | scoped token grants, branch creation, base-revision checks, reviewed diff, explicit commit/PR approval and resulting GitHub evidence |
| Rust three Android ABIs | Passed in current CI run #150 | `cargo-ndk` built arm64-v8a, armeabi-v7a and x86_64 in run #150 |
| Android Preview APK | Passed in current PR-head CI run #150 | Artifact 14748302818; package `com.pocketforge.app.preview`, version `0.1.0-keystone-preview`, min API 26, target API 35; APK signature verified. Temporary debug certificate; not update-safe across Preview builds |
| actionlint and foundation verification | Passed on current PR head in security run #163 | `actionlint` and `scripts/verify-foundation.sh` succeeded in run #163 |
| Rust dependency policy | Passed in security run #163 | `cargo deny check` reports advisories, bans, licenses and sources all OK; `cargo audit` passed against the checked-in lockfile |
| GitHub Dependency Review integration | Blocked by repository configuration, not Rust dependency scan | Run #163 reports Dependency Review unsupported because Dependency Graph is disabled; enable it in repository Settings → Security → Code security and analysis → Dependency graph |
| SBOM and provenance attestation | Passed in current CI run #150 | SBOM artifact 14747723954; APK SHA-256 `a306cca7de8c4b7cd4076517fd4fe7c103908e1b017baff7bf08185383badd9e`; SBOM SHA-256 `6230f46ebd46303cd439956a284508e32cea97ddaf0fc0da5a4777d1de78b06d`; provenance attestation emitted and uploaded by run #150 |
| iOS contract-only scaffold | Authored; intentionally gated | no Xcode build job |
| Protected release signing | User reports signing configured; workflow not yet run/verified | Dispatch `.github/workflows/android.yml` on protected `main`; verify APK signature and provenance with the configured `android-release` environment |
| Physical device acceptance | Initial APK launch observed by user; revised onboarding not yet tested | On device: import a folder, inspect/browse/open; connect public GitHub URL, inspect pinned SHA and browse; separately test private token and reviewed write approval |
| Persistent Preview signing and safe install migration | **Not verified.** Run #374 used the runner's temporary debug key because the workflow compared an unset `GITHUB_HEAD_REPOSITORY` variable and misclassified this same-repository PR as a fork. The observed fingerprint `27a42a6843f5ed401cd5e3cb5014b342e2025d719c2b1e36e0b4f1c497d14dac` is ephemeral and must not be treated as persistent. CI detection fixed in commit `67a5244`; trusted signing rerun pending | Require a trusted push/PR run to report the persistent keystore fingerprint, compare it with the known private bundle fingerprint and installed Preview signer, and verify a subsequent build repeats it |
| Gradle dependency verification metadata | Deferred Phase 1 gate | committed verification-metadata.xml and strict CI mode |

Never mark pending/not-tested as passed without command, run URL, revision and result.

## GitHub runner allocation observation — 2026-11-14

The Git-native workflows are registered and triggered on main commits. However, all build jobs tested so far terminate before runner allocation: `runner_id: 0`, empty runner name, zero steps and no logs. Downloading the job logs returns GitHub's `BlobNotFound`, confirming that no runner log stream was created. No source/build step ran, so this does not establish a Kotlin, Rust, Gradle or actionlint failure.

Both runner labels have now been tested:
- `ubuntu-latest`: earlier runs fail before runner assignment.
- `ubuntu-24.04`: the explicit-label diagnostic also fails before runner assignment.

Explicit-label diagnostic runs for source commit `66c1254`:
- Direct CI pipeline: https://github.com/ysfclc/PocketForge/actions/runs/43651673973
- Supply-chain/actionlint checks: https://github.com/ysfclc/PocketForge/actions/runs/43651673846

The subsequent documentation commit `093a254` triggered the same two workflows and reproduced the failure:
- Direct CI pipeline: https://github.com/ysfclc/PocketForge/actions/runs/43651709151
- Supply-chain/actionlint checks: https://github.com/ysfclc/PocketForge/actions/runs/43651709132

This earlier runner-allocation diagnosis was superseded on 2026-11-20: after the repository became public, run #87 allocated a hosted runner and completed the full Rust, Android APK, signature, SBOM and provenance pipeline successfully.


## GitHub connector CI attempt — 2026-11-20

The new connector source and static contract checks were committed to the feature branch. The latest pipeline run (44712322777) and security run (44712322822) still fail before runner allocation: runner_id 0, empty runner name, zero executed steps and no artifacts. These runs therefore do not compile or validate the new Kotlin implementation. Build and Redmi device acceptance remain pending.


## Verified PocketForge build — 2026-11-20

- Successful CI run: https://github.com/ysfclc/PocketForge/actions/runs/44784314374
- Source commit: `3d92e1d2463dbd60101d9be2232a582c82e94dd0`.
- Rust cross-compilation for arm64-v8a, armeabi-v7a and x86_64 passed; Rust tests and Clippy passed.
- Android debug APK assembly and `apksigner verify` passed. Package metadata: `com.pocketforge.app.debug`, version `0.1.0-keystone`, min API 26, target API 35.
- APK artifact: `pocketforge-debug-81463e9decd62968bbe432f47dca1e4cde2a7464` (artifact ID 14653222608).
- APK SHA-256: `b2cead4187ac591c76e8d971a44576da4e9934ad24efde331bb37a7249fc79ac`.
- SBOM artifact ID 14654355782. Build provenance attestation: https://github.com/ysfclc/PocketForge/attestations/70253439.
- This verifies PocketForge's own Android debug package. It does not verify local compilation of imported projects or mission/agent execution.
- The user launched the initial APK and reported two usability defects: GitHub connection did not complete from the first UI attempt, and folder import produced no useful visible outcome. The next feature revision adds URL normalization, a connected-project handoff, persisted folder-permission validation, SAF folder inspection and browsing, and an accurate Factory status. That follow-up revision still requires CI and physical-device acceptance.


## Verified first-use revision — 2026-11-20

- Current PR head: `c0b829cff3500c54903c047cc8375bed323e3dcb`.
- Successful Android pipeline #109: https://github.com/ysfclc/PocketForge/actions/runs/44793160312.
- Rust cross-compilation for arm64-v8a, armeabi-v7a and x86_64 passed; Rust tests and Clippy passed; Android Gradle assembly and APK signature verification passed.
- APK artifact ID 14657897652. Package `com.pocketforge.app.debug`, version `0.1.0-keystone`, min API 26, target API 35.
- APK SHA-256: `fe90729305a10ee1e9c8bfdc3e4507bdccb8bc100ba6fc149cd18ee59da7846b`. This matches the SHA-256 in the GitHub build provenance attestation.
- SBOM artifact ID 14657571912.
- Security run #122: actionlint, the expanded foundation verifier, `cargo deny check` (advisories, bans, licenses and sources all OK) and `cargo audit` passed. The overall workflow still reports failure because the separate Dependency Review action says Dependency Graph is disabled for this repository. Enable it under GitHub repository Settings → Security → Code security and analysis → Dependency graph to clear that integration-level block.
- This verifies PocketForge's own debug package and static/source security checks. It does not prove real-device behavior of folder/GitHub flows, local compilation of imported projects, or agent execution.
- Physical device acceptance remains pending. The next acceptance pass must exercise local folder selection/permission persistence/inspection/browsing and public/private GitHub connect/pinned browsing on the target phone.


## Safe Preview package migration — 2026-11-20

- Comparing the first delivered APK artifact (#87) with the later CI APK (#109) shows different v2 signer certificate SHA-256 fingerprints: `00af6871dc79aed713536c7dad0ff6f7337bec3b2ade2c9c9f080f4364ff2e43` versus `d3a0f59cf47490a88a3edb4908abf3b57bec9227fe834f4464b326e9e0a52ba8`. The second cannot update the first installation in place.
- To avoid telling the user to uninstall an app containing local project metadata, the development build is being moved to a separate `com.pocketforge.app.preview` package labelled **PocketForge Preview**. The old `com.pocketforge.app.debug` app stays installed. Preview has a fresh app-private store; re-link the original folder inside Preview (the source files themselves remain untouched).
- A private PKCS12 Preview keystore has been generated outside the repository. It is not committed or uploaded as a public artifact. The trusted CI Preview build is gated on four repository Actions secrets, documented in [preview-signing.md](preview-signing.md).
- Next gate: user configures those four secrets, then dispatches the PocketForge CI pipeline. The resulting stable Preview signer will be reused for future Preview updates. Release signing remains separate.


- Preview certificate fingerprint validated from the private PKCS12 bundle with `keytool`: `9D:89:C5:B3:3B:9D:B7:56:B6:00:84:22:8B:B9:58:98:12:C7:1F:D5:F7:56:2E:30:A7:D4:AB:39:06:D0:6D:5F`. This is the expected signer for every trusted Preview build after the four repository secrets are configured.


## Imported-project build preflight — 2026-11-22

- Added a read-only SAF scanner that requires the persisted folder read grant and caps traversal at five levels, 500 visited entries, 200 entries per directory and 128 KiB per inspected text file.
- Detects Gradle settings/root/module build files, wrapper script/JAR/properties and configured Gradle distribution, Android manifests and Android Gradle plugin declarations, plus Cargo manifests.
- The linked-folder overview now exposes **Assess build** and reports findings, structural blockers and scan truncation. The report explicitly says no source was executed, downloaded, changed or built.
- This is project preflight only. An on-device Gradle/JDK/Android SDK executor, process isolation, cancellation, build logs, artifact verification and install flow remain unimplemented.
- Android CI run #150 passed the foundation verifier, Rust cross-build/tests/lints, Android Preview APK assembly, signature/package checks, SBOM generation and provenance upload. The source and CI compile gates are passed; target-device SAF acceptance remains open. The actual imported-project build runner is still not implemented.


## Snapshot integrity verification — 2026-11-22

- Added `ProjectSnapshotVerifier` and wired it into the local snapshot preparation path before the project-linked index is written.
- Verification checks the saved manifest SHA-256, manifest schema and snapshot ID, expected file count/total bytes, safe relative paths, canonical containment under the exact app-owned `filesDir/build-workspaces` root, per-file size and SHA-256, aggregate byte totals, and rejects missing or unlisted files.
- A failed verification deletes the just-created snapshot and prevents indexing. Successful UI confirmation reports the verified file count and size.
- Expanded `scripts/verify-foundation.py` to enforce the verifier and UI wiring contract.
- Source authored and repository contract assertions added. Android CI compilation and target-device mutation/tamper tests are pending; this entry is not a claim of a successful build or device acceptance.


## Governance branch CI trigger — 2026-11-22

- Added the active governance branch to push triggers for the Android pipeline and static security workflow.
- Added the same branch push trigger to the minimal Ubuntu 22.04 runner-allocation diagnostic.
- This ensures commits on the working branch receive compile/security/runner checks even if they are not currently the head of an open pull request. Trigger configuration is authored; run results are pending.


## Snapshot source provenance and build-run binding — 2026-11-22

- Snapshot manifests now capture SHA-256 fingerprints of the selected SAF tree URI and root document ID, provider display name, and root last-modified timestamp when available. The raw URI/document ID is not persisted.
- Snapshot verification requires valid source fingerprints and a non-empty provider display name; the snapshot index persists the provenance fields.
- BuildRun records now optionally bind to a project-matched StoredProjectSnapshot and persist its snapshot ID, manifest SHA-256, source URI fingerprint and last-modified timestamp. Older records remain readable when these fields are absent.
- Extended the foundation contract verifier and updated architecture/roadmap documentation.
- Source and contract changes are committed. Kotlin compilation, CI execution, runtime manifest compatibility, and device validation remain pending. The build-run binding is a persistence capability; no UI or executor invokes it yet.

- Follow-up hardening: moved source metadata lookup inside the snapshot staging cleanup boundary, so a provider metadata-query failure also removes the partial directory. BuildRun provenance now retains and requires both source URI and root-document fingerprints.


## CI feedback and workflow repair — 2026-11-22

- Hosted-runner diagnostic completed successfully on GitHub-hosted Ubuntu 22.04 (run #46, run ID 45061210703). This confirms runner allocation is working; the previous no-run observations were not evidence of a persistent runner outage.
- The first full PR validation reached the hosted runner and actionlint passed, then the foundation contract check failed because it still expected the pipeline's old main-only push trigger and exactly three workflow files. Updated those assertions for the governance branch and runner diagnostic workflow.
- Security validation also exposed that GitHub Dependency Review is unsupported for this private repository without the required Advanced Security/dependency graph entitlement. Guarded that optional PR job to public repositories; the static security job remains active.
- Re-run of the updated contracts and Android build is pending. The failed attempt stopped before Kotlin/Android compilation, so no compile success is claimed.


## Snapshot revalidation at build-run binding — 2026-11-22

- Extended the verifier to compare both source fingerprints in the manifest against the saved snapshot record, not merely validate their format.
- The snapshot-preparation UI passes the expected fingerprints into verification.
- `BuildRunStore.create(..., snapshot)` now verifies canonical app-private containment and reruns complete manifest/file/source-fingerprint verification immediately before persisting a snapshot-bound run. It rejects tampered, missing, relocated, or cross-project snapshot input.
- Foundation assertions and architecture/roadmap documentation updated. This is a persistence gate only; no executor or UI currently creates these runs.
- Android CI and runtime tests are pending. No APK/device acceptance is claimed.

- CI concurrency now cancels superseded validation for the same ref, preventing every rapid commit from leaving a long queue of obsolete Android/security builds. Only the latest revision is intended to consume the full validation slot.


## Latest hosted validation result — 2026-11-22

- On branch commit `da900373d1ea23d9b5d6ab71d7d1731badab9c6d`, hosted runner diagnostic passed and the foundation contract verifier passed.
- Rust core compilation for all configured Android ABIs passed, and Rust tests/lint checks passed.
- The Android APK pipeline stopped at **Prepare persistent Preview signing key** because the four `POCKETFORGE_DEBUG_*` Actions secrets are not configured. APK assembly and signature verification were skipped. No APK was produced by this run and no Kotlin/Gradle Android compilation is claimed.
- The separate static security job is still queued; cargo-deny/cargo-audit results are not yet available.
- Required release gate: configure the four persistent signing secrets in GitHub Actions, then rerun the pipeline. Do not substitute an ephemeral signing key for a trusted updateable Preview APK.

- CI now runs `gradle :app:compileDebugKotlin` before the persistent-signing-secret gate. This separates Kotlin compilation feedback from trusted APK signing, so missing release credentials no longer hide source compile errors. Validation of this new step is pending.


## Latest CI validation — 2026-11-22 (commit f105799)

- Source commit: `f1057999346bc8f72a3dd08bf5dae13dfdca3e64`.
- Successful PR pipeline run: https://github.com/ysfclc/PocketForge/actions/runs/45062059882. Foundation verifier passed; Rust cross-compilation for arm64-v8a, armeabi-v7a and x86_64 passed; Rust tests and Clippy passed; `:app:compileDebugKotlin` passed; Android Preview APK assembly and APK v2 signature verification passed.
- Kotlin emitted deprecation warnings for legacy Android UI APIs and one unchecked cast; no compilation errors.
- APK package `com.pocketforge.app.preview`, version `0.1.0-keystone-preview`, versionCode 1, target API 35. APK SHA-256: `429e86ee7b57a04174cc075679d0db95f8e0eb7a1e2395b889b7817a1a237b0c`.
- APK artifact ID 14770361593 (artifact ZIP digest `65d81abcd452800c5a8ce325ea35fd46e98dbddb68c416179d3b090cc3344859`). The artifact contains the APK and CycloneDX SBOM. Separate SBOM artifact ID 14769827121.
- Important signing limitation: this was a fork-PR build and the four private Preview signing secrets were unavailable. CI explicitly used the runner's temporary Android debug key; signer certificate SHA-256 is `aba48d750ef6efb3d8f003b12844997d0053c060ee4e257b905cd1aebeae347b`. This APK is signature-valid but is NOT update-safe for later builds and must not be presented as the persistent Preview signer.
- Build provenance attestation was created and uploaded: https://github.com/ysfclc/PocketForge/attestations/70830650.
- Successful supply-chain security run: https://github.com/ysfclc/PocketForge/actions/runs/45062059829. actionlint, foundation verification, cargo-deny policy checks and cargo-audit all passed.
- The matching push pipeline run 45062056610 failed only at persistent signing-key preparation because the four `POCKETFORGE_DEBUG_*` secrets are missing; Rust and Kotlin compilation had already passed. The PR run used the explicit fork-PR temporary-key fallback.
- No physical-device installation/acceptance is claimed. Imported-project build execution, local toolchain provisioning and process supervision remain unimplemented. PR #2 remains open, draft and unmerged.


## CI dependency throttling incident — 2026-11-22

- Commit `c4717167de7a1b7679628b57bea49bb780904ccb` PR pipeline: foundation checks passed, Rust compilation for all Android ABIs passed, Rust tests/Clippy passed; Android Kotlin compile failed during Gradle dependency resolution because Maven Central returned HTTP 429 for `com.google.code.findbugs:jsr305:3.0.2`. This is an external repository throttling error, not a source compiler diagnostic.
- Added bounded retry (3 attempts with 20s then 40s delay) to both `:app:compileDebugKotlin` and `assembleDebug` to improve resilience to transient repository rate limits. Retries remain bounded and do not suppress persistent failures.
- Follow-up CI on retry commit is pending; no success claim for this retry change until runs complete.


## CI retry fix verified — 2026-11-23

- Source commit: `a7831db058ad13b14a1910f2a6e8bc9f9a00abbf`.
- PR #2 pipeline run 45123921939 passed: foundation contracts, Rust cross-compilation for arm64-v8a/armeabi-v7a/x86_64, Rust tests and Clippy, Kotlin compilation, Android Preview APK assembly, signature verification, SBOM generation and build provenance attestation.
- Supply-chain security run 45123915487 passed on the same source commit: actionlint, foundation verification, cargo-deny policy checks and cargo-audit.
- Hosted runner diagnostic run 45123921938 passed.
- The bounded Gradle retry/backoff successfully allowed Kotlin compilation and APK assembly to complete after the earlier Maven Central HTTP 429 incident. Persistent failures remain fatal after the bounded attempts.
- PR Preview artifact ID 14795062280; APK SHA-256 `49a381552660c040c3de3f40d0e85a8d5d376626d833c510487685b1dc074f3f`. Build provenance: https://github.com/ysfclc/PocketForge/attestations/70958638.
- Signing caveat confirmed in the build log: fork PRs cannot access private Preview signing secrets and used a temporary runner debug key; certificate SHA-256 `debce42c2271dbb0f0e0663247329892e3f569afd5e5a60f56efac352c885f9f`. APK signature is valid but it is not update-safe across Preview builds.
- Physical-device acceptance is not claimed. Imported-project execution and toolchain provisioning remain unimplemented. PR #2 remains open, draft and unmerged.


## 2026-11-23 — Android process probe compiled; Preview signing remains blocked

- Current source commit `002aa909c1d4cf21c0489c4f6f4c3466ee9896e8` passed the foundation verifier, Rust cross-compilation for arm64-v8a/armeabi-v7a/x86_64, Rust tests and Clippy, and `:app:compileDebugKotlin` in hosted pipeline run https://github.com/ysfclc/PocketForge/actions/runs/45125579701.
- The Kotlin process-execution feasibility spike compiles. It remains a fixed-command Preview-only probe (`/system/bin/id`, `/system/bin/sleep 30`), not a Java/Gradle executor. No physical-device execution has been verified.
- The same push run failed at `Prepare persistent Preview signing key`: all four `POCKETFORGE_DEBUG_*` values were empty in the trusted push workflow. APK assembly, signature verification, SBOM and artifact upload were skipped. Do not classify the Preview artifact as persistent/update-safe.
- This is distinct from Release signing, which uses the four `ANDROID_*` secrets in `.github/workflows/android.yml`, targets protected `main`, and is manually dispatched. No Release workflow was run from this feature branch.
- Physical Android acceptance and the next execution gate (a pinned Java runtime probe, then a harmless Gradle diagnostic against a verified snapshot) remain pending.


## 2026-11-23 — Child ART runtime probe compiled and packaged

- Added a fixed, side-effect-free Java entry point `RuntimeProcessProbe` and extended the Preview-only process service to launch it from the app APK using `/system/bin/app_process` with the APK as `CLASSPATH`, a 10-second timeout, and bounded output capture. The probe reports Java runtime properties and a success marker before the existing fixed `/system/bin/id` and `/system/bin/sleep 30` checks.
- Clarified in the Factory UI and architecture docs that Android ART is not a standalone OpenJDK/JDK and that this does not establish Gradle compatibility. No project code is executed.
- Foundation verifier passed on commit `546f7007a7f131934cd53311bf5102bc28541b2a` in security workflow run https://github.com/ysfclc/PocketForge/actions/runs/45181745089. Hosted runner diagnostic passed.
- PR Android pipeline run https://github.com/ysfclc/PocketForge/actions/runs/45181745077 passed Rust builds for all three Android ABIs, Rust tests/Clippy, Kotlin compilation, APK assembly, APK signature verification, SBOM generation and provenance attestation. The APK contains `RuntimeProcessProbe` and the `POCKETFORGE_ART_RUNTIME_OK` marker.
- APK artifact ID `14820713553`; APK SHA-256 `27f79929a32df1574cdf8694bb91066eef2facb86b0ec522a322897e1dbbe99e`; signer certificate SHA-256 `158c274d2f4502dfa1088c28e47374ad9e33b03c873627ef51c440ae5a3ca40e`. This is the PR's temporary Android debug key, not the persistent Preview PKCS12 key; it is not safe for in-place updates. Build provenance attestation: https://github.com/ysfclc/PocketForge/attestations/71104390. SBOM artifact ID `14821517465`.
- Trusted push pipeline run `45181735470` compiled Kotlin successfully but stopped because the four `POCKETFORGE_DEBUG_*` secrets are absent. Thus no trusted persistent-key Preview APK was produced. This is separate from the user's Release `ANDROID_*` signing setup.
- Physical-device execution remains untested. Next gate is the user-run Preview acceptance procedure for app_process/ART, process survival, notification, cancellation and interruption. Only after that device evidence should a pinned standalone JDK/Gradle runtime spike begin.


## 2026-11-25 — Emulator, supply-chain and signing evidence (run #374)

- Source commit `2359923261ac3239cb4b95e1689eab640f84ecc7` on the governance branch passed Android pipeline run [45639952786](https://github.com/ysfclc/PocketForge/actions/runs/45639952786): foundation checks; Rust builds for arm64-v8a, armeabi-v7a and x86_64; Rust tests/Clippy; Kotlin compilation; Preview APK assembly; APK signature/package verification; emulator ART/process tests (2/2 passed, 0 skipped, 0 failed); signing-material cleanup; CycloneDX SBOM; provenance attestation; and artifact upload.
- Security workflow run [45639953087](https://github.com/ysfclc/PocketForge/actions/runs/45639953087) passed actionlint/workflow validation, foundation checks, cargo-deny (advisories, bans, licenses and sources OK) and cargo-audit. Hosted runner diagnostic #193 also passed.
- Preview artifact ID `15035173017`, ZIP SHA-256 `f97b8bed4b93565cc2404fc165b6d21c7621df4772c828b8bf9025a429da892b`; embedded APK SHA-256 `ecd9dcce4d005167a37b1eda40d489261434da1f2da5975fd4492005ff8b190b`. Package `com.pocketforge.app.preview`, version `0.1.0-keystone-preview`, min API 26, target API 35. CycloneDX SBOM artifact ID `15033833370`, artifact digest `30851359ba4ad1111aca4dc2041e529ddea05d9c7bbcad798ab5dd394cd71e04`.
- APK signer certificate SHA-256 observed by `apksigner`: `27a42a6843f5ed401cd5e3cb5014b342e2025d719c2b1e36e0b4f1c497d14dac`. The workflow's persistent Preview signing preparation step passed and this was not a fork PR, but this fingerprint differs from the previously documented locally generated PKCS12 key fingerprint `9d89c5b33b9db756b60084228bb9589812c71fd5f7562e30a7d4ab3906d06d5f`. Do not claim continuity with that earlier key or install as an in-place update until a second trusted build repeats the observed fingerprint and the installed app's signer is compared.
- APK build provenance attestation `72216088` was uploaded: https://github.com/ysfclc/PocketForge/attestations/72216088. GitHub reports the artifact ZIP digest above; the downloaded archive was inspected and contains the APK and CycloneDX JSON SBOM.
- No application source changes were made in this CI-fix cycle. Emulator tests establish hosted x86_64 ART/process behavior only, not physical Redmi acceptance. Imported-project build execution and standalone JDK/Gradle provisioning remain unimplemented.


## 2026-11-25 — Correction: run #374 used an ephemeral signing key

- A direct inspection of the full workflow job log for run [#374](https://github.com/ysfclc/PocketForge/actions/runs/45639952786) shows the signing step emitted `Fork PR: private Preview signing secrets are unavailable; using the runner's temporary debug key.` The PR API confirms both head and base repositories are `ysfclc/PocketForge`; this was not a fork PR.
- Root cause: the workflow tested `GITHUB_HEAD_REPOSITORY`, which is not a default Actions environment variable. It was empty, so every PR event compared an empty value with `GITHUB_REPOSITORY` and incorrectly took the fork fallback. The resulting APK signer fingerprint `27a42a6843f5ed401cd5e3cb5014b342e2025d719c2b1e36e0b4f1c497d14dac` is a temporary runner debug key. Artifact `15035173017` and attestation `72216088` are historical CI outputs only; do not install this APK over an existing Preview build or claim persistent signing from them.
- Fixed in workflow commit `67a5244ff6e967f3b6a34889fbaaa03b838cda3`: compare explicit `PREVIEW_HEAD_REPOSITORY` and `PREVIEW_BASE_REPOSITORY` values populated from the GitHub event context. For genuine fork PRs, APK provenance attestation and APK/SBOM artifact upload are now skipped; the ephemeral APK cannot be distributed as a trusted Preview artifact.
- A trusted push build on the same branch is now required to verify that repository signing secrets are actually available, confirm the certificate fingerprint against the previously recorded private PKCS12 fingerprint `9d89c5b33b9db756b60084228bb9589812c71fd5f7562e30a7d4ab3906d06d5f`, and repeat it on a subsequent build. If the trusted run reports missing secrets or a different fingerprint, stop and investigate; do not publish an update-safe APK.
- The Android emulator process tests and security scans from #374 remain valid CI test evidence for the source code, but the APK from that run is not an acceptable persistent-signed distribution artifact. Physical Redmi acceptance remains pending.

- Added an explicit trusted-build certificate gate in pipeline commit `5247c31f586768fda40d84c5f02f62826335d73b`: after APK signature verification, trusted same-repository PR/push builds must match the approved certificate SHA-256 `9d89c5b33b9db756b60084228bb9589812c71fd5f7562e30a7d4ab3906d06d5f`. A mismatch fails the pipeline before attestation or artifact upload. Genuine fork PRs skip this trusted-key gate and cannot publish APK/provenance.


## 2026-11-25 — CI/test and trusted-artifact gates separated

- Trusted push pipeline run [#384](https://github.com/ysfclc/PocketForge/actions/runs/45687426894) confirmed the four `POCKETFORGE_DEBUG_*` repository secrets are currently empty/unavailable. Rust and Kotlin compilation had succeeded before the signing preparation step failed. This is a repository configuration gap, not an app compilation failure.
- Workflow commit `e287cd15f2f67c8219b89d2815c11df98115ad79` changes missing-secret behavior: the job records `trusted=false`, continues with Android's runner-local debug signing identity for build and emulator validation, and skips persistent-certificate enforcement, APK provenance attestation and APK/SBOM publication. When all four secrets exist, it records `trusted=true`, decodes the private PKCS12 key, and requires the exact approved certificate fingerprint before any artifact publication.
- This keeps CI test evidence flowing without representing an ephemeral-key APK as an update-safe deliverable. The old run #374 artifact remains untrusted. Persistent Preview signing remains blocked until repository secrets are configured from the private bundle and the strict fingerprint check passes.
- New pipeline validation is running on this commit; do not mark the workflow fix accepted until the CI and security jobs finish. Physical Redmi acceptance remains pending.


## 2026-11-25 — Snapshot verification tests and CI-only signing fallback verified

- Pipeline [#398](https://github.com/ysfclc/PocketForge/actions/runs/45699248293) passed on source/test commit `01e770083dbfdece8e1e3a9cfa18bd66f1cbafc9`: foundation checks; Rust builds for arm64-v8a, armeabi-v7a and x86_64; Rust tests/Clippy; Kotlin compilation; Preview APK assembly and signature/package verification; emulator instrumentation (10/10 tests, 0 skipped, 0 failed); signing-material cleanup; and CycloneDX SBOM generation.
- The 10 emulator tests comprise the existing two ART/process supervision tests plus eight new `ProjectSnapshotVerifierInstrumentedTest` cases: valid nested snapshot; tampered file; unlisted file; path traversal; duplicate manifest path; mismatched source identity; directory outside snapshot storage; and manifest hash mismatch.
- Security workflow [#409](https://github.com/ysfclc/PocketForge/actions/runs/45699248150) passed actionlint/workflow syntax, repository contract checks, cargo-deny and cargo-audit. Push pipeline [#397](https://github.com/ysfclc/PocketForge/actions/runs/45699232857) also passed; emulator step is PR-only.
- Persistent Preview signing secrets remain absent. The run explicitly recorded `trusted=false`; persistent signer enforcement, APK provenance attestation and APK artifact upload were skipped. No update-safe APK was produced or published. SBOM-only artifact ID `15063150758`, SHA-256 `60409ca33dcbbf88f549a34c9cc4c42926ada3a299051607ef64133f425aca07`; it contains no APK.
- Follow-up workflow commit `787c631edb63d2a5e5cf5e79ae2ec17f8d336b20` disables the SBOM action's automatic standalone artifact upload. On trusted builds, the SBOM is bundled only with the persistent-signed APK; on CI-only fallback runs it remains a local job output and is not published.
- Hosted emulator acceptance is now green for the ten instrumentation tests. Physical Redmi acceptance is still pending and is not inferred from emulator success. No JDK/Gradle toolchain or imported-project execution has been implemented.
