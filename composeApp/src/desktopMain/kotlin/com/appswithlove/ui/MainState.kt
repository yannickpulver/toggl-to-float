package com.appswithlove.ui

import com.appswithlove.floaat.*
import com.appswithlove.timetracking.ActiveTimer
import com.appswithlove.timetracking.EntryRecommendation
import com.appswithlove.timetracking.LocalTimeEntry
import com.appswithlove.ui.feature.update.LatestRelease
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.time.LocalDate

data class MainState(
    val floatApiKey: String? = null,
    val peopleId: Int? = null,
    val people: List<FloatPeopleItem> = emptyList(),
    val logs: List<Pair<String, LogLevel>> = emptyList(),
    val loading: Boolean = false,
    val lastEntryDate: LocalDate? = null,
    val weeklyOverview: Map<FloatProject?, List<FloatOverview>> = emptyMap(),
    val missingEntryDates: List<LocalDate> = emptyList(),
    val latestRelease: LatestRelease? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val localTimeEntries: List<LocalTimeEntry> = emptyList(),
    val activeTimer: ActiveTimer? = null,
    val floatProjects: List<SelectableProject> = emptyList(),
    val floatPhases: List<SelectablePhase> = emptyList(),
    val currentTime: Instant = Clock.System.now(),
    val recommendations: List<EntryRecommendation> = emptyList()
) {
    val isValid = !floatApiKey.isNullOrEmpty() && peopleId != null && peopleId != -1

    companion object {
        val Preview = MainState(weeklyOverview = mapOf(FloatProject.Preview to listOf(FloatOverview.Preview, FloatOverview.Preview), (FloatProject.Preview.copy(project_id = 2, color = "FCB9B2") to listOf(FloatOverview.Preview))))
    }
}

val Map<FloatProject?, List<FloatOverview>>.totalHours get() = values.flatten().totalHours