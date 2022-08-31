package com.appswithlove.floaat

import TimeEntryForPublishing
import com.appswithlove.json
import com.appswithlove.store.DataStore
import com.appswithlove.ui.Logger
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.LocalDate
import kotlin.math.roundToInt

class FloatRepo constructor(private val dataStore: DataStore) {
    fun pushToFloat(from: LocalDate, to: LocalDate, pairs: List<TimeEntryForPublishing>) {
        Logger.log("⬆️ Uploading ${pairs.size} time entries to Float!")
        Logger.log("---")
        val floatUrl = getFloatUrl()
        val endpoint = "$floatUrl/logged-time"

        val timeEntries = pairs.map {

            val description = it.timeEntry.description

            FloatTimeEntriesItem(
                project_id = it.projectId,
                date = it.timeEntry.start.split("T").firstOrNull().orEmpty(),
                hours = it.timeEntry.duration / 60.0 / 60.0,
                notes = description,
                people_id = getFloatClientId(),
                phase_id = it.phaseId
            )
        }

        timeEntries.forEachIndexed { index, it ->
            val data = json.encodeToString(it)
            val request = postRequest(endpoint, data)
            if (request.statusCode() != 200) {
                Logger.log("An error occurred when uploading: ${it.notes}")
                Logger.log(request.body() + request.statusCode())
                return
            }
            Logger.log("Posting (${index + 1}/${timeEntries.size}): ${it.notes}")
        }
        Logger.log("💯 Uploaded all time entries to Float for $from - $to")
        val totalEntriesSaved = dataStore.addAndGetTimeEntryCount(timeEntries.size)
        val timeSaved = getTimeSaved(timeEntries.size)
        val totalTimeSaved = getTimeSaved(totalEntriesSaved)
        Logger.log("---")
        Logger.log("🎉 You just saved $timeSaved. And a total of $totalTimeSaved!")
    }

    private fun getTimeSaved(size: Int): String {
        val seconds = size * 30
        return when {
            seconds > 3600 -> "${"%.2f".format(seconds / 60f / 60f)}h"
            seconds > 60 -> "${(seconds / 60f).roundToInt()}m"
            else -> "${seconds}s"
        }
    }


    fun getFloatUrl(): String {
        return "https://api.float.com/v3"
    }

    private fun getFloatApiKey(): String {
        var key: String? = dataStore.getStore.floatKey
        while (key.isNullOrEmpty()) {
            Logger.log("🔑 Setup Float API Key: Please get the API key from an account owner (e.g. Andrea Zeller) + click Enter:")
            key = readLine()
            dataStore.setFloatApiKey(key)
        }
        return key
    }

    fun getFloatPeople(): List<FloatPeopleItem> {
        val floatUrl = getFloatUrl()
        val endpoint = "$floatUrl/people"
        return getAllPages<FloatPeopleItem>(endpoint).sortedBy { it.name }
    }


    private inline fun <reified T> getAllPages(baseUrl: String): List<T> {
        var page = 1
        val peopleList = mutableListOf<T>()

        while (true) { //fcking dangerous
            val api = "$baseUrl?page=$page&per-page=200"
            val response = getRequest(url = api)
            val projects = json.decodeFromString<List<T>>(response.body())
            if (projects.isEmpty()) break
            peopleList.addAll(projects)
            val totalItems =
                response.headers().map().getOrDefault("x-pagination-total-count", emptyList()).firstOrNull()
                    ?.toFloatOrNull() ?: 1f
            Logger.log("Downloading - Progress: ${(peopleList.size.toFloat() / totalItems) * 100f}%")
            page += 1
        }

        return peopleList
    }

    fun getFloatTimeEntries(from: LocalDate, to: LocalDate): List<FloatTimeEntriesItem> {
        val floatUrl = getFloatUrl()
        val userId = getFloatClientId()
        val endpoint = "$floatUrl/logged-time?start_date=$from&end_date=$to&people_id=$userId"

        val response = getRequest(url = endpoint)
        return json.decodeFromString(response.body())
    }

    fun getFloatClientId(): Int {
        var clientId: Int? = dataStore.getStore.floatClientId
        while (clientId == null || clientId == -1) {
            val people = getFloatPeople().sortedBy { it.name }
            Logger.log("📜 Setup Float Client — Whats your name? This format: (Max Müller):")
            val name = readLine()
            val person = people.firstOrNull { it.name == name }

            if (person == null) {
                Logger.log("📜 Couldn't find you. Here's the list of all:")
                people.forEach {
                    Logger.log("${it.people_id}: ${it.name}")
                }
                Logger.log("----")
                Logger.log("ℹ️ Add Number that is stated next to your name + press Enter:")
                clientId = readLine()?.toIntOrNull()
            } else {
                clientId = person.people_id
            }

            if (!people.any { it.people_id == clientId }) {
                clientId = -1
            } else {
                dataStore.setFloatClientId(clientId)
            }
        }
        return clientId
    }

    fun getFloatProjects(): List<String> {
        val floatUrl = getFloatUrl()
        Logger.log("Downloading Float Projects ⬇️")

        val projectList = getAllPages<FloatProject>("$floatUrl/projects")

        val phases = getAllPages<FloatPhaseItem>("$floatUrl/phases")
        val grouped =
            projectList.associate { project -> project to phases.filter { it.project_id == project.project_id } }


        val projects = grouped.map { project ->
            buildList {
                add(project.key.asString())
                project.value.forEach {
                    add(project.key.asString(it))
                }
            }
        }.flatten()
        return projects
    }

    private fun FloatProject.asString(item: FloatPhaseItem? = null): String {
        return buildString {
            append("$name ($project_id)")
            if (item != null) {
                append(" - ${item.name} (${item.phase_id})")
            }
        }
    }

    private fun getRequest(
        url: String,
        headers: Map<String, String> = mapOf()
    ): HttpResponse<String> {
        val client = HttpClient.newBuilder().build();
        var request = HttpRequest.newBuilder().uri(URI.create(url))
            .header("Authorization", "Bearer ${getFloatApiKey()}")
            .GET()

        headers.forEach {
            request = request.header(it.key, it.value)
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString())
    }

    private fun postRequest(url: String, data: String): HttpResponse<String> {
        val client = HttpClient.newBuilder().build();
        val request = HttpRequest.newBuilder().uri(URI.create(url)).POST(HttpRequest.BodyPublishers.ofString(data))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${getFloatApiKey()}")
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString())
    }
}