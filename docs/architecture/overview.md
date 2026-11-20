# Architecture

PocketForge is a modular native Android app plus supervised local execution process. It is not a distributed microservice system. Remote models/tools are capability adapters.

Modules: Rust platform-independent core; narrow C ABI/FFI crate; Android Kotlin shell/JNI loading/packaging; project/mission/provider/connector/execution/artifact modules added when real boundaries exist; iOS Swift shell remains scaffold-only.

Runtime rules: UI never runs long builds on main thread; agents have no ambient filesystem/shell/signing access; connectors receive scoped grants; build scripts/dependencies are untrusted; FFI contains panics and uses stable error codes.

Reuse official Rust, NDK, Gradle and Android toolchain. cargo-ndk outputs jniLibs/<abi>/. On-device build feasibility is a separate Phase 1 gate. Future iOS may use UniFFI or C ABI only after macOS runner, signing and parity ADR.

## Project-source modes

The mission engine operates on two first-class project sources:

- **Local workspace adapter:** Android Storage Access Framework/document-tree grants and local project state. It exposes scoped project inspection and file operations to the local execution runtime. Local build/test/sign/install capabilities remain available without GitHub connectivity.
- **GitHub repository adapter:** the first Android implementation verifies a selected repository and reads its metadata, directory contents and text files from the default branch. Optional fine-grained tokens are encrypted with Android Keystore AES-GCM. This slice is read-only; branch creation, patch application, diff review, commits, pull requests, checks and artifact retrieval remain planned capabilities, each requiring its own scoped grant and observable postcondition.

Both adapters are represented as first-class project sources in the workspace store. The current GitHub implementation is a read-only API adapter; the shared mission/capability engine is not implemented yet. They do not create separate mission engines. A local SAF folder is not presumed to be a Git repository; linking, cloning and synchronization are explicit operations with source revisions and conflict handling.

GitHub's repository API and GitHub Actions are separate capabilities. Actions can provide optional remote execution, but hosted runners are not a dependency for the local factory. Failure or unavailability of GitHub Actions must not disable local compilation, signing or installation.

## Mission intelligence and execution boundary

The mission engine is one bounded, durable loop for every development objective. It interprets the desired outcome, inspects project context, selects a compatible local/frontier intelligence provider and registered capabilities, validates each proposed invocation against grants and policy, executes through the capability gateway, observes the real output, verifies it and continues/replans or stops.

Intelligence providers are not execution tools. A local model or frontier coding agent may reason and propose a structured action; it does not receive ambient shell, filesystem, network, repository or signing access. GitHub, scoped source inspection/editing, diff generation, build/test, diagnostics, signing and artifact storage are independently registered executable capabilities.

Every capability must have a versioned ID, input/output contract, concrete adapter, readiness state, resource scope/grant, risk policy, bounded lifecycle and observable postcondition. Mission records, plans and agent-role definitions do not themselves execute work. All invocations and results flow through one durable mission journal so approvals, diffs, tests, artifacts and recovery remain connected.

See [Mission Execution Invariants](mission-execution-invariants.md). This is an architecture constraint; it is not a claim that the engine or capability adapters are implemented yet.
