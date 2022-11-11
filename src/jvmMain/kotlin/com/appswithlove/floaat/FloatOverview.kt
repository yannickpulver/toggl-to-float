package com.appswithlove.floaat

data class FloatOverview(
    val task: FloatTask,
    val project: FloatProject? = null,
    val phase: FloatPhaseItem? = null,
) {
    val weekHours = task.hours * 5
}

val List<FloatOverview>.totalHours get() = this.sumOf { it.weekHours }