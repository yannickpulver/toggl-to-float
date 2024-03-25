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
import com.appswithlove.ui.feature.snackbar.SnackbarStateHolder
import com.appswithlove.ui.feature.update.GithubRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MainViewModel constructor(
    private val dataStore: DataStore,
    private val floatRepo: FloatRepo,
    private val togglRepo: TogglRepo,
    private val githubRepo: GithubRepo
) {

    private var _initDone: Boolean = false
    private var _lastRefresh: LocalDateTime? = null

    private val _loadingCounter = MutableStateFlow(0)

    private val _state = MutableStateFlow(MainState(loading = true))
    val state: StateFlow<MainState> =
        combine(_state, Logger.logs, _loadingCounter) { state, logs, loadingCounter ->
            state.copy(logs = logs, loading = loadingCounter > 0)
        }.stateIn(
            scope = CoroutineScope(Dispatchers.Default),
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MainState(),
        )

    init {
        refresh(true)

        CoroutineScope(Dispatchers.IO).launch {
            _state.collectLatest {
                if (it.isValid && !_initDone) {
                    _initDone = true
                    loadData()
                }
            }
        }
    }

    private suspend fun loadData() {
        getLastEntry()
        getMissingEntries()
        getWeeklyOverview()
        checkLastRelease()
    }

    private suspend fun checkLastRelease() {
        val lastRelease = githubRepo.hasNewRelease()
        _state.update { it.copy(latestRelease = lastRelease) }
    }

    private fun getMissingEntries() {
        CoroutineScope(Dispatchers.IO).launch {
            val start = LocalDate.now().minusWeeks(2)
            val end = LocalDate.now()
            val entries = floatRepo.getDatesWithoutTimeEntries(start = start, end = end.plusDays(1))
            val togglEntries = togglRepo.getDatesWithTimeEntries(start, end.plusDays(1))
            val missingEntries = entries.filter { togglEntries.contains(it) }
            _state.update { it.copy(missingEntryDates = missingEntries.sorted()) }
        }
    }

    private fun getWeeklyOverview() {
        CoroutineScope(Dispatchers.IO).launch {
            withLoading {
                val overview = floatRepo.getWeeklyOverview()
                _state.update { it.copy(weeklyOverview = overview) }
            }
        }
    }

    private suspend fun withLoading(block: suspend () -> Unit) {
        _loadingCounter.update { it + 1 }
        block()
        _loadingCounter.update { it - 1 }
    }

    private fun getLastEntry() {
        CoroutineScope(Dispatchers.IO).launch {
            val entries = floatRepo.getFloatTimeEntries(
                from = LocalDate.now().minusWeeks(2),
                to = LocalDate.now()
            )
            val max = entries.maxByOrNull { it.date }
            val parsedDate = max?.date?.let { LocalDate.parse(it) }
            _state.update { it.copy(lastEntryDate = parsedDate) }
        }
    }

    fun loadTimeLastWeek(projectId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val lastMonday =
                LocalDate.now().with(WeekFields.of(Locale.FRANCE).firstDayOfWeek).minusWeeks(1)
            val lastSunday = lastMonday.plusWeeks(1).minusDays(1)
            val timeEntries = floatRepo.getFloatTimeEntries(lastMonday, lastSunday)
            val projectEntries = timeEntries.filter { it.project_id == projectId }
            projectEntries.sortedBy { it.date }.groupBy { it.date }.forEach { (date, entries) ->
                Logger.log(date)

                val newEntries =
                    entries.groupBy { it.notes to it.project_id }.map { (pair, entries) ->
                        entries.first().copy(hours = entries.sumOf { it.hours })
                    }

                newEntries.forEach {
                    val duration = it.hours.toDuration(DurationUnit.HOURS)
                    Logger.log(
                        "${
                            duration.toComponents { hours, minutes, _, _ ->
                                "${hours}:${
                                    String.format(
                                        "%02d",
                                        minutes
                                    )
                                }"
                            }
                        } — ${it.notes} (${it.phase_id})"
                    )
                }
            }
        }
    }

    fun reset() {
        Logger.clear()
        dataStore.clear()
        refresh(true)
        _state.update { it.copy(togglApiKey = null, floatApiKey = null, peopleId = null) }
    }

    fun fetchProjects() {
        CoroutineScope(Dispatchers.IO).launch {
            withLoading {
                fetchProjectsInt()
            }
        }
    }

    fun archiveProjects() {
        CoroutineScope(Dispatchers.IO).launch {
            togglRepo.getTogglProjects()
        }
    }

    fun removeProjects() {
        CoroutineScope(Dispatchers.IO).launch {
            removeOldProjects()
        }
    }

    fun addTimeEntries(from: LocalDate?) {
        if (from == null) {
            CoroutineScope(Dispatchers.IO).launch {
                SnackbarStateHolder.error("Double check your date")
            }
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            withLoading {
                try {
                    val success = addTimeEntries(from)
                    if (success && state.value.missingEntryDates.contains(from)) {
                        _state.update { it.copy(missingEntryDates = it.missingEntryDates.filter { it != from }) }
                    }
                } catch (exception: java.lang.Exception) {
                    Logger.err("Double check your dates to have format YYYY-MM-DD")
                    exception.message?.let { Logger.err(it) }
                }
            }
        }
    }

    fun save(togglApiKey: String?, floatApiKey: String?, peopleItem: FloatPeopleItem?) {
        togglApiKey?.let { dataStore.setTogglApiKey(togglApiKey) }
        floatApiKey?.let { dataStore.setFloatApiKey(floatApiKey) }
        peopleItem?.let { dataStore.setFloatClientId(peopleItem.people_id) }
        refresh(true)
    }

    fun refresh(force: Boolean = false) {
        if (!force && _lastRefresh?.plusMinutes(3)?.isAfter(LocalDateTime.now()) == true) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val store = dataStore.getStore
            _state.update {
                it.copy(
                    togglApiKey = store.togglKey,
                    floatApiKey = store.floatKey,
                    peopleId = store.floatClientId,
                    people = if (store.shouldLoadPeople) floatRepo.getFloatPeople() else emptyList(),
                    loading = false
                )
            }

            if (_initDone && _state.value.isValid) {
                loadData()
            }
            _lastRefresh = LocalDateTime.now()
        }
    }

    private suspend fun fetchProjectsInt() {
        val workspace =
            togglRepo.getWorkspaces() ?: throw Exception("Couldn't get Toggle Workspace")

        // projects
        val floatProjects = floatRepo.getFloatProjects().map { it.asNumberList }.flatten()
        val togglProjects = togglRepo.getTogglProjects()

        val modifiedProjects =
            floatProjects.filter { floatProject -> togglProjects.any { it.projectIdNew == floatProject.id && (it.name != floatProject.name || it.active != floatProject.isActive) } }
                .map { floatProject ->
                    val colorString = floatColorToTogglColor(floatProject.color)
                    TogglProjectCreate(
                        name = floatProject.name,
                        color = colorString,
                        id = togglProjects.firstOrNull { it.projectIdNew == floatProject.id }?.id
                            ?: -1,
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

        if (newProjects.isNotEmpty()) {
            Logger.log("⬆️ Syncing new Float projects to Toggl — (${newProjects.size}) of ${floatProjects.size}")
            Logger.log(Logger.SPACER)
            togglRepo.pushProjectsToToggl(workspace.id, newProjects)
        }
        if (modifiedProjects.isNotEmpty()) {
            Logger.log("⬆️ Syncing modified Float projects to Toggl — (${newProjects.size}) of ${floatProjects.size}")
            togglRepo.putProjectsToToggl(workspace.id, modifiedProjects)
        }

        // tags
        val togglTags = togglRepo.getTogglTags()
        val floatTags = floatRepo.getFloatTaskNames()

        val newTags = floatTags.filterNot { floatTag -> togglTags.any { it.name == floatTag } }
        if (newTags.isNotEmpty()) {
            Logger.log("⬆️ Syncing new Float tags to Toggl")
            togglRepo.pushTagsToToggl(workspace.id, newTags)
        }

        // time entries
        migrateTimeEntries(workspace)

        // clean old projects
        removeOldProjects()

        Logger.log("🎉 Sync Complete.")
    }

    fun clearLogs() {
        Logger.clear()
    }

    suspend fun removeOldProjects() {
        val workspace =
            togglRepo.getWorkspaces() ?: throw Exception("Couldn't get Toggle Workspace")
        val togglProjects = togglRepo.getTogglProjects()

        val toremove = togglProjects.filter { it.projectId != null && it.projectIdNew == null }
        togglRepo.deleteProjects(workspace.id, toremove.map { it.id })
    }

    suspend fun migrateTimeEntries(workspace: TogglWorkspaceItem) {
        Logger.log("🐧 Checking for migrations...")
        delay(2000)
        // Modify entries
        val entries = togglRepo.getTogglTimeEntries(LocalDate.now().minusMonths(2), LocalDate.now())
        val modifiedEntries = mutableListOf<Pair<Long, TimeEntryUpdate>>()

        val projects = togglRepo.getTogglProjects()

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
        togglRepo.putTimeEntries(workspace.id, modifiedEntries)
    }

    fun startTimer(id: Int, tag: String) {

        CoroutineScope(Dispatchers.IO).launch {
            val workspace =
                togglRepo.getWorkspaces() ?: throw Exception("Couldn't get Toggle Workspace")
            // get id of project or phase
            val project = togglRepo.getTogglProjects().first { it.name.contains(id.toString()) }

            // find toggl project that contains this id
            togglRepo.startTimer(workspace.id, project, tag)
        }
    }

    private fun floatColorToTogglColor(colorString: String?): String? {
        val color = try {
            hex2Rgb(colorString)?.let { togglRepo.getClosestTogglColor(it) }
        } catch (exception: Exception) {
            null
        }
        return color?.toArgb()?.let { Integer.toHexString(it) }?.drop(2)?.let { "#$it" }
    }

    private suspend fun addTimeEntries(date: LocalDate): Boolean {
        val timeEntries = togglRepo.getTogglTimeEntries(date, date)
        Logger.log("⏱ Found ${timeEntries.size} time entries for $date on Toggl!")
        if (timeEntries.isEmpty()) {
            Logger.log("Noting to do here. Do you even work?")
            return true
        }
        val projects = togglRepo.getTogglProjects()
        val pairs =
            timeEntries.map { time -> time to projects.firstOrNull { it.id == time.project_id } }

        val timeEntriesOnDate = floatRepo.getFloatTimeEntries(date, date)
        if (timeEntriesOnDate.isNotEmpty()) {
            Logger.log(Logger.SPACER)
            Logger.err("⚠️ There are already existing time entries for $date. Please remove them and try again.")
            return false
        }

        if (pairs.any { it.second?.projectIdNew == null }) {
            Logger.err("⚠️ Some time entries don't have a valid project assigned. Please fix this and try again.")
            pairs.filter { it.second?.projectIdNew == null }.forEach {
                Logger.log("  - ${it.first.description}")
            }
            return false
        }

        val data = pairs.map { (timeEntry, project) ->
            TimeEntryForPublishing(
                timeEntry = timeEntry,
                id = project?.projectIdNew ?: -1,
            )
        }

        return floatRepo.pushToFloat(date, data)
    }
}