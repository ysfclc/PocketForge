# Principles

1. Local-first, not cloud-hostile: project state and Android compilation must work locally.
2. The checked-out source tree is code truth; Project Passport preserves intent and decisions.
3. Providers declare capabilities. A chat API is not presumed to be a coding agent.
4. Missions are durable; approvals, diffs, tests and artifacts survive restarts.
5. Human approval gates signing, release, remote writes and destructive actions.
6. Least privilege: no ambient shell, filesystem or signing-key access for agents.
7. Build evidence beats confidence. No gate passes without reproducible evidence.
8. Stable contracts precede parallel agents.
9. Native mobile UX only; no HTML, web UI or desktop IDE imitation.
10. Prefer reversible decisions and record trade-offs in ADRs.
11. Preserve user source and project context; imports and migrations must be recoverable.
12. Android is the initial wedge; general-purpose expansion is not an MVP promise.
