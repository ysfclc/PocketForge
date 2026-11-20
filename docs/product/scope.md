# Scope and non-goals

Keystone foundation: product/architecture/data/FFI contracts; Rust workspace placeholder for three Android targets; native Android shell packaging Rust; iOS contract scaffold only; agent roster, skills, security and Git-native phase protocol.

First Local Factory slice implemented in the Android shell: local project workspace; Project Passport creation; existing folder linking through Android's Storage Access Framework; locally persisted mission queue with Draft / Ready states; and a Factory status screen that does not claim unverified build steps.

Two first-class project modes are in scope and share the same mission engine:

- **Local project mode:** select/link an Android document tree, preserve scoped URI access, inspect and modify permitted project files, run supported local toolchains, test, sign with explicit approval and install artifacts.
- **GitHub project mode:** authenticate to GitHub with least-privilege scopes; select repositories; inspect files, branches and commit history; create a working branch; prepare scoped changes and diffs; commit and open pull requests with approval; inspect checks and retrieve artifacts.

A mission is bound to a source identity and revision. Local and GitHub modes must not silently overwrite each other or assume that a folder URI is a Git checkout. Explicit clone/link/sync behavior and conflict handling are required when work crosses modes.

First GitHub slice authored in the Android shell: connect a repository by owner/name, verify repository metadata, browse directories and preview text files from its default branch; optionally authenticate with a fine-grained token encrypted by Android Keystore. This is read-only and awaits build/device verification.

Remaining intended scope: editable/exportable Project Passports; safe project file access; local Android toolchain/build execution; GitHub branch creation, approved file changes, diff review, commits, pull requests, checks and artifact retrieval; diagnostics, tests, cancellation and recovery; local/frontier provider adapters; capability-scoped Memo automation adapter; signing/artifacts; optional GitHub Actions execution and sync; additional project types only with evidence.

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
