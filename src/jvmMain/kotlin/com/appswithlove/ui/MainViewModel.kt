package com.appswithlove.ui

import TimeEntryForPublishing
import androidx.compose.ui.graphics.toArgb
import com.appswithlove.floaat.FloatPeopleItem
import com.appswithlove.floaat.FloatRepo
import com.appswithlove.floaat.hex2Rgb
import com.appswithlove.store.DataStore
import com.appswithlove.toggl.TogglProject
import com.appswithlove.toggl.TogglProjectCreate
import com.appswithlove.toggl.TogglRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.*
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MainViewModel {

    private val dataStore = DataStore()
    private val float = FloatRepo(dataStore)
    private val toggl = TogglRepo(dataStore)
    private var initDone: Boolean = false

    private val _state = MutableStateFlow(MainState(loading = true))
    val state: StateFlow<MainState> = combine(_state, Logger.logs) { state, logs ->
        state.copy(logs = logs)
    }.stateIn(
        scope = CoroutineScope(Dispatchers.Default),
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainState(),
    )

    init {
        refresh()

        CoroutineScope(Dispatchers.IO).launch {
            _state.collectLatest {
                if (it.isValid && !initDone) {
                    initDone = true
                    //loadSwicaWeek()
                    getLastEntry()
                    getWeeklyOverview()
                }
            }
        }
    }

    private fun getWeeklyOverview() {
        CoroutineScope(Dispatchers.IO).launch {
            val overview = float.getWeeklyOverview()
            _state.update { it.copy(weeklyOverview = overview) }
        }
    }

    private fun getLastEntry() {
        CoroutineScope(Dispatchers.IO).launch {
            val entries = float.getFloatTimeEntries(from = LocalDate.now().minusWeeks(2), to = LocalDate.now())
            val max = entries.maxByOrNull { it.date }
            val parsedDate = max?.date?.let { LocalDate.parse(it) }
            _state.update { it.copy(lastEntryDate = parsedDate) }
        }
    }

    private fun loadSwicaWeek() {
        CoroutineScope(Dispatchers.IO).launch {
            val lastMonday = LocalDate.now().with(WeekFields.of(Locale.FRANCE).firstDayOfWeek).minusWeeks(1)
            val lastSunday = lastMonday.plusWeeks(1).minusDays(1)
            val timeEntries = float.getFloatTimeEntries(lastMonday, lastSunday)
            val swicaEntries = timeEntries.filter { it.project_id == 7728055 }
            swicaEntries.sortedBy { it.date }.groupBy { it.date }.forEach { (date, entries) ->
                println(date)

                val newEntries = entries.groupBy { it.notes to it.project_id }.map { (pair, entries) ->
                    entries.first().copy(hours = entries.sumOf { it.hours })
                }

                newEntries.forEach {
                    val duration = it.hours.toDuration(DurationUnit.HOURS)
                    val phase = if (it.phase_id == 305879) " (SLA)" else ""
                    println(
                        "${
                            duration.toComponents { hours, minutes, _, _ ->
                                "${hours}:${
                                    String.format(
                                        "%02d",
                                        minutes
                                    )
                                }"
                            }
                        } — ${it.notes} $phase"
                    )
                }
            }
        }
    }

    fun clear() {
        Logger.clear()
        dataStore.clear()
        refresh()
    }

    fun fetchProjects() {
        CoroutineScope(Dispatchers.IO).launch {
            fetchProjectsInt()
        }
    }

    fun archiveProjects() {
        CoroutineScope(Dispatchers.IO).launch {
            toggl.getTogglProjects()
        }
    }

    fun updateColors() {
        CoroutineScope(Dispatchers.IO).launch {
            updateProjectColors()
        }
    }

    fun addTimeEntries(from: LocalDate?) {
        from ?: return // todo add snackbar

        try {
            //val toDate = LocalDate.parse(to)

            CoroutineScope(Dispatchers.IO).launch {
                addTimeEntries(from, from)
            }
        } catch (exception: java.lang.Exception) {
            Logger.err("Double check your dates to have format YYYY-MM-DD")
        }

    }

    fun save(togglApiKey: String?, floatApiKey: String?, peopleItem: FloatPeopleItem?) {
        togglApiKey?.let { dataStore.setTogglApiKey(togglApiKey) }
        floatApiKey?.let { dataStore.setFloatApiKey(floatApiKey) }
        peopleItem?.let { dataStore.setFloatClientId(peopleItem.people_id) }

        refresh()
    }

    private fun refresh() {
        CoroutineScope(Dispatchers.IO).launch {
            val store = dataStore.getStore
            _state.update {
                it.copy(
                    togglApiKey = store.togglKey,
                    floatApiKey = store.floatKey,
                    peopleId = store.floatClientId,
                    people = if (store.shouldLoadPeople) float.getFloatPeople() else emptyList(),
                    loading = false
                )
            }
        }
    }

    private suspend fun fetchProjectsInt() {
        val workspace = toggl.getWorkspaces() ?: throw Exception("Couldn't get Toggle Workspace")

        val floatProjects = float.getFloatProjects()
        val togglProjects = toggl.getTogglProjects()

        Logger.log(togglProjects.toString())
        val newProjects =
            floatProjects.filter { floatProject -> !togglProjects.any { it.name.contains(floatProject.first) } }
                .map {
                    val colorString = floatColorToTogglColor(it.second)
                    TogglProjectCreate(name = it.first, color = colorString)
                }

        if (newProjects.isNotEmpty()) {
            Logger.log("⬆️ Syncing new Float projects to Toggl — (${newProjects.size}) of ${floatProjects.size}")
            Logger.log("---")
        } else {
            Logger.log("🎉 All Float Projects already up-to-date in Toggl!")
            return
        }

        toggl.pushProjectsToToggl(workspace.id, newProjects)
    }

    private suspend fun updateProjectColors() {
        val workspace = toggl.getWorkspaces() ?: throw Exception("Couldn't get Toggle Workspace")

        val floatProjects = float.getFloatProjects()
        val togglProjects = toggl.getTogglProjects()

        val existingProjects =
            floatProjects.filter { floatProject -> togglProjects.any { it.name.contains(floatProject.first) } }
                .map { floatProject ->
                    val colorString = floatColorToTogglColor(floatProject.second)
                    TogglProject(
                        name = floatProject.first,
                        color = colorString,
                        project_id = togglProjects.firstOrNull { it.name.contains(floatProject.first) }?.id ?: -1
                    )
                }

        if (existingProjects.isNotEmpty()) {
            Logger.log("⬆️ Syncing Colors to Toggl — (${existingProjects.size}) of ${floatProjects.size}")
            Logger.log("---")
        } else {
            Logger.log("🎉 All Float projects already up-to-date in Toggl!")
            return
        }

        toggl.updateProjectColors(workspace.id, existingProjects)
    }


    private fun floatColorToTogglColor(colorString: String?): String? {
        val color = try {
            hex2Rgb(colorString)?.let { toggl.getClosestTogglColor(it) }
        } catch (exception: Exception) {
            null
        }
        return color?.toArgb()?.let { Integer.toHexString(it) }?.drop(2)?.let { "#$it" }
    }


    private suspend fun addTimeEntries(from: LocalDate, to: LocalDate) {
        val timeEntries = toggl.getTogglTimeEntries(from, to)
        Logger.log("⏱ Found ${timeEntries.size} time entries for $from - $to on Toggl!")
        if (timeEntries.isEmpty()) {
            Logger.log("Noting to do here. Do you even work?")
            return
        }
        val projects = toggl.getTogglProjects()
        val pairs = timeEntries.map { time -> time to projects.firstOrNull { it.id == time.project_id } }

        val timeEntriesOnDate = float.getFloatTimeEntries(from, to)
        if (timeEntriesOnDate.isNotEmpty()) {
            Logger.log("---")
            Logger.err("⚠️ There are already existing time entries for that date. Can't guarantee to not mess up. So please remove them first for $from - $to")
            return
        }

        if (pairs.any { it.second?.projectId == null }) {
            Logger.err("⚠️ Some time entries don't have a valid project assigned. Please fix this and try again.")
            pairs.filter { it.second?.projectId == null }.forEach {
                Logger.log("  - ${it.first.description}")
            }
            return
        }

        val data = pairs.map {
            TimeEntryForPublishing(
                timeEntry = it.first,
                projectId = it.second?.projectId ?: -1,
                phaseId = it.second?.phaseId
            )
        }

        float.pushToFloat(from, to, data)
    }


}