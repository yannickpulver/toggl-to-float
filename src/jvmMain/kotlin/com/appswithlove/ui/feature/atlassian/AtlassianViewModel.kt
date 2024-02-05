package com.appswithlove.ui.feature.atlassian

import com.appswithlove.atlassian.AtlassianRepository
import com.appswithlove.store.DataStore
import com.appswithlove.toggl.TogglRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import java.lang.Exception
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date

class AtlassianViewModel(
    private val togglRepo: TogglRepo,
    private val repo: AtlassianRepository,
    private val dataStore: DataStore
) {

    private val _loadingCounter = MutableStateFlow(0)
    val state = MutableStateFlow(AtlassianState.EMPTY)

    init {
        refreshFromStore()
    }

    private fun refreshFromStore() {
        dataStore.getStore.apply {
            state.update {
                it.copy(
                    email = this.atlassianEmail,
                    apiKey = atlassianApiKey,
                    host = atlassianHost,
                    prefix = atlassianPrefix
                )
            }
        }
    }

    fun save(email: String, apiKey: String, host: String, prefix: String) {
        dataStore.setAtlassianEmail(email)
        dataStore.setAtlassianApiKey(apiKey)
        dataStore.setAtlassianHost(host)
        dataStore.setAtlassianPrefix(prefix)

        state.update {
            it.copy(email = email, apiKey = apiKey, host = host)
        }
    }

    fun addTimeEntries(date: LocalDate) {
        CoroutineScope(Dispatchers.IO).launch {
            withLoading {
                val timeEntries = togglRepo.getTogglTimeEntries(date, date)

                val prefix = dataStore.getStore.atlassianPrefix ?: throw Exception("Prefix not set")
                val filteredEntries = timeEntries.filter { it.description?.startsWith(prefix) == true }



                filteredEntries.forEach {
                    val time = Instant.parse(it.start)
                    val formattedTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(Date(time.toEpochMilliseconds()))
                    val description = it.description ?: return@forEach
                    val issueId = Regex("($prefix-\\d+)").find(description)?.groupValues?.get(1) ?: return@forEach
                    repo.postWorklog(issueId, formattedTime, it.duration, description.substringAfter(issueId).trim())
                }
            }
        }
    }

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
        val prefix: String?
    ) {

        val incomplete get() = email.isNullOrBlank() || apiKey.isNullOrBlank() || host.isNullOrBlank()

        companion object {
            val EMPTY = AtlassianState("", "", "", "")
        }
    }
