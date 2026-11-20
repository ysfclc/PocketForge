# PocketForge Mission Execution Invariants

**Status:** Binding architecture constraints  
**Established:** 20 November 2026  
**Scope:** PocketForge's specialized Android development-factory missions. These rules do not merge PocketForge into Memo or turn PocketForge into a general-purpose consumer assistant.

PocketForge's core interaction is outcome-first: the user gives a development objective, and PocketForge chooses the compatible intelligence provider and scoped project capabilities needed to deliver a reviewable, tested software change.

## 1. Mission objective, not a hardcoded workflow

A mission expresses the desired outcome, constraints, target project/source and revision, and acceptance conditions. It does not prescribe a fixed sequence of screens or agent roles. The orchestrator determines the work from the objective and the current project state.

Do not add a new hardcoded workflow for each language, framework, build system, repository host, bug category or example mission. Add a real adapter/capability or improve the shared mission engine. Provider-specific compatibility belongs inside the adapter.

PocketForge remains a specialized development factory. This constraint does not authorize unrelated domains, a universal no-code builder, a web UI, a cloud-only build service, or a merge with Memo.

## 2. Intelligence providers are separate from tools

Local models and frontier coding agents are intelligence providers. GitHub, project inspection/editing, diff creation, tests, Gradle, Rust/Cargo, diagnostics, signing and artifact storage are separate capabilities.

A generic chat API must not be assumed to be a coding agent. A provider must declare verified capabilities and constraints (structured tool use, context limits, supported modalities, streaming, cancellation and remote execution semantics). Do not silently substitute a weaker provider or claim that a provider executed a tool it merely described.

The engine may select local or frontier intelligence according to mission needs, user settings, actual provider capability and explicit data permissions. Tool access does not depend on which intelligence provider is selected.

## 3. One bounded mission-execution lifecycle

All missions use one durable execution engine:

`understand objective → inspect project/context → select provider and capabilities → propose next action → validate grants/policy → execute → observe output → verify → continue/replan/review`.

Plans are optional for trivial work and dynamic for complex work. Agent roles and parallel contributors are optional strategies; they do not form separate mission engines. An execution graph or mission record is not proof that a task ran.

Every invocation must pass through an explicit capability gateway. Agents never receive ambient filesystem, shell, network, repository or signing-key authority. Adapters receive narrow grants for a named project, repository, path set, command class, provider account or artifact scope. Tool results return structured outputs, logs and evidence to the orchestrator.

## 4. Project source and revision invariants

A mission targets either a local workspace or a connected GitHub repository, using the same lifecycle and acceptance model. Its durable record identifies the source kind and immutable or observable revision: for example, a Git commit/ref for a remote repository or a tracked local workspace checkpoint.

A folder URI is not proof of a Git checkout. Linking, cloning and syncing local and GitHub representations are explicit capabilities. The engine must detect revision drift and conflicts rather than silently overwrite one side.

GitHub writes are scoped to the selected repository and working branch. The engine records the base revision, proposed patch and resulting commit/PR. Commit, push, merge, release and other consequential remote writes require their applicable grants and approval. Never bypass branch protections or force-push as an automatic recovery strategy.

GitHub repository operations and GitHub Actions execution are distinct capabilities. Remote Actions checks/builds may be used when configured and available, but local project compilation, signing and installation must remain independent of hosted runners.

## 5. Capabilities must be executable, scoped and verifiable

Each capability must declare a stable ID/version, input/output schema, concrete implementation, provider/runtime, readiness, required grant, risk/side-effect class, resource/time limits, cancellation/retry/idempotency rules and observable postcondition.

A capability may not appear as available until its adapter is installed, configured and ready. No stub connectors, simulated test output or imaginary build state.

The engine must observe actual repository revisions, diffs, test reports, build logs, artifact hashes and installation evidence. A model's assertion that code was changed or tests passed is never sufficient.

## 6. Durable missions and safe recovery

Persist the mission objective, source revision, plan revisions, provider/model identity, granted scopes, tool invocations, approvals, outputs, diffs, test evidence, decisions, checkpoints, failures and final artifact provenance.

Support bounded execution, cancellation, process-death recovery and re-planning. Never replay uncertain writes or overwrite a changed source revision without review. Signing, release, destructive migrations and publication remain explicit human approval gates.

## 7. Acceptance across project modes

The same mission lifecycle must handle unrelated development objectives, such as explaining a repository, diagnosing a test failure, implementing a bounded code change, running its tests, reviewing a diff and preparing an APK artifact. These objectives must work against both local and GitHub project targets where the required capabilities are available.

Acceptance must prove that the selected provider actually invoked permitted capabilities, outputs were observed, tests ran against the stated revision, the change was reviewed, and the artifact can be verified. These are tests of the shared engine—not separate product workflows.
