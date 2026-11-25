# Repository log — 2026-11-14

## Keystone foundation

Created product, architecture, data-model and FFI contracts; 15 ADRs; seven agent roles; ten skills; security and operations documentation; Rust/Android scaffolds; iOS contract-only scaffold; and pinned GitHub Actions workflows.

## Delivery model correction

The user confirmed that PocketForge is developed directly in `ysfclc/PocketForge`. ZIP handoff, ZIP ingestion, secondary repository provisioning and the `REPO_CREATE_TOKEN` setup were not wanted. The repository was corrected accordingly:

- `.github/workflows/pipeline.yml` builds the exact GitHub commit: foundation checks → Rust Android ABI cross-build/tests → Android debug APK → APK verification → CycloneDX SBOM → build provenance → Actions artifact.
- `.github/workflows/android.yml` is manual release-only and protected by the `android-release` Environment.
- `.github/workflows/security.yml` validates action syntax and repository contracts, then runs pinned Cargo security scanners.
- ZIP validator/publisher scripts, `incoming/`, archive manifest and ZIP-specific skill were removed/replaced.
- README, SETUP, scope, secrets policy, threat model, phase protocol, release process and acceptance ledger now describe Git-native delivery.
- ADR-0015 records the decision and supersedes the old archive-based direction.

## First native product slice

Commit `c57dc23` replaces the placeholder text-only Android screen with a first interactive workspace:

- Local project list and Project Passport creation.
- Android Storage Access Framework folder linking with persistable URI permission.
- Mission creation, optional project association and local Draft / Ready state.
- Workspace, Missions and Factory navigation.
- Factory screen reports the native bridge status and explicitly shows unverified build gates as pending.
- `WorkspaceStore` persists project and mission records in app-private SharedPreferences. This is a starter local schema; it is not the final database or full Passport model.

Commit `7e181f4` corrected Kotlin string interpolation and dialog newline escapes found during source review. The source is now checked for these accidental escape patterns by the repository contract verifier.

Commit `66c1254` pins all GitHub-hosted jobs to `ubuntu-24.04` rather than the floating `ubuntu-latest` label as a runner-allocation diagnostic. Both labels produce the same pre-runner failure. The documentation-only follow-up commit `093a254` also retriggered both workflows and reproduced it (pipeline run 43651709151; security run 43651709132).

This slice is authored, not yet build- or device-verified. No agent execution, project compilation, GitHub/Memo connector or real build artifact is claimed.

## Verification status

A local mirror of the original foundation layout passed `scripts/verify-foundation.py`; PyYAML parsed all three workflow files; action SHA pins and explicit per-job permissions passed the static verifier. Rust, Android SDK/Gradle, actionlint and physical-device checks remain unverified. The latest explicit `ubuntu-24.04` pipeline run (43651673973) and security run (43651673846) each failed before runner assignment with `runner_id: 0`, empty steps and no runner name. Job-log retrieval returns `BlobNotFound`; the pipeline produced zero artifacts. The same pre-runner failure also occurred on `ubuntu-latest`. Account-level Actions settings and billing/runner entitlements are not available through the connected repository API. No build gate is marked passed until a runner executes the full pipeline.

## 2026-11-20 — Mission execution constraints locked

- Added [mission execution invariants](../architecture/mission-execution-invariants.md) defining outcome-driven missions, provider/tool separation, one bounded durable mission loop, scoped callable capabilities, result verification and safe recovery.
- Updated `AGENTS.md`, `PRINCIPLES.md`, the architecture overview, product vision and Phase 2 roadmap to make the constraints mandatory.
- This is a documentation/architecture constraint update only. The current Rust core remains a foundation scaffold and no agent execution, capability gateway or build artifact is claimed as implemented.
- The user-approved product boundary remains PocketForge as a specialized Android development factory, independent from Memo.
- Draft review is open as [PR #2](https://github.com/ysfclc/PocketForge/pull/2); it is not merged.

## 2026-11-20 — Direct GitHub mode confirmed as first-class

- The user confirmed that PocketForge must support direct work against connected GitHub repositories as well as local Android project folders.
- Local and GitHub targets share the same mission engine; they are project-source adapters, not separate AI workflows.
- GitHub mode covers scoped repository inspection, branches, diffs, approved commits/PRs, checks and artifact retrieval. GitHub Actions is a separate optional remote execution capability.
- Local compilation, signing and installation remain independent of GitHub accounts and hosted-runner availability.
- Updated the vision, scope, roadmap, architecture, mission invariants and README. This is a documentation/contract update, not an implemented GitHub connector.


## 2026-11-20 — First direct GitHub project workflow authored

- Added an Android GitHub REST adapter to verify a repository, capture its default-branch commit SHA, browse directory contents and read text files at the pinned revision.
- Added repository-scoped GitHub token storage using AES-GCM with a non-exportable Android Keystore key. The encrypted preference key is derived from a SHA-256 digest of the repository identity; tokens are not stored in project records.
- Extended the local workspace project schema compatibly with source kind, repository identity, default branch and pinned revision. Existing records without these fields continue to load as local projects.
- Added a controlled single-file remote change path: create a working branch from the pinned commit, compare the current blob SHA to the reviewed base, show a line-based diff, require an explicit commit action, and require a separate explicit action to create a pull request. No merge action exists.
- GitHub API mutations require a repository-scoped token; editing requires Contents read/write and PR creation additionally requires Pull requests read/write. Public repository browsing remains possible without a token.
- Implementation is authored but not build/device verified. GitHub Actions still fails before runner allocation (runner_id 0, zero steps, no artifacts), so no APK or Kotlin compilation result is claimed.

- Extended `scripts/verify-foundation.py` to require the native GitHub connect/browser/edit/PR flow, pinned revision model, stale-blob guard, strict UTF-8 handling, repository-scoped Android Keystore credential vault and INTERNET permission.
- The latest CI pipeline run (44712322777) and security run (44712322822) still terminate before runner assignment (runner_id 0, zero steps, no artifacts). The new Kotlin source has not yet been compiled by CI.


## 2026-11-20 — First successful hosted build

- The repository was made public and GitHub Actions began assigning hosted runners. The first allocated run exposed an obsolete Android SDK setup request for the removed `tools` package; both debug and release workflows now request `platform-tools` explicitly.
- CI scripts were committed as mode `100644` and therefore could not be invoked directly. The debug, release and security workflows now call the scripts with `bash`.
- The first Kotlin build exposed two compile errors in `MainActivity.kt`: a shadowed `background` reference passed to `setBackgroundColor`, and an invalid `singleLine` property. Both were corrected.
- Successful run #87: https://github.com/ysfclc/PocketForge/actions/runs/44784314374, source commit `3d92e1d2463dbd60101d9be2232a582c82e94dd0`.
- Rust cross-compilation for arm64-v8a, armeabi-v7a and x86_64 passed; Rust tests and Clippy passed; Gradle assembled the Android debug APK; `apksigner` verified its v2 signature; SBOM and build provenance were emitted.
- APK artifact ID 14653222608; SBOM artifact ID 14654355782; provenance attestation 70253439. APK SHA-256: `b2cead4187ac591c76e8d971a44576da4e9934ad24efde331bb37a7249fc79ac`.
- This closes the Keystone hosted-build gate for PocketForge's own debug APK. It does not establish local compilation of imported projects, agent execution, release signing, or physical-device acceptance of the new GitHub feature.

## 2026-11-20 — First-use onboarding audit and corrections

- The user installed and launched the first APK, then reported that GitHub could not be connected and importing a project folder had no visible useful outcome.
- Inspection found that GitHub connection only accepted `owner/repository` even though users may paste a normal GitHub URL; on success it only showed a short toast and re-rendered the workspace.
- Local folder import only stored the SAF tree URI, showed a toast and re-rendered. It did not verify the persisted read permission, inspect the folder, identify project markers or provide a way to browse its contents.
- Added `LocalProjectInspector.kt` to validate the persisted SAF read grant, inspect the selected folder, identify common project types, list entries and create document URIs scoped to the selected tree. Files remain in place; no upload/copy is performed.
- Updated local onboarding to reject missing persisted permission, explain that files are not uploaded, inspect the folder after linking, show a project summary and allow scoped folder/file browsing. The overview explicitly states that the folder is not yet edited or built.
- Added GitHub URL normalization for HTTPS, github.com, www.github.com, SSH-style `git@github.com:owner/repository.git`, and plain `owner/repository`. Successful connection now opens the connected project details so the user can continue directly to repository browsing or mission creation.
- Updated the Factory screen to distinguish the verified PocketForge CI APK from local compilation of imported projects and agent execution, which remain unimplemented.
- Extended the foundation verifier and updated README, product scope, roadmap and acceptance ledger. The onboarding revision has been authored directly on the existing draft PR branch; its own CI build and physical-device acceptance are still pending.


## 2026-11-20 — Verified first-use build and security checks

- Current app source includes the first-use fixes: local SAF permission validation, folder inspection and scoped browsing; GitHub URL normalization and post-connect project handoff; strict immutable commit pinning; a reconnect path for legacy unpinned connections; and tolerant handling of Android document providers that omit optional file-size metadata.
- Successful current-source Android pipeline #109: https://github.com/ysfclc/PocketForge/actions/runs/44793160312 (source commit `c0b829cff3500c54903c047cc8375bed323e3dcb`). Rust builds for arm64-v8a, armeabi-v7a and x86_64, Rust tests, Clippy, foundation verification, Gradle assembly, APK v2 signature verification, SBOM, provenance and artifact upload all passed.
- APK artifact ID 14657897652; SBOM artifact ID 14657571912; provenance attestation 70276389. APK SHA-256 `fe90729305a10ee1e9c8bfdc3e4507bdccb8bc100ba6fc149cd18ee59da7846b`; downloaded APK hash matches the attestation subject.
- Security run #122: `actionlint`, the expanded foundation verifier, `cargo deny check` and `cargo audit` all passed. `cargo deny` reports advisories, bans, licenses and sources all OK.
- The separate Dependency Review action remains unavailable because GitHub reports Dependency Graph disabled for the repository. This setting must be enabled in repository Settings → Security → Code security and analysis → Dependency graph; the workflow has not been weakened or skipped.
- The app has not yet been physically tested on the target device after these changes. The user must verify local folder linking/inspection/browsing and public/private GitHub connection and pinned-revision browsing. Local compilation of imported projects and mission/agent execution remain unimplemented.
- Pull request #2 remains open/draft and unmerged pending user approval.


## 2026-11-20 — Signing identity mismatch and safe Preview migration

- Compared the APK v2 certificates embedded in the initial delivered APK (run #87, artifact 14653222608) and the later onboarding APK (run #109, artifact 14657897652). Their SHA-256 fingerprints differ: `00af6871dc79aed713536c7dad0ff6f7337bec3b2ade2c9c9f080f4364ff2e43` and `d3a0f59cf47490a88a3edb4908abf3b57bec9227fe834f4464b326e9e0a52ba8`. Android will not accept the latter as an in-place update of the first installation.
- The development variant now uses package `com.pocketforge.app.preview` and a separate launcher label, **PocketForge Preview**, so the earlier `com.pocketforge.app.debug` installation remains untouched.
- Added a dedicated Gradle Preview signing configuration with explicit `PKCS12` store type. Trusted same-repository CI builds require a private persistent keystore supplied through four repository Actions secrets. The workflow materializes it only in runner temp storage and removes it after APK assembly. Fork PRs cannot access repository secrets and are explicitly marked non-updatable.
- Generated the Preview PKCS12 keystore and a local secret bundle under `/mnt/data`; neither is committed or uploaded to the public repository. The keystore was validated with `keytool` as alias `pocketforge-preview`, entry type `PrivateKeyEntry`.
- Preview certificate SHA-256 fingerprint: `9D:89:C5:B3:3B:9D:B7:56:B6:00:84:22:8B:B9:58:98:12:C7:1F:D5:F7:56:2E:30:A7:D4:AB:39:06:D0:6D:5F`.
- User action is required to add the four values from the private bundle to repository Settings → Secrets and variables → Actions. After that, dispatch the pipeline to produce the first trusted Preview APK. Production release signing remains separate.
- Preview has a fresh app-private store; the original app is not removed. Re-select the source folder inside Preview to create its local record and persisted Android folder permission.


## 2026-11-22 — Snapshot integrity gate

- Added `ProjectSnapshotVerifier.kt` as a separate integrity boundary for staged project inputs.
- The verifier checks manifest hash/schema/identity, safe unique relative paths, exact app-private root containment, each file's byte length and SHA-256, aggregate counts/bytes, and exact agreement between the manifest file set and files physically present.
- Integrated verification on the snapshot worker thread before the snapshot is indexed. Invalid snapshots are deleted; the success dialog explicitly reports the verified file count and bytes.
- Extended the foundation verifier and updated the local-build architecture and roadmap.
- Verification remains source-level only until CI compiles the Kotlin changes and the target Android device exercises both successful verification and deliberate file/manifest tampering. No imported-project build executor is claimed.


## 2026-11-22 — Governance branch CI coverage

- Added push triggers for `governance/mission-execution-invariants-2026-11-20` to the Android build pipeline, static security workflow and minimal hosted-runner diagnostic.
- The change closes a workflow-observability gap: commits on this working branch now trigger validation without relying on pull-request association or manual dispatch.
- Awaiting GitHub Actions run results for the snapshot integrity verifier and current Android source.


## 2026-11-22 — Source provenance and build-run binding

- Extended `ProjectSnapshotter` to record privacy-preserving source identity fingerprints and provider metadata in the content-hashed manifest.
- Extended `ProjectSnapshotVerifier` to require source identity fields before a snapshot can be indexed.
- Persisted source provenance in `StoredProjectSnapshot` and added optional immutable snapshot provenance to `BuildRun` creation. Snapshot/project mismatch is rejected; legacy records deserialize with absent provenance.
- Added source-provenance assertions to the foundation verifier and documented limitations: provider last-modified metadata is optional and fingerprints are not a complete source revision proof.
- Awaiting Android CI compilation and device tests. No build executor or APK is claimed.

- Follow-up hardening: moved source metadata lookup inside the snapshot staging cleanup boundary, so a provider metadata-query failure also removes the partial directory. BuildRun provenance now retains and requires both source URI and root-document fingerprints.


## 2026-11-22 — CI feedback loop

- Confirmed hosted runner allocation: runner diagnostic #46 completed successfully on Ubuntu 22.04.
- Retrieved CI logs. `actionlint` passed; the foundation verifier was stale against the intentional governance branch push trigger and newly added runner workflow. Updated its workflow expectations and added a guard assertion for the private-repository Dependency Review limitation.
- GitHub Dependency Review itself failed because this private repository lacks the required Advanced Security/dependency graph support. Conditioned the optional review job on public repositories; cargo-deny/cargo-audit static checks remain unaffected.
- Initial Android pipeline run stopped at the same stale foundation assertion before Rust/Gradle compilation. Awaiting validation of the repaired verifier and the latest Kotlin source.


## 2026-11-22 — Revalidate snapshot at run binding

- Strengthened `ProjectSnapshotVerifier` to compare manifest source fingerprints to the values in the saved snapshot record.
- Updated the preparation path to supply the captured fingerprints.
- Strengthened `BuildRunStore.create` so an optional snapshot-bound run is persisted only after canonical path containment and a complete second verification of the manifest, files and source identity. Cross-project, relocated and tampered snapshots are rejected.
- Updated foundation contract checks, architecture and roadmap. Build-run creation remains unconnected to UI/execution pending the actual local runner design.

- CI concurrency now cancels superseded validation for the same ref, preventing every rapid commit from leaving a long queue of obsolete Android/security builds. Only the latest revision is intended to consume the full validation slot.


## 2026-11-22 — Hosted validation result and signing blocker

- Latest observed branch commit: `da900373d1ea23d9b5d6ab71d7d1731badab9c6d`.
- Ubuntu 22.04 runner diagnostic: passed. Foundation verifier: passed. Rust compilation for all configured Android ABIs: passed. Rust tests and lint: passed.
- Android APK pipeline stopped at persistent Preview signing-key preparation because repository Actions secrets `POCKETFORGE_DEBUG_KEYSTORE_BASE64`, `POCKETFORGE_DEBUG_KEYSTORE_PASSWORD`, `POCKETFORGE_DEBUG_KEY_ALIAS`, and `POCKETFORGE_DEBUG_KEY_PASSWORD` are missing. Gradle Android assembly was not reached; no APK artifact was generated.
- Do not weaken signing or use a new ephemeral key for an updateable Preview artifact. Configure the persistent signing secrets before rerunning the trusted APK pipeline.
- Static cargo-deny/cargo-audit validation remains queued.

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


## 2026-11-23 — Gradle throttling resilience verified

- Current source commit: `a7831db058ad13b14a1910f2a6e8bc9f9a00abbf`.
- PR pipeline #45123921939 passed end-to-end: foundation checks, all three Rust Android ABIs, Rust tests/Clippy, Kotlin compilation, Android APK assembly/signature verification, SBOM and provenance.
- Supply-chain security #45123915487 passed on the same commit; hosted runner diagnostic #45123921938 passed.
- The bounded Gradle retry/backoff fixed the earlier transient Maven Central HTTP 429 failure without weakening failure handling.
- Preview APK artifact 14795062280, SHA-256 `49a381552660c040c3de3f40d0e85a8d5d376626d833c510487685b1dc074f3f`; attestation https://github.com/ysfclc/PocketForge/attestations/70958638.
- Fork PR signing fallback remains temporary and non-update-safe (certificate SHA-256 `debce42c2271dbb0f0e0663247329892e3f569afd5e5a60f56efac352c885f9f`). No device acceptance claimed.
- Next phase: build a debug-only Android process-execution feasibility probe. It must use fixed harmless commands only, a user-visible foreground service, persisted results and cancellation; it must not execute project scripts or expose a general shell. Device validation is required before selecting the production local runner.


## 2026-11-23 — Process probe source/CI checkpoint

- Reviewed the Preview-only process probe service, debug manifest, Factory UI and local execution architecture. The service uses only fixed system binaries, foreground notification, bounded persisted output, cancellation and interrupted-state handling. The release manifest does not declare the service.
- Hosted run `45125579701` on `002aa909c1d4cf21c0489c4f6f4c3466ee9896e8`: foundation checks, Rust ABI builds/tests/Clippy and Kotlin compilation passed. Persistent Preview signing preparation failed because the four `POCKETFORGE_DEBUG_*` secrets were unavailable; APK assembly and downstream artifact steps were skipped.
- This does not contradict the user's separate Release signing setup: the Release workflow uses `ANDROID_*` secrets on protected `main` and is manually dispatched. Neither workflow's secret values are exposed to the repository review.
- No device execution claim. Next technical gate remains a controlled Java runtime probe on-device, followed by a harmless Gradle diagnostic against a verified private snapshot. PR #2 stays open/draft/unmerged.


## 2026-11-23 — Child ART runtime probe source and CI evidence

- Added `RuntimeProcessProbe.java`, a fixed Java entry point with no inputs or side effects. Preview process service starts it through `/system/bin/app_process`, passes only the app APK as `CLASSPATH`, caps execution at 10 seconds and records its Java runtime properties before the fixed `id` and `sleep` child-process checks.
- Updated Factory UI and execution architecture to distinguish Android ART from a standalone JDK. The code does not run Gradle or imported project code.
- On commit `546f7007a7f131934cd53311bf5102bc28541b2a`, the foundation verifier passed. PR Android pipeline #45181745077 passed Rust ABI builds/tests/lints, Kotlin compilation, APK assembly, APK signature verification, SBOM and provenance. Artifact 14820713553, APK SHA-256 `27f79929a32df1574cdf8694bb91066eef2facb86b0ec522a322897e1dbbe99e`, temporary debug signer fingerprint `158c274d2f4502dfa1088c28e47374ad9e33b03c873627ef51c440ae5a3ca40e`, attestation 71104390.
- The PR artifact uses a temporary Android debug key and is not update-safe. Trusted push pipeline #45181735470 confirms persistent Preview secrets `POCKETFORGE_DEBUG_*` are missing; it compiled Kotlin but did not produce an APK. This does not affect the separate Release signing workflow's `ANDROID_*` credentials.
- Physical-device ART/process acceptance remains pending. Do not proceed to standalone JDK/Gradle provisioning until the Preview device probe has real-device evidence. PR #2 remains open/draft/unmerged.


## 2026-11-25 — Emulator and supply-chain gates passed; signing continuity needs confirmation

- On commit `2359923261ac3239cb4b95e1689eab640f84ecc7`, Android pipeline #374 passed all 19 steps, including Kotlin compilation, Preview APK signature/package verification, Android emulator ART/process instrumentation (2/2 passed), signing-material cleanup, CycloneDX SBOM generation, provenance attestation and artifact upload. The KVM check now relies on `emulator -accel-check` after setting the hosted runner device mode, eliminating the previous false-negative Bash permission test.
- Security run #385 passed actionlint/workflow validation, foundation checks, cargo-deny (advisories/bans/licenses/sources OK) and cargo-audit. Runner diagnostic #193 passed.
- Artifact ID `15035173017`, ZIP SHA-256 `f97b8bed4b93565cc2404fc165b6d21c7621df4772c828b8bf9025a429da892b`; extracted APK SHA-256 `ecd9dcce4d005167a37b1eda40d489261434da1f2da5975fd4492005ff8b190b`. Package `com.pocketforge.app.preview`, version `0.1.0-keystone-preview`, min API 26, target API 35. SBOM artifact `15033833370`; provenance attestation `72216088`.
- `apksigner` reports certificate SHA-256 `27a42a6843f5ed401cd5e3cb5014b342e2025d719c2b1e36e0b4f1c497d14dac`. The persistent Preview keystore preparation step passed in this same-repository PR build, but the fingerprint differs from the previously recorded PKCS12 certificate `9d89c5b33b9db756b60084228bb9589812c71fd5f7562e30a7d4ab3906d06d5f`. Signing continuity is therefore not yet accepted; verify repeatability on another trusted build and compare against the installed Preview signer before claiming update-safe delivery.
- Physical Redmi acceptance remains pending. This emulator result is x86_64 hosted CI evidence only. No JDK/Gradle project execution has been enabled; the next technical gate remains real-device validation of ART, process lifecycle, notification, cancellation and recovery.


## 2026-11-25 — Corrected Preview signing-context detection

- Re-read the full Android job log for run #374. Although all 19 pipeline steps passed, the signing step explicitly selected the fork fallback and used the runner's temporary debug key. PR #2's API metadata confirms head and base are both `ysfclc/PocketForge`; the earlier claim that run #374 used the persistent key was wrong.
- Root cause: `GITHUB_HEAD_REPOSITORY` is not populated as a default Actions environment variable. The unset value compared unequal to `GITHUB_REPOSITORY`, so same-repository PRs were misclassified as forks. The APK signer fingerprint `27a42a6843f5ed401cd5e3cb5014b342e2025d719c2b1e36e0b4f1c497d14dac` is ephemeral. The prior artifact/attestation are not update-safe distribution artifacts.
- Fixed pipeline in commit `67a5244ff6e967f3b6a34889fbaaa03b838cda3`: explicitly populate head/base repo names from the GitHub event context. Attestation and artifact upload are gated off for genuine fork PRs, preventing temporary-key APKs from being presented as trusted Preview builds.
- Acceptance ledger corrected. Next gate is a trusted same-repository push run, verifying the configured Preview keystore is accessible and matches the known private PKCS12 fingerprint `9d89c5b33b9db756b60084228bb9589812c71fd5f7562e30a7d4ab3906d06d5f`, followed by a second build to prove repeatability. Physical Redmi acceptance remains pending.

- Added an explicit trusted-build certificate gate in pipeline commit `5247c31f586768fda40d84c5f02f62826335d73b`: after APK signature verification, trusted same-repository PR/push builds must match the approved certificate SHA-256 `9d89c5b33b9db756b60084228bb9589812c71fd5f7562e30a7d4ab3906d06d5f`. A mismatch fails the pipeline before attestation or artifact upload. Genuine fork PRs skip this trusted-key gate and cannot publish APK/provenance.
