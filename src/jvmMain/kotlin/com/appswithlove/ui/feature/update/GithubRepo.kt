package com.appswithlove.ui.feature.update

import com.appswithlove.json
import com.appswithlove.toggl_to_float.BuildConfig
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

        val comparison = compareVersions(BuildConfig.APP_VERSION, latestVersion.name)
        return if (comparison < 0) latestVersion else null
    }

    private fun compareVersions(version1: String, version2: String): Int {
        val comparator: Comparator<String> = Comparator { o1, o2 ->
            val parts1 = o1.split("\\.".toRegex()).toTypedArray()
            val parts2 = o2.split("\\.".toRegex()).toTypedArray()
            val length = Math.max(parts1.size, parts2.size)
            for (i in 0 until length) {
                val part1 = if (i < parts1.size && parts1[i].isNotEmpty()) Integer.parseInt(parts1[i]) else 0
                val part2 = if (i < parts2.size && parts2[i].isNotEmpty()) Integer.parseInt(parts2[i]) else 0
                if (part1 < part2) {
                    return@Comparator -1
                }
                if (part1 > part2) {
                    return@Comparator 1
                }
            }
            return@Comparator 0
        }
        return comparator.compare(version1, version2)
    }
}