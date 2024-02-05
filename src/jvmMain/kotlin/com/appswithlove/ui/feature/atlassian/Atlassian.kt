package com.appswithlove.ui.feature.atlassian

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.vanpra.composematerialdialogs.DesktopWindowPosition
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogProperties
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import io.kanro.compose.jetbrains.expui.control.TextField
import org.koin.compose.koinInject
import java.time.LocalDate

@Composable
fun AddTimeAtlassian(modifier: Modifier = Modifier, viewModel: AtlassianViewModel = koinInject()) {
    val state = viewModel.state.collectAsState()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Jira Worklog",
            style = MaterialTheme.typography.h4,
        )

        if (state.value.incomplete) {
            Form(state, viewModel)
        } else {
            AddAtlassianTimeEntries(viewModel::addTimeEntries)
        }
    }
}

@Composable
private fun AddAtlassianTimeEntries(addTimeEntries: (LocalDate) -> Unit) {
    val dialogState = rememberMaterialDialogState()
    MaterialDialog(
        dialogState = dialogState,
        buttons = {
            positiveButton("Ok")
            negativeButton("Cancel")
        },
        properties = MaterialDialogProperties(
            size = DpSize(300.dp, 500.dp),
            position = DesktopWindowPosition(Alignment.Center)
        )
    ) {
        datepicker { date ->
            addTimeEntries(LocalDate.of(date.year, date.month, date.dayOfMonth))
        }
    }

    OutlinedButton(
        onClick = { dialogState.show() },
        contentPadding = PaddingValues(8.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Icon(Icons.Default.DateRange, null)
        Text("Select date...", Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun Form(
    state: State<AtlassianState>,
    viewModel: AtlassianViewModel
) {
    val email = remember { mutableStateOf(state.value.email.orEmpty()) }
    TextField(email.value, onValueChange = { email.value = it }, placeholder = { Text("Email") })

    val apiKey = remember { mutableStateOf(state.value.apiKey.orEmpty()) }
    TextField(
        apiKey.value,
        onValueChange = { apiKey.value = it },
        placeholder = { Text("API Key") })

    val host = remember { mutableStateOf(state.value.host.orEmpty()) }
    TextField(
        host.value,
        onValueChange = { host.value = it },
        placeholder = { Text("Host (something.atlassian.net)") })

    val prefix = remember { mutableStateOf(state.value.host.orEmpty()) }
    TextField(
        prefix.value,
        onValueChange = { prefix.value = it },
        placeholder = { Text("Issue prefix (for ABC-123 that would be ABC") })

    Button(onClick = {
        viewModel.save(email.value, apiKey.value, host.value, prefix.value)
    }) {
        Text("Save")
    }
}