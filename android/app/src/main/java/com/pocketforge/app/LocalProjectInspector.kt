package com.pocketforge.app

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.database.Cursor

data class LocalProjectEntry(
    val documentId: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long?
) {
    val isDirectory: Boolean
        get() = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
}

data class LocalProjectDirectory(
    val entries: List<LocalProjectEntry>,
    val totalEntries: Int,
    val truncated: Boolean
)

data class LocalProjectSnapshot(
    val rootDocumentId: String,
    val rootName: String,
    val readOnly: Boolean,
    val directory: LocalProjectDirectory,
    val detectedProjectType: String
)

/**
 * Inspects folders granted through Android's Storage Access Framework.
 *
 * The original files are never copied or uploaded. Every query remains scoped to
 * the persisted tree URI selected by the user.
 */
object LocalProjectInspector {
    private const val MAX_DIRECTORY_ENTRIES = 200

    fun inspect(context: Context, treeUri: Uri): LocalProjectSnapshot {
        val permission = context.contentResolver.persistedUriPermissions
            .firstOrNull { it.uri == treeUri && it.isReadPermission }
            ?: throw SecurityException("Folder access expired. Re-link this folder to continue.")

        val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocumentId)
        val rootName = queryDisplayName(context, rootUri)
            ?: rootDocumentId.substringAfterLast('/').ifBlank { "Local project" }
        val directory = listChildren(context, treeUri, rootDocumentId)
        return LocalProjectSnapshot(
            rootDocumentId = rootDocumentId,
            rootName = rootName,
            readOnly = !permission.isWritePermission,
            directory = directory,
            detectedProjectType = detectProjectType(directory.entries)
        )
    }

    fun listChildren(context: Context, treeUri: Uri, parentDocumentId: String): LocalProjectDirectory {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )
        val entries = ArrayList<LocalProjectEntry>()
        var totalEntries = 0
        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            totalEntries = cursor.count
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
            while (cursor.moveToNext() && entries.size < MAX_DIRECTORY_ENTRIES) {
                entries.add(
                    LocalProjectEntry(
                        documentId = cursor.getString(idColumn),
                        name = cursor.getString(nameColumn) ?: "Unnamed item",
                        mimeType = cursor.getString(mimeColumn) ?: "application/octet-stream",
                        sizeBytes = if (cursor.isNull(sizeColumn)) null else cursor.getLong(sizeColumn)
                    )
                )
            }
        } ?: throw IllegalStateException("Android could not read this folder. Check the folder permission and try again.")

        return LocalProjectDirectory(
            entries = entries.sortedWith(compareBy<LocalProjectEntry> { !it.isDirectory }.thenBy { it.name.lowercase() }),
            totalEntries = totalEntries,
            truncated = totalEntries > entries.size
        )
    }

    fun documentUri(treeUri: Uri, documentId: String): Uri =
        DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)

    private fun queryDisplayName(context: Context, documentUri: Uri): String? {
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        return context.contentResolver.query(documentUri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    private fun detectProjectType(entries: List<LocalProjectEntry>): String {
        val names = entries.map { it.name.lowercase() }.toSet()
        return when {
            "pubspec.yaml" in names -> "Flutter / Dart project"
            "cargo.toml" in names -> "Rust project"
            "package.json" in names -> "Node.js project"
            "pyproject.toml" in names || "requirements.txt" in names -> "Python project"
            "pom.xml" in names -> "Maven / Java project"
            "package.swift" in names -> "Swift package"
            "settings.gradle" in names || "settings.gradle.kts" in names ||
                "build.gradle" in names || "build.gradle.kts" in names ->
                if ("app" in names) "Android / Gradle project" else "Gradle project"
            else -> "Project type not identified yet"
        }
    }
}
