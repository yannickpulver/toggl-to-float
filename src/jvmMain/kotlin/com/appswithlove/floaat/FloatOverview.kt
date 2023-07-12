package com.appswithlove.floaat

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.graphics.Color

data class FloatOverview(
    val task: FloatTask,
    val project: FloatProject? = null,
    val phase: FloatPhaseItem? = null,
) {
    val weekHours = (task.hours * 5 * 100).toInt() / 100.0
    val color = (project?.color?.let(::hex2Rgb) ?: Color.DarkGray).copy(alpha = 1f)


    val title = (task.name.ifEmpty { null } ?: phase?.name ?: project?.name ?: "Unknown")
    val id = phase?.phase_id ?: project?.project_id

    companion object {
        val Preview = FloatOverview(FloatTask.Preview)
    }
}

val List<FloatOverview>.totalHours get() = this.sumOf { it.weekHours }


