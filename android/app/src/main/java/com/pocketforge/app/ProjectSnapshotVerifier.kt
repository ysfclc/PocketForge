package com.pocketforge.app

import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

data class SnapshotVerificationResult(
    val valid: Boolean,
    val verifiedFiles: Int,
    val verifiedBytes: Long,
    val manifestSha256: String,
    val failure: String? = null
)

/**
 * Re-checks a completed app-private snapshot before it can be considered build input.
 * This validates integrity and containment; it does not make project build scripts safe.
 */
class ProjectSnapshotVerifier(
    private val snapshotRoot: File,
    private val maxFiles: Int = 10_000,
    private val maxTotalBytes: Long = 512L * 1024L * 1024L,
    private val maxFileBytes: Long = 64L * 1024L * 1024L
) {
    init {
        require(maxFiles > 0 && maxTotalBytes > 0 && maxFileBytes > 0)
        require(maxFileBytes <= maxTotalBytes)
    }

    fun verify(
        directory: File,
        expectedSnapshotId: String,
        expectedManifestSha256: String,
        expectedFileCount: Int,
        expectedTotalBytes: Long
    ): SnapshotVerificationResult {
        return try {
            require(expectedSnapshotId.isNotBlank()) { "Snapshot ID is missing." }
            require(expectedManifestSha256.matches(SHA256_PATTERN)) { "Expected manifest hash is invalid." }
            require(expectedFileCount in 0..maxFiles) { "Snapshot file count exceeds verification limits." }
            require(expectedTotalBytes in 0..maxTotalBytes) { "Snapshot size exceeds verification limits." }

            val root = directory.canonicalFile
            require(root.isDirectory && root.parentFile == snapshotRoot.canonicalFile) {
                "Snapshot directory is not inside PocketForge snapshot storage."
            }
            val manifest = File(root, MANIFEST_NAME).canonicalFile
            require(manifest.parentFile == root && manifest.isFile) { "Snapshot manifest is missing or unsafe." }
            val manifestBytes = manifest.readBytes()
            val manifestHash = sha256(manifestBytes)
            require(manifestHash.equals(expectedManifestSha256, ignoreCase = true)) {
                "Snapshot manifest hash does not match the saved record."
            }

            val json = JSONObject(String(manifestBytes, Charsets.UTF_8))
            require(json.optInt("schemaVersion", -1) == 1) { "Unsupported snapshot manifest version." }
            require(json.optString("snapshotId") == expectedSnapshotId) { "Snapshot ID does not match its manifest." }
            require(json.optInt("fileCount", -1) == expectedFileCount) { "Snapshot file count does not match its manifest." }
            require(json.optLong("totalBytes", -1L) == expectedTotalBytes) { "Snapshot size does not match its manifest." }

            val entries = json.optJSONArray("files") ?: error("Snapshot file list is missing.")
            require(entries.length() == expectedFileCount && entries.length() <= maxFiles) {
                "Snapshot file list count is invalid."
            }
            val seen = HashSet<String>()
            var totalBytes = 0L
            for (index in 0 until entries.length()) {
                val entry = entries.optJSONObject(index) ?: error("Snapshot manifest contains an invalid file record.")
                val relativePath = entry.optString("path")
                require(isSafeRelativePath(relativePath)) { "Snapshot contains an unsafe path: $relativePath" }
                require(seen.add(relativePath)) { "Snapshot contains a duplicate path: $relativePath" }

                val expectedBytes = entry.optLong("bytes", -1L)
                require(expectedBytes in 0..maxFileBytes) { "Snapshot file size is invalid: $relativePath" }
                require(totalBytes <= maxTotalBytes - expectedBytes) { "Snapshot exceeds verification size limits." }
                totalBytes += expectedBytes

                val target = File(root, relativePath).canonicalFile
                require(target.path.startsWith(root.path + File.separator) && target.isFile) {
                    "Snapshot file is missing or escapes the snapshot directory: $relativePath"
                }
                require(target.length() == expectedBytes) { "Snapshot file size changed: $relativePath" }

                val expectedHash = entry.optString("sha256")
                require(expectedHash.matches(SHA256_PATTERN)) { "Snapshot file hash is invalid: $relativePath" }
                require(sha256(target).equals(expectedHash, ignoreCase = true)) {
                    "Snapshot file integrity check failed: $relativePath"
                }
            }
            require(totalBytes == expectedTotalBytes) { "Verified file sizes do not match the snapshot total." }

            SnapshotVerificationResult(true, entries.length(), totalBytes, manifestHash)
        } catch (error: Exception) {
            SnapshotVerificationResult(false, 0, 0, "", error.message ?: "Snapshot verification failed.")
        }
    }

    private fun isSafeRelativePath(path: String): Boolean {
        if (path.isBlank() || path.startsWith("/") || path.startsWith("\\") || '\u0000' in path) return false
        if (path.contains('\\')) return false
        val parts = path.split('/')
        return parts.all { it.isNotBlank() && it != "." && it != ".." } && path != MANIFEST_NAME
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }

    companion object {
        private const val MANIFEST_NAME = "snapshot-manifest.json"
        private val SHA256_PATTERN = Regex("^[a-fA-F0-9]{64}$")
    }
}
