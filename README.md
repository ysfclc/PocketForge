# PocketForge

A local-first, mobile-native development Nexus and production factory. PocketForge coordinates on-device intelligence, frontier agents, repository providers and automation capabilities, then turns approved work into tested, installable Android software locally. Android is the first production target; iOS is contract-scaffolded and gated.

## Phase map
| Phase | Outcome | Gate |
|---|---|---|
| 0 — Keystone | Contracts, agents, security, CI, placeholder APK | Rust ABIs, debug APK, actionlint, archive tests |
| 1 — Local factory | Import, workspace, local build | Two real Android projects build/install on target device |
| 2 — Intelligence Nexus | Local/frontier providers, durable missions | Traceable delegated task with approved changes |
| 3 — Connectors | GitHub + Memo adapters | Remote change reviewed and imported locally |
| 4 — Production loop | Diagnostics, tests, signing, artifacts | Agent-to-verified-APK workflow |
| 5 — Expansion | More agents/platforms/project types | Evidence-backed expansion; iOS gate separately approved |

## Quickstart
Run scripts/verify-foundation.sh, scripts/build-rust.sh and scripts/build-android.sh debug. See docs/operations/local-development.md.

## Architecture
Native mobile workspace → Mission and project context → Intelligence orchestrator → Local models / frontier agents / capability gateway → GitHub and Memo adapters → Local Rust and Android execution → APK and gated release.

## Navigation
Product: docs/product. Architecture: docs/architecture. Decisions: docs/decisions. Security: docs/security. Operations: docs/operations. Agents and skills: .agent. Native scaffolds: core, android and ios. CI: .github/workflows.

PocketForge is separate from Memo. Memo is an external browser/app automation provider. GitHub and frontier agents are independent adapters. No HTML or web UI, no mandatory cloud build service, and no release signing of unreviewed ZIP content.

Foundation status: contracts and scaffolds only. CI and device acceptance are not claimed until evidenced in the acceptance ledger.
