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
    val createdAt: Long
)

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
                        createdAt = item.optLong("createdAt")
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
        val array = readArray(KEY_PROJECTS)
        array.put(project.toJson())
        prefs.edit().putString(KEY_PROJECTS, array.toString()).apply()
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

    private fun readArray(key: String): JSONArray =
        runCatching { JSONArray(prefs.getString(key, "[]")) }.getOrElse { JSONArray() }

    private fun WorkspaceProject.toJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("description", description)
        put("folderUri", folderUri ?: "")
        put("createdAt", createdAt)
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
