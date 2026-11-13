# Architecture

PocketForge is a modular native Android app plus supervised local execution process. It is not a distributed microservice system. Remote models/tools are capability adapters.

Modules: Rust platform-independent core; narrow C ABI/FFI crate; Android Kotlin shell/JNI loading/packaging; project/mission/provider/connector/execution/artifact modules added when real boundaries exist; iOS Swift shell remains scaffold-only.

Runtime rules: UI never runs long builds on main thread; agents have no ambient filesystem/shell/signing access; connectors receive scoped grants; build scripts/dependencies are untrusted; FFI contains panics and uses stable error codes.

Reuse official Rust, NDK, Gradle and Android toolchain. cargo-ndk outputs jniLibs/<abi>/. On-device build feasibility is a separate Phase 1 gate. Future iOS may use UniFFI or C ABI only after macOS runner, signing and parity ADR.
