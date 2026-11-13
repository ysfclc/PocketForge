# Phase ZIP protocol

Name: project-phase-NN-description.zip; workflow_dispatch path must be directly under incoming/. Defaults: 50 MiB compressed, 250 MiB expanded, 5,000 entries, 240-byte path. Reject absolute/drive/parent paths, NUL, symlink/hardlink/special files, duplicate normalized paths, forbidden extensions and known secret markers. Strip embedded .git; normalize one root folder.

The validator emits source archive hash, workflow run, timestamp, phase and normalized tree hash. The target branch is content-addressed by tree hash; repeat identical content reuses its commit. The workflow opens/updates a PR and never pushes ZIP content to default branch. Build exact published commit. Review PR before merge.
