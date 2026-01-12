package com.appswithlove.timetracking

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.appswithlove.database.TimeTrackingDatabase
import com.appswithlove.floaat.FloatPhaseItem
import com.appswithlove.floaat.FloatProject
import com.appswithlove.floaat.SelectablePhase
import com.appswithlove.floaat.SelectableProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.appswithlove.database.TimeEntry as DbTimeEntry
import com.appswithlove.database.ActiveTimer as DbActiveTimer

class TimeTrackingRepository(
    private val database: TimeTrackingDatabase,
    private val json: Json
) {
    private val queries get() = database.timeTrackingQueries

    fun getEntriesForDate(date: LocalDate): List<LocalTimeEntry> {
        val dateStr = date.toString()
        return queries.getTimeEntriesForDate(dateStr).executeAsList().map { it.toLocalTimeEntry() }
    }

    fun getEntriesForDateRange(startDate: LocalDate, endDate: LocalDate): List<LocalTimeEntry> {
        return queries.getTimeEntriesForDateRange(startDate.toString(), endDate.toString())
            .executeAsList()
            .map { it.toLocalTimeEntry() }
    }

    fun getUnpublishedToFloat(): List<LocalTimeEntry> {
        return queries.getUnpublishedToFloat().executeAsList().map { it.toLocalTimeEntryFromFloat() }
    }

    fun getUnpublishedToFloatForDate(date: LocalDate): List<LocalTimeEntry> {
        return queries.getUnpublishedToFloatForDate(date.toString())
            .executeAsList()
            .map { it.toLocalTimeEntryFromUnpublished() }
    }

    fun getUnpublishedToJira(issuePrefix: String): List<LocalTimeEntry> {
        return queries.getUnpublishedToJira(issuePrefix).executeAsList().map { it.toLocalTimeEntryFromJira() }
    }

    fun getDatesWithEntries(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
        return queries.getDatesWithEntries(startDate.toString(), endDate.toString())
            .executeAsList()
            .mapNotNull { dateStr -> runCatching { LocalDate.parse(dateStr) }.getOrNull() }
    }

    fun getDatesWithUnpublishedEntries(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
        return queries.getDatesWithUnpublishedEntries(startDate.toString(), endDate.toString())
            .executeAsList()
            .mapNotNull { dateStr -> runCatching { LocalDate.parse(dateStr) }.getOrNull() }
    }

    fun insertEntry(
        description: String?,
        projectId: Int?,
        phaseId: Int?,
        startTime: Instant,
        endTime: Instant?,
        durationSeconds: Int,
        tags: List<String>
    ): Long {
        val now = Clock.System.now().toString()
        queries.insertTimeEntry(
            description = description,
            project_id = projectId?.toLong(),
            phase_id = phaseId?.toLong(),
            start_time = startTime.toString(),
            end_time = endTime?.toString(),
            duration_seconds = durationSeconds.toLong(),
            tags = json.encodeToString(tags),
            created_at = now,
            modified_at = now
        )
        return queries.getLastInsertId().executeAsOne()
    }

    fun updateEntry(entry: LocalTimeEntry) {
        queries.updateTimeEntry(
            id = entry.id,
            description = entry.description,
            projectId = entry.projectId?.toLong(),
            phaseId = entry.phaseId?.toLong(),
            startTime = entry.startTime.toString(),
            endTime = entry.endTime?.toString(),
            durationSeconds = entry.durationSeconds.toLong(),
            tags = json.encodeToString(entry.tags),
            modifiedAt = Clock.System.now().toString()
        )
    }

    fun updateEntryTimes(id: Long, newStartTime: Instant, newEndTime: Instant) {
        val durationSeconds = (newEndTime - newStartTime).inWholeSeconds
        queries.updateTimeEntryTimes(
            id = id,
            startTime = newStartTime.toString(),
            endTime = newEndTime.toString(),
            durationSeconds = durationSeconds,
            modifiedAt = Clock.System.now().toString()
        )
    }

    fun updateEntryFull(
        id: Long,
        projectId: Int?,
        phaseId: Int?,
        description: String?,
        startTime: Instant,
        endTime: Instant
    ) {
        val durationSeconds = (endTime - startTime).inWholeSeconds
        queries.updateTimeEntry(
            id = id,
            description = description,
            projectId = projectId?.toLong(),
            phaseId = phaseId?.toLong(),
            startTime = startTime.toString(),
            endTime = endTime.toString(),
            durationSeconds = durationSeconds,
            tags = "[]",
            modifiedAt = Clock.System.now().toString()
        )
    }

    fun updateActiveTimerStartTime(newStartTime: Instant) {
        queries.updateActiveTimerStartTime(newStartTime.toString())
    }

    fun deleteEntry(id: Long) {
        queries.deleteTimeEntry(id)
    }

    fun markSyncedToFloat(id: Long) {
        queries.markSyncedToFloat(id)
    }

    fun markSyncedToJira(id: Long) {
        queries.markSyncedToJira(id)
    }

    fun getActiveTimer(): ActiveTimer? {
        return queries.getActiveTimer().executeAsOneOrNull()?.toActiveTimer()
    }

    fun startTimer(
        projectId: Int?,
        phaseId: Int?,
        description: String?,
        tags: List<String>
    ) {
        queries.startTimer(
            projectId = projectId?.toLong(),
            phaseId = phaseId?.toLong(),
            description = description,
            startTime = Clock.System.now().toString(),
            tags = json.encodeToString(tags)
        )
    }

    fun stopTimer(): LocalTimeEntry? {
        val timer = getActiveTimer() ?: return null
        val endTime = Clock.System.now()
        val durationSeconds = (endTime - timer.startTime).inWholeSeconds.toInt()

        val id = insertEntry(
            description = timer.description,
            projectId = timer.projectId,
            phaseId = timer.phaseId,
            startTime = timer.startTime,
            endTime = endTime,
            durationSeconds = durationSeconds,
            tags = timer.tags
        )

        queries.stopTimer()

        return queries.getTimeEntryById(id).executeAsOneOrNull()?.toLocalTimeEntry()
    }

    private fun parseTags(tagsJson: String?): List<String> {
        if (tagsJson.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(String.serializer()), tagsJson)
        }.getOrElse { emptyList() }
    }

    private fun DbTimeEntry.toLocalTimeEntry(): LocalTimeEntry {
        return LocalTimeEntry(
            id = id,
            description = description,
            projectId = project_id?.toInt(),
            phaseId = phase_id?.toInt(),
            startTime = Instant.parse(start_time),
            endTime = end_time?.let { Instant.parse(it) },
            durationSeconds = duration_seconds.toInt(),
            tags = parseTags(tags),
            syncedToFloat = synced_to_float == 1L,
            syncedToJira = synced_to_jira == 1L
        )
    }

    private fun com.appswithlove.database.GetUnpublishedToFloat.toLocalTimeEntryFromFloat(): LocalTimeEntry {
        return LocalTimeEntry(
            id = id,
            description = description,
            projectId = project_id?.toInt(),
            phaseId = phase_id?.toInt(),
            startTime = Instant.parse(start_time),
            endTime = Instant.parse(end_time),
            durationSeconds = duration_seconds.toInt(),
            tags = parseTags(tags),
            syncedToFloat = synced_to_float == 1L,
            syncedToJira = synced_to_jira == 1L
        )
    }

    private fun com.appswithlove.database.GetUnpublishedToFloatForDate.toLocalTimeEntryFromUnpublished(): LocalTimeEntry {
        return LocalTimeEntry(
            id = id,
            description = description,
            projectId = project_id?.toInt(),
            phaseId = phase_id?.toInt(),
            startTime = Instant.parse(start_time),
            endTime = Instant.parse(end_time),
            durationSeconds = duration_seconds.toInt(),
            tags = parseTags(tags),
            syncedToFloat = synced_to_float == 1L,
            syncedToJira = synced_to_jira == 1L
        )
    }

    private fun com.appswithlove.database.GetUnpublishedToJira.toLocalTimeEntryFromJira(): LocalTimeEntry {
        return LocalTimeEntry(
            id = id,
            description = description,
            projectId = project_id?.toInt(),
            phaseId = phase_id?.toInt(),
            startTime = Instant.parse(start_time),
            endTime = Instant.parse(end_time),
            durationSeconds = duration_seconds.toInt(),
            tags = parseTags(tags),
            syncedToFloat = synced_to_float == 1L,
            syncedToJira = synced_to_jira == 1L
        )
    }

    private fun DbActiveTimer.toActiveTimer(): ActiveTimer {
        return ActiveTimer(
            projectId = project_id?.toInt(),
            phaseId = phase_id?.toInt(),
            description = description,
            startTime = Instant.parse(start_time),
            tags = parseTags(tags)
        )
    }

    // Flow-based methods for reactive UI
    fun getEntriesForDateFlow(date: LocalDate): Flow<List<LocalTimeEntry>> {
        return queries.getTimeEntriesForDate(date.toString())
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { entries -> entries.map { it.toLocalTimeEntry() } }
    }

    fun getActiveTimerFlow(): Flow<ActiveTimer?> {
        return queries.getActiveTimer()
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.toActiveTimer() }
    }

    fun getSelectableProjectsFlow(): Flow<List<SelectableProject>> {
        return queries.getSelectableProjects()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows ->
                rows.map { row ->
                    SelectableProject(
                        projectId = row.project_id.toInt(),
                        name = row.name,
                        color = row.color
                    )
                }
            }
    }

    fun getSelectablePhasesFlow(): Flow<List<SelectablePhase>> {
        return queries.getAllSelectablePhases()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows ->
                rows.map { row ->
                    SelectablePhase(
                        phaseId = row.phase_id.toInt(),
                        projectId = row.project_id.toInt(),
                        name = row.name,
                        color = row.color
                    )
                }
            }
    }

    // Project/Phase cache operations
    fun cacheProjects(projects: List<FloatProject>) {
        val now = Clock.System.now().toString()
        queries.transaction {
            queries.clearProjectCache()
            projects.forEach { project ->
                queries.insertOrUpdateProject(
                    project_id = project.project_id.toLong(),
                    name = project.name,
                    color = project.color,
                    active = if (project.active != 0) 1L else 0L,
                    cached_at = now
                )
            }
        }
    }

    fun cachePhases(phases: List<FloatPhaseItem>) {
        val now = Clock.System.now().toString()
        queries.transaction {
            queries.clearPhaseCache()
            phases.forEach { phase ->
                queries.insertOrUpdatePhase(
                    phase_id = phase.phase_id.toLong(),
                    project_id = phase.project_id.toLong(),
                    name = phase.name,
                    color = phase.color,
                    active = if (phase.active != 0) 1L else 0L,
                    cached_at = now
                )
            }
        }
    }

    fun hasCachedProjects(): Boolean {
        return queries.getProjectCacheAge().executeAsOneOrNull()?.oldest != null
    }

    fun getProjectName(projectId: Int): String? {
        return queries.getProjectById(projectId.toLong()).executeAsOneOrNull()?.name
    }

    fun getPhaseName(phaseId: Int): String? {
        return queries.getPhaseById(phaseId.toLong()).executeAsOneOrNull()?.name
    }

    // Recommendations
    fun getRecommendations(query: String): List<EntryRecommendation> {
        return if (query.isBlank()) {
            queries.getRecentEntryRecommendations().executeAsList().mapNotNull { row ->
                toRecommendation(row.description, row.project_id, row.phase_id)
            }
        } else {
            queries.searchRecommendations(query).executeAsList().mapNotNull { row ->
                toRecommendation(row.description, row.project_id, row.phase_id)
            }
        }
    }

    private fun toRecommendation(description: String?, projectId: Long?, phaseId: Long?): EntryRecommendation? {
        val desc = description ?: return null
        val projId = projectId?.toInt() ?: return null
        val projectName = getProjectName(projId) ?: return null
        val phId = phaseId?.toInt()
        val phaseName = phId?.let { getPhaseName(it) }

        return EntryRecommendation(
            description = desc,
            projectId = projId,
            phaseId = phId,
            projectName = projectName,
            phaseName = phaseName
        )
    }
}
