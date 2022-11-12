package com.appswithlove.floaat

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.graphics.Color

data class FloatOverview(
    val task: FloatTask,
    val project: FloatProject? = null,
    val phase: FloatPhaseItem? = null,
) {
    val weekHours = task.hours * 5
    //val color = (phase?.color?.let(::hex2Rgb) ?: project?.color?.let(::hex2Rgb) ?: Color.DarkGray).copy(alpha = 1f)
    val color = (project?.color?.let(::hex2Rgb) ?: Color.DarkGray).copy(alpha = 1f)
}

val List<FloatOverview>.totalHours get() = this.sumOf { it.weekHours }