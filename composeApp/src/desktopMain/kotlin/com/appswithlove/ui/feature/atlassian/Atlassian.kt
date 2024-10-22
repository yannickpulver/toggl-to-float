package com.appswithlove.ui.feature.atlassian

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.appswithlove.DoOnFocus
import com.google.accompanist.flowlayout.FlowRow
import com.vanpra.composematerialdialogs.DesktopWindowPosition
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogProperties
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import io.kanro.compose.jetbrains.expui.control.TextField
import org.koin.compose.koinInject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AddTimeAtlassian(modifier: Modifier = Modifier, viewModel: AtlassianViewModel = koinInject()) {
    val state = viewModel.state.collectAsState()

    DoOnFocus { viewModel.refresh() }



    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {

        val showForm = remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Jira Worklog",
                style = MaterialTheme.typography.h4,
                modifier = Modifier.weight(1f)
            )
            if (!state.value.incomplete && !showForm.value) {
                TextButton(
                    onClick = { showForm.value = !showForm.value },
                    modifier = Modifier.height(24.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Change Config")
                }
            }
        }

        if (state.value.incomplete || showForm.value) {
            Text(
                "Add your jira credentials / info to track your worklog.",
                style = MaterialTheme.typography.body2
            )
            Form(state.value) { email, apiKey, host, prefix, round, quote ->
                showForm.value = false
                viewModel.save(email, apiKey, host, prefix, round, quote)
            }
        } else {
            Text(
                "All worklogs starting with an issue id (e.g. '${state.value.prefix}-123') will be added to Jira.",
                style = MaterialTheme.typography.body2
            )
            AddAtlassianTimeEntries(viewModel::addTimeEntries, state.value.missingEntryDates)
        }
    }
}

@Composable
private fun AddAtlassianTimeEntries(
    addTimeEntries: (LocalDate) -> Unit,
    missingEntryDates: List<LocalDate>
) {
    val dialogState = rememberMaterialDialogState()
    MaterialDialog(
        dialogState = dialogState,
        buttons = {
            positiveButton("Ok")
            negativeButton("Cancel")
        },
        properties = MaterialDialogProperties(
            windowTitle = "Select Date",
            windowSize = DpSize(300.dp, 500.dp),
            windowPosition = DesktopWindowPosition(Alignment.Center)
        )
    ) {
        datepicker { date ->
            addTimeEntries(LocalDate.of(date.year, date.month, date.dayOfMonth))
        }
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        mainAxisSpacing = 8.dp,
        crossAxisSpacing = 8.dp
    ) {

        OutlinedButton(
            onClick = { dialogState.show() },
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Icon(Icons.Default.DateRange, null)
            Text("Select date...", Modifier.padding(start = 8.dp))
        }

        missingEntryDates.forEach {
            OutlinedButton(
                onClick = { addTimeEntries(it) },
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(it.format(DateTimeFormatter.ofPattern("EEE, dd.MM")))
            }

        }
    }
}

@Composable
private fun Form(
    state: AtlassianState,
    save: (String, String, String, String, Boolean, String) -> Unit
) {
    val email = remember { mutableStateOf(state.email.orEmpty()) }
    TextField(
        email.value, onValueChange = { email.value = it },
        placeholder = { Text("Email") },
        modifier = Modifier.fillMaxWidth()
    )

    val apiKey = remember { mutableStateOf(state.apiKey.orEmpty()) }
    TextField(
        apiKey.value,
        onValueChange = { apiKey.value = it },
        placeholder = { Text("API Key") },
        modifier = Modifier.fillMaxWidth()
    )

    val host = remember { mutableStateOf(state.host.orEmpty()) }
    TextField(
        host.value,
        onValueChange = { host.value = it },
        placeholder = { Text("Host (something.atlassian.net)") },
        modifier = Modifier.fillMaxWidth()
    )

    val prefix = remember { mutableStateOf(state.prefix.orEmpty()) }
    TextField(
        prefix.value,
        onValueChange = { prefix.value = it },
        placeholder = { Text("Issue prefix (for ABC-123 that would be ABC") },
        modifier = Modifier.fillMaxWidth()
    )

    val quote = remember { mutableStateOf(state.quote) }
    TextField(
        quote.value,
        onValueChange = { quote.value = it },
        placeholder = { Text("Quote how much of your time should be reported (defaults to 1.0)") },
        modifier = Modifier.fillMaxWidth()
    )

    val checked = remember { mutableStateOf(state.round) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
            .clickable { checked.value = !checked.value },
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = checked.value,
            onCheckedChange = { checked.value = it },
            modifier = Modifier.size(24.dp)
        )
        Text("Round to quarter hour", style = MaterialTheme.typography.body2)
    }


    Button(onClick = { save(email.value, apiKey.value, host.value, prefix.value, checked.value, quote.value) }) {
        Text("Save")
    }
}