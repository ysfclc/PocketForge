#!/usr/bin/env python3
"""Static repository-contract checks; no third-party Python packages required."""
from pathlib import Path
import re

required = [
    "README.md", "AGENTS.md", "PRINCIPLES.md", "SECURITY.md", "CONTRIBUTING.md",
    "CODEOWNERS", "docs/product/vision.md", "docs/product/personas.md",
    "docs/product/scope.md", "docs/product/roadmap.md",
    "docs/architecture/overview.md", "docs/architecture/data-model.md",
    "docs/architecture/ffi-contract.md", "core/Cargo.toml",
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
):
    assert contract in github_client, f"GitHub repository adapter missing contract: {contract}"
for contract in (
    "AndroidKeyStore", "AES/GCM/NoPadding", "SHA-256",
    "preferenceKey(repository)", "fun clear(repository: String)",
):
    assert contract in github_vault, f"GitHub credential vault missing security contract: {contract}"
for contract in ("SOURCE_GITHUB", "githubRepository", "githubDefaultBranch", "githubRevision"):
    assert contract in store, f"Workspace source model missing GitHub field: {contract}"
manifest = Path("android/app/src/main/AndroidManifest.xml").read_text()
assert "android.permission.INTERNET" in manifest, "GitHub API requires INTERNET permission"
assert r"\${" not in app, "Kotlin interpolation is accidentally escaped"
assert r"\\n" not in app, "Kotlin dialog newlines are double-escaped"

pipeline = Path(".github/workflows/pipeline.yml").read_text()
for contract in (
    "pull_request:", "push:", "branches: [main]", "workflow_dispatch:",
    "scripts/verify-foundation.sh", "scripts/build-rust.sh",
    "cargo +1.82.0 test", "assembleDebug", "apksigner",
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
