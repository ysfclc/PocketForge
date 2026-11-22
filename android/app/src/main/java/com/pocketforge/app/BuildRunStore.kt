package com.pocketforge.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Persisted, auditable build lifecycle. This store does not execute project code. */
data class BuildRun(
    val id: String,
    val projectId: String,
    val projectName: String,
    val task: String,
    val state: String,
    val createdAt: Long,
    val updatedAt: Long,
    val startedAt: Long?,
    val finishedAt: Long?,
    val exitCode: Int?,
    val logReference: String?,
    val outputArtifacts: List<String>,
    val failureReason: String?
)

class BuildRunStore(context: Context) {
    private val prefs = context.getSharedPreferences("pocketforge.build-runs.v1", Context.MODE_PRIVATE)

    @Synchronized
    fun all(): List<BuildRun> = readRuns().sortedByDescending { it.createdAt }

    @Synchronized
    fun get(id: String): BuildRun? = readRuns().firstOrNull { it.id == id }

    @Synchronized
    fun create(projectId: String, projectName: String, task: String = "assembleDebug"): BuildRun {
        require(projectId.isNotBlank()) { "Build run requires a project ID." }
        require(task.isNotBlank()) { "Build run requires a task." }
        val now = System.currentTimeMillis()
        val run = BuildRun(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            projectName = projectName.ifBlank { "Unnamed project" },
            task = task.trim(),
            state = State.QUEUED,
            createdAt = now,
            updatedAt = now,
            startedAt = null,
            finishedAt = null,
            exitCode = null,
            logReference = null,
            outputArtifacts = emptyList(),
            failureReason = null
        )
        write(readRuns() + run)
        return run
    }

    /** Enforces legal lifecycle edges; terminal runs cannot be restarted or rewritten. */
    @Synchronized
    fun transition(id: String, nextState: String, exitCode: Int? = null, logReference: String? = null,
                  artifacts: List<String>? = null, failureReason: String? = null): BuildRun {
        require(nextState in State.ALL) { "Unknown build state: $nextState" }
        val runs = readRuns()
        val current = runs.firstOrNull { it.id == id } ?: error("Build run not found: $id")
        require(nextState in State.transitions[current.state].orEmpty()) {
            "Illegal build transition: ${current.state} -> $nextState"
        }
        val now = System.currentTimeMillis()
        val updated = current.copy(
            state = nextState,
            updatedAt = now,
            startedAt = current.startedAt ?: if (nextState == State.RUNNING) now else null,
            finishedAt = if (nextState in State.TERMINAL) now else null,
            exitCode = exitCode ?: current.exitCode,
            logReference = logReference ?: current.logReference,
            outputArtifacts = artifacts ?: current.outputArtifacts,
            failureReason = failureReason ?: current.failureReason
        )
        write(runs.map { if (it.id == id) updated else it })
        return updated
    }

    private fun readRuns(): List<BuildRun> {
        val array = runCatching { JSONArray(prefs.getString(KEY_RUNS, "[]")) }.getOrElse { JSONArray() }
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val artifacts = item.optJSONArray("outputArtifacts") ?: JSONArray()
                add(BuildRun(
                    id = item.optString("id"),
                    projectId = item.optString("projectId"),
                    projectName = item.optString("projectName", "Unnamed project"),
                    task = item.optString("task", "assembleDebug"),
                    state = item.optString("state", State.QUEUED).takeIf { it in State.ALL } ?: State.INTERRUPTED,
                    createdAt = item.optLong("createdAt"),
                    updatedAt = item.optLong("updatedAt"),
                    startedAt = item.optLong("startedAt").takeIf { it > 0 },
                    finishedAt = item.optLong("finishedAt").takeIf { it > 0 },
                    exitCode = if (item.has("exitCode") && !item.isNull("exitCode")) item.optInt("exitCode") else null,
                    logReference = item.optString("logReference").takeIf { it.isNotBlank() },
                    outputArtifacts = buildList { for (i in 0 until artifacts.length()) { val artifact = artifacts.optString(i); if (artifact.isNotBlank()) add(artifact) } },
                    failureReason = item.optString("failureReason").takeIf { it.isNotBlank() }
                ))
            }
        }
    }

    private fun write(runs: List<BuildRun>) {
        val array = JSONArray()
        runs.forEach { run ->
            array.put(JSONObject().apply {
                put("id", run.id)
                put("projectId", run.projectId)
                put("projectName", run.projectName)
                put("task", run.task)
                put("state", run.state)
                put("createdAt", run.createdAt)
                put("updatedAt", run.updatedAt)
                put("startedAt", run.startedAt ?: 0)
                put("finishedAt", run.finishedAt ?: 0)
                if (run.exitCode == null) put("exitCode", JSONObject.NULL) else put("exitCode", run.exitCode)
                put("logReference", run.logReference ?: "")
                put("outputArtifacts", JSONArray(run.outputArtifacts))
                put("failureReason", run.failureReason ?: "")
            })
        }
        prefs.edit().putString(KEY_RUNS, array.toString()).commit()
    }

    private object State {
        const val QUEUED = "queued"
        const val PREPARING = "preparing"
        const val RUNNING = "running"
        const val CANCELLING = "cancelling"
        const val SUCCEEDED = "succeeded"
        const val FAILED = "failed"
        const val CANCELLED = "cancelled"
        const val INTERRUPTED = "interrupted"
        val ALL = setOf(QUEUED, PREPARING, RUNNING, CANCELLING, SUCCEEDED, FAILED, CANCELLED, INTERRUPTED)
        val TERMINAL = setOf(SUCCEEDED, FAILED, CANCELLED, INTERRUPTED)
        val transitions = mapOf(
            QUEUED to setOf(PREPARING, CANCELLED, INTERRUPTED),
            PREPARING to setOf(RUNNING, FAILED, CANCELLED, INTERRUPTED),
            RUNNING to setOf(CANCELLING, SUCCEEDED, FAILED, INTERRUPTED),
            CANCELLING to setOf(CANCELLED, FAILED, INTERRUPTED)
        )
    }

    private companion object { const val KEY_RUNS = "buildRuns" }
}
