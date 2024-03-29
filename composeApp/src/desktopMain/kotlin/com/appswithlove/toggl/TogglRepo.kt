package com.appswithlove.toggl

import TimeEntry
import TimeEntryUpdate
import TimeEntryUpdateFull
import androidx.compose.ui.graphics.Color
import com.appswithlove.json
import com.appswithlove.store.DataStore
import com.appswithlove.ui.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import kotlin.math.abs

private val HttpResponse.isSuccess: Boolean
    get() {
        return status == HttpStatusCode.OK || status == HttpStatusCode.Created
    }

class TogglRepo constructor(private val dataStore: DataStore) {

    suspend fun getTogglTimeEntries(from: LocalDate, to: LocalDate): List<TimeEntry> {
        val apiKey = getTogglApiKey()
        val start = from.atStartOfDay().format(DateTimeFormatter.ISO_DATE)
        val end = to.plusDays(1).atStartOfDay().format(DateTimeFormatter.ISO_DATE)
        val timeEntriesApi =
            "https://api.track.toggl.com/api/v9/me/time_entries?start_date=$start&end_date=$end"
        val response = getRequest(apiKey, timeEntriesApi)
        return json.decodeFromString(response.body())
    }

    suspend fun getDatesWithTimeEntries(from: LocalDate, to: LocalDate): List<LocalDate> {
        val entries = getTogglTimeEntries(from, to)
        return entries.map { ZonedDateTime.parse(it.start).toLocalDate() }.distinct()
    }

    suspend fun getDatesWithTimeEntriesAndPrefix(from: LocalDate, to: LocalDate, prefix: String): List<Pair<LocalDate, String>> {
        val entries = getTogglTimeEntries(from, to)
        return entries.filter { it.description?.startsWith(prefix) == true }
            .map { ZonedDateTime.parse(it.start).toLocalDate() to it.description.orEmpty() }
            .distinct()
    }

    var projects: List<Project>? = null
    suspend fun getTogglProjects(): List<Project> {
        val apiKey = getTogglApiKey()
        val projectsApi = "https://api.track.toggl.com/api/v9/me/projects"
        val response = getRequest(apiKey, projectsApi)
        return projects ?: json.decodeFromString<List<Project>>(response.body())
            .also { projects = it }
    }

    suspend fun getTogglProject(workspaceId: Int, id: Int): Project? {
        val apiKey = getTogglApiKey()
        val projectsApi =
            "https://api.track.toggl.com/api/v9/workspaces/${workspaceId}/projects/$id"
        val response = getRequest(apiKey, projectsApi)
        return json.decodeFromString(response.body())
    }

    suspend fun getWorkspaces(): TogglWorkspaceItem? {
        var response: HttpResponse? = null
        while (response?.status != HttpStatusCode.OK) {
            val apiKey = getTogglApiKey()
            val workspacesApi = "https://api.track.toggl.com/api/v9/me/workspaces"
            response = getRequest(apiKey, workspacesApi)
            if (response.status == HttpStatusCode.Forbidden) {
                getTogglApiKey()
            }
        }
        return json.decodeFromString<List<TogglWorkspaceItem>>(response.body()).firstOrNull()
    }

    private fun getTogglApiKey(): String {
        var key: String? = dataStore.getStore.togglKey
        if (key.isNullOrEmpty()) {
            Logger.log("Please Reset T2R with the button on the bottom right.")
            throw Exception("Toggl API Key is missing")
        }
        return key
    }

    suspend fun startTimer(workspaceId: Int, project: Project, task: String) {
        Logger.log("Trying to start timer...")

        val currentEntry = getCurrentTimeEntryIfEmpty()
        val togglApiKey = getTogglApiKey()

        if (currentEntry == null) {
            // Update current running
            val api = "https://api.track.toggl.com/api/v9/workspaces/${workspaceId}/time_entries"

            val timeEntry = TimeEntryCreate(
                project_id = project.id,
                tags = listOf(task),
                workspace_id = workspaceId
            )
            val response = postRequest(api, json.encodeToString(timeEntry), togglApiKey)
            if (response.isSuccess) {
                Logger.log("✅Started timer for ${project.name}")
            } else {
                Logger.log("Error happened - ${response.bodyAsText()}")
            }
        } else {
            val api = "https://api.track.toggl.com/api/v9/workspaces/${workspaceId}/time_entries/${currentEntry.id}"

            val update = TimeEntryUpdateFull(
                id = currentEntry.id,
                project_id = project.id,
                tags = listOf(task),
                workspace_id = workspaceId,
            )
            val response = putRequest(api, json.encodeToString(update), togglApiKey)
            if (response.isSuccess) {
                Logger.log("✅ Updated current empty timer for ${project.name}")
            } else {
                Logger.log("Error happened - ${response.bodyAsText()}")
            }
        }

        // call the api with the correct model
    }

    private suspend fun getCurrentTimeEntryIfEmpty(): TimeEntry? {
        val currentEntry = getCurrentTimeEntry()
        return if (currentEntry != null && currentEntry.description.isNullOrEmpty() && currentEntry.project_id == null && currentEntry.tags.isNullOrEmpty()) {
            currentEntry
        } else {
            null
        }
    }

    private suspend fun getCurrentTimeEntry(): TimeEntry? {
        return try {
            val apiKey = getTogglApiKey()
            val projectsApi = "https://api.track.toggl.com/api/v9/me/time_entries/current"
            val response = getRequest(apiKey, projectsApi)
            json.decodeFromString(response.body())
        } catch (e: Exception) {
            null
        }
    }

    suspend fun pushProjectsToToggl(
        workspaceId: Int,
        newProjects: List<TogglProjectCreate>
    ) {
        val togglApiKey = getTogglApiKey()
        val api = "https://api.track.toggl.com/api/v9/workspaces/${workspaceId}/projects"
        newProjects.forEachIndexed { index, it ->
            var projectResponse: HttpResponse? = null
            while (projectResponse?.status != HttpStatusCode.OK) {
                projectResponse = postRequest(api, json.encodeToString(it), togglApiKey)
                if (projectResponse.status != HttpStatusCode.OK && projectResponse.status != HttpStatusCode.TooManyRequests) {
                    Logger.log("Error happened - ${it.name} - ${projectResponse.bodyAsText()}")
                }
            }
            Logger.log("Progress:️ ${index + 1}/${newProjects.size}")
        }
        Logger.log("🎉 Synced new Float projects to Toggl!")
    }

    suspend fun putProjectsToToggl(
        workspaceId: Int,
        updatedProjects: List<TogglProjectCreate>
    ) {
        val togglApiKey = getTogglApiKey()
        updatedProjects.forEachIndexed { index, it ->
            val api =
                "https://api.track.toggl.com/api/v9/workspaces/${workspaceId}/projects/${it.id}"
            var projectResponse: HttpResponse? = null
            while (projectResponse?.status != HttpStatusCode.OK) {
                projectResponse = putRequest(
                    api,
                    json.encodeToString(TogglProjectUpdate(it.name, it.color, it.active)),
                    togglApiKey
                )
                if (projectResponse.status != HttpStatusCode.OK && projectResponse.status != HttpStatusCode.TooManyRequests) {
                    Logger.log("Error happened - ${projectResponse.bodyAsText()}")
                }
            }
            Logger.log("Progress:️ ${index + 1}/${updatedProjects.size}")
        }
        Logger.log("🎉 Synced changed Float projects to Toggl!")
    }

    suspend fun deleteProjects(
        workspaceId: Int,
        ids: List<Int>
    ) {
        val togglApiKey = getTogglApiKey()
        ids.forEachIndexed { index, it ->
            val api = "https://api.track.toggl.com/api/v9/workspaces/${workspaceId}/projects/${it}"
            var projectResponse: HttpResponse? = null
            while (projectResponse?.status != HttpStatusCode.OK) {
                projectResponse = deleteRequest(togglApiKey, api)
                if (projectResponse.status != HttpStatusCode.OK && projectResponse.status != HttpStatusCode.TooManyRequests) {
                    Logger.log("Error happened - ${projectResponse.bodyAsText()}")
                }
            }
            Logger.log("Progress:️ ${index + 1}/${ids.size}")
        }
        Logger.log("🎉 Fully cleaned the projects!")
    }

    suspend fun getTogglTags(): List<TogglTag> {
        val apiKey = getTogglApiKey()
        val projectsApi = "https://api.track.toggl.com/api/v9/me/tags"
        val response = getRequest(apiKey, projectsApi)
        return json.decodeFromString(response.body())
    }

    suspend fun pushTagsToToggl(
        workspaceId: Int,
        newTags: List<String>
    ) {
        val togglApiKey = getTogglApiKey()
        val api = "https://api.track.toggl.com/api/v9/workspaces/${workspaceId}/tags"
        newTags.forEachIndexed { index, it ->
            var projectResponse: HttpResponse? = null
            while (projectResponse?.status != HttpStatusCode.OK) {
                projectResponse = postRequest(
                    api,
                    json.encodeToString(TogglTagCreate(workspaceId, it)),
                    togglApiKey
                )
                if (projectResponse.status != HttpStatusCode.OK && projectResponse.status != HttpStatusCode.TooManyRequests) {
                    Logger.log("Error happened - $it - ${projectResponse.bodyAsText()}")
                }
            }
            Logger.log("Progress:️ ${index + 1}/${newTags.size}")
        }
        Logger.log("🎉 Synced new Float tags to Toggl!")
    }

    suspend fun putTimeEntries(
        workspaceId: Int,
        updatesTimeEntries: List<Pair<Long, TimeEntryUpdate>>
    ) {
        val togglApiKey = getTogglApiKey()
        updatesTimeEntries.forEachIndexed { index, (id, updateJson) ->
            val api =
                "https://api.track.toggl.com/api/v9/workspaces/${workspaceId}/time_entries/${id}"
            var projectResponse: HttpResponse? = null
            while (projectResponse?.status != HttpStatusCode.OK) {
                projectResponse = putRequest(api, json.encodeToString(updateJson), togglApiKey)
                if (projectResponse.status != HttpStatusCode.OK && projectResponse.status != HttpStatusCode.TooManyRequests) {
                    Logger.log("Error happened- ${projectResponse.bodyAsText()}")
                }
            }
            Logger.log("Progress:️ ${index + 1}/${updatesTimeEntries.size}")
        }
        Logger.log("🎉 Time entries are uptodate in Toggl!")
    }

    suspend fun updateProjectColors(
        workspaceId: Int,
        newProjects: List<TogglProject>
    ) {
        val togglApiKey = getTogglApiKey()
        val api = "https://api.track.toggl.com/api/v9/workspaces/${workspaceId}/projects"
        newProjects.forEachIndexed { index, it ->
            var projectResponse: HttpResponse? = null
            while (projectResponse?.status != HttpStatusCode.OK) {
                projectResponse = putRequest(
                    api + "/${it.project_id}",
                    json.encodeToString(TogglProjectUpdate(color = it.color)),
                    togglApiKey
                )
                if (projectResponse.status != HttpStatusCode.OK) {
                    Logger.log("Error happened - retrying")
                }
            }
            Logger.log("Progress:️ ${index + 1}/${newProjects.size}")
        }
        Logger.log("🎉 Your Float colors are now synced to Toggl!")
    }

    private suspend fun deleteRequest(
        togglApiKey: String,
        url: String
    ): HttpResponse {
        val client = HttpClient(CIO)
        val authToken = Base64.getEncoder().encodeToString("$togglApiKey:api_token".toByteArray())

        val response: HttpResponse = client.delete(url) {
            header(HttpHeaders.Authorization, "Basic $authToken")
        }
        return response
    }

    private suspend fun getRequest(
        togglApiKey: String,
        url: String
    ): HttpResponse {
        val client = HttpClient(CIO)
        val authToken = Base64.getEncoder().encodeToString("$togglApiKey:api_token".toByteArray())

        val response: HttpResponse = client.get(url) {
            header(HttpHeaders.Authorization, "Basic $authToken")
        }
        return response
    }

    private suspend fun postRequest(url: String, data: String, togglApiKey: String): HttpResponse {
        val client = HttpClient(CIO)
        val authToken = Base64.getEncoder().encodeToString("$togglApiKey:api_token".toByteArray())

        val response: HttpResponse = client.post(url) {
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Authorization, "Basic $authToken")
            setBody(data)
        }
        return response
    }

    private suspend fun putRequest(url: String, data: String, togglApiKey: String): HttpResponse {
        val client = HttpClient(CIO)
        val authToken = Base64.getEncoder().encodeToString("$togglApiKey:api_token".toByteArray())

        val response: HttpResponse = client.put(url) {
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Authorization, "Basic $authToken")
            setBody(data)
        }
        return response
    }

    fun getClosestTogglColor(color: Color): Color {
        val togglColors = listOf(
            Color(11, 131, 217),
            Color(158, 91, 217),
            Color(217, 65, 130),
            Color(227, 106, 0),
            Color(191, 112, 0),
            Color(45, 166, 8),
            Color(6, 168, 147),
            Color(201, 128, 107),
            Color(70, 91, 179),
            Color(153, 0, 153),
            Color(199, 175, 20),
            Color(86, 102, 20),
            Color(217, 43, 43),
            Color(82, 82, 102),
            Color(153, 17, 2)
        )

        var closestColor = togglColors.first()
        var closestDistance = -1f
        togglColors.forEach {
            val dist = getColorDistance(it, color)

            if (dist < closestDistance || closestDistance == -1f) {
                closestDistance = dist
                closestColor = it
            }
        }
        return closestColor
    }

    private fun getColorDistance(it: Color, color: Color): Float {
        return abs(it.red - color.red) + abs(it.blue - color.blue) + abs(it.green - color.green)
    }
}