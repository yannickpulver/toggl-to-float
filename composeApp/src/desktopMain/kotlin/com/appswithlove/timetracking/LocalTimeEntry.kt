package com.appswithlove.timetracking

import TimeEntry
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class LocalTimeEntry(
    val id: Long,
    val description: String?,
    val projectId: Int?,
    val phaseId: Int?,
    val startTime: Instant,
    val endTime: Instant?,
    val durationSeconds: Int,
    val tags: List<String>,
    val syncedToFloat: Boolean,
    val syncedToJira: Boolean
) {
    val isRunning: Boolean get() = endTime == null

    val date: LocalDate
        get() = startTime.toLocalDateTime(TimeZone.currentSystemDefault()).date

    val durationFormatted: String
        get() {
            val hours = durationSeconds / 3600
            val minutes = (durationSeconds % 3600) / 60
            return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }

    val durationHours: Double
        get() = durationSeconds / 3600.0

    fun toTogglTimeEntry(): TimeEntry {
        return TimeEntry(
            at = startTime.toString(),
            billable = false,
            description = description,
            duration = durationSeconds,
            duronly = false,
            id = id,
            project_id = projectId,
            start = startTime.toString(),
            stop = endTime?.toString(),
            tag_ids = null,
            tags = tags.ifEmpty { null },
            uid = 0,
            user_id = 0,
            wid = 0,
            workspace_id = 0
        )
    }
}

data class ActiveTimer(
    val projectId: Int?,
    val phaseId: Int?,
    val description: String?,
    val startTime: Instant,
    val tags: List<String>
) {
    fun elapsedSeconds(): Long {
        val now = kotlinx.datetime.Clock.System.now()
        return (now - startTime).inWholeSeconds
    }
}

data class EntryRecommendation(
    val description: String,
    val projectId: Int,
    val phaseId: Int?,
    val projectName: String,
    val phaseName: String?
) {
    val displayText: String
        get() = buildString {
            append(description)
            append(" - ")
            append(projectName)
            if (phaseName != null) append(" ($phaseName)")
        }
}
