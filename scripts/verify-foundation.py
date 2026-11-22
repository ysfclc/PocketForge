#!/usr/bin/env python3
"""Static repository-contract checks; no third-party Python packages required."""
from pathlib import Path
import re

required = [
    "README.md", "AGENTS.md", "PRINCIPLES.md", "SECURITY.md", "CONTRIBUTING.md",
    "CODEOWNERS", "docs/product/vision.md", "docs/product/personas.md",
    "docs/product/scope.md", "docs/product/roadmap.md",
    "docs/architecture/overview.md", "docs/architecture/data-model.md",
    "docs/architecture/ffi-contract.md", "docs/architecture/local-build-execution.md", "core/Cargo.toml",
    "android/app/build.gradle.kts", "ios/README.md",
    ".github/workflows/pipeline.yml", ".github/workflows/android.yml",
    ".github/workflows/security.yml",
]
missing = [path for path in required if not Path(path).is_file()]
assert not missing, f"Missing foundation files: {missing}"
assert len(list(Path(".agent/agents").glob("*.md"))) == 7
assert len(list(Path(".agent/skills").glob("*.md"))) == 10
assert len(list(Path("docs/decisions").glob("ADR-*.md"))) >= 15
assert not Path("incoming").exists(), "ZIP drop-zone must not exist"
assert not Path("scripts/validate-phase-zip.py").exists()
assert not Path("scripts/publish-phase.py").exists()
assert not list(Path(".").rglob("*.html"))

app = Path("android/app/src/main/java/com/pocketforge/app/MainActivity.kt").read_text()
store = Path("android/app/src/main/java/com/pocketforge/app/WorkspaceStore.kt").read_text()
for contract in (
    "Tab.WORKSPACE", "Tab.MISSIONS", "Tab.FACTORY",
    "ACTION_OPEN_DOCUMENT_TREE", "takePersistableUriPermission",
    "showCreateProject", "showCreateMission", "nativeStatus",
):
    assert contract in app, f"Native workspace missing contract: {contract}"
for contract in (
    "SharedPreferences", "WorkspaceProject", "WorkspaceMission",
    "KEY_PROJECTS", "KEY_MISSIONS", "setMissionState",
):
    assert contract in store, f"Local workspace store missing contract: {contract}"
assert "No product features included." not in app

local_inspector_path = Path("android/app/src/main/java/com/pocketforge/app/LocalProjectInspector.kt")
assert local_inspector_path.is_file(), "Missing SAF-scoped local project inspector"
local_inspector = local_inspector_path.read_text()
for contract in (
    "persistedUriPermissions", "buildChildDocumentsUriUsingTree",
    "buildDocumentUriUsingTree", "detectProjectType", "MAX_DIRECTORY_ENTRIES",
):
    assert contract in local_inspector, f"Local project inspector missing contract: {contract}"
for contract in (
    "persistedUriPermissions.any", "inspectLocalProject", "showLocalProjectOverview",
    "browseLocalDirectory", "normalizeGitHubRepositoryInput", "runBackgroundTask",
):
    assert contract in app, f"Native project onboarding missing contract: {contract}"
assert "runGitHubRequest" not in app, "Obsolete GitHub-only background task label remains"
assert "pinnedRevisionOrReconnect" in app, "GitHub browsing must require a pinned commit"
assert "initialRepository: String? = null" in app, "GitHub reconnect should prefill the repository"
assert "project.githubRevision ?: project.githubDefaultBranch" not in app, "GitHub browsing must not fall back to a moving branch"
assert "sizeColumn < 0" in local_inspector, "Local inspection must tolerate providers without an optional size column"

build_preflight_path = Path("android/app/src/main/java/com/pocketforge/app/BuildPreflight.kt")
assert build_preflight_path.is_file(), "Missing read-only imported-project build preflight"
build_preflight = build_preflight_path.read_text()
for contract in (
    "persistedUriPermissions", "MAX_DEPTH", "MAX_ENTRIES", "MAX_TEXT_BYTES",
    "gradle-wrapper.properties", "gradle-wrapper.jar", "com.android.application",
    "AndroidManifest.xml", "cargo.toml", "distributionUrl", "candidateRoots", "relativeToBuildRoot", "buildRoot",
    "never copies, edits, executes or uploads",
):
    assert contract in build_preflight, f"Build preflight missing safety/detection contract: {contract}"
assert "ProcessBuilder" not in build_preflight and "Runtime.getRuntime" not in build_preflight, "Preflight must never execute imported project code"

build_run_store_path = Path("android/app/src/main/java/com/pocketforge/app/BuildRunStore.kt")
assert build_run_store_path.is_file(), "Missing persisted build-run lifecycle store"
build_run_store = build_run_store_path.read_text()
for contract in (
    "data class BuildRun", "class BuildRunStore", "fun create(", "fun transition(",
    "fun all()", "fun get(id: String)", "pocketforge.build-runs.v1",
    "QUEUED", "PREPARING", "RUNNING", "CANCELLING", "SUCCEEDED",
    "FAILED", "CANCELLED", "INTERRUPTED", "Illegal build transition",
    "outputArtifacts", "logReference", "failureReason", "commit()",
):
    assert contract in build_run_store, f"Build-run persistence missing contract: {contract}"
assert "ProcessBuilder" not in build_run_store and "Runtime.getRuntime" not in build_run_store, "Build-run store must not execute project code"

snapshot_path = Path("android/app/src/main/java/com/pocketforge/app/ProjectSnapshotter.kt")
assert snapshot_path.is_file(), "Missing bounded app-private project snapshot preparation"
snapshot_source = snapshot_path.read_text()
for contract in (
    "class ProjectSnapshotter", "data class ProjectSnapshot", "data class SnapshotLimits",
    "persistedUriPermissions", "filesDir", "build-workspaces", "renameTo(destination)",
    "isCancelled", "onProgress", "maxEntries", "maxDepth", "maxTotalBytes", "maxFileBytes",
    "EXCLUDED_DIRECTORIES", ".git", ".gradle", "node_modules", "canonicalFile.parentFile",
    "SnapshotCancelledException", "output.fd.sync()",
):
    assert contract in snapshot_source, f"Project snapshot safety contract missing: {contract}"
assert "ProcessBuilder" not in snapshot_source and "Runtime.getRuntime" not in snapshot_source, "Snapshot preparation must never execute project code"
for contract in (
    "manifestFile", "manifestSha256", "snapshot-manifest.json", "schemaVersion",
    "FileRecord", "MessageDigest.getInstance(\\"SHA-256\\")", "progress.records.sortedBy { it.path }",
):
    assert contract in snapshot_source, f"Snapshot manifest/hash contract missing: {contract}"
assert "Prepare snapshot" in app and "prepareProjectSnapshot(project)" in app, "Local project UI must expose snapshot preparation"
for contract in ("Snapshot ready", "Manifest SHA-256", "Snapshot cancelled", "No build scripts have been executed"):
    assert contract in app, f"Snapshot UI result missing safety/status contract: {contract}"

# Build-root scoring must use actual Android manifest evidence, not treat the
# empty repository root as Android merely because it is a candidate.
preflight_source = build_preflight
assert 'if (candidateRoot.isEmpty()) it.isNotBlank() else it == candidateRoot || it.startsWith("$candidateRoot/")' in preflight_source, "Kotlin manifest scoring must explicitly guard empty root"
assert 'if (candidateRoot.isEmpty()) true else it == candidateRoot || it.startsWith("$candidateRoot/")' not in preflight_source, "Empty candidate root must not match every manifest"
def preflight_root_score(candidate_root, manifest_paths, wrapper_roots, root_build_roots, settings_roots):
    has_manifest = any(
        (path.startswith(candidate_root + "/") or path == candidate_root)
        if candidate_root else bool(path)
        for path in manifest_paths
    )
    return (8 if has_manifest else 0) + (8 if candidate_root in wrapper_roots else 0) + (2 if candidate_root in root_build_roots else 0) + (1 if candidate_root in settings_roots else 0)

assert preflight_root_score("", [], set(), {""}, {""}) == 3, "Empty root must not gain Android manifest score"
assert preflight_root_score("android", ["android/app/src/main/AndroidManifest.xml"], set(), {"android"}, {"android"}) == 11, "Nested Android root must score its manifest and Gradle evidence"
assert preflight_root_score("", ["app/src/main/AndroidManifest.xml"], {""}, {""}, {""}) == 19, "Root Android project must retain manifest evidence"
for contract in ("BuildPreflight.assess", "showBuildAssessment", "Assess build", "structural assessment only", "Read-only preflight is available", "assessment.buildRoot"):
    assert contract in app, f"Local build preflight UI missing contract: {contract}"

github_client_path = Path("android/app/src/main/java/com/pocketforge/app/GitHubRepositoryClient.kt")
github_vault_path = Path("android/app/src/main/java/com/pocketforge/app/GitHubTokenVault.kt")
assert github_client_path.is_file(), "Missing native GitHub repository adapter"
assert github_vault_path.is_file(), "Missing encrypted GitHub token vault"
github_client = github_client_path.read_text()
github_vault = github_vault_path.read_text()
for contract in (
    "showConnectGitHubRepository", "browseGitHubPath", "showGitHubEditForm",
    "buildReviewDiff", "createGitHubPullRequest",
):
    assert contract in app, f"Native GitHub workspace missing contract: {contract}"
for contract in (
    "fun getRepository(", "fun listContents(", "fun readTextFileWithSha(",
    "fun createBranch(", "fun updateTextFile(", "fun createPullRequest(",
    "currentSha != expectedBlobSha", "CodingErrorAction.REPORT",
    "SHA_PATTERN.matches(sha)", "could not pin the default branch",
):
    assert contract in github_client, f"GitHub repository adapter missing contract: {contract}"
for contract in (
    "AndroidKeyStore", "AES/GCM/NoPadding", "SHA-256",
    "preferenceKey(repository)", "fun clear(repository: String)",
):
    assert contract in github_vault, f"GitHub credential vault missing security contract: {contract}"
for contract in ("SOURCE_GITHUB", "githubRepository", "githubDefaultBranch", "githubRevision"):
    assert contract in store, f"Workspace source model missing GitHub field: {contract}"
ffi_manifest = Path("core/pocketforge-ffi/Cargo.toml").read_text()
core_manifest = Path("core/pocketforge-core/Cargo.toml").read_text()
deny_policy = Path("core/deny.toml").read_text()
assert "publish = false" in ffi_manifest and "publish = false" in core_manifest, "Proprietary workspace crates must remain unpublished"
assert 'version = "0.1.0", path = "../pocketforge-core"' in ffi_manifest, "Internal path dependency must have an explicit version"
assert "private = { ignore = true }" in deny_policy, "License checks must ignore only unpublished private workspace members"

security_workflow = Path(".github/workflows/security.yml").read_text()
assert "run: cargo audit" in security_workflow, "Use cargo-audit's supported command syntax"
assert "cargo audit --locked" not in security_workflow, "cargo-audit does not accept Cargo's --locked flag"

android_build = Path("android/app/build.gradle.kts").read_text()
preview_manifest_path = Path("android/app/src/debug/AndroidManifest.xml")
assert 'applicationIdSuffix = ".preview"' in android_build, "Preview must install alongside the earlier debug package"
assert "previewStoreFile" in android_build and 'signingConfigs.getByName("preview")' in android_build, "Preview needs configurable persistent signing"
assert 'storeType = "PKCS12"' in android_build, "Preview keystore format must be explicit"
assert preview_manifest_path.is_file() and "PocketForge Preview" in preview_manifest_path.read_text(), "Preview app label must be distinguishable"
gitignore = Path(".gitignore").read_text()
assert "*.p12" in gitignore and "*.jks" in gitignore, "Private signing keystores must be ignored"
manifest = Path("android/app/src/main/AndroidManifest.xml").read_text()
assert "android.permission.INTERNET" in manifest, "GitHub API requires INTERNET permission"
assert r"\${" not in app, "Kotlin interpolation is accidentally escaped"
assert r"\\n" not in app, "Kotlin dialog newlines are double-escaped"

pipeline = Path(".github/workflows/pipeline.yml").read_text()
for contract in (
    "pull_request:", "push:", "branches: [main]", "workflow_dispatch:",
    "scripts/verify-foundation.sh", "scripts/build-rust.sh",
    "cargo +1.82.0 test", "assembleDebug", "apksigner",
    "POCKETFORGE_DEBUG_KEYSTORE_BASE64", "POCKETFORGE_DEBUG_KEYSTORE_PASSWORD",
    "POCKETFORGE_DEBUG_KEY_ALIAS", "POCKETFORGE_DEBUG_KEY_PASSWORD",
    "Remove Preview signing material", "Prepare persistent Preview signing key",
    "sbom-action", "attest-build-provenance", "upload-artifact",
):
    assert contract in pipeline, f"Direct CI pipeline missing contract: {contract}"
for forbidden in ("incoming/", "zip_path", "REPO_CREATE_TOKEN", "validate-phase-zip", "publish-phase.py"):
    assert forbidden not in pipeline, f"Obsolete ZIP/provisioning contract found: {forbidden}"

workflow_paths = sorted(Path(".github/workflows").glob("*.yml"))
assert {p.name for p in workflow_paths} == {"pipeline.yml", "android.yml", "security.yml"}
for path in workflow_paths:
    content = path.read_text()
    for action in re.findall(r"^\s*uses:\s*([^\s]+)", content, re.M):
        assert re.search(r"@[0-9a-f]{40}$", action), f"Action is not pinned to a full SHA: {path}: {action}"
    jobs_block = content.split("jobs:", 1)[1]
    job_headers = list(re.finditer(r"^  ([a-zA-Z0-9_-]+):\s*$", jobs_block, re.M))
    for index, match in enumerate(job_headers):
        end = job_headers[index + 1].start() if index + 1 < len(job_headers) else len(jobs_block)
        job_body = jobs_block[match.end():end]
        assert re.search(r"^    permissions:", job_body, re.M), f"Job lacks explicit permissions: {path}:{match.group(1)}"

print("PocketForge direct-delivery checks passed: foundation contracts, native workspace, local persistence, Git-native CI, SHA-pinned actions, per-job permissions, no ZIP ingestion or HTML.")
