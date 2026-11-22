# Local build execution architecture

## Product invariant

A linked local project must be buildable on the Android device without requiring a GitHub account, hosted CI runner, Memo, or a second-party build service. GitHub Actions may be offered as an optional remote executor, but it is never presented as the local build path.

The current Android application only links and inspects project folders and performs a read-only build preflight. It does not yet execute imported-project build scripts. This document defines the next implementation gate; it is not evidence that a runner already exists.

## Execution boundary

1. **Link, do not take ownership.** The original project remains at its selected Storage Access Framework (SAF) location. PocketForge keeps the persisted tree grant and a stable project reference.
2. **Snapshot before execution.** When the user explicitly starts a build, copy the selected project tree into a private, per-run staging directory. Do not execute from a cloud provider, removable mount, or mutable source tree. Record a SHA-256 manifest of the staged inputs and the source URI/revision.
3. **Treat build logic as code.** Gradle settings, plugins, build scripts, wrapper JARs and compiler plugins can execute arbitrary code. Preflight is read-only and never invokes them. A build requires an explicit user action with the project, task, variant, toolchain, network access and expected output shown.
4. **Keep the runner local.** The required executor and build outputs live on the device. Termux may be explored as an optional adapter, but installing Termux or another app must not be a prerequisite for the PocketForge local factory.
5. **No hidden remote fallback.** If the local toolchain is absent or the device cannot safely run the build, report the exact blocker. Never silently send source to GitHub Actions or another remote service.

## Toolchain provider

The toolchain provider owns a manifest of supported, version-pinned components and their cryptographic hashes:

- A compatible Java runtime (JDK 17 baseline for the current Android Gradle plugin).
- The Gradle distribution required by the project wrapper, subject to an explicit compatibility policy.
- Android SDK platform and build-tools versions declared by the project; the current PocketForge project baseline is API 35 / build-tools 35.0.0.
- NDK and CMake only when the project actually requires native compilation.
- Rust toolchain and Android targets only when Cargo/native Rust markers are present.

Toolchains are installed into an app-private, versioned cache. Every downloaded archive must be size-limited, checksum-verified against a pinned manifest, safely extracted against path traversal, and atomically promoted only after validation. Do not run a downloaded executable before its archive and manifest have passed verification. The UI must show required storage and download size before provisioning.

A project wrapper may request a Gradle version that is unsupported or incompatible with the installed JDK/plugin. The provider must report that mismatch rather than silently replacing the wrapper version.

## Android process execution feasibility gate

Before implementing a full Gradle runtime, run a device spike on the supported Android versions to prove how a child Java runtime can be launched and kept alive under Android's app sandbox and execution restrictions. Do not assume that arbitrary extracted ELF executables are executable from writable app-private storage.

The spike must prove, on the target device:

- A pinned Java runtime can start and report its exact version.
- Gradle can run a harmless diagnostic task using the staged project and pinned runtime.
- Android SDK tools can be invoked and find the expected platform/build-tools.
- Output and exit codes survive activity recreation and app backgrounding.
- A foreground execution service can keep the build visible, cancellable and correctly attributed to its project.
- The operating system can terminate the process without corrupting the source tree or losing the recorded run state.

If the native child-process approach fails, evaluate a separately installed, explicitly permissioned local runner companion as an adapter. That alternative must remain on-device and must not silently become a hosted build service. A Termux adapter is optional and requires a clear user-facing installation/permission flow.

## Build lifecycle

Every build is a persisted BuildRun with:

- Project ID, immutable staged-source manifest and source revision.
- Requested task, variant, build type and environment/toolchain manifest.
- User approval record and network policy.
- Queued / preparing / running / cancelling / succeeded / failed / cancelled / interrupted states.
- Start/end timestamps, exit code, bounded stdout/stderr log references and failure classification.
- Output artifact paths, SHA-256 hashes, package metadata and signing identity.

The lifecycle must be idempotent. Process death or device restart marks an active run interrupted unless the executor can prove safe resumption. Never replay a build automatically after restart. Cancellation stops the process tree, records the outcome and cleans only that run's staging directory. Source files are never deleted or rewritten by cleanup.

## Resource and safety policy

- Probe free storage, available memory, thermal state and battery before a build; show a warning or refuse when the configured minimum budget is not met.
- Bound log size, output size, file count, path depth, archive expansion and build duration.
- Do not follow symlinks outside the staged workspace. Reject path traversal and unsafe archive entries.
- Keep credentials and signing keys outside the staged project and never expose them to build scripts by default.
- Network dependency resolution is opt-in per project/build. Show that dependencies may be downloaded and enforce a configurable byte/time budget.
- A build script can execute arbitrary code with the app's UID. The UI must not describe this as a sandbox that neutralizes malicious build scripts.

## Artifact acceptance

A successful Gradle exit is not enough. Before an APK appears in the artifact vault:

1. Confirm the output is inside the current run's staging/output area.
2. Verify APK structure and signature with a trusted, pinned Android build-tools version.
3. Read package name, version code/name, min/target SDK and supported ABIs.
4. Calculate SHA-256 and attach the source manifest and toolchain manifest.
5. Sign with a project-specific or user-controlled key protected by Android Keystore; provide an explicit backup/export path and explain that losing the key prevents update-installing future APKs over the same package.
6. Install only after a separate user approval. Never replace an installed app or clear its data automatically.

## Acceptance gates

- [ ] Android 14 target-device execution spike passes.
- [ ] Toolchain manifest and archive verification tests pass.
- [ ] A small, known Android Gradle sample builds entirely on-device without GitHub Actions.
- [ ] A second real Android project with a different Gradle wrapper/plugin combination builds or receives a precise compatibility diagnosis.
- [ ] Process cancellation, app backgrounding, process death, low storage and thermal warning tests pass.
- [ ] Build logs, exit codes, artifact hashes and signer identity are persisted and inspectable.
- [ ] The resulting signed APK installs and launches without deleting or replacing PocketForge's own app data.
- [ ] The same local build path works with no GitHub account and no network after all required toolchains/dependencies have been provisioned.
