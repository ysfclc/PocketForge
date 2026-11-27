# Vision

PocketForge is a local-first, mobile-native development Nexus and production factory. It gives builders one durable project workspace in which local intelligence, frontier coding agents, repository services and automation capabilities collaborate under explicit permissions. Its outcome is an approved, traceable change that can be built, tested, signed and installed as real Android software locally.

Nexus + Bridge + Factory: intelligence providers and tools, durable missions/context/permissions, and local build/test/release execution.

Confirmed: Android-first, local-first, optional frontier providers, direct GitHub integration and Memo as an independent automation connector. Unproven: on-device compilation of useful projects, broad device compatibility, willingness to pay and local-model coding quality.

## Two first-class project modes

PocketForge supports both local-device projects and connected GitHub repositories as first-class mission targets.

- **Local mode** works directly with folders on the Android device. It can use local intelligence and local project/build/test/sign/install capabilities, and must not depend on a GitHub account, hosted runner or cloud build service.
- **GitHub mode** works directly with a selected repository: inspect project context and history, create a working branch, propose and apply scoped changes, review diffs, commit, open pull requests, inspect checks and retrieve artifacts.

The two modes share one project/mission model and one execution engine. They are not separate workflows or separate AI products. A mission is anchored to a specific local workspace state or a GitHub repository/ref/commit. Moving work between local and remote representations must preserve source identity and make synchronization explicit.

GitHub Actions may execute optional remote checks or builds where available. It is not a substitute for, nor a prerequisite to, PocketForge's local Android factory. Local compilation, signing and installation remain core capabilities even when GitHub services or hosted runners are unavailable.

## Binding execution constraint

[Mission Execution Invariants](../architecture/mission-execution-invariants.md) govern every implementation. The user supplies a development outcome; PocketForge determines how to achieve it using actual, permitted capabilities. Local models and frontier agents are interchangeable intelligence providers, while repository, project, build, test, diagnostic and artifact operations are separate callable tools. One durable, bounded mission engine selects, validates, invokes, observes, verifies and replans. Do not build a separate workflow for each mission example or model provider.
