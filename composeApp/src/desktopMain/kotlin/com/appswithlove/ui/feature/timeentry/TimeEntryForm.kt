package com.appswithlove.ui.feature.timeentry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.appswithlove.floaat.SelectablePhase
import com.appswithlove.floaat.SelectableProject
import com.appswithlove.timetracking.EntryRecommendation
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
    initialStartTime: Instant? = null,
    initialEndTime: Instant? = null,
    existingEntry: LocalTimeEntry? = null,
    getRecommendations: (String) -> List<EntryRecommendation>,
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
    var showRecommendations by remember { mutableStateOf(false) }
    var recommendations by remember { mutableStateOf<List<EntryRecommendation>>(emptyList()) }

    // Filter phases for selected project
    val availablePhases = remember(selectedProject, floatPhases) {
        selectedProject?.let { proj ->
            floatPhases.filter { it.projectId == proj.projectId }
        } ?: emptyList()
    }

    // Update recommendations when description changes
    LaunchedEffect(description) {
        recommendations = if (description.isNotEmpty()) {
            getRecommendations(description)
        } else {
            getRecommendations("")
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isEditing) "Edit Time Entry" else "Add Time Entry",
                    style = MaterialTheme.typography.h6
                )

                // Project dropdown
                Box {
                    OutlinedButton(
                        onClick = { projectDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedProject?.name ?: "Select project")
                    }

                    DropdownMenu(
                        expanded = projectDropdownExpanded,
                        onDismissRequest = { projectDropdownExpanded = false }
                    ) {
                        floatProjects.forEach { project ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedProject = project
                                    selectedPhase = null // Reset phase when project changes
                                    projectDropdownExpanded = false
                                }
                            ) {
                                Text(project.name)
                            }
                        }
                    }
                }

                // Phase dropdown (only shown if project has phases)
                if (availablePhases.isNotEmpty()) {
                    Box {
                        OutlinedButton(
                            onClick = { phaseDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedPhase?.name ?: "Select phase (optional)")
                        }

                        DropdownMenu(
                            expanded = phaseDropdownExpanded,
                            onDismissRequest = { phaseDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    selectedPhase = null
                                    phaseDropdownExpanded = false
                                }
                            ) {
                                Text("No phase")
                            }
                            availablePhases.forEach { phase ->
                                DropdownMenuItem(
                                    onClick = {
                                        selectedPhase = phase
                                        phaseDropdownExpanded = false
                                    }
                                ) {
                                    Text(phase.name)
                                }
                            }
                        }
                    }
                }

                // Description field with recommendations
                Box {
                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            showRecommendations = it.isNotEmpty() || recommendations.isNotEmpty()
                        },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DropdownMenu(
                        expanded = showRecommendations && recommendations.isNotEmpty(),
                        onDismissRequest = { showRecommendations = false }
                    ) {
                        recommendations.take(5).forEach { rec ->
                            DropdownMenuItem(
                                onClick = {
                                    description = rec.description
                                    // Find matching project and phase
                                    selectedProject = floatProjects.find { it.projectId == rec.projectId }
                                    selectedPhase = rec.phaseId?.let { phId ->
                                        floatPhases.find { it.phaseId == phId }
                                    }
                                    showRecommendations = false
                                }
                            ) {
                                Column {
                                    Text(rec.description, style = MaterialTheme.typography.body2)
                                    Text(
                                        "${rec.projectName}${rec.phaseName?.let { " - $it" } ?: ""}",
                                        style = MaterialTheme.typography.caption,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Start Time", style = MaterialTheme.typography.caption)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = startHour,
                                onValueChange = { if (it.length <= 2) startHour = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Text(":", modifier = Modifier.padding(vertical = 16.dp))
                            OutlinedTextField(
                                value = startMinute,
                                onValueChange = { if (it.length <= 2) startMinute = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("End Time", style = MaterialTheme.typography.caption)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = endHour,
                                onValueChange = { if (it.length <= 2) endHour = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Text(":", modifier = Modifier.padding(vertical = 16.dp))
                            OutlinedTextField(
                                value = endMinute,
                                onValueChange = { if (it.length <= 2) endMinute = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }

                // Delete button row (only when editing)
                if (isEditing && existingEntry != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        TextButton(
                            onClick = {
                                onDelete(existingEntry.id)
                                onDismiss()
                            }
                        ) {
                            Text("Delete", color = Color.Red)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    TextButton(
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
                        Text("Save")
                    }
                }
            }
        }
    }
}
