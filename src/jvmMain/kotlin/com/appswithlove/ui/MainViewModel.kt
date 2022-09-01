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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainViewModel {

    private val dataStore = DataStore()
    private val float = FloatRepo(dataStore)
    private val toggl = TogglRepo(dataStore)

    private val _state = MutableStateFlow(MainState(loading = true))
    val state: StateFlow<MainState> = combine(_state, Logger.logs) { state, logs ->
        state.copy(logs = logs)
    }.stateIn(
        scope = GlobalScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainState(),
    )

    init {
        refresh()
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

    fun addTimeEntries(from: String, to: String) {

        try {
            val fromDate = LocalDate.parse(from)
            val toDate = LocalDate.parse(to)

            CoroutineScope(Dispatchers.IO).launch {
                addTimeEntries(fromDate, toDate)
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
                        project_id = togglProjects.firstOrNull { it.name.contains(floatProject.first) }?.id ?: -1)
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