package com.appswithlove.toggl

import TimeEntry
import com.appswithlove.store.DataStore
import com.appswithlove.json
import com.appswithlove.ui.Logger
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class TogglRepo constructor(private val dataStore: DataStore) {

    fun getTogglTimeEntries(from: LocalDate, to: LocalDate): List<TimeEntry> {
        val apiKey = getTogglApiKey()
        val start = from.atStartOfDay().format(DateTimeFormatter.ISO_DATE)
        val end = to.plusDays(1).atStartOfDay().format(DateTimeFormatter.ISO_DATE)
        val timeEntriesApi = "https://api.track.toggl.com/api/v9/me/time_entries?start_date=$start&end_date=$end"
        val response = getRequest(apiKey, timeEntriesApi)
        return json.decodeFromString(response.body())
    }

    fun getTogglProjects(): List<Project> {
        val apiKey = getTogglApiKey()
        val projectsApi = "https://api.track.toggl.com/api/v9/me/projects"
        val response = getRequest(apiKey, projectsApi)
        return json.decodeFromString(response.body())
    }



    fun getWorkspaces(): TogglWorkspaceItem? {
        var response: HttpResponse<String>? = null
        while (response?.statusCode() != 200) {
            val apiKey = getTogglApiKey()
            val workspacesApi = "https://api.track.toggl.com/api/v9/me/workspaces"
            response = getRequest(apiKey, workspacesApi)
            if (response.statusCode() == 403) {
                getTogglApiKey(true)
            }
        }
        return json.decodeFromString<List<TogglWorkspaceItem>>(response.body()).firstOrNull()
    }

    private fun getTogglApiKey(renew: Boolean = false): String {
        var key: String? = if (renew) null else dataStore.getStore.togglKey
        while (key.isNullOrEmpty()) {
            Logger.log("🔑 Setup Toggl API Key: Please visit https://track.toggl.com/profile and copy the key from the 'API Token' section here + click Enter:")
            key = readLine()
            dataStore.setTogglApiKey(key)
        }
        return key
    }

    fun pushProjectsToToggl(
        workspaceId: Int,
        newProjects: List<TogglProject>
    ) {
        val togglApiKey = getTogglApiKey()
        val api = "https://api.track.toggl.com/api/v9/workspaces/${workspaceId}/projects"
        newProjects.forEachIndexed { index, it ->
            var projectResponse: HttpResponse<String>? = null
            while (projectResponse?.statusCode() != 200) {
                projectResponse = postRequest(api, json.encodeToString(it), togglApiKey)
            }
            Logger.log("Progress:️ ${index + 1}/${newProjects.size}")
        }
        Logger.log("🎉 Your Redmine projects are now available in Toggl!")
    }


    private fun getRequest(
        togglApiKey: String,
        url: String
    ): HttpResponse<String> {
        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder().uri(URI.create(url)).GET()

        val authToken = Base64.getEncoder().encodeToString("$togglApiKey:api_token".toByteArray())
        request.setHeader("Authorization", "Basic $authToken")

        return client.send(request.build(), HttpResponse.BodyHandlers.ofString())
    }


    private fun postRequest(url: String, data: String, togglApiKey: String): HttpResponse<String> {
        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder().uri(URI.create(url)).POST(HttpRequest.BodyPublishers.ofString(data))
            .header("Content-Type", "application/json")

        val authToken = Base64.getEncoder().encodeToString("$togglApiKey:api_token".toByteArray())
        request.setHeader("Authorization", "Basic $authToken")

        return client.send(request.build(), HttpResponse.BodyHandlers.ofString())
    }

}