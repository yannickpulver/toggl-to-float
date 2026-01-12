package com.appswithlove.ui.feature.timeentry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Label
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.appswithlove.floaat.FloatOverview
import com.appswithlove.floaat.FloatProject
import com.appswithlove.floaat.SelectablePhase
import com.appswithlove.floaat.SelectableProject
import com.appswithlove.floaat.rgbColor
import com.appswithlove.timetracking.LocalTimeEntry
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@Composable
fun TimeEntryFormDialog(
    selectedDate: java.time.LocalDate,
    floatProjects: List<SelectableProject>,
    floatPhases: List<SelectablePhase>,
    weeklyOverview: Map<FloatProject?, List<FloatOverview>>,
    initialStartTime: Instant? = null,
    initialEndTime: Instant? = null,
    existingEntry: LocalTimeEntry? = null,
    onDismiss: () -> Unit,
    onSave: (projectId: Int?, phaseId: Int?, description: String?, startTime: Instant, endTime: Instant) -> Unit,
    onUpdate: (id: Long, projectId: Int?, phaseId: Int?, description: String?, startTime: Instant, endTime: Instant) -> Unit = { _, _, _, _, _, _ -> },
    onDelete: (Long) -> Unit = {}
) {
    val isEditing = existingEntry != null
    val tz = TimeZone.currentSystemDefault()

    // Use existing entry values if editing, otherwise use initial times
    val entryStart = existingEntry?.startTime?.toLocalDateTime(tz)?.time
    val entryEnd = existingEntry?.endTime?.toLocalDateTime(tz)?.time
    val initialStart = entryStart ?: initialStartTime?.toLocalDateTime(tz)?.time
    val initialEnd = entryEnd ?: initialEndTime?.toLocalDateTime(tz)?.time

    var selectedProject by remember {
        mutableStateOf(existingEntry?.projectId?.let { pid -> floatProjects.find { it.projectId == pid } })
    }
    var selectedPhase by remember {
        mutableStateOf(existingEntry?.phaseId?.let { phid -> floatPhases.find { it.phaseId == phid } })
    }
    var description by remember { mutableStateOf(existingEntry?.description ?: "") }
    var startHour by remember { mutableStateOf(initialStart?.hour?.toString()?.padStart(2, '0') ?: "09") }
    var startMinute by remember { mutableStateOf(initialStart?.minute?.toString()?.padStart(2, '0') ?: "00") }
    var endHour by remember { mutableStateOf(initialEnd?.hour?.toString()?.padStart(2, '0') ?: "10") }
    var endMinute by remember { mutableStateOf(initialEnd?.minute?.toString()?.padStart(2, '0') ?: "00") }
    var projectDropdownExpanded by remember { mutableStateOf(false) }
    var phaseDropdownExpanded by remember { mutableStateOf(false) }
    var hasFocus by remember { mutableStateOf(false) }

    // Convert weeklyOverview to suggestions (same as TimerControls)
    val allSuggestions = remember(weeklyOverview) {
        weeklyOverview.flatMap { (project, items) ->
            items.map { item ->
                WorkItemSuggestion(
                    title = item.title,
                    projectName = project?.name,
                    phaseName = item.phase?.name,
                    projectId = item.project?.project_id,
                    phaseId = item.phase?.phase_id,
                    color = item.phase?.rgbColor ?: project?.rgbColor ?: Color.Gray
                )
            }
        }
    }

    // Filter suggestions based on description input
    val filteredSuggestions = remember(allSuggestions, description) {
        if (description.isBlank()) {
            allSuggestions
        } else {
            allSuggestions.filter { suggestion ->
                suggestion.title.contains(description, ignoreCase = true) ||
                    suggestion.projectName?.contains(description, ignoreCase = true) == true ||
                    suggestion.phaseName?.contains(description, ignoreCase = true) == true
            }
        }
    }

    val showSuggestions = hasFocus && filteredSuggestions.isNotEmpty()

    // Filter phases for selected project
    val availablePhases = remember(selectedProject, floatPhases) {
        selectedProject?.let { proj ->
            floatPhases.filter { it.projectId == proj.projectId }
        } ?: emptyList()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Description field on top (like TimerControls)
                Box {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { hasFocus = it.isFocused },
                        singleLine = true
                    )

                    if (showSuggestions) {
                        Popup(
                            onDismissRequest = { hasFocus = false },
                            properties = PopupProperties(focusable = false)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f), RoundedCornerShape(4.dp)),
                                elevation = 8.dp,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    filteredSuggestions.forEach { suggestion ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    description = suggestion.title
                                                    selectedProject = floatProjects.find { it.projectId == suggestion.projectId }
                                                    selectedPhase = floatPhases.find { it.phaseId == suggestion.phaseId }
                                                    hasFocus = false
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(suggestion.color)
                                            )
                                            Column {
                                                Text(suggestion.title, style = MaterialTheme.typography.body2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                suggestion.projectName?.let {
                                                    Text(it, style = MaterialTheme.typography.caption, color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Project & phase selectors below description (like TimerControls)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { projectDropdownExpanded = true }
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (selectedProject != null) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                selectedProject?.name ?: "Project",
                                style = MaterialTheme.typography.caption,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(
                            expanded = projectDropdownExpanded,
                            onDismissRequest = { projectDropdownExpanded = false }
                        ) {
                            floatProjects.forEach { project ->
                                DropdownMenuItem(
                                    onClick = {
                                        selectedProject = project
                                        selectedPhase = null
                                        projectDropdownExpanded = false
                                    }
                                ) {
                                    Text(project.name)
                                }
                            }
                        }
                    }

                    if (availablePhases.isNotEmpty()) {
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { phaseDropdownExpanded = true }
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Label,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (selectedPhase != null) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                                )
                                if (selectedPhase != null) {
                                    Text(
                                        selectedPhase!!.name,
                                        style = MaterialTheme.typography.caption,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = phaseDropdownExpanded,
                                onDismissRequest = { phaseDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(onClick = { selectedPhase = null; phaseDropdownExpanded = false }) {
                                    Text("No phase")
                                }
                                availablePhases.forEach { phase ->
                                    DropdownMenuItem(onClick = { selectedPhase = phase; phaseDropdownExpanded = false }) {
                                        Text(phase.name)
                                    }
                                }
                            }
                        }
                    }
                }

                // Compact time picker row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Start time picker
                    CompactTimePicker(
                        hour = startHour,
                        minute = startMinute,
                        onHourChange = { startHour = it },
                        onMinuteChange = { startMinute = it }
                    )

                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.padding(horizontal = 12.dp).size(16.dp),
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                    )

                    // End time picker
                    CompactTimePicker(
                        hour = endHour,
                        minute = endMinute,
                        onHourChange = { endHour = it },
                        onMinuteChange = { endMinute = it }
                    )
                }

                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Delete button (only when editing)
                    if (isEditing && existingEntry != null) {
                        IconButton(
                            onClick = {
                                onDelete(existingEntry.id)
                                onDismiss()
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                    } else {
                        Box(modifier = Modifier.size(48.dp)) // Spacer
                    }

                    Row {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }

                        IconButton(
                            onClick = {
                                val tz = TimeZone.currentSystemDefault()
                                val kotlinDate = LocalDate(selectedDate.year, selectedDate.monthValue, selectedDate.dayOfMonth)
                                val startTime = kotlinDate.atTime(
                                    LocalTime(startHour.toIntOrNull() ?: 9, startMinute.toIntOrNull() ?: 0)
                                ).toInstant(tz)
                                val endTime = kotlinDate.atTime(
                                    LocalTime(endHour.toIntOrNull() ?: 10, endMinute.toIntOrNull() ?: 0)
                                ).toInstant(tz)

                                if (isEditing && existingEntry != null) {
                                    onUpdate(existingEntry.id, selectedProject?.projectId, selectedPhase?.phaseId, description.ifBlank { null }, startTime, endTime)
                                } else {
                                    onSave(selectedProject?.projectId, selectedPhase?.phaseId, description.ifBlank { null }, startTime, endTime)
                                }
                                onDismiss()
                            },
                            enabled = selectedProject != null
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Save",
                                tint = if (selectedProject != null) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactTimePicker(
    hour: String,
    minute: String,
    onHourChange: (String) -> Unit,
    onMinuteChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Hour
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = "Increase hour",
                modifier = Modifier
                    .size(16.dp)
                    .clickable {
                        val h = (hour.toIntOrNull() ?: 0)
                        onHourChange(((h + 1) % 24).toString().padStart(2, '0'))
                    },
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = hour.padStart(2, '0'),
                style = MaterialTheme.typography.body1
            )
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Decrease hour",
                modifier = Modifier
                    .size(16.dp)
                    .clickable {
                        val h = (hour.toIntOrNull() ?: 0)
                        onHourChange(((h - 1 + 24) % 24).toString().padStart(2, '0'))
                    },
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
        }

        Text(":", style = MaterialTheme.typography.body1)

        // Minute
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = "Increase minute",
                modifier = Modifier
                    .size(16.dp)
                    .clickable {
                        val m = (minute.toIntOrNull() ?: 0)
                        onMinuteChange(((m + 15) % 60).toString().padStart(2, '0'))
                    },
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = minute.padStart(2, '0'),
                style = MaterialTheme.typography.body1
            )
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Decrease minute",
                modifier = Modifier
                    .size(16.dp)
                    .clickable {
                        val m = (minute.toIntOrNull() ?: 0)
                        onMinuteChange(((m - 15 + 60) % 60).toString().padStart(2, '0'))
                    },
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
