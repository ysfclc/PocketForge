package com.pocketforge.app

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

data class ProjectSnapshot(
    val id: String,
    val directory: File,
    val fileCount: Int,
    val totalBytes: Long,
    val skippedEntries: List<String>,
    val manifestFile: File,
    val manifestSha256: String,
    val createdAt: Long,
    val sourceUriSha256: String,
    val sourceRootIdSha256: String,
    val sourceDisplayName: String,
    val sourceLastModified: Long?
)

data class SnapshotLimits(
    val maxEntries: Int = 10_000,
    val maxDepth: Int = 32,
    val maxTotalBytes: Long = 512L * 1024L * 1024L,
    val maxFileBytes: Long = 64L * 1024L * 1024L
) {
    init {
        require(maxEntries > 0 && maxDepth > 0)
        require(maxTotalBytes > 0 && maxFileBytes > 0 && maxFileBytes <= maxTotalBytes)
    }
}

class SnapshotCancelledException : IllegalStateException("Project snapshot was cancelled.")

/**
 * Copies an explicitly linked SAF project into app-private storage.
 * It never executes project code or writes back to the source tree.
 */
class ProjectSnapshotter(
    private val context: Context,
    private val limits: SnapshotLimits = SnapshotLimits()
) {
    private data class Node(val id: String, val name: String, val mime: String) {
        val isDirectory: Boolean get() = mime == DocumentsContract.Document.MIME_TYPE_DIR
    }

    private data class FileRecord(val path: String, val bytes: Long, val sha256: String) 
    private data class Progress(var entries: Int = 0, var files: Int = 0, var bytes: Long = 0, val records: MutableList<FileRecord> = mutableListOf())

    fun create(
        treeUri: Uri,
        isCancelled: () -> Boolean = { false },
        onProgress: (files: Int, bytes: Long) -> Unit = { _, _ -> }
    ): ProjectSnapshot {
        requireReadPermission(treeUri)
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val id = UUID.randomUUID().toString()
        val parent = File(context.filesDir, SNAPSHOT_DIRECTORY)
        check(parent.exists() || parent.mkdirs()) { "Cannot create private snapshot storage." }
        val staging = File(parent, ".$id.partial")
        val destination = File(parent, id)
        check(staging.mkdir()) { "Cannot create snapshot staging directory." }
        val progress = Progress()
        val skipped = mutableListOf<String>()
        try {
            val sourceIdentity = readSourceIdentity(treeUri, rootId)
            copyDirectory(treeUri, rootId, staging, "", 0, progress, skipped, isCancelled, onProgress)
            check(!isCancelled()) { throw SnapshotCancelledException() }
            val manifest = JSONObject().apply {
                put("schemaVersion", 1)
                put("snapshotId", id)
                put("createdAt", System.currentTimeMillis())
                put("sourceIdentity", JSONObject().apply {
                    put("treeUriSha256", sourceIdentity.treeUriSha256)
                    put("rootDocumentIdSha256", sourceIdentity.rootDocumentIdSha256)
                    put("displayName", sourceIdentity.displayName)
                    if (sourceIdentity.lastModified == null) put("lastModified", JSONObject.NULL)
                    else put("lastModified", sourceIdentity.lastModified)
                })
                put("fileCount", progress.files)
                put("totalBytes", progress.bytes)
                put("skippedEntries", JSONArray(skipped))
                put("files", JSONArray().apply {
                    progress.records.sortedBy { it.path }.forEach { record ->
                        put(JSONObject().apply {
                            put("path", record.path)
                            put("bytes", record.bytes)
                            put("sha256", record.sha256)
                        })
                    }
                })
            }
            val manifestFile = File(staging, MANIFEST_NAME)
            val manifestBytes = manifest.toString(2).toByteArray(Charsets.UTF_8)
            FileOutputStream(manifestFile).use { output ->
                output.write(manifestBytes)
                output.fd.sync()
            }
            val manifestSha = sha256(manifestBytes)
            check(staging.renameTo(destination)) { "Could not finalize project snapshot." }
            return ProjectSnapshot(
                id, destination, progress.files, progress.bytes, skipped.toList(),
                File(destination, MANIFEST_NAME), manifestSha, System.currentTimeMillis(),
                sourceIdentity.treeUriSha256, sourceIdentity.rootDocumentIdSha256,
                sourceIdentity.displayName, sourceIdentity.lastModified
            )
        } catch (error: Throwable) {
            staging.deleteRecursively()
            destination.deleteRecursively()
            throw error
        }
    }

    private fun copyDirectory(
        treeUri: Uri,
        parentId: String,
        destination: File,
        relativePath: String,
        depth: Int,
        progress: Progress,
        skipped: MutableList<String>,
        isCancelled: () -> Boolean,
        onProgress: (Int, Long) -> Unit
    ) {
        check(!isCancelled()) { throw SnapshotCancelledException() }
        if (depth > limits.maxDepth) throw IllegalStateException("Snapshot depth limit exceeded at $relativePath")
        val children = listChildren(treeUri, parentId)
        val names = HashSet<String>()
        for (node in children) {
            check(!isCancelled()) { throw SnapshotCancelledException() }
            progress.entries++
            if (progress.entries > limits.maxEntries) throw IllegalStateException("Snapshot entry limit exceeded.")
            val name = safeName(node.name)
            val path = if (relativePath.isEmpty()) name else "$relativePath/$name"
            if (!names.add(name)) throw IllegalStateException("Duplicate source entry name: $path")
            if (node.name in EXCLUDED_DIRECTORIES && node.isDirectory) {
                skipped += path
                continue
            }
            val target = File(destination, name)
            check(target.canonicalFile.parentFile == destination.canonicalFile) { "Unsafe snapshot path: $path" }
            if (node.isDirectory) {
                check(target.mkdir()) { "Cannot create snapshot directory: $path" }
                copyDirectory(treeUri, node.id, target, path, depth + 1, progress, skipped, isCancelled, onProgress)
            } else {
                copyFile(treeUri, node, target, path, progress, isCancelled)
                onProgress(progress.files, progress.bytes)
            }
        }
    }

    private fun copyFile(
        treeUri: Uri,
        node: Node,
        target: File,
        path: String,
        progress: Progress,
        isCancelled: () -> Boolean
    ) {
        val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, node.id)
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open source file: $path")
        input.use { source ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                val digest = MessageDigest.getInstance("SHA-256")
                var fileBytes = 0L
                while (true) {
                    check(!isCancelled()) { throw SnapshotCancelledException() }
                    val count = source.read(buffer)
                    if (count < 0) break
                    fileBytes += count
                    if (fileBytes > limits.maxFileBytes) throw IllegalStateException("File exceeds snapshot limit: $path")
                    if (progress.bytes + count > limits.maxTotalBytes) throw IllegalStateException("Snapshot exceeds total size limit.")
                    output.write(buffer, 0, count)
                    digest.update(buffer, 0, count)
                    progress.bytes += count
                }
                output.fd.sync()
                progress.files++
                val fileHash = digest.digest().joinToString("") { "%02x".format(it) }
                progress.records += FileRecord(path, fileBytes, fileHash)
            }
        }
    }

    private fun listChildren(treeUri: Uri, parentId: String): List<Node> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        val result = ArrayList<Node>()
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            while (cursor.moveToNext()) {
                if (result.size >= limits.maxEntries) {
                    throw IllegalStateException("Snapshot entry limit exceeded.")
                }
                result += Node(
                    cursor.getString(idColumn),
                    cursor.getString(nameColumn) ?: "Unnamed item",
                    cursor.getString(mimeColumn) ?: "application/octet-stream"
                )
            }
        } ?: throw IllegalStateException("Android could not read a project directory.")
        return result
    }

    private data class SourceIdentity(
        val treeUriSha256: String,
        val rootDocumentIdSha256: String,
        val displayName: String,
        val lastModified: Long?
    )

    private fun readSourceIdentity(treeUri: Uri, rootId: String): SourceIdentity {
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootId)
        var displayName = "Linked project folder"
        var lastModified: Long? = null
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
        context.contentResolver.query(documentUri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                if (nameIndex >= 0) displayName = cursor.getString(nameIndex) ?: displayName
                if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) {
                    lastModified = cursor.getLong(modifiedIndex).takeIf { it > 0L }
                }
            }
        }
        return SourceIdentity(
            treeUriSha256 = sha256(treeUri.toString().toByteArray(Charsets.UTF_8)),
            rootDocumentIdSha256 = sha256(rootId.toByteArray(Charsets.UTF_8)),
            displayName = displayName,
            lastModified = lastModified
        )
    }

    private fun requireReadPermission(treeUri: Uri) {
        val allowed = context.contentResolver.persistedUriPermissions.any {
            it.uri == treeUri && it.isReadPermission
        }
        if (!allowed) throw SecurityException("Folder access expired. Re-link this project before snapshotting it.")
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256") 
        .digest(bytes).joinToString("") { "%02x".format(it) } 
 
    private fun safeName(raw: String): String {
        val name = raw.trim()
        require(name.isNotEmpty() && name != "." && name != "..") { "Invalid source entry name." }
        require('/' !in name && '\\' !in name && !name.contains('\u0000')) { "Unsafe source entry name." }
        return name
    }

    companion object {
        private const val SNAPSHOT_DIRECTORY = "build-workspaces" 
        private const val MANIFEST_NAME = "snapshot-manifest.json"
        private val EXCLUDED_DIRECTORIES = setOf(".git", ".gradle", "build", ".idea", ".kotlin", "node_modules")
    }
}
