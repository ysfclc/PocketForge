# Brainstorm archive

The supplied Keystone brief contained a placeholder instead of the full transcript. This is a reconstruction of decisions available in conversation context, not a verbatim transcript. No missing dialogue is fabricated.

Confirmed: PocketForge remains separate from Memo; Memo is a browser/app automation connector; integrate GitHub and frontier agents; combine local and frontier intelligence; local-first with optional cloud integrations; Android APK production is the first wedge; iOS is gated.

Hypotheses: initial users are AI-assisted builders unfamiliar with Android toolchains; durable missions/project context differentiate from a model aggregator; useful on-device compilation is feasible for a subset of projects.

Constraints: Redmi Note 12 Pro 5G, Dimensity 1080, 8 GB RAM, Android 14; limited budget and desire to avoid mandatory cloud build costs.


## Conversation-derived clarifications — 2026-11-20 to 2026-11-27

These points were recovered from prior PocketForge conversations and confirmed against the accepted product/architecture documents. They are a decision summary, not a verbatim transcript.

- **One natural-language development mission:** the user describes the software outcome; PocketForge determines the plan and the required providers/capabilities. Do not create a separate hardcoded workflow for every example, language, framework, or task.
- **Real execution, not chat theatre:** model text, plans, generated manifests, or claimed tool calls are not execution. Capabilities must be callable; PocketForge must observe real outputs, verify postconditions, show evidence, replan when needed, and recover durable state after interruption.
- **Provider/tool separation:** local intelligence and frontier coding agents provide reasoning. Project files, GitHub, build/test, diagnostics, signing, and artifact storage are distinct capabilities behind one scoped gateway. Agents never receive ambient shell, filesystem, network, repository, or signing-key authority.
- **Two equal project sources:** a local Android project folder and a connected GitHub repository are first-class mission targets using the same mission lifecycle. They are not separate products or separate AI workflows. GitHub Actions is an optional remote executor, never a prerequisite for local compilation.
- **Local factory outcome:** inspect a linked project without executing it; create a verified private snapshot; provision pinned Java/Gradle/Android SDK/NDK components; execute an explicitly approved build; stream bounded logs and progress; support cancellation, background operation and process-death recovery; verify output APK/package/signature; retain immutable artifacts and provenance; and install/update safely without deleting project data.
- **GitHub outcome:** inspect pinned repository context and history; create working branches; propose multi-file changes; show/review diffs; guard against stale revisions; require explicit approvals for writes/commits/PRs/merges; inspect checks and retrieve artifacts.
- **Trust and delivery:** never silently fall back to cloud builds, never execute mutable source folders, never run imported project scripts during preflight, never expose signing secrets to agents, never publish a temporary-key APK as update-safe, and never claim device acceptance from emulator tests.
- **Product boundary:** PocketForge remains a specialized native Android development factory. Memo is independent and may be integrated only as an automation adapter. No HTML/web UI, ZIP ingestion, Android Studio clone, mandatory cloud compilation, universal no-code builder, public marketplace MVP, or early iOS expansion.
- **Acceptance device:** Redmi Note 12 Pro 5G, Dimensity 1080, 8 GB RAM, Android 14. Emulator/CI success and hands-on device acceptance must be tracked separately.

The broader early brainstorming about a general local-AI assistant is deliberately constrained here by the later, binding PocketForge decision: the MVP is a development factory, not an unrestricted general-purpose assistant.