# Scope and non-goals

Keystone foundation: product/architecture/data/FFI contracts; Rust workspace placeholder for three Android targets; native Android shell packaging Rust; iOS contract scaffold only; agent roster, skills, security and Git-native phase protocol.

First Local Factory slice implemented in the Android shell: local project workspace; Project Passport creation; existing folder linking through Android's Storage Access Framework; locally persisted mission queue with Draft / Ready states; and a Factory status screen that does not claim unverified build steps.

Remaining intended scope: editable/exportable Project Passports; safe project file access; local Android toolchain/build execution; diagnostics, tests, cancellation and recovery; local/frontier provider adapters; capability-scoped GitHub and Memo connectors; signing/artifacts; optional sync and additional project types only with evidence.

Explicit non-goals:
- No HTML, browser UI, web app or web control plane.
- No merging PocketForge into Memo.
- No mandatory cloud compilation or hosted backend.
- No claim that every Android project builds on every phone.
- No custom Gradle/compiler implementation or full Android Studio clone.
- No public agent marketplace or multi-agent swarm in MVP.
- No autonomous release signing or credentials in agent context.
- No iOS build until separate runner/signing/parity approval.
- No universal natural-language no-code builder in MVP.
- No ZIP-based source ingestion, secondary repository provisioning or archive-driven phase delivery.

Any change to a non-goal requires product decision and ADR plus architecture/security review.
