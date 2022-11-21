package com.appswithlove.ui

import TimeEntryForPublishing
import TimeEntryUpdate
import androidx.compose.ui.graphics.toArgb
import com.appswithlove.floaat.FloatPeopleItem
import com.appswithlove.floaat.FloatRepo
import com.appswithlove.floaat.hex2Rgb
import com.appswithlove.store.DataStore
import com.appswithlove.toggl.TogglProjectCreate
import com.appswithlove.toggl.TogglRepo
import com.appswithlove.toggl.TogglWorkspaceItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
                    getMissingEntries()
                    getWeeklyOverview()
                }
            }
        }
    }

    private fun getMissingEntries() {
        CoroutineScope(Dispatchers.IO).launch {
            val start = LocalDate.now().minusWeeks(2)
            val end = LocalDate.now()
            val entries = float.getDatesWithoutTimeEntries(start = start, end = end)
            val togglEntries = toggl.getDatesWithTimeEntries(start, end)
            val missingEntries = entries.filter { togglEntries.contains(it) }
            _state.update { it.copy(missingEntryDates = missingEntries.sorted()) }
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
        _state.update { it.copy(togglApiKey = null, floatApiKey = null, peopleId = null) }
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

    fun removeProjects() {
        CoroutineScope(Dispatchers.IO).launch {
            removeOldProjects()
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
                addTimeEntries(from)
                if (state.value.missingEntryDates.contains(from)) {
                    _state.update { it.copy(missingEntryDates = it.missingEntryDates.filter { it != from }) }
                }
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

        val floatProjects = float.getFloatProjects().map { it.asNumberList }.flatten()
        val togglProjects = toggl.getTogglProjects()

        val modifiedProjects =
            floatProjects.filter { floatProject -> togglProjects.any { it.projectIdNew == floatProject.id && (it.name != floatProject.name || it.active != floatProject.isActive) } }
                .map { floatProject ->
                    val colorString = floatColorToTogglColor(floatProject.color)
                    TogglProjectCreate(
                        name = floatProject.name,
                        color = colorString,
                        id = togglProjects.firstOrNull { it.projectIdNew == floatProject.id }?.id ?: -1,
                        active = floatProject.isActive
                    )
                }

        val newProjects =
            floatProjects.filterNot { floatProject -> togglProjects.any { it.projectIdNew == floatProject.id } }
                .filter { it.isActive }
                .map {
                    val colorString = floatColorToTogglColor(it.color)
                    TogglProjectCreate(name = it.name, color = colorString, id = it.id)
                }


//        val newProjects =
//            floatProjects.filter { floatProject -> !togglProjects.any { it.name.contains(floatProject.first) } }
//                .map {
//                    val colorString = floatColorToTogglColor(it.second)
//                    TogglProjectCreate(name = it.first, color = colorString)
//                }

        if (newProjects.isNotEmpty()) {
            Logger.log("⬆️ Syncing new Float projects to Toggl — (${newProjects.size}) of ${floatProjects.size}")
            Logger.log("---")
            toggl.pushProjectsToToggl(workspace.id, newProjects)
        } else {
            Logger.log("🎉 No new Projects found")
        }
        if (modifiedProjects.isNotEmpty()) {
            Logger.log("⬆️ Syncing modified Float projects to Toggl — (${newProjects.size}) of ${floatProjects.size}")
            toggl.putProjectsToToggl(workspace.id, modifiedProjects)
        } else {
            Logger.log("🎉 No modified Projects found")

        }

        migrateTimeEntries(workspace)
        Logger.log("🎉 Sync Complete.")

    }

    suspend fun removeOldProjects() {
        val workspace = toggl.getWorkspaces() ?: throw Exception("Couldn't get Toggle Workspace")
        val togglProjects = toggl.getTogglProjects()

        val toremove = togglProjects.filter { it.projectId != null && it.projectIdNew == null }
        toggl.deleteProjects(workspace.id, toremove.map { it.id })
    }


    suspend fun migrateTimeEntries(workspace: TogglWorkspaceItem) {
        Logger.log("🐧 Checking if migrations needed for time entries in the past 2 months")
        delay(2000)
        // Modify entries
        val entries = toggl.getTogglTimeEntries(LocalDate.now().minusMonths(2), LocalDate.now())
        val modifiedEntries = mutableListOf<Pair<Long, TimeEntryUpdate>>()

        val projects = toggl.getTogglProjects()

        entries.forEachIndexed { index, it ->
            val project = it.project_id?.let { id -> projects.find { it.id == id } }
            if (project != null) {
                val id = project.phaseId ?: project.projectId
                val projectId = projects.find { it.name.contains("[$id]") }?.id
                if (projectId != null) {
                    modifiedEntries.add(it.id to TimeEntryUpdate(projectId))
                }
            }
        }
        toggl.putTimeEntries(workspace.id, modifiedEntries)
    }


    private suspend fun updateProjectColors() {
        val workspace = toggl.getWorkspaces() ?: throw Exception("Couldn't get Toggle Workspace")

        val floatProjects = float.getFloatProjects()
        val togglProjects = toggl.getTogglProjects()

//        val existingProjects =
//            floatProjects.filter { floatProject -> togglProjects.any { it.name.contains(floatProject.first) } }
//                .map { floatProject ->
//                    val colorString = floatColorToTogglColor(floatProject.second)
//                    TogglProject(
//                        name = floatProject.first,
//                        color = colorString,
//                        project_id = togglProjects.firstOrNull { it.name.contains(floatProject.first) }?.id ?: -1
//                    )
//                }
//
//        if (existingProjects.isNotEmpty()) {
//            Logger.log("⬆️ Syncing Colors to Toggl — (${existingProjects.size}) of ${floatProjects.size}")
//            Logger.log("---")
//        } else {
//            Logger.log("🎉 All Float projects already up-to-date in Toggl!")
//            return
//        }
//
//        toggl.updateProjectColors(workspace.id, existingProjects)
    }


    private fun floatColorToTogglColor(colorString: String?): String? {
        val color = try {
            hex2Rgb(colorString)?.let { toggl.getClosestTogglColor(it) }
        } catch (exception: Exception) {
            null
        }
        return color?.toArgb()?.let { Integer.toHexString(it) }?.drop(2)?.let { "#$it" }
    }


    private suspend fun addTimeEntries(date: LocalDate) {
        val timeEntries = toggl.getTogglTimeEntries(date, date)
        Logger.log("⏱ Found ${timeEntries.size} time entries for $date on Toggl!")
        if (timeEntries.isEmpty()) {
            Logger.log("Noting to do here. Do you even work?")
            return
        }
        val projects = toggl.getTogglProjects()
        val pairs = timeEntries.map { time -> time to projects.firstOrNull { it.id == time.project_id } }

        val timeEntriesOnDate = float.getFloatTimeEntries(date, date)
        if (timeEntriesOnDate.isNotEmpty()) {
            Logger.log("---")
            Logger.err("⚠️ There are already existing time entries for that date. Can't guarantee to not mess up. So please remove them first for $date")
            return
        }

        if (pairs.any { it.second?.projectIdNew == null }) {
            Logger.err("⚠️ Some time entries don't have a valid project assigned. Please fix this and try again.")
            pairs.filter { it.second?.projectIdNew == null }.forEach {
                Logger.log("  - ${it.first.description}")
            }
            return
        }

        val data = pairs.map { (timeEntry, project) ->
            TimeEntryForPublishing(
                timeEntry = timeEntry,
                id = project?.projectIdNew ?: -1,
            )
        }

        float.pushToFloat(date, data)
    }


}