package com.appswithlove.ui.feature.atlassian

import TimeEntry
import com.appswithlove.atlassian.AtlassianRepository
import com.appswithlove.store.DataStore
import com.appswithlove.toggl.TogglRepo
import com.appswithlove.ui.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

class AtlassianViewModel(
    private val togglRepo: TogglRepo,
    private val repo: AtlassianRepository,
    private val dataStore: DataStore,
) {

    private var _lastRefresh: LocalDateTime? = null

    private val _loadingCounter = MutableStateFlow(0)
    private val _state = MutableStateFlow(AtlassianState.EMPTY)
    val state = _state.asStateFlow()

    init {
        refresh(true)
    }

    fun refresh(force: Boolean = false) {
        if (!force && _lastRefresh?.plusMinutes(3)?.isAfter(LocalDateTime.now()) == true) {
            return
        }

        refreshFromStore()
        getMissingEntries()

        _lastRefresh = LocalDateTime.now()
    }

    private fun getMissingEntries() {
        val prefix = dataStore.getStore.atlassianPrefix ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val start = LocalDate.now().minusWeeks(2)
            val end = LocalDate.now()
            val togglEntries =
                togglRepo.getDatesWithTimeEntriesAndPrefix(start, end.plusDays(1), prefix)

            val missingEntries = togglEntries.filter { (date, description) ->
                val issueId = getIssueId(prefix, description) ?: return@filter false
                val hasWorklog = repo.hasWorklog(issueId, date)
                !hasWorklog
            }.map { it.first }

            _state.update { it.copy(missingEntryDates = missingEntries.toSet().sorted()) }
        }
    }

    private fun refreshFromStore() {
        dataStore.getStore.apply {
            _state.update {
                it.copy(
                    email = atlassianEmail,
                    apiKey = atlassianApiKey,
                    host = atlassianHost,
                    prefix = atlassianPrefix,
                    round = attlasianRoundToQuarterHour,
                    quote = atlassianQuote.toString()
                )
            }
        }
    }

    fun save(
        email: String,
        apiKey: String,
        host: String,
        prefix: String,
        round: Boolean,
        quote: String
    ) {
        val doubleQuote = minOf(quote.toDoubleOrNull() ?: 1.0, 1.0)
        dataStore.setAtlassianInfo(email, apiKey, host, prefix, round, doubleQuote)

        _state.update {
            it.copy(
                email = email,
                apiKey = apiKey,
                host = host,
                prefix = prefix,
                quote = quote,
                round = round
            )
        }
    }

    fun addTimeEntries(date: LocalDate) {
        CoroutineScope(Dispatchers.IO).launch {
            var success = true
            withLoading {
                val timeEntries = togglRepo.getTogglTimeEntries(date, date)

                val prefix = dataStore.getStore.atlassianPrefix ?: throw Exception("Prefix not set")
                val filteredEntries =
                    timeEntries.filter { it.description?.startsWith(prefix) == true }

                // Sort entries by start time and adjust overlaps
                val sortedEntries = filteredEntries.sortedBy { Instant.parse(it.start) }.map {
                    val duration = (it.duration * dataStore.getStore.atlassianQuote).roundToInt()
                    val timeSpentSeconds = if (dataStore.getStore.attlasianRoundToQuarterHour) {
                        roundSecondsToNearestQuarterHour(duration)
                    } else {
                        duration
                    }
                    it.copy(duration = timeSpentSeconds)

                }
                val adjustedEntries = mutableListOf<TimeEntry>()

                for (entry in sortedEntries) {
                    val startTime = Instant.parse(entry.start)
                    val duration = entry.duration.toLong()

                    // Find the next available start time that doesn't overlap
                    var adjustedStartTime = startTime
                    if (adjustedEntries.isNotEmpty()) {
                        val lastEntry = adjustedEntries.last()
                        val lastEndTime =
                            Instant.parse(lastEntry.start).plus(lastEntry.duration.toLong().seconds)
                        if (startTime < lastEndTime) {
                            adjustedStartTime = lastEndTime
                        }
                    }

                    // Create adjusted entry
                    val adjustedEntry = entry.copy(
                        start = adjustedStartTime.toString(),
                        stop = adjustedStartTime.plus(duration.seconds).toString()
                    )
                    adjustedEntries.add(adjustedEntry)
                }

                // Post adjusted entries

                val errors = adjustedEntries.mapNotNull {
                    val description = it.description ?: return@mapNotNull null
                    val issueId = getIssueId(prefix, description) ?: return@mapNotNull null
                    issueId
                }.toSet().filter {
                    val hasPermission = repo.hasPermission(it)
                    !hasPermission
                }

                if (errors.isNotEmpty()) {
                    Logger.err("Can't add worklog to these issues: $errors. Either set them to time logging or change to the correct issue id.")
                    success = false
                } else {
                    adjustedEntries.forEach {
                        val time = Instant.parse(it.start)
                        val formattedTime =
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(Date(time.toEpochMilliseconds()))
                        val description = it.description ?: return@forEach
                        val issueId = getIssueId(prefix, description) ?: return@forEach
                        success = success && repo.postWorklog(
                            issueId,
                            formattedTime,
                            it.duration,
                            description.substringAfter(issueId).trim()
                        )
                    }
                }

            }

            if (success && state.value.missingEntryDates.contains(date)) {
                _state.update { it.copy(missingEntryDates = it.missingEntryDates.filter { it != date }) }
            }
        }
    }

    fun roundSecondsToNearestQuarterHour(duration: Int): Int {
        val durationInMinutes = duration / 60f
        val remainder = durationInMinutes % 15
        val roundedDurationInMinutes = if (remainder <= 5) {
            durationInMinutes - remainder
        } else {
            ceil(durationInMinutes / 15) * 15
        }
        return roundedDurationInMinutes.toInt() * 60
    }

    private fun getIssueId(prefix: String, description: String) =
        Regex("($prefix-\\d+)").find(description)?.groupValues?.get(1)

    private suspend fun withLoading(block: suspend () -> Unit) {
        _loadingCounter.update { it + 1 }
        block()
        _loadingCounter.update { it - 1 }
    }
}

data class AtlassianState(
    val email: String?,
    val apiKey: String?,
    val host: String?,
    val prefix: String?,
    val quote: String = "1.0",
    val round: Boolean = false,
    val missingEntryDates: List<LocalDate> = emptyList()
) {

    val incomplete get() = email.isNullOrBlank() || apiKey.isNullOrBlank() || host.isNullOrBlank() || prefix.isNullOrBlank()

    companion object {
        val EMPTY = AtlassianState("", "", "", "")
    }
}
