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
    val htmlUrl: String
)

data class GitHubEntry(
    val name: String,
    val path: String,
    val type: String,
    val size: Long
)

/** Read-only GitHub REST adapter. It never mutates a repository. */
class GitHubRepositoryClient(private val token: String? = null) {
    fun getRepository(fullName: String): GitHubRepository {
        require(REPOSITORY_PATTERN.matches(fullName)) { "Use owner/repository format." }
        val json = request("/repos/$fullName") as JSONObject
        return GitHubRepository(
            fullName = json.optString("full_name", fullName),
            name = json.optString("name", fullName.substringAfter('/')),
            description = json.optString("description"),
            isPrivate = json.optBoolean("private"),
            defaultBranch = json.optString("default_branch", "main"),
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
    }
}

class GitHubApiException(message: String) : Exception(message)
