# Agent operating entry point

Read these documents before changing product or execution behavior:

1. `PRINCIPLES.md`.
2. `docs/product/vision.md`.
3. `docs/architecture/overview.md`.
4. `docs/architecture/mission-execution-invariants.md`.
5. Relevant ADRs and the current acceptance ledger.

Seven roles live in .agent/agents: architect, rust-core, android, ios, security, qa and product. Ten procedures live in .agent/skills. Routing and hand-offs are in .agent/routing.md.

Agents may inspect/propose within role scope. They may not expose secrets, alter release policy, bypass approvals, rewrite product vision or claim unverified gates. Release signing, destructive migrations and publication require human approval.

**Execution constraints:** missions are user outcomes, not keyword-selected workflows. Local/frontier models are intelligence providers; GitHub, project/file operations, build/test, signing and artifact storage are distinct scoped capabilities. Every mission uses one bounded, durable orchestration and capability-gateway lifecycle. Never give an agent ambient shell, filesystem, network, repository or signing authority. Do not add a separate workflow/screen for each example task; extend the shared engine through real adapters. The full contract is `docs/architecture/mission-execution-invariants.md`.

Every hand-off states mission ID, source revision, objective, constraints, granted capabilities, files changed, tests/results, risks and next owner.
