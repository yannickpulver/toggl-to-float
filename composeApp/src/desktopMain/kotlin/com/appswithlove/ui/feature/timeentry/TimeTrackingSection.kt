package com.appswithlove.ui.feature.timeentry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.appswithlove.floaat.FloatOverview
import com.appswithlove.floaat.FloatProject
import com.appswithlove.floaat.SelectablePhase
import com.appswithlove.floaat.SelectableProject
import com.appswithlove.timetracking.ActiveTimer
import com.appswithlove.timetracking.EntryRecommendation
import com.appswithlove.timetracking.LocalTimeEntry
import kotlinx.datetime.Instant
import java.time.LocalDate

private enum class ViewMode { TIMELINE, LIST }

@Composable
fun TimeTrackingSection(
    selectedDate: LocalDate,
    entries: List<LocalTimeEntry>,
    activeTimer: ActiveTimer?,
    currentTime: Instant,
    floatProjects: List<SelectableProject>,
    floatPhases: List<SelectablePhase>,
    weeklyOverview: Map<FloatProject?, List<FloatOverview>>,
    getProjectName: (Int?, Int?) -> String?,
    getRecommendations: (String) -> List<EntryRecommendation>,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onStartTimer: (projectId: Int?, phaseId: Int?, description: String?) -> Unit,
    onStopTimer: () -> Unit,
    onDeleteEntry: (Long) -> Unit,
    onAddEntry: (projectId: Int?, phaseId: Int?, description: String?, startTime: Instant, endTime: Instant) -> Unit,
    onUpdateEntry: (id: Long, newStartTime: Instant, newEndTime: Instant) -> Unit,
    onUpdateEntryFull: (id: Long, projectId: Int?, phaseId: Int?, description: String?, startTime: Instant, endTime: Instant) -> Unit,
    onUpdateTimer: (newStartTime: Instant) -> Unit,
    onPublishToFloat: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingStartTime by remember { mutableStateOf<Instant?>(null) }
    var pendingEndTime by remember { mutableStateOf<Instant?>(null) }
    var editingEntry by remember { mutableStateOf<LocalTimeEntry?>(null) }
    var viewMode by remember { mutableStateOf(ViewMode.TIMELINE) }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        TimerControls(
            activeTimer = activeTimer,
            floatProjects = floatProjects,
            floatPhases = floatPhases,
            weeklyOverview = weeklyOverview,
            onStartTimer = onStartTimer,
            onStopTimer = onStopTimer
        )

        ViewModeToggle(
            currentMode = viewMode,
            onModeChange = { viewMode = it },
            modifier = Modifier.fillMaxWidth()
        )

        val totalLoggedSeconds = remember(entries) {
            entries.sumOf { it.durationSeconds.toLong() }
        }

        DateNavigator(
            selectedDate = selectedDate,
            totalLoggedSeconds = totalLoggedSeconds,
            onPreviousDay = onPreviousDay,
            onNextDay = onNextDay,
            onToday = onToday
        )

        Divider()

        when (viewMode) {
            ViewMode.TIMELINE -> {
                TimelineView(
                    selectedDate = selectedDate,
                    entries = entries,
                    activeTimer = activeTimer,
                    currentTime = currentTime,
                    getProjectName = getProjectName,
                    onCreateEntry = { startTime, endTime ->
                        pendingStartTime = startTime
                        pendingEndTime = endTime
                        showAddDialog = true
                    },
                    onUpdateEntry = onUpdateEntry,
                    onUpdateTimer = onUpdateTimer,
                    onEntryClick = { entry ->
                        editingEntry = entry
                    }
                )
            }
            ViewMode.LIST -> {
                TimeEntryList(
                    entries = entries,
                    onDeleteEntry = onDeleteEntry,
                    onAddEntry = {
                        pendingStartTime = null
                        pendingEndTime = null
                        showAddDialog = true
                    }
                )
            }
        }

        if (entries.any { !it.syncedToFloat }) {
            TextButton(onClick = onPublishToFloat) {
                Text("Publish to Float")
            }
        }
    }

    if (showAddDialog) {
        TimeEntryFormDialog(
            selectedDate = selectedDate,
            floatProjects = floatProjects,
            floatPhases = floatPhases,
            initialStartTime = pendingStartTime,
            initialEndTime = pendingEndTime,
            getRecommendations = getRecommendations,
            onDismiss = {
                showAddDialog = false
                pendingStartTime = null
                pendingEndTime = null
            },
            onSave = { projectId, phaseId, description, startTime, endTime ->
                onAddEntry(projectId, phaseId, description, startTime, endTime)
            }
        )
    }

    // Edit dialog for timeline entry clicks
    editingEntry?.let { entry ->
        TimeEntryFormDialog(
            selectedDate = selectedDate,
            floatProjects = floatProjects,
            floatPhases = floatPhases,
            existingEntry = entry,
            getRecommendations = getRecommendations,
            onDismiss = { editingEntry = null },
            onSave = { _, _, _, _, _ -> },
            onUpdate = { id, projectId, phaseId, description, startTime, endTime ->
                onUpdateEntryFull(id, projectId, phaseId, description, startTime, endTime)
            },
            onDelete = { id ->
                onDeleteEntry(id)
            }
        )
    }
}

@Composable
private fun ViewModeToggle(
    currentMode: ViewMode,
    onModeChange: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        OutlinedButton(
            onClick = { onModeChange(ViewMode.TIMELINE) },
            shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 0.dp, bottomEnd = 0.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = if (currentMode == ViewMode.TIMELINE) MaterialTheme.colors.primary else Color.Transparent,
                contentColor = if (currentMode == ViewMode.TIMELINE) Color.White else MaterialTheme.colors.primary
            ),
            modifier = Modifier.weight(1f)
        ) {
            Text("Timeline")
        }
        OutlinedButton(
            onClick = { onModeChange(ViewMode.LIST) },
            shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 4.dp, bottomEnd = 4.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = if (currentMode == ViewMode.LIST) MaterialTheme.colors.primary else Color.Transparent,
                contentColor = if (currentMode == ViewMode.LIST) Color.White else MaterialTheme.colors.primary
            ),
            modifier = Modifier.weight(1f)
        ) {
            Text("List")
        }
    }
}
