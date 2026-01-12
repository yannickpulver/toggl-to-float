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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.appswithlove.floaat.FloatOverview
import com.appswithlove.floaat.FloatProject
import com.appswithlove.floaat.SelectablePhase
import com.appswithlove.floaat.SelectableProject
import com.appswithlove.floaat.rgbColor
import com.appswithlove.timetracking.ActiveTimer
import kotlinx.coroutines.delay

data class WorkItemSuggestion(
    val title: String,
    val projectName: String?,
    val phaseName: String?,
    val projectId: Int?,
    val phaseId: Int?,
    val color: Color
)

@Composable
fun TimerControls(
    activeTimer: ActiveTimer?,
    floatProjects: List<SelectableProject>,
    floatPhases: List<SelectablePhase>,
    weeklyOverview: Map<FloatProject?, List<FloatOverview>>,
    onStartTimer: (projectId: Int?, phaseId: Int?, description: String?) -> Unit,
    onStopTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedProject by remember { mutableStateOf<SelectableProject?>(null) }
    var selectedPhase by remember { mutableStateOf<SelectablePhase?>(null) }
    var description by remember { mutableStateOf("") }
    var projectDropdownExpanded by remember { mutableStateOf(false) }
    var phaseDropdownExpanded by remember { mutableStateOf(false) }
    var hasFocus by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0L) }

    // Convert weeklyOverview to suggestions
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

    val showSuggestions = hasFocus && activeTimer == null && filteredSuggestions.isNotEmpty()

    // Filter phases for selected project
    val availablePhases = remember(selectedProject, floatPhases) {
        selectedProject?.let { proj ->
            floatPhases.filter { it.projectId == proj.projectId }
        } ?: emptyList()
    }

    LaunchedEffect(activeTimer) {
        while (activeTimer != null) {
            elapsedSeconds = activeTimer.elapsedSeconds()
            delay(1000)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = if (activeTimer != null) activeTimer.description ?: "" else description,
                onValueChange = { description = it },
                enabled = activeTimer == null,
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { hasFocus = it.isFocused },
                textStyle = TextStyle(
                    color = MaterialTheme.colors.onSurface,
                    fontSize = MaterialTheme.typography.body1.fontSize
                ),
                cursorBrush = SolidColor(MaterialTheme.colors.primary),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (description.isEmpty() && activeTimer == null) {
                        Text(
                            "What are you working on?",
                            style = MaterialTheme.typography.body1,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            )

            Text(
                text = if (activeTimer != null) formatDuration(elapsedSeconds) else "0:00:00",
                style = MaterialTheme.typography.body1,
                color = if (activeTimer != null) MaterialTheme.colors.onSurface else MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (activeTimer != null) MaterialTheme.colors.error
                        else if (selectedProject != null) MaterialTheme.colors.primary
                        else MaterialTheme.colors.primary.copy(alpha = 0.3f)
                    )
                    .clickable(enabled = activeTimer != null || selectedProject != null) {
                        if (activeTimer != null) {
                            onStopTimer()
                        } else {
                            onStartTimer(selectedProject?.projectId, selectedPhase?.phaseId, description.ifBlank { null })
                            description = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (activeTimer != null) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Suggestions popup
        Box {
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

        // Project & phase selectors
        if (activeTimer == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { projectDropdownExpanded = true }.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (selectedProject != null) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                        if (selectedProject != null) {
                            Text(selectedProject!!.name, style = MaterialTheme.typography.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    DropdownMenu(expanded = projectDropdownExpanded, onDismissRequest = { projectDropdownExpanded = false }) {
                        floatProjects.forEach { project ->
                            DropdownMenuItem(onClick = { selectedProject = project; selectedPhase = null; projectDropdownExpanded = false }) {
                                Text(project.name)
                            }
                        }
                    }
                }

                if (availablePhases.isNotEmpty()) {
                    Box {
                        Row(
                            modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { phaseDropdownExpanded = true }.padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Label, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (selectedPhase != null) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                            if (selectedPhase != null) {
                                Text(selectedPhase!!.name, style = MaterialTheme.typography.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        DropdownMenu(expanded = phaseDropdownExpanded, onDismissRequest = { phaseDropdownExpanded = false }) {
                            DropdownMenuItem(onClick = { selectedPhase = null; phaseDropdownExpanded = false }) { Text("No phase") }
                            availablePhases.forEach { phase ->
                                DropdownMenuItem(onClick = { selectedPhase = phase; phaseDropdownExpanded = false }) { Text(phase.name) }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, secs)
}
