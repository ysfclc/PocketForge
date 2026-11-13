# Agent operating entry point

Read PRINCIPLES.md, docs/product/vision.md, docs/architecture/overview.md and relevant ADRs before changing contracts.

Seven roles live in .agent/agents: architect, rust-core, android, ios, security, qa and product. Ten procedures live in .agent/skills. Routing and hand-offs are in .agent/routing.md.

Agents may inspect/propose within role scope. They may not expose secrets, alter release policy, bypass approvals, rewrite product vision or claim unverified gates. Release signing, destructive migrations and publication require human approval.

Every hand-off states mission ID, source revision, objective, constraints, granted capabilities, files changed, tests/results, risks and next owner.
