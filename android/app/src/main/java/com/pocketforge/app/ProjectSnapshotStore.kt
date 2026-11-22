package com.pocketforge.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class StoredProjectSnapshot(
    val snapshotId: String,
    val projectId: String,
    val projectName: String,
    val directoryPath: String,
    val manifestPath: String,
    val manifestSha256: String,
    val fileCount: Int,
    val totalBytes: Long,
    val createdAt: Long,
    val sourceUriSha256: String,
    val sourceRootIdSha256: String,
    val sourceDisplayName: String,
    val sourceLastModified: Long?
)

/** Persists completed snapshots as project-linked records; only app-owned directories can be deleted. */
class ProjectSnapshotStore(context: Context) {
    private val appFiles = context.filesDir.canonicalFile
    private val prefs = context.getSharedPreferences("pocketforge.snapshots.v1", Context.MODE_PRIVATE)

    @Synchronized
    fun all(): List<StoredProjectSnapshot> = read().sortedByDescending { it.createdAt }

    @Synchronized
    fun forProject(projectId: String): List<StoredProjectSnapshot> =
        all().filter { it.projectId == projectId }

    @Synchronized
    fun add(snapshot: ProjectSnapshot, projectId: String, projectName: String): StoredProjectSnapshot {
        require(projectId.isNotBlank()) { "Snapshot requires a project ID." }
        require(snapshot.directory.isDirectory && snapshot.manifestFile.isFile) { "Snapshot files are missing." }
        val directory = snapshot.directory.canonicalFile
        require(directory.parentFile == File(appFiles, SNAPSHOT_DIRECTORY).canonicalFile) {
            "Snapshot directory is outside PocketForge-owned storage."
        }
        val stored = StoredProjectSnapshot(
            snapshotId = snapshot.id,
            projectId = projectId,
            projectName = projectName,
            directoryPath = directory.path,
            manifestPath = snapshot.manifestFile.canonicalPath,
            manifestSha256 = snapshot.manifestSha256,
            fileCount = snapshot.fileCount,
            totalBytes = snapshot.totalBytes,
            createdAt = snapshot.createdAt,
            sourceUriSha256 = snapshot.sourceUriSha256,
            sourceRootIdSha256 = snapshot.sourceRootIdSha256,
            sourceDisplayName = snapshot.sourceDisplayName,
            sourceLastModified = snapshot.sourceLastModified
        )
        write(read() + stored)
        return stored
    }

    @Synchronized
    fun remove(snapshotId: String): Boolean {
        val records = read()
        val target = records.firstOrNull { it.snapshotId == snapshotId } ?: return false
        val directory = File(target.directoryPath).canonicalFile
        val allowedParent = File(appFiles, SNAPSHOT_DIRECTORY).canonicalFile
        require(directory.parentFile == allowedParent) { "Refusing to delete a directory outside snapshot storage." }
        directory.deleteRecursively()
        write(records.filterNot { it.snapshotId == snapshotId })
        return true
    }

    private fun read(): List<StoredProjectSnapshot> {
        val array = runCatching { JSONArray(prefs.getString(KEY_SNAPSHOTS, "[]")) }.getOrElse { JSONArray() }
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val directoryPath = item.optString("directoryPath")
                val manifestPath = item.optString("manifestPath")
                if (directoryPath.isBlank() || manifestPath.isBlank()) continue
                add(StoredProjectSnapshot(
                    snapshotId = item.optString("snapshotId"),
                    projectId = item.optString("projectId"),
                    projectName = item.optString("projectName", "Project"),
                    directoryPath = directoryPath,
                    manifestPath = manifestPath,
                    manifestSha256 = item.optString("manifestSha256"),
                    fileCount = item.optInt("fileCount"),
                    totalBytes = item.optLong("totalBytes"),
                    createdAt = item.optLong("createdAt"),
                    sourceUriSha256 = item.optString("sourceUriSha256"),
                    sourceRootIdSha256 = item.optString("sourceRootIdSha256"),
                    sourceDisplayName = item.optString("sourceDisplayName", "Linked project folder"),
                    sourceLastModified = if (item.has("sourceLastModified") && !item.isNull("sourceLastModified")) item.optLong("sourceLastModified").takeIf { it > 0L } else null
                ))
            }
        }
    }

    private fun write(records: List<StoredProjectSnapshot>) {
        val array = JSONArray()
        records.forEach { item ->
            array.put(JSONObject().apply {
                put("snapshotId", item.snapshotId)
                put("projectId", item.projectId)
                put("projectName", item.projectName)
                put("directoryPath", item.directoryPath)
                put("manifestPath", item.manifestPath)
                put("manifestSha256", item.manifestSha256)
                put("fileCount", item.fileCount)
                put("totalBytes", item.totalBytes)
                put("createdAt", item.createdAt)
                put("sourceUriSha256", item.sourceUriSha256)
                put("sourceRootIdSha256", item.sourceRootIdSha256)
                put("sourceDisplayName", item.sourceDisplayName)
                if (item.sourceLastModified == null) put("sourceLastModified", JSONObject.NULL)
                else put("sourceLastModified", item.sourceLastModified)
            })
        }
        check(prefs.edit().putString(KEY_SNAPSHOTS, array.toString()).commit()) {
            "Could not persist snapshot index."
        }
    }

    companion object {
        private const val KEY_SNAPSHOTS = "snapshots"
        private const val SNAPSHOT_DIRECTORY = "build-workspaces"
    }
}
