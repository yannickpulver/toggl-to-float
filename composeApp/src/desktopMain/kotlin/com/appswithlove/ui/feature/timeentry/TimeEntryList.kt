package com.appswithlove.ui.feature.timeentry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.appswithlove.timetracking.LocalTimeEntry
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun TimeEntryList(
    entries: List<LocalTimeEntry>,
    onDeleteEntry: (Long) -> Unit,
    onAddEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Time Entries (${entries.size})",
                style = MaterialTheme.typography.subtitle1
            )

            TextButton(onClick = onAddEntry) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Add Entry", modifier = Modifier.padding(start = 4.dp))
            }
        }

        if (entries.isEmpty()) {
            Text(
                text = "No entries for this date",
                style = MaterialTheme.typography.body2,
                color = Color.Gray
            )
        } else {
            entries.forEach { entry ->
                TimeEntryCard(
                    entry = entry,
                    onDelete = { onDeleteEntry(entry.id) }
                )
            }
        }
    }
}

@Composable
private fun TimeEntryCard(
    entry: LocalTimeEntry,
    onDelete: () -> Unit
) {
    val tz = TimeZone.currentSystemDefault()
    val startTime = entry.startTime.toLocalDateTime(tz)
    val endTime = entry.endTime?.toLocalDateTime(tz)

    val timeRange = if (endTime != null) {
        "${formatTime(startTime.hour, startTime.minute)} - ${formatTime(endTime.hour, endTime.minute)}"
    } else {
        "${formatTime(startTime.hour, startTime.minute)} - running..."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeRange,
                        style = MaterialTheme.typography.body1
                    )
                    Text(
                        text = entry.durationFormatted,
                        style = MaterialTheme.typography.caption,
                        color = Color.Gray
                    )
                }

                entry.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (entry.syncedToFloat) {
                        SyncBadge("Float")
                    }
                    if (entry.syncedToJira) {
                        SyncBadge("Jira")
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
            }
        }
    }
}

@Composable
private fun SyncBadge(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.padding(end = 2.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = Color(0xFF4CAF50)
        )
    }
}

