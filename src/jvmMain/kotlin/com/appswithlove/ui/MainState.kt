package com.appswithlove.ui

import com.appswithlove.floaat.FloatOverview
import com.appswithlove.floaat.FloatPeopleItem
import com.appswithlove.floaat.FloatRepo
import java.time.LocalDate

data class MainState(
    val togglApiKey: String? = null,
    val floatApiKey: String? = null,
    val peopleId: Int? = null,
    val people: List<FloatPeopleItem> = emptyList(),
    val logs: List<Pair<String, LogLevel>> = emptyList(),
    val loading: Boolean = false,
    val lastEntryDate: LocalDate? = null,
    val weeklyOverview: List<FloatOverview> = emptyList()
) {
    val isValid get() = !togglApiKey.isNullOrEmpty() && !floatApiKey.isNullOrEmpty() && peopleId != null && peopleId != -1
}