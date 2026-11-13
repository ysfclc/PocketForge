# Roadmap

0. **Keystone foundation:** product/architecture/security contracts, agent roster and skills, Rust ABI scaffold, Android placeholder APK and Git-native CI. Gate: all three Rust Android ABIs compile, debug APK assembles and verifies, actionlint/foundation checks pass, SBOM and provenance are emitted.
1. **Local factory:** project import, workspace, Project Passport and local toolchain/build. Gate: two real Android projects build and install on the target device; cancellation, recovery and no-data-loss behavior are verified.
2. **Intelligence Nexus:** local model, frontier provider, capability router, durable mission and patch review. Gate: a real bounded task is delegated, reviewed and approved with traceable changes.
3. **Connectors:** direct GitHub and Memo adapters. Gate: remote changes are reviewed and imported locally without manual file shuffling.
4. **Production loop:** diagnostics, tests, signing and artifact vault. Gate: a reviewed agent change becomes a tested signed APK with verifiable provenance.
5. **Expansion:** more project types, agent collaboration, sync and iOS evaluation. Each requires demand evidence, an ADR and a reversible rollout. iOS remains gated until bridge parity, signing ownership and macOS CI are explicitly approved.
