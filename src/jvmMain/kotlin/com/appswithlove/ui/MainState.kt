package com.appswithlove.ui

import com.appswithlove.floaat.*
import java.time.LocalDate

data class MainState(
    val togglApiKey: String? = null,
    val floatApiKey: String? = null,
    val peopleId: Int? = null,
    val people: List<FloatPeopleItem> = emptyList(),
    val logs: List<Pair<String, LogLevel>> = emptyList(),
    val loading: Boolean = false,
    val lastEntryDate: LocalDate? = null,
    val weeklyOverview: Map<FloatProject?, List<FloatOverview>> = emptyMap()
) {
    val isValid get() = !togglApiKey.isNullOrEmpty() && !floatApiKey.isNullOrEmpty() && peopleId != null && peopleId != -1
}

val Map<FloatProject?, List<FloatOverview>>.totalHours get() = values.flatten().totalHours