package com.pocketforge.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class WorkspaceProject(
    val id: String,
    val name: String,
    val description: String,
    val folderUri: String?,
    val createdAt: Long,
    val sourceKind: String = SOURCE_LOCAL,
    val githubRepository: String? = null,
    val githubDefaultBranch: String? = null,
    val githubRevision: String? = null
) {
    companion object {
        const val SOURCE_LOCAL = "local"
        const val SOURCE_GITHUB = "github"
    }
}

data class WorkspaceMission(
    val id: String,
    val title: String,
    val brief: String,
    val projectId: String?,
    val state: String,
    val createdAt: Long
)

/** Small, versioned local store. Project and mission records stay on-device. */
class WorkspaceStore(context: Context) {
    private val prefs = context.getSharedPreferences("pocketforge.workspace.v1", Context.MODE_PRIVATE)

    fun projects(): List<WorkspaceProject> {
        val array = readArray(KEY_PROJECTS)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                add(
                    WorkspaceProject(
                        id = item.optString("id"),
                        name = item.optString("name", "Untitled project"),
                        description = item.optString("description"),
                        folderUri = item.optString("folderUri").takeIf { it.isNotBlank() },
                        createdAt = item.optLong("createdAt"),
                        sourceKind = item.optString("sourceKind", WorkspaceProject.SOURCE_LOCAL),
                        githubRepository = item.optString("githubRepository").takeIf { it.isNotBlank() },
                        githubDefaultBranch = item.optString("githubDefaultBranch").takeIf { it.isNotBlank() },
                        githubRevision = item.optString("githubRevision").takeIf { it.isNotBlank() }
                    )
                )
            }
        }.sortedByDescending { it.createdAt }
    }

    fun missions(): List<WorkspaceMission> {
        val array = readArray(KEY_MISSIONS)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                add(
                    WorkspaceMission(
                        id = item.optString("id"),
                        title = item.optString("title", "Untitled mission"),
                        brief = item.optString("brief"),
                        projectId = item.optString("projectId").takeIf { it.isNotBlank() },
                        state = item.optString("state", "Draft"),
                        createdAt = item.optLong("createdAt")
                    )
                )
            }
        }.sortedByDescending { it.createdAt }
    }

    fun addProject(name: String, description: String, folderUri: String? = null): WorkspaceProject {
        val existing = folderUri?.let { uri -> projects().firstOrNull { it.folderUri == uri } }
        if (existing != null) return existing
        val project = WorkspaceProject(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifBlank { "Untitled project" },
            description = description.trim(),
            folderUri = folderUri,
            createdAt = System.currentTimeMillis()
        )
        persistProject(project)
        return project
    }

    fun addGitHubProject(repository: GitHubRepository): WorkspaceProject {
        projects().firstOrNull {
            it.sourceKind == WorkspaceProject.SOURCE_GITHUB &&
                it.githubRepository.equals(repository.fullName, ignoreCase = true)
        }?.let { existing ->
            val refreshed = existing.copy(
                name = repository.name,
                description = repository.description,
                githubDefaultBranch = repository.defaultBranch,
                githubRevision = repository.defaultBranchSha ?: existing.githubRevision
            )
            replaceProject(refreshed)
            return refreshed
        }
        val project = WorkspaceProject(
            id = UUID.randomUUID().toString(),
            name = repository.name,
            description = repository.description,
            folderUri = null,
            createdAt = System.currentTimeMillis(),
            sourceKind = WorkspaceProject.SOURCE_GITHUB,
            githubRepository = repository.fullName,
            githubDefaultBranch = repository.defaultBranch,
            githubRevision = repository.defaultBranchSha
        )
        persistProject(project)
        return project
    }

    fun addMission(title: String, brief: String, projectId: String?): WorkspaceMission {
        val mission = WorkspaceMission(
            id = UUID.randomUUID().toString(),
            title = title.trim().ifBlank { "Untitled mission" },
            brief = brief.trim(),
            projectId = projectId,
            state = "Draft",
            createdAt = System.currentTimeMillis()
        )
        val array = readArray(KEY_MISSIONS)
        array.put(mission.toJson())
        prefs.edit().putString(KEY_MISSIONS, array.toString()).apply()
        return mission
    }

    fun setMissionState(id: String, state: String) {
        val array = readArray(KEY_MISSIONS)
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            if (item.optString("id") == id) item.put("state", state)
        }
        prefs.edit().putString(KEY_MISSIONS, array.toString()).apply()
    }

    private fun persistProject(project: WorkspaceProject) {
        val array = readArray(KEY_PROJECTS)
        array.put(project.toJson())
        prefs.edit().putString(KEY_PROJECTS, array.toString()).apply()
    }

    private fun replaceProject(project: WorkspaceProject) {
        val array = readArray(KEY_PROJECTS)
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            if (item.optString("id") == project.id) {
                array.put(i, project.toJson())
                break
            }
        }
        prefs.edit().putString(KEY_PROJECTS, array.toString()).apply()
    }

    private fun readArray(key: String): JSONArray =
        runCatching { JSONArray(prefs.getString(key, "[]")) }.getOrElse { JSONArray() }

    private fun WorkspaceProject.toJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("description", description)
        put("folderUri", folderUri ?: "")
        put("createdAt", createdAt)
        put("sourceKind", sourceKind)
        put("githubRepository", githubRepository ?: "")
        put("githubDefaultBranch", githubDefaultBranch ?: "")
        put("githubRevision", githubRevision ?: "")
    }

    private fun WorkspaceMission.toJson() = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("brief", brief)
        put("projectId", projectId ?: "")
        put("state", state)
        put("createdAt", createdAt)
    }

    private companion object {
        const val KEY_PROJECTS = "projects"
        const val KEY_MISSIONS = "missions"
    }
}
