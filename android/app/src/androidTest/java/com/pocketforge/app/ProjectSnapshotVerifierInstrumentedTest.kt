package com.pocketforge.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ProjectSnapshotVerifierInstrumentedTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val snapshotRoot get() = File(context.cacheDir, "snapshot-verifier-tests")

    @After
    fun cleanSnapshots() {
        snapshotRoot.deleteRecursively()
    }

    @Test
    fun intactNestedSnapshotPassesWithExactCounts() {
        val fixture = createFixture(
            actualFiles = listOf(ManifestFile("app/src/Main.kt", "class Main"))
        )

        val result = verify(fixture)

        assertTrue("Unexpected verification failure: ${result.failure}", result.valid)
        assertEquals(1, result.verifiedFiles)
        assertEquals("class Main".toByteArray().size.toLong(), result.verifiedBytes)
        assertEquals(fixture.manifestSha256, result.manifestSha256)
    }

    @Test
    fun modifiedFileIsRejectedEvenWhenManifestIsUnchanged() {
        val fixture = createFixture(listOf(ManifestFile("src/Main.kt", "original")))
        File(fixture.directory, "src/Main.kt").writeText("modified")

        val result = verify(fixture)

        assertFalse(result.valid)
        assertTrue(result.failure.orEmpty().contains("integrity check failed"))
    }

    @Test
    fun unlistedFileIsRejected() {
        val fixture = createFixture(listOf(ManifestFile("src/Main.kt", "class Main")))
        File(fixture.directory, "unlisted.txt").writeText("not in manifest")

        val result = verify(fixture)

        assertFalse(result.valid)
        assertTrue(result.failure.orEmpty().contains("unlisted files"))
    }

    @Test
    fun traversalPathIsRejected() {
        val malicious = ManifestFile("../outside.txt", "outside")
        val fixture = createFixture(actualFiles = emptyList(), manifestFiles = listOf(malicious))

        val result = verify(fixture)

        assertFalse(result.valid)
        assertTrue(result.failure.orEmpty().contains("unsafe path"))
    }

    @Test
    fun duplicateManifestPathsAreRejected() {
        val file = ManifestFile("src/Main.kt", "class Main")
        val fixture = createFixture(actualFiles = listOf(file), manifestFiles = listOf(file, file))

        val result = verify(fixture)

        assertFalse(result.valid)
        assertTrue(result.failure.orEmpty().contains("duplicate path"))
    }

    @Test
    fun mismatchedSourceIdentityIsRejected() {
        val fixture = createFixture(listOf(ManifestFile("README.md", "project")))

        val result = ProjectSnapshotVerifier(snapshotRoot).verify(
            directory = fixture.directory,
            expectedSnapshotId = fixture.id,
            expectedManifestSha256 = fixture.manifestSha256,
            expectedFileCount = fixture.fileCount,
            expectedTotalBytes = fixture.totalBytes,
            expectedSourceUriSha256 = "c".repeat(64),
            expectedSourceRootIdSha256 = ROOT_HASH
        )

        assertFalse(result.valid)
        assertTrue(result.failure.orEmpty().contains("source URI fingerprint"))
    }

    @Test
    fun directoryOutsideSnapshotRootIsRejected() {
        val outside = File(context.cacheDir, "not-a-snapshot-${UUID.randomUUID()}").apply { mkdirs() }

        try {
            val result = ProjectSnapshotVerifier(snapshotRoot).verify(
                directory = outside,
                expectedSnapshotId = UUID.randomUUID().toString(),
                expectedManifestSha256 = "0".repeat(64),
                expectedFileCount = 0,
                expectedTotalBytes = 0,
                expectedSourceUriSha256 = URI_HASH,
                expectedSourceRootIdSha256 = ROOT_HASH
            )
            assertFalse(result.valid)
            assertTrue(result.failure.orEmpty().contains("not inside PocketForge snapshot storage"))
        } finally {
            outside.deleteRecursively()
        }
    }

    @Test
    fun manifestHashMismatchIsRejectedBeforeParsing() {
        val fixture = createFixture(listOf(ManifestFile("README.md", "project")))

        val result = ProjectSnapshotVerifier(snapshotRoot).verify(
            directory = fixture.directory,
            expectedSnapshotId = fixture.id,
            expectedManifestSha256 = "0".repeat(64),
            expectedFileCount = fixture.fileCount,
            expectedTotalBytes = fixture.totalBytes,
            expectedSourceUriSha256 = URI_HASH,
            expectedSourceRootIdSha256 = ROOT_HASH
        )

        assertFalse(result.valid)
        assertTrue(result.failure.orEmpty().contains("manifest hash does not match"))
    }

    private fun verify(fixture: Fixture): SnapshotVerificationResult =
        ProjectSnapshotVerifier(snapshotRoot).verify(
            directory = fixture.directory,
            expectedSnapshotId = fixture.id,
            expectedManifestSha256 = fixture.manifestSha256,
            expectedFileCount = fixture.fileCount,
            expectedTotalBytes = fixture.totalBytes,
            expectedSourceUriSha256 = URI_HASH,
            expectedSourceRootIdSha256 = ROOT_HASH
        )

    private fun createFixture(
        actualFiles: List<ManifestFile>,
        manifestFiles: List<ManifestFile> = actualFiles
    ): Fixture {
        check(snapshotRoot.exists() || snapshotRoot.mkdirs())
        val id = UUID.randomUUID().toString()
        val directory = File(snapshotRoot, id)
        check(directory.mkdirs())

        actualFiles.forEach { entry ->
            val target = File(directory, entry.path)
            val parent = target.parentFile
            check(parent != null && (parent.mkdirs() || parent.isDirectory))
            target.writeBytes(entry.content.toByteArray(Charsets.UTF_8))
        }

        val totalBytes = manifestFiles.sumOf { it.content.toByteArray(Charsets.UTF_8).size.toLong() }
        val manifest = JSONObject().apply {
            put("schemaVersion", 1)
            put("snapshotId", id)
            put("sourceIdentity", JSONObject().apply {
                put("treeUriSha256", URI_HASH)
                put("rootDocumentIdSha256", ROOT_HASH)
                put("displayName", "Instrumented test project")
            })
            put("fileCount", manifestFiles.size)
            put("totalBytes", totalBytes)
            put("files", JSONArray().apply {
                manifestFiles.forEach { entry ->
                    val bytes = entry.content.toByteArray(Charsets.UTF_8)
                    put(JSONObject().apply {
                        put("path", entry.path)
                        put("bytes", bytes.size)
                        put("sha256", sha256(bytes))
                    })
                }
            })
        }
        val manifestBytes = manifest.toString(2).toByteArray(Charsets.UTF_8)
        File(directory, "snapshot-manifest.json").writeBytes(manifestBytes)
        return Fixture(
            directory = directory,
            id = id,
            manifestSha256 = sha256(manifestBytes),
            fileCount = manifestFiles.size,
            totalBytes = totalBytes
        )
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private data class ManifestFile(val path: String, val content: String)

    private data class Fixture(
        val directory: File,
        val id: String,
        val manifestSha256: String,
        val fileCount: Int,
        val totalBytes: Long
    )

    companion object {
        private val URI_HASH = "a".repeat(64)
        private val ROOT_HASH = "b".repeat(64)
    }
}
