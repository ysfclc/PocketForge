package com.pocketforge.app

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.ByteArrayOutputStream

data class BuildAssessment(
    val projectType: String,
    val buildSystem: String,
    val buildRoot: String,
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
)
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
    private data class Listing(val entries: List<Node>, val truncated: Boolean)

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
            val listing = children(context, treeUri, parentId)
            if (listing.truncated) truncated = true
            for (node in listing.entries) {
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
        val rootNames = root.entries.map { it.name }
        if (root.truncated) truncated = true
        val settingsCandidates = files.keys.filter {
            val leaf = it.substringAfterLast('/')
            leaf.equals("settings.gradle", true) || leaf.equals("settings.gradle.kts", true)
        }
        val buildFileCandidates = files.keys.filter {
            val leaf = it.substringAfterLast('/')
            leaf.equals("build.gradle", true) || leaf.equals("build.gradle.kts", true)
        }
        val manifestCandidates = files.keys.filter { it.substringAfterLast('/').equals("AndroidManifest.xml", true) }
        fun parentPath(path: String): String = if ('/' in path) path.substringBeforeLast('/') else ""
        fun hasManifestBelow(candidateRoot: String): Boolean = manifestCandidates.any {
            candidateRoot.isEmpty() || it == candidateRoot || it.startsWith("$candidateRoot/")
        }
        val candidateRoots = (settingsCandidates + buildFileCandidates)
            .map { parentPath(it) }
            .distinct()
        fun rootScore(candidateRoot: String): Int {
            val prefix = if (candidateRoot.isEmpty()) "" else "$candidateRoot/"
            val wrapperScriptExists = files.keys.any { it.equals(prefix + "gradlew", true) }
            val wrapperJarExists = files.keys.any { it.equals(prefix + "gradle/wrapper/gradle-wrapper.jar", true) }
            val wrapperPropertiesExist = files.keys.any { it.equals(prefix + "gradle/wrapper/gradle-wrapper.properties", true) }
            val rootBuildExists = files.keys.any {
                it.equals(prefix + "build.gradle", true) || it.equals(prefix + "build.gradle.kts", true)
            }
            return (if (hasManifestBelow(candidateRoot)) 8 else 0) +
                (if (wrapperScriptExists && wrapperJarExists && wrapperPropertiesExist) 8 else 0) +
                (if (rootBuildExists) 2 else 0) +
                (if (settingsCandidates.any { parentPath(it) == candidateRoot }) 1 else 0)
        }
        val buildRoot = candidateRoots.maxByOrNull { rootScore(it) } ?: ""
        fun relativeToBuildRoot(path: String): String? = when {
            buildRoot.isEmpty() -> path
            path.startsWith("$buildRoot/") -> path.removePrefix("$buildRoot/")
            else -> null
        }
        val settings = settingsCandidates.firstOrNull { parentPath(it) == buildRoot }
        val rootBuild = buildFileCandidates.firstOrNull { relativeToBuildRoot(it)?.let { relative ->
            relative.equals("build.gradle", true) || relative.equals("build.gradle.kts", true)
        } == true }
        val wrapperProperties = files.keys.firstOrNull { relativeToBuildRoot(it)?.equals("gradle/wrapper/gradle-wrapper.properties", true) == true }
        val wrapperJar = files.keys.firstOrNull { relativeToBuildRoot(it)?.equals("gradle/wrapper/gradle-wrapper.jar", true) == true }
        val wrapperScript = files.keys.firstOrNull { relativeToBuildRoot(it)?.equals("gradlew", true) == true }
        val manifests = manifestCandidates.filter { relativeToBuildRoot(it) != null }
        val moduleBuildFiles = buildFileCandidates.filter { path ->
            val relative = relativeToBuildRoot(path) ?: return@filter false
            '/' in relative
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
        val modules = moduleBuildFiles.mapNotNull { path ->
            relativeToBuildRoot(path)?.substringBeforeLast('/')
        }.distinct().sorted()
        val isAndroid = manifests.isNotEmpty() || hasAndroidPlugin
        val hasGradle = settings != null || rootBuild != null || moduleBuildFiles.isNotEmpty()
        val findings = mutableListOf<String>()
        val blockers = mutableListOf<String>()
        if (settings != null) findings += "Gradle settings found: $settings"
        if (rootBuild != null) findings += "Root Gradle build file found: $rootBuild"
        if (modules.isNotEmpty()) findings += "Gradle modules detected: ${modules.joinToString()}"
        if (manifests.isNotEmpty()) findings += "Android manifest(s): ${manifests.size}"
        if (hasAndroidPlugin) findings += "Android Gradle plugin declaration detected"
        if (hasRust) findings += "Cargo manifest detected; native Rust build steps may be required"
        if (hasGradle) {
            if (wrapperScript != null && wrapperJar != null && wrapperProperties != null) {
                findings += "Complete Gradle wrapper files are present"
            } else {
                if (wrapperScript == null) blockers += "gradlew wrapper script is missing"
                if (wrapperJar == null) blockers += "Gradle wrapper JAR is missing"
                if (wrapperProperties == null) blockers += "gradle/wrapper/gradle-wrapper.properties is missing"
            }
        }
        if (!isAndroid) blockers += "No Android Gradle plugin or AndroidManifest.xml was detected"
        if (!hasGradle) blockers += "No Gradle settings or build file was detected"
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
            buildRoot = buildRoot,
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

    private fun children(context: Context, treeUri: Uri, parentId: String): Listing {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        val result = ArrayList<Node>()
        var wasTruncated = false
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val name = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mime = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            while (cursor.moveToNext() && result.size < 200) {
                result += Node(
                    cursor.getString(id),
                    cursor.getString(name) ?: "Unnamed item",
                    cursor.getString(mime) ?: "application/octet-stream"
                )
            }
            wasTruncated = cursor.count > result.size
        } ?: throw IllegalStateException("Android could not read a project directory.")
        return Listing(result, wasTruncated)
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

}
