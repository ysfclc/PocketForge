# Android process execution feasibility probe

## Purpose

This is a debug/Preview-only spike to answer a narrow question before PocketForge commits to a local build executor: can the Android app launch a child process under its own UID, keep a user-visible foreground service alive while the app is backgrounded, persist the result, and cancel the child process?

It is not a build runner. It does not prove that a JDK, Gradle, Android SDK tools, or arbitrary extracted executables can run on the target device.

## Scope and safety boundary

The Preview build exposes **Run process probe** in Factory. The service is declared only in `src/debug/AndroidManifest.xml` and uses Android's `specialUse` foreground-service type with a declared local-process-probe subtype.

The probe first starts a fixed Java entry point from PocketForge's own APK using `/system/bin/app_process` and the `CLASSPATH` environment variable. `RuntimeProcessProbe` prints the Android runtime's Java properties and a success marker, with a 10-second process timeout. This tests a child ART runtime loading a class from the app APK; it is not a standalone OpenJDK/JDK distribution and cannot establish Gradle compatibility.

It then launches two fixed absolute Android system paths:

1. `/system/bin/id` to prove that a native child process can start and return output/exit status.
2. `/system/bin/sleep 30` to keep a harmless child alive long enough to test app backgrounding and cancellation.

No command, executable path, project path, Gradle task, or shell source is accepted from an Intent, UI input, project, model, or network. No shell is launched. No imported project content is read or executed. The service is not declared in the release manifest.

The service persists its state, bounded output and exit code in app-private preferences. A notification exposes cancellation. If the app process is recreated while a run is still marked active, the Factory screen reports it as interrupted rather than replaying it.

## Device acceptance procedure

Run this only on the target Android phone using a Preview APK:

1. Open Factory and tap **Run process probe**.
2. Confirm the state changes to running and the ongoing PocketForge notification appears.
3. Confirm the completed output contains `POCKETFORGE_ART_RUNTIME_OK`, `java.vm.name`, `java.vm.version`, and the `id` output.
4. Send PocketForge to the background for at least 10 seconds; do not force-stop it.
5. Return to PocketForge. Confirm the Factory screen still reports running or the completed result, without losing the run record.
6. Let the 30-second wait finish. Confirm success and a zero exit code.
7. Start a second probe and cancel it from the notification. Confirm the state becomes cancelled and the service notification disappears.
8. Repeat once while rotating/recreating the Activity if supported by the device configuration.
9. Separately force-stop the app only as a destructive interruption test. On reopening, a stale running state must be reported as interrupted; the probe must not replay automatically.

Record device model, Android version, outcome for each step, and any notification/foreground-service restriction. A CI compile is not device acceptance.

## What a pass proves

A pass proves only that a child ART runtime can load the app's Java entry point from the APK, and that the fixed Android system binaries can be spawned and supervised by a user-visible Preview foreground service on that device, with basic persisted state and cancellation.

It does not prove a standalone OpenJDK/JDK exists or can be provisioned, Gradle compatibility, process-tree cancellation for Gradle, background survival under all OEM battery policies, writable-directory executable permissions, toolchain provisioning, SDK/NDK availability, memory/thermal feasibility, build output correctness, or artifact signing.

## Next gate after a device pass

Only after device evidence is recorded should PocketForge implement a second, isolated spike that provisions a checksum-pinned Java runtime and runs a harmless Gradle diagnostic task against a verified private snapshot. That later spike must separately establish executable placement, process-tree termination, bounded logs, storage budgets, foreground-service policy, dependency-network consent, and recovery after process death. No imported build script should be run before an explicit per-build approval UI exists.
