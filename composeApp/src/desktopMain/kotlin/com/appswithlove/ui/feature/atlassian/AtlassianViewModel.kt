package com.appswithlove.ui.feature.atlassian

import com.appswithlove.atlassian.AtlassianRepository
import com.appswithlove.store.DataStore
import com.appswithlove.timetracking.TimeTrackingRepository
import com.appswithlove.ui.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinLocalDate
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

class AtlassianViewModel(
    private val timeTrackingRepo: TimeTrackingRepository,
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
        getSprintIssues()

        _lastRefresh = LocalDateTime.now()
    }

    private fun getSprintIssues() {
        if (dataStore.getStore.atlassianEmail.isNullOrBlank() ||
            dataStore.getStore.atlassianApiKey.isNullOrBlank() ||
            dataStore.getStore.atlassianHost.isNullOrBlank()) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val issues = repo.getCurrentSprintIssues()
            _state.update { it.copy(sprintIssues = issues) }
        }
    }

    private fun getMissingEntries() {
        val prefix = dataStore.getStore.atlassianPrefix ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val entries = timeTrackingRepo.getUnpublishedToJira(prefix)

            val missingEntries = entries.mapNotNull { entry ->
                val description = entry.description ?: return@mapNotNull null
                val issueId = getIssueId(prefix, description) ?: return@mapNotNull null
                val date = entry.startTime.toString().substringBefore("T")
                val localDate = LocalDate.parse(date)
                val hasWorklog = repo.hasWorklog(issueId, localDate)
                if (!hasWorklog) localDate else null
            }

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

    fun startTimeTracking(issueKey: String, issueName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                timeTrackingRepo.startTimer(
                    projectId = null,
                    phaseId = null,
                    description = "$issueKey $issueName",
                    tags = emptyList()
                )
                Logger.log("Timer started for $issueKey")
            } catch (e: Exception) {
                Logger.err("Error starting time tracking: ${e.message}")
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
                val prefix = dataStore.getStore.atlassianPrefix ?: throw Exception("Prefix not set")
                val localEntries = timeTrackingRepo.getEntriesForDate(date.toKotlinLocalDate())
                val filteredEntries = localEntries.filter { it.description?.startsWith(prefix) == true }

                if (filteredEntries.isEmpty()) {
                    Logger.log("No entries with prefix $prefix found for $date")
                    return@withLoading
                }

                // Check permissions first
                val errors = filteredEntries.mapNotNull { entry ->
                    val description = entry.description ?: return@mapNotNull null
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
                    filteredEntries.forEach { entry ->
                        val duration = (entry.durationSeconds * dataStore.getStore.atlassianQuote).roundToInt()
                        val timeSpentSeconds = if (dataStore.getStore.attlasianRoundToQuarterHour) {
                            roundSecondsToNearestQuarterHour(duration)
                        } else {
                            duration
                        }

                        val startTime = if (dataStore.getStore.attlasianRoundToQuarterHour) {
                            entry.startTime.roundToQuarter()
                        } else {
                            entry.startTime
                        }

                        val formattedTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                            .format(Date(startTime.toEpochMilliseconds()))
                        val description = entry.description ?: return@forEach
                        val issueId = getIssueId(prefix, description) ?: return@forEach

                        val posted = repo.postWorklog(
                            issueId,
                            formattedTime,
                            timeSpentSeconds,
                            description.substringAfter(issueId).trim()
                        )

                        if (posted) {
                            timeTrackingRepo.markSyncedToJira(entry.id)
                        }
                        success = success && posted
                    }
                }
            }

            if (success && state.value.missingEntryDates.contains(date)) {
                _state.update { it.copy(missingEntryDates = it.missingEntryDates.filter { it != date }) }
            }
        }
    }

    private val QUARTER_SEC = 15 * 60L           // 900 s

    fun Instant.roundToQuarter(): Instant {
        val rounded = ((epochSeconds + QUARTER_SEC / 2) / QUARTER_SEC) * QUARTER_SEC
        return Instant.fromEpochSeconds(rounded)
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
    val missingEntryDates: List<LocalDate> = emptyList(),
    val sprintIssues: List<com.appswithlove.atlassian.JiraIssue> = emptyList()
) {

    val incomplete get() = email.isNullOrBlank() || apiKey.isNullOrBlank() || host.isNullOrBlank() || prefix.isNullOrBlank()

    companion object {
        val EMPTY = AtlassianState("", "", "", "")
    }
}
