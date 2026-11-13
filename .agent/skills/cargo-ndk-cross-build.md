# Skill: cargo-ndk-cross-build

## When to invoke
Rust core must target Android.

## Preconditions
Pinned Rust/NDK toolchains available.

## Exact procedure
Install three Android targets and cargo-ndk; build arm64-v8a, armeabi-v7a and x86_64; copy .so to jniLibs/<abi>; verify ELF architecture.

## Verification
All three libraries exist and APK packages them.

## Failure modes
Do not silently drop an ABI; fail with diagnostics.
