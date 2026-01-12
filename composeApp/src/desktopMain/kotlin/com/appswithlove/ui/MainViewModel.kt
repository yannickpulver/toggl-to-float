package com.appswithlove.ui

import TimeEntryForPublishing
import com.appswithlove.floaat.FloatPeopleItem
import com.appswithlove.floaat.FloatRepo
import com.appswithlove.floaat.SelectablePhase
import com.appswithlove.floaat.SelectableProject
import com.appswithlove.store.DataStore
import com.appswithlove.timetracking.ActiveTimer
import com.appswithlove.timetracking.EntryRecommendation
import com.appswithlove.timetracking.LocalTimeEntry
import com.appswithlove.timetracking.TimeTrackingRepository
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MainViewModel constructor(
    private val dataStore: DataStore,
    private val floatRepo: FloatRepo,
    private val githubRepo: GithubRepo,
    private val timeTrackingRepo: TimeTrackingRepository
) {

    private var _initDone: Boolean = false
    private var _lastRefresh: LocalDateTime? = null

    private val _loadingCounter = MutableStateFlow(0)
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _currentTime = MutableStateFlow(Clock.System.now())

    private val _state = MutableStateFlow(MainState(loading = true))

    // Reactive entries based on selected date
    private val entriesFlow = _selectedDate.flatMapLatest { date ->
        timeTrackingRepo.getEntriesForDateFlow(date.toKotlinLocalDate())
    }

    // Reactive active timer
    private val activeTimerFlow = timeTrackingRepo.getActiveTimerFlow()

    // Reactive projects from cache
    private val projectsFlow = timeTrackingRepo.getSelectableProjectsFlow()

    // Reactive phases from cache
    private val phasesFlow = timeTrackingRepo.getSelectablePhasesFlow()

    val state: StateFlow<MainState> =
        combine(
            _state,
            Logger.logs,
            _loadingCounter,
            _currentTime,
            entriesFlow,
            activeTimerFlow,
            projectsFlow,
            phasesFlow
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            val baseState = values[0] as MainState
            val logs = values[1] as List<Pair<String, LogLevel>>
            val loadingCounter = values[2] as Int
            val currentTime = values[3] as Instant
            val entries = values[4] as List<LocalTimeEntry>
            val timer = values[5] as ActiveTimer?
            val projects = values[6] as List<SelectableProject>
            val phases = values[7] as List<SelectablePhase>

            baseState.copy(
                logs = logs,
                loading = loadingCounter > 0,
                currentTime = currentTime,
                localTimeEntries = entries,
                activeTimer = timer,
                floatProjects = projects.ifEmpty { baseState.floatProjects },
                floatPhases = phases.ifEmpty { baseState.floatPhases }
            )
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

        // Timer tick every second when there's an active timer
        CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                delay(1000)
                _currentTime.value = Clock.System.now()
            }
        }
    }

    private suspend fun loadData() {
        getLastEntry()
        getMissingEntries()
        getWeeklyOverview()
        checkLastRelease()
        loadFloatProjects()
    }

    private suspend fun checkLastRelease() {
        val lastRelease = githubRepo.hasNewRelease()
        _state.update { it.copy(latestRelease = lastRelease) }
    }

    private fun getMissingEntries() {
        CoroutineScope(Dispatchers.IO).launch {
            val start = LocalDate.now().minusWeeks(2)
            val end = LocalDate.now()
            val missingEntries = timeTrackingRepo.getDatesWithUnpublishedEntries(
                start.toKotlinLocalDate(),
                end.toKotlinLocalDate()
            ).map { it.toJavaLocalDate() }
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
        _state.update { it.copy(floatApiKey = null, peopleId = null) }
    }

    fun save(floatApiKey: String?, peopleItem: FloatPeopleItem?) {
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

    fun clearLogs() {
        Logger.clear()
    }

    fun startTimer(id: Int, tag: String) {
        CoroutineScope(Dispatchers.IO).launch {
            startLocalTimer(phaseId = id, description = null, tags = if (tag.isNotEmpty()) listOf(tag) else emptyList())
        }
    }

    // Date Navigation
    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        _state.update { it.copy(selectedDate = date) }
    }

    fun goToPreviousDay() {
        selectDate(_state.value.selectedDate.minusDays(1))
    }

    fun goToNextDay() {
        selectDate(_state.value.selectedDate.plusDays(1))
    }

    fun goToToday() {
        selectDate(LocalDate.now())
    }

    // Timer Control
    fun startLocalTimer(projectId: Int? = null, phaseId: Int? = null, description: String? = null, tags: List<String> = emptyList()) {
        CoroutineScope(Dispatchers.IO).launch {
            timeTrackingRepo.startTimer(projectId, phaseId, description, tags)
            Logger.log("Timer started")
        }
    }

    fun stopLocalTimer() {
        CoroutineScope(Dispatchers.IO).launch {
            val entry = timeTrackingRepo.stopTimer()
            if (entry != null) {
                Logger.log("Timer stopped - ${entry.durationFormatted}")
            }
        }
    }

    private fun loadFloatProjects() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Logger.log("Loading Float projects...")
                val projects = floatRepo.getFloatProjects()

                // Cache to database
                timeTrackingRepo.cacheProjects(projects.map { it.project })
                timeTrackingRepo.cachePhases(projects.flatMap { it.phases })

                Logger.log("Cached ${projects.size} projects")
            } catch (e: Exception) {
                Logger.log("Failed to load Float projects: ${e.message}")
            }
        }
    }

    // Recommendations
    fun getRecommendations(query: String): List<EntryRecommendation> {
        return timeTrackingRepo.getRecommendations(query)
    }

    // Entry CRUD
    fun addEntry(
        description: String?,
        projectId: Int?,
        phaseId: Int?,
        startTime: kotlinx.datetime.Instant,
        endTime: kotlinx.datetime.Instant,
        tags: List<String> = emptyList()
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val durationSeconds = (endTime - startTime).inWholeSeconds.toInt()
            timeTrackingRepo.insertEntry(
                description = description,
                projectId = projectId,
                phaseId = phaseId,
                startTime = startTime,
                endTime = endTime,
                durationSeconds = durationSeconds,
                tags = tags
            )
            Logger.log("Entry added")
        }
    }

    fun updateEntry(entry: LocalTimeEntry) {
        CoroutineScope(Dispatchers.IO).launch {
            timeTrackingRepo.updateEntry(entry)
            Logger.log("Entry updated")
        }
    }

    fun updateEntry(id: Long, newStartTime: Instant, newEndTime: Instant) {
        CoroutineScope(Dispatchers.IO).launch {
            timeTrackingRepo.updateEntryTimes(id, newStartTime, newEndTime)
            Logger.log("Entry times updated")
        }
    }

    fun updateEntryFull(
        id: Long,
        projectId: Int?,
        phaseId: Int?,
        description: String?,
        startTime: Instant,
        endTime: Instant
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            timeTrackingRepo.updateEntryFull(id, projectId, phaseId, description, startTime, endTime)
            Logger.log("Entry updated")
        }
    }

    fun updateTimerStartTime(newStartTime: Instant) {
        CoroutineScope(Dispatchers.IO).launch {
            timeTrackingRepo.updateActiveTimerStartTime(newStartTime)
            Logger.log("Timer start time updated")
        }
    }

    fun deleteEntry(id: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            timeTrackingRepo.deleteEntry(id)
            Logger.log("Entry deleted")
        }
    }

    // Publishing
    fun publishToFloat(date: LocalDate?) {
        if (date == null) {
            CoroutineScope(Dispatchers.IO).launch {
                SnackbarStateHolder.error("Select a date first")
            }
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            withLoading {
                try {
                    val kotlinDate = date.toKotlinLocalDate()
                    val entries = timeTrackingRepo.getUnpublishedToFloatForDate(kotlinDate)

                    if (entries.isEmpty()) {
                        Logger.log("No unpublished entries for $date")
                        return@withLoading
                    }

                    val timeEntriesOnDate = floatRepo.getFloatTimeEntries(date, date)
                    if (timeEntriesOnDate.isNotEmpty()) {
                        Logger.err("There are already time entries on Float for $date")
                        return@withLoading
                    }

                    val data = entries.mapNotNull { entry ->
                        // Use phaseId if available, otherwise projectId
                        val id = entry.phaseId ?: entry.projectId ?: return@mapNotNull null
                        TimeEntryForPublishing(
                            timeEntry = entry.toTogglTimeEntry(),
                            id = id
                        )
                    }

                    if (data.isEmpty()) {
                        Logger.err("No entries with valid project/phase IDs to publish")
                        return@withLoading
                    }

                    val success = floatRepo.pushToFloat(date, data)
                    if (success) {
                        entries.forEach { timeTrackingRepo.markSyncedToFloat(it.id) }
                        if (_state.value.missingEntryDates.contains(date)) {
                            _state.update { it.copy(missingEntryDates = it.missingEntryDates.filter { d -> d != date }) }
                        }
                    }
                } catch (e: Exception) {
                    Logger.err("Error publishing: ${e.message}")
                }
            }
        }
    }

    // Helper to get project/phase name for display
    fun getProjectName(projectId: Int?, phaseId: Int?): String? {
        val st = state.value
        val projectName = st.floatProjects.find { it.projectId == projectId }?.name
        val phaseName = phaseId?.let { id -> st.floatPhases.find { it.phaseId == id }?.name }
        return when {
            projectName != null && phaseName != null -> "$projectName - $phaseName"
            projectName != null -> projectName
            else -> null
        }
    }
}
