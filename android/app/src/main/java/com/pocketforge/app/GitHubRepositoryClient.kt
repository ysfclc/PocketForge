package com.pocketforge.app

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

data class GitHubRepository(
    val fullName: String,
    val name: String,
    val description: String,
    val isPrivate: Boolean,
    val defaultBranch: String,
    val defaultBranchSha: String?,
    val htmlUrl: String
)

data class GitHubEntry(
    val name: String,
    val path: String,
    val type: String,
    val size: Long
)

data class GitHubTextFile(val content: String, val blobSha: String)

/** Read-only GitHub REST adapter. It never mutates a repository. */
class GitHubRepositoryClient(private val token: String? = null) {
    fun getRepository(fullName: String): GitHubRepository {
        require(REPOSITORY_PATTERN.matches(fullName)) { "Use owner/repository format." }
        val json = request("/repos/$fullName") as JSONObject
        val defaultBranch = json.optString("default_branch", "main")
        val branchSha = runCatching {
            val ref = request("/repos/$fullName/git/ref/heads/${encode(defaultBranch)}") as JSONObject
            ref.getJSONObject("object").getString("sha")
        }.getOrNull()
        return GitHubRepository(
            fullName = json.optString("full_name", fullName),
            name = json.optString("name", fullName.substringAfter('/')),
            description = json.optString("description"),
            isPrivate = json.optBoolean("private"),
            defaultBranch = defaultBranch,
            defaultBranchSha = branchSha,
            htmlUrl = json.optString("html_url", "https://github.com/$fullName")
        )
    }

    fun listContents(repository: String, path: String, ref: String): List<GitHubEntry> {
        require(REPOSITORY_PATTERN.matches(repository)) { "Invalid repository name." }
        val encodedPath = path.split('/').filter { it.isNotEmpty() }
            .joinToString("/") { encode(it) }
        val suffix = if (encodedPath.isEmpty()) "" else "/$encodedPath"
        val json = request("/repos/$repository/contents$suffix?ref=${encode(ref)}")
        if (json is JSONObject) {
            return listOf(
                GitHubEntry(
                    name = json.optString("name"),
                    path = json.optString("path"),
                    type = json.optString("type"),
                    size = json.optLong("size")
                )
            )
        }
        val array = json as? JSONArray ?: throw GitHubApiException("Unexpected contents response.")
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    GitHubEntry(
                        name = item.optString("name"),
                        path = item.optString("path"),
                        type = item.optString("type"),
                        size = item.optLong("size")
                    )
                )
            }
        }.sortedWith(compareBy<GitHubEntry> { it.type != "dir" }.thenBy { it.name.lowercase() })
    }

    fun readTextFile(repository: String, path: String, ref: String): String {
        require(REPOSITORY_PATTERN.matches(repository)) { "Invalid repository name." }
        val encodedPath = path.split('/').filter { it.isNotEmpty() }
            .joinToString("/") { encode(it) }
        val json = request("/repos/$repository/contents/$encodedPath?ref=${encode(ref)}") as? JSONObject
            ?: throw GitHubApiException("The selected path is not a file.")
        if (json.optString("encoding") != "base64") {
            throw GitHubApiException("GitHub did not return a supported text encoding.")
        }
        val encoded = json.optString("content").replace("\n", "").replace("\r", "")
        val bytes = try {
            Base64.decode(encoded, Base64.DEFAULT)
        } catch (_: IllegalArgumentException) {
            throw GitHubApiException("GitHub returned invalid base64 content.")
        }
        return bytes.toString(Charsets.UTF_8)
    }

    fun readTextFileWithSha(repository: String, path: String, ref: String): GitHubTextFile {
        require(REPOSITORY_PATTERN.matches(repository)) { "Invalid repository name." }
        val encodedPath = path.split('/').filter { it.isNotEmpty() }
            .joinToString("/") { encode(it) }
        val json = request("/repos/$repository/contents/$encodedPath?ref=${encode(ref)}") as? JSONObject
            ?: throw GitHubApiException("The selected path is not a file.")
        if (json.optString("encoding") != "base64") {
            throw GitHubApiException("GitHub did not return a supported text encoding.")
        }
        val encoded = json.optString("content").replace("\n", "").replace("\r", "")
        val bytes = try {
            Base64.decode(encoded, Base64.DEFAULT)
        } catch (_: IllegalArgumentException) {
            throw GitHubApiException("GitHub returned invalid base64 content.")
        }
        return GitHubTextFile(
            content = bytes.toString(Charsets.UTF_8),
            blobSha = json.optString("sha")
        )
    }

    /** Create a new branch from the exact revision selected by the mission. */
    fun createBranch(repository: String, branchName: String, baseSha: String) {
        require(REPOSITORY_PATTERN.matches(repository)) { "Invalid repository name." }
        require(SHA_PATTERN.matches(baseSha)) { "A full commit SHA is required." }
        require(BRANCH_PATTERN.matches(branchName) && !branchName.contains("..")) { "Invalid working branch name." }
        val payload = JSONObject()
            .put("ref", "refs/heads/$branchName")
            .put("sha", baseSha)
        mutate("POST", "/repos/$repository/git/refs", payload)
    }

    /** Update one text file only when its blob SHA still matches the reviewed base. */
    fun updateTextFile(
        repository: String,
        path: String,
        branch: String,
        message: String,
        content: String,
        expectedBlobSha: String
    ): String {
        require(REPOSITORY_PATTERN.matches(repository)) { "Invalid repository name." }
        require(SHA_PATTERN.matches(expectedBlobSha)) { "A full base blob SHA is required." }
        val encodedPath = path.split('/').filter { it.isNotEmpty() }
            .joinToString("/") { encode(it) }
        val current = request("/repos/$repository/contents/$encodedPath?ref=${encode(branch)}") as? JSONObject
            ?: throw GitHubApiException("The selected path is not a file.")
        val currentSha = current.optString("sha")
        if (currentSha != expectedBlobSha) {
            throw GitHubApiException("This file changed on GitHub after it was reviewed. Reload it before proposing another change.")
        }
        val payload = JSONObject()
            .put("message", message)
            .put("content", Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
            .put("sha", expectedBlobSha)
            .put("branch", branch)
        val response = mutate("PUT", "/repos/$repository/contents/$encodedPath", payload)
        return response.optJSONObject("commit")?.optString("sha")
            ?.takeIf { it.isNotBlank() }
            ?: throw GitHubApiException("GitHub accepted the file update but returned no commit SHA.")
    }

    fun createPullRequest(repository: String, title: String, body: String, head: String, base: String): String {
        require(REPOSITORY_PATTERN.matches(repository)) { "Invalid repository name." }
        val payload = JSONObject()
            .put("title", title)
            .put("body", body)
            .put("head", head)
            .put("base", base)
        val response = mutate("POST", "/repos/$repository/pulls", payload)
        return response.optString("html_url").takeIf { it.isNotBlank() }
            ?: throw GitHubApiException("GitHub created no pull-request URL.")
    }

    private fun mutate(method: String, path: String, payload: JSONObject): JSONObject {
        val credential = token?.takeIf { it.isNotBlank() }
            ?: throw GitHubApiException("A repository-scoped GitHub token is required for remote changes.")
        val connection = (URL("https://api.github.com$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "PocketForge-Android")
            setRequestProperty("Authorization", "Bearer $credential")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        try {
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching { JSONObject(body).optString("message") }.getOrNull()
                throw GitHubApiException(
                    when (status) {
                        401 -> "GitHub rejected this token. Check it and try again."
                        403 -> "GitHub denied this write. Check repository Contents/Pull requests permissions and branch protection."
                        404 -> "Repository, branch or file not found, or this token cannot access it."
                        409, 422 -> message?.takeIf { it.isNotBlank() } ?: "GitHub rejected this change because the repository state or request is invalid."
                        else -> message?.takeIf { it.isNotBlank() } ?: "GitHub write failed (HTTP $status)."
                    }
                )
            }
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun request(path: String): Any {
        val connection = (URL("https://api.github.com$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "PocketForge-Android")
            token?.takeIf { it.isNotBlank() }?.let { setRequestProperty("Authorization", "Bearer $it") }
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching { JSONObject(body).optString("message") }.getOrNull()
                throw GitHubApiException(
                    when (status) {
                        401 -> "GitHub rejected this token. Check it and try again."
                        403 -> "GitHub denied access or rate-limited this request."
                        404 -> "Repository or path not found, or this token cannot access it."
                        else -> message?.takeIf { it.isNotBlank() } ?: "GitHub request failed (HTTP $status)."
                    }
                )
            }
            return if (body.trimStart().startsWith("[")) JSONArray(body) else JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    companion object {
        private val REPOSITORY_PATTERN = Regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$")
        private val SHA_PATTERN = Regex("^[a-fA-F0-9]{40}$")
        private val BRANCH_PATTERN = Regex("^[A-Za-z0-9._/-]{1,180}$")
    }
}

class GitHubApiException(message: String) : Exception(message)
