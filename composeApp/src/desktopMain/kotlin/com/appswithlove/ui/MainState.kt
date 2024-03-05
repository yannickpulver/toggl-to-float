package com.appswithlove.ui

import com.appswithlove.floaat.*
import com.appswithlove.ui.feature.update.LatestRelease
import java.time.LocalDate

data class MainState(
    val togglApiKey: String? = null,
    val floatApiKey: String? = null,
    val peopleId: Int? = null,
    val people: List<FloatPeopleItem> = emptyList(),
    val logs: List<Pair<String, LogLevel>> = emptyList(),
    val loading: Boolean = false,
    val lastEntryDate: LocalDate? = null,
    val weeklyOverview: Map<FloatProject?, List<FloatOverview>> = emptyMap(),
    val missingEntryDates: List<LocalDate> = emptyList(),
    val latestRelease: LatestRelease? = null
) {
    val isValid = !togglApiKey.isNullOrEmpty() && !floatApiKey.isNullOrEmpty() && peopleId != null && peopleId != -1

    companion object {
        val Preview = MainState(weeklyOverview = mapOf(FloatProject.Preview to listOf(FloatOverview.Preview, FloatOverview.Preview), (FloatProject.Preview.copy(project_id = 2, color = "FCB9B2") to listOf(FloatOverview.Preview))))
    }
}

val Map<FloatProject?, List<FloatOverview>>.totalHours get() = values.flatten().totalHours