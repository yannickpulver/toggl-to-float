package com.appswithlove.toggl

import TimeEntry
import com.appswithlove.json
import com.appswithlove.store.DataStore
import com.appswithlove.ui.Logger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class TogglRepo constructor(private val dataStore: DataStore) {

    suspend fun getTogglTimeEntries(from: LocalDate, to: LocalDate): List<TimeEntry> {
        val apiKey = getTogglApiKey()
        val start = from.atStartOfDay().format(DateTimeFormatter.ISO_DATE)
        val end = to.plusDays(1).atStartOfDay().format(DateTimeFormatter.ISO_DATE)
        val timeEntriesApi = "https://api.track.toggl.com/api/v9/me/time_entries?start_date=$start&end_date=$end"
        val response = getRequest(apiKey, timeEntriesApi)
        return json.decodeFromString(response.body())
    }

    suspend fun getTogglProjects(): List<Project> {
        val apiKey = getTogglApiKey()
        val projectsApi = "https://api.track.toggl.com/api/v9/me/projects"
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

    private fun getTogglApiKey(renew: Boolean = false): String {
        var key: String? = if (renew) null else dataStore.getStore.togglKey
        while (key.isNullOrEmpty()) {
            Logger.log("Please Reset T2R with the button on the bottom right.")
            dataStore.setTogglApiKey(key)
        }
        return key
    }

    suspend fun pushProjectsToToggl(
        workspaceId: Int,
        newProjects: List<TogglProject>
    ) {
        val togglApiKey = getTogglApiKey()
        val api = "https://api.track.toggl.com/api/v9/workspaces/${workspaceId}/projects"
        newProjects.forEachIndexed { index, it ->
            var projectResponse: HttpResponse? = null
            while (projectResponse?.status != HttpStatusCode.OK) {
                projectResponse = postRequest(api, json.encodeToString(it), togglApiKey)
            }
            Logger.log("Progress:️ ${index + 1}/${newProjects.size}")
        }
        Logger.log("🎉 Your Redmine projects are now available in Toggl!")
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

}