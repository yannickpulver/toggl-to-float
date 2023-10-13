package com.appswithlove.ui.feature.update

import com.appswithlove.json
import com.appswithlove.version
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import kotlinx.serialization.decodeFromString

class GithubRepo {

    private suspend fun fetchLatestVersion(): LatestRelease? {
        val client = HttpClient(CIO)
        val response =
            client.get("https://api.github.com/repos/yannickpulver/toggl-2-float-compose/releases/latest")
        return try {
            val release = json.decodeFromString<LatestRelease>(response.body())
            client.close()
            release
        } catch (e: Exception) {
            null
        }
    }

    suspend fun hasNewRelease(): LatestRelease? {
        val latestVersion = fetchLatestVersion()

        if (latestVersion == null) {
            println("Failed to fetch the latest version.")
            return null
        }

        return if (latestVersion.name != version) latestVersion else null
    }
}