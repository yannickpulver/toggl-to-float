package com.appswithlove.floaat

import TimeEntryForPublishing
import com.appswithlove.json
import com.appswithlove.store.DataStore
import com.appswithlove.ui.Logger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.*
import kotlin.math.roundToInt

class FloatRepo constructor(private val dataStore: DataStore) {
    suspend fun pushToFloat(from: LocalDate, to: LocalDate, pairs: List<TimeEntryForPublishing>) {
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
            if (request.status != HttpStatusCode.OK) {
                Logger.log("An error occurred when uploading: ${it.notes}")
                Logger.log(request.bodyAsText() + request.status)
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

    private fun getFloatApiKey(): String? {
        return dataStore.getStore.floatKey
    }

    suspend fun getFloatPeople(): List<FloatPeopleItem> {
        val floatUrl = getFloatUrl()
        val endpoint = "$floatUrl/people"
        return getAllPages<FloatPeopleItem>(endpoint).sortedBy { it.name }
    }

    suspend fun getWeeklyOverview(): List<FloatOverview> {
        val monday = LocalDate.now().with(WeekFields.of(Locale.FRANCE).firstDayOfWeek)
        val sunday = monday.plusWeeks(1).minusDays(1)
        val tasks = getFloatTasks(monday, sunday)

        return tasks
            .sortedByDescending { it.hours }
            .map {
                val project = getProject(it.project_id)
                val phase = if (it.phase_id != 0) {
                    getPhase(it.phase_id)
                } else null
                FloatOverview(it, project, phase)
            }

    }


    private suspend fun getFloatTasks(start: LocalDate, end: LocalDate): List<FloatTask> {
        val floatUrl = getFloatUrl()
        val userId = getFloatClientId()
        val endpoint = "$floatUrl/tasks?people_id=$userId&start_date=$start&end_date=$end"
        val response = getRequest(url = endpoint)
        return json.decodeFromString(response.body())
    }

    private suspend fun getProject(id: Int): FloatProject? {
        val floatUrl = getFloatUrl()
        val endpoint = "$floatUrl/projects/$id"
        val response = getRequest(url = endpoint)
        return json.decodeFromString(response.body())

    }

    private suspend fun getTask(id: Int): FloatTask? {
        val floatUrl = getFloatUrl()
        val endpoint = "$floatUrl/tasks/$id"
        val response = getRequest(url = endpoint)
        return json.decodeFromString(response.body())

    }

    private suspend fun getPhase(id: Int): FloatPhaseItem? {
        val floatUrl = getFloatUrl()
        val endpoint = "$floatUrl/phases/$id"
        val response = getRequest(url = endpoint)
        return json.decodeFromString(response.body())

    }

    private suspend inline fun <reified T> getAllPages(baseUrl: String): List<T> {
        var page = 1
        val peopleList = mutableListOf<T>()

        while (true) { //fcking dangerous
            val api = "$baseUrl?page=$page&per-page=200"
            val response = getRequest(url = api)
            val projects = json.decodeFromString<List<T>>(response.body())
            if (projects.isEmpty()) break
            peopleList.addAll(projects)
            val totalItems =
                response.headers.toMap().getOrDefault("X-Pagination-Total-Count", emptyList()).firstOrNull()
                    ?.toFloatOrNull() ?: 1f
            Logger.log("Downloading - Progress: ${(peopleList.size.toFloat() / totalItems) * 100f}%")
            page += 1
        }

        return peopleList
    }

    suspend fun getFloatTimeEntries(from: LocalDate, to: LocalDate): List<FloatTimeEntriesItem> {
        val floatUrl = getFloatUrl()
        val userId = getFloatClientId()
        val endpoint = "$floatUrl/logged-time?start_date=$from&end_date=$to&people_id=$userId"

        val response = getRequest(url = endpoint)
        return json.decodeFromString(response.body())
    }


    suspend fun getFloatTimeEntries(): List<FloatTimeEntriesItem> {
        val floatUrl = getFloatUrl()
        val userId = getFloatClientId()
        val endpoint = "$floatUrl/logged-time?people_id=$userId"

        val response = getRequest(url = endpoint)
        return json.decodeFromString(response.body())
    }


    suspend fun getFloatClientId(): Int {
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

    suspend fun getFloatProjects(): List<Pair<String, String?>> {
        val floatUrl = getFloatUrl()
        Logger.log("Downloading Float Projects ⬇️")

        val projectList = getAllPages<FloatProject>("$floatUrl/projects")

        val phases = getAllPages<FloatPhaseItem>("$floatUrl/phases")
        val grouped = projectList.associateWith { project -> phases.filter { it.project_id == project.project_id } }


        val projects = grouped.map { project ->
            buildList {
                add(project.key.asString() to project.key.color)
                project.value.forEach {
                    add(project.key.asString(it) to it.color)
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

    private suspend fun getRequest(
        url: String,
        headers: Map<String, String> = mapOf()
    ): HttpResponse {
        val client = HttpClient(CIO)

        val response: HttpResponse = client.get(url) {
            header(HttpHeaders.Authorization, "Bearer ${getFloatApiKey()}")
            headers.forEach {
                header(it.key, it.value)
            }
        }
        return response
    }

    private suspend fun postRequest(url: String, data: String): HttpResponse {

        val client = HttpClient(CIO)

        val response: HttpResponse = client.post(url) {
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Authorization, "Bearer ${getFloatApiKey()}")
            setBody(data)
        }
        return response
    }


}