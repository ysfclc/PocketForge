package com.pocketforge.app

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.ByteArrayOutputStream

data class BuildAssessment(
    val projectType: String,
    val buildSystem: String,
    val rootEntries: List<String>,
    val modules: List<String>,
    val hasGradleWrapperScript: Boolean,
    val hasGradleWrapperJar: Boolean,
    val gradleDistribution: String?,
    val hasAndroidManifest: Boolean,
    val hasAndroidPlugin: Boolean,
    val hasRustSources: Boolean,
    val scanTruncated: Boolean,
    val findings: List<String>,
    val blockers: List<String>
) {
    val canStartBuild: Boolean get() = false
}

/**
 * Read-only project preflight over the SAF tree selected by the user.
 * It never copies, edits, executes or uploads project files.
 */
object BuildPreflight {
    private const val MAX_DEPTH = 5
    private const val MAX_ENTRIES = 500
    private const val MAX_TEXT_BYTES = 128 * 1024

    private data class Node(val id: String, val name: String, val mime: String) {
        val directory: Boolean get() = mime == DocumentsContract.Document.MIME_TYPE_DIR
    }

    fun assess(context: Context, treeUri: Uri): BuildAssessment {
        requirePersistedReadPermission(context, treeUri)
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val root = children(context, treeUri, rootId)
        val files = LinkedHashMap<String, Node>()
        var visited = 0
        var truncated = false

        fun walk(parentId: String, prefix: String, depth: Int) {
            if (depth > MAX_DEPTH || visited >= MAX_ENTRIES) {
                truncated = true
                return
            }
            val entries = children(context, treeUri, parentId)
            for (node in entries) {
                if (visited >= MAX_ENTRIES) {
                    truncated = true
                    return
                }
                visited++
                val path = if (prefix.isEmpty()) node.name else "$prefix/${node.name}"
                files[path] = node
                if (node.directory && node.name !in setOf(".git", ".gradle", "build", "node_modules", ".idea")) {
                    walk(node.id, path, depth + 1)
                }
            }
        }
        walk(rootId, "", 0)

        val names = files.keys.map { it.lowercase() }.toSet()
        val rootNames = root.map { it.name }
        val settings = findPath(files.keys, "settings.gradle", "settings.gradle.kts")
        val rootBuild = findPath(files.keys, "build.gradle", "build.gradle.kts")
        val wrapperProperties = files.keys.firstOrNull { it.equals("gradle/wrapper/gradle-wrapper.properties", true) }
        val wrapperJar = files.keys.firstOrNull { it.equals("gradle/wrapper/gradle-wrapper.jar", true) }
        val wrapperScript = files.keys.firstOrNull { it.equals("gradlew", true) }
        val manifests = files.keys.filter { it.substringAfterLast('/').equals("AndroidManifest.xml", true) }
        val moduleBuildFiles = files.keys.filter {
            val leaf = it.substringAfterLast('/')
            leaf.equals("build.gradle", true) || leaf.equals("build.gradle.kts", true)
        }
        val gradleBuildText = (listOfNotNull(rootBuild, settings) + moduleBuildFiles.take(12))
            .distinct()
            .mapNotNull { path -> readText(context, treeUri, files[path]?.id)?.let { path to it } }
        val hasAndroidPlugin = gradleBuildText.any { (_, text) ->
            text.contains("com.android.application") || text.contains("com.android.library") ||
                text.contains("com.android.tools.build:gradle")
        }
        val hasRust = names.any { it.endsWith("/cargo.toml") || it == "cargo.toml" }
        val gradlePropertiesText = wrapperProperties?.let { readText(context, treeUri, files[it]?.id) }
        val distribution = gradlePropertiesText
            ?.lineSequence()
            ?.firstOrNull { it.trim().startsWith("distributionUrl=") }
            ?.substringAfter('=')
            ?.trim()
            ?.replace("\\:", ":")
        val modules = moduleBuildFiles.map { it.substringBeforeLast('/') }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        val isAndroid = manifests.isNotEmpty() || hasAndroidPlugin
        val findings = mutableListOf<String>()
        val blockers = mutableListOf<String>()
        if (settings != null) findings += "Gradle settings found: $settings"
        if (rootBuild != null) findings += "Root Gradle build file found: $rootBuild"
        if (modules.isNotEmpty()) findings += "Gradle modules detected: ${modules.joinToString()}"
        if (manifests.isNotEmpty()) findings += "Android manifest(s): ${manifests.size}"
        if (hasAndroidPlugin) findings += "Android Gradle plugin declaration detected"
        if (hasRust) findings += "Cargo manifest detected; native Rust build steps may be required"
        if (wrapperScript != null && wrapperJar != null && wrapperProperties != null) {
            findings += "Complete Gradle wrapper files are present"
        } else {
            if (wrapperScript == null) blockers += "gradlew wrapper script is missing"
            if (wrapperJar == null) blockers += "Gradle wrapper JAR is missing"
            if (wrapperProperties == null) blockers += "gradle/wrapper/gradle-wrapper.properties is missing"
        }
        if (!isAndroid) blockers += "No Android Gradle plugin or AndroidManifest.xml was detected"
        if (settings == null && rootBuild == null) blockers += "No Gradle settings or root build file was detected"
        if (truncated) blockers += "Project scan reached its safety limit; nested modules may not have been inspected"

        val buildSystem = when {
            isAndroid && hasRust -> "Android Gradle + Rust/NDK"
            isAndroid -> "Android Gradle"
            hasRust -> "Rust / Cargo"
            settings != null || rootBuild != null -> "Gradle (platform not confirmed)"
            else -> "Unknown"
        }
        return BuildAssessment(
            projectType = if (isAndroid) "Android project" else if (hasRust) "Rust project" else "Unclassified project",
            buildSystem = buildSystem,
            rootEntries = rootNames,
            modules = modules,
            hasGradleWrapperScript = wrapperScript != null,
            hasGradleWrapperJar = wrapperJar != null,
            gradleDistribution = distribution,
            hasAndroidManifest = manifests.isNotEmpty(),
            hasAndroidPlugin = hasAndroidPlugin,
            hasRustSources = hasRust,
            scanTruncated = truncated,
            findings = findings,
            blockers = blockers
        )
    }

    private fun requirePersistedReadPermission(context: Context, treeUri: Uri) {
        val allowed = context.contentResolver.persistedUriPermissions.any {
            it.uri == treeUri && it.isReadPermission
        }
        if (!allowed) throw SecurityException("Folder access expired. Re-link this project before assessing it.")
    }

    private fun children(context: Context, treeUri: Uri, parentId: String): List<Node> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        val result = ArrayList<Node>()
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val name = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mime = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            while (cursor.moveToNext()) {
                result += Node(
                    cursor.getString(id),
                    cursor.getString(name) ?: "Unnamed item",
                    cursor.getString(mime) ?: "application/octet-stream"
                )
            }
        } ?: throw IllegalStateException("Android could not read a project directory.")
        return result
    }

    private fun readText(context: Context, treeUri: Uri, documentId: String?): String? {
        if (documentId == null) return null
        val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_TEXT_BYTES) return@use null
                    output.write(buffer, 0, count)
                }
                output.toString(Charsets.UTF_8.name())
            }
        }.getOrNull()
    }

    private fun findPath(paths: Set<String>, vararg names: String): String? =
        paths.firstOrNull { path -> names.any { path.equals(it, true) } }
}
