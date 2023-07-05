package com.appswithlove.floaat

import TimeEntryForPublishing
import com.appswithlove.json
import com.appswithlove.jsonNoDefaults
import com.appswithlove.store.DataStore
import com.appswithlove.ui.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.util.toMap
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToInt

class FloatRepo constructor(private val dataStore: DataStore) {
    suspend fun pushToFloat(date: LocalDate, pairs: List<TimeEntryForPublishing>) {
        Logger.log("⬆️ Uploading ${pairs.size} time entries to Float!")
        Logger.log("---")
        val floatUrl = getFloatUrl()
        val endpoint = "$floatUrl/logged-time"

        val timeEntries = pairs.map {
            val phase = getPhase(it.id)
            val projectId = phase?.project_id ?: it.id
            val task = it.timeEntry.tags.orEmpty().map { tag ->
                getFloatTask(projectId, tag, date)
            }.firstOrNull()

            val description = it.timeEntry.description

            FloatTimeEntriesItem(
                project_id = phase?.project_id ?: it.id,
                date = it.timeEntry.start.split("T").firstOrNull().orEmpty(),
                hours = it.timeEntry.duration / 60.0 / 60.0,
                notes = description,
                people_id = getFloatClientId(),
                phase_id = phase?.phase_id,
                task_id = task?.task_id,
                task_meta_id = task?.task_meta_id,
                task_name = task?.name
            )
        }

        timeEntries.forEachIndexed { index, it ->
            val data = jsonNoDefaults.encodeToString(it)
            val request = postRequest(endpoint, data)
            if (request.status != HttpStatusCode.OK) {
                Logger.log("An error occurred when uploading: ${it.notes}")
                Logger.log(request.bodyAsText() + request.status)
                return
            }
            Logger.log("Posting (${index + 1}/${timeEntries.size}): ${it.notes}")
        }
        Logger.log("💯 Uploaded all time entries to Float for $date")
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

    val projects = mutableMapOf<Int, FloatProject>()
    suspend fun getWeeklyOverview(): Map<FloatProject?, List<FloatOverview>> {
        val monday = LocalDate.now().with(WeekFields.of(Locale.FRANCE).firstDayOfWeek)
        val saturday = monday.plusWeeks(1).minusDays(2)
        val tasks = getFloatTasks(monday, saturday)

        return tasks
            .filter {
                monday <= LocalDate.parse(it.end_date) ||
                    (it.repeat_end_date != null && monday <= LocalDate.parse(it.repeat_end_date))
            }
            .map {
                val project = projects[it.project_id] ?: getProject(it.project_id)
                project?.let {
                    projects[it.project_id] = it
                }

                val phase = if (it.phase_id != 0) {
                    getPhase(it.phase_id)
                } else null
                FloatOverview(it, project, phase)
            }
            .distinctBy { it.task.task_meta_id }
            .sortedByDescending { it.weekHours }
            .groupBy { it.project }
    }

    private suspend fun getFloatTasks(start: LocalDate, end: LocalDate): List<FloatTask> {
        val floatUrl = getFloatUrl()
        val userId = getFloatClientId()
        val endpoint = "$floatUrl/tasks?people_id=$userId&start_date=$start&end_date=$end"
        val response = getRequest(url = endpoint)
        return json.decodeFromString(response.body())
    }

    private suspend fun getFloatTasks(): List<FloatTask> {
        val floatUrl = getFloatUrl()
        val endpoint = "$floatUrl/tasks"
        return getAllPages<FloatTask>(endpoint).sortedBy { it.name }
    }

    private suspend fun getFloatTask(projectId: Int, name: String, date: LocalDate): FloatTask? {
        val floatUrl = getFloatUrl()
        val userId = getFloatClientId()
        val endpoint = "$floatUrl/tasks"
        val tasks = getAllPages<FloatTask>(
            endpoint,
            "people_id=$userId&project_id=$projectId"
        ).sortedBy { it.name }

        return tasks.filter {
            date <= LocalDate.parse(it.end_date) || (it.repeat_end_date != null && date <= LocalDate.parse(
                it.repeat_end_date
            ))
        }.find { it.name == name }
    }

    suspend fun getFloatTaskNames(): List<String> {
        return getFloatTasks().filter { it.name.isNotEmpty() }.map { it.name }.distinct()
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
        return try {
            json.decodeFromString(response.body())
        } catch (e: Exception) {
            null
        }
    }

    private suspend inline fun <reified T> getAllPages(
        baseUrl: String,
        params: String? = null
    ): List<T> {
        var page = 1
        val peopleList = mutableListOf<T>()

        while (true) { //fcking dangerous
            val api = "$baseUrl?page=$page&per-page=200${params?.let { "&$it" }}"
            val response = getRequest(url = api)
            val projects = json.decodeFromString<List<T>>(response.body())
            if (projects.isEmpty()) break
            peopleList.addAll(projects)
            val totalItems =
                response.headers.toMap().getOrDefault("X-Pagination-Total-Count", emptyList())
                    .firstOrNull()
                    ?.toFloatOrNull() ?: 1f
            //Logger.log("Downloading - Progress: ${(peopleList.size.toFloat() / totalItems) * 100f}%")
            page += 1
        }

        return peopleList
    }

    suspend fun getFloatTimeEntries(from: LocalDate, to: LocalDate): List<FloatTimeEntriesItem> {
        val floatUrl = getFloatUrl()
        val userId = getFloatClientId()
        return getAllPages(
            "$floatUrl/logged-time",
            "start_date=$from&end_date=$to&people_id=$userId"
        )
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

    data class FloatProjectCreate(
        val id: Int,
        val name: String,
        val color: String?,
        val active: Int? = null
    ) {
        val isActive = active != 0
    }

    data class FloatProjectItem(
        val project: FloatProject,
        val phases: List<FloatPhaseItem>
    ) {

        val projectStrings = buildList {
            add(project.asString() to project.color)
            phases.forEach {
                add(project.asString(it) to it.color)
            }
        }

        val asNumberList = buildList {
            add(
                FloatProjectCreate(
                    project.project_id,
                    project.asStringNew(),
                    project.color,
                    project.active
                )
            )
            phases.forEach {
                add(FloatProjectCreate(it.phase_id, project.asStringNew(it), it.color, it.active))
            }
        }
    }

    suspend fun getFloatProjects(): List<FloatProjectItem> {
        val floatUrl = getFloatUrl()
        Logger.log("Downloading Float Projects ⬇️")

        val projectList = getAllPages<FloatProject>("$floatUrl/projects")

        val phases = getAllPages<FloatPhaseItem>("$floatUrl/phases")
        val grouped =
            projectList.associateWith { project -> phases.filter { it.project_id == project.project_id } }

        return grouped.map { FloatProjectItem(it.key, it.value) }
    }

    suspend fun getDatesWithoutTimeEntries(start: LocalDate, end: LocalDate): List<LocalDate> {
        val entries = getFloatTimeEntries(start, end)
        val datesWithEntries = entries.groupBy { it.date }.map { LocalDate.parse(it.key) }

        return start.datesUntil(end).toList()
            //.filterNot { it.dayOfWeek in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) } // only weekdays for now
            .filterNot { datesWithEntries.contains(it) }
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

fun FloatProject.asString(item: FloatPhaseItem? = null): String {
    return buildString {
        append("$name ($project_id)")
        if (item != null) {
            append(" - ${item.name} (${item.phase_id})")
        }
    }
}

fun FloatProject.asStringNew(item: FloatPhaseItem? = null): String {
    return buildString {
        if (item != null) {
            append("$name - ${item.name} [${item.phase_id}]")
        } else {
            append("$name [$project_id]")
        }
    }
}