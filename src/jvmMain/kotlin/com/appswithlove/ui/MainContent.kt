package com.appswithlove.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.appswithlove.floaat.FloatPeopleItem
import com.appswithlove.floaat.rgbColor
import com.appswithlove.floaat.totalHours
import com.appswithlove.ui.setup.SetupForm
import com.appswithlove.ui.theme.FloaterTheme
import com.appswithlove.version
import com.google.accompanist.flowlayout.FlowRow
import com.vanpra.composematerialdialogs.DesktopWindowPosition
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogProperties
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainContent(viewModel: MainViewModel) {

    val state = viewModel.state.collectAsState()

    val focusRequester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    MainContent(
        state = state.value,
        syncProjects = viewModel::fetchProjects,
        removeProjects = viewModel::removeProjects,
        addTimeEntries = viewModel::addTimeEntries,
        save = viewModel::save,
        loadLastWeek = viewModel::loadTimeLastWeek,
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged {
                hasFocus = it.hasFocus
            }
            .focusable(),
        clearLogs = viewModel::clearLogs
    )

    if (!hasFocus) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

}

@Composable
fun Version(modifier: Modifier = Modifier) {
    Text(
        "Version $version",
        modifier = modifier,
        style = MaterialTheme.typography.caption
    )
}

@Composable
private fun MainContent(
    state: MainState,
    syncProjects: () -> Unit,
    removeProjects: () -> Unit,
    addTimeEntries: (LocalDate?) -> Unit,
    save: (String?, String?, FloatPeopleItem?) -> Unit,
    loadLastWeek: (Int) -> Unit,
    clearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier) {
        Box {
            Row {
                Column(
                    modifier = Modifier.weight(1f).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when {
                        state.isValid -> {
                            Welcome(syncProjects, removeProjects)
                            Divider()
                            AddTime(addTimeEntries = addTimeEntries, missingEntryDates = state.missingEntryDates)
                            Divider()
                            Logs(list = state.logs, clearLogs = clearLogs, modifier = Modifier.weight(1f))
                        }

                        state.loading -> {
                            Loading()
                        }

                        else -> {
                            SetupForm(state, save)
                        }
                    }
                    Divider()

                }
                Divider(modifier = Modifier.width(1.dp).fillMaxHeight())
                AnimatedVisibility(state.isValid) {
                    YourWeek(state, loadLastWeek)
                }
            }

            Version(modifier = Modifier.align(Alignment.BottomStart))
        }
    }
}

@Composable
private fun YourWeek(state: MainState, loadLastWeek: (Int) -> Unit) {
    val (toggle, onToggleChange) = remember { mutableStateOf(true) }

    Column(Modifier.background(MaterialTheme.colors.onSurface.copy(0.05f)).fillMaxHeight().padding(8.dp)) {
        if (!toggle) {
            TextButton(onClick = { onToggleChange(!toggle) }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.KeyboardArrowLeft, null)
            }
        } else {
            Column(Modifier.width(250.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { onToggleChange(!toggle) },
                        modifier = Modifier.size(40.dp).padding(bottom = 2.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, null)
                    }
                    Text("Your Week", style = MaterialTheme.typography.h4)
                }
                if (state.weeklyOverview.isNotEmpty()) {
                    Text("Planned hours: ${state.weeklyOverview.totalHours}h", style = MaterialTheme.typography.caption)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 16.dp)) {
                        //Text("Your Week: (Total hours: ${state.weeklyOverview.totalHours}) ")
                        state.weeklyOverview.forEach { (project, items) ->
                            val expanded = remember { mutableStateOf(false) }
                            Box(modifier = Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { expanded.value = !expanded.value }
                                .background(MaterialTheme.colors.surface)
                            ) {
                                Column(Modifier.padding(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        //modifier = Modifier.padding(8.dp)
                                    ) {
                                        project?.rgbColor?.let {
                                            Box(
                                                Modifier.clip(CircleShape).background(it).size(10.dp)
                                                    .clickable { loadLastWeek(project.project_id) })
                                        }

                                        project?.name?.let {
                                            Text(it, modifier = Modifier.weight(1f))
                                        }
                                        Text("${items.totalHours}h")
                                    }

                                    AnimatedVisibility(
                                        expanded.value,
                                        enter = expandVertically(),
                                        exit = shrinkVertically()
                                    ) {
                                        Column {
                                            items.forEach {
                                                Divider()
                                                Row {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        it.phase?.name?.let {
                                                            Text(
                                                                it,
                                                                style = MaterialTheme.typography.caption,

                                                                )
                                                        }
                                                        if (it.task.name.isNotEmpty()) {
                                                            Text(it.task.name, style = MaterialTheme.typography.caption)
                                                        }
                                                    }

                                                    if (items.size > 1) {
                                                        Text(
                                                            "${it.weekHours}h",
                                                            style = MaterialTheme.typography.caption
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Loading()
                }
            }
        }
    }
}

@Composable
private fun Welcome(syncProjects: () -> Unit, removeProjects: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Happy ${LocalDate.now().dayOfWeek.toString().lowercase().capitalize()}! 🎉",
            style = MaterialTheme.typography.h4
        )

        Button(onClick = syncProjects, contentPadding = PaddingValues(8.dp), modifier = Modifier.height(32.dp)) {
            Text("Sync Projects", style = MaterialTheme.typography.caption)
        }
    }
    Text(
        "If you need to add all of your Float projects & tasks to Toggl - Use the button to the right. You can also run it again to get the latest projects updated.",
        modifier = Modifier.fillMaxWidth()
    )


}

@Composable
private fun Loading() {
    Box(modifier = Modifier.fillMaxWidth()) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).size(20.dp))
    }
}

@Composable
private fun AddTime(addTimeEntries: (LocalDate?) -> Unit, missingEntryDates: List<LocalDate>) {
    var from by remember { mutableStateOf<LocalDate?>(null) }
    Box {
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
                from = LocalDate.of(date.year, date.month, date.dayOfMonth)
            }
        }

        Column {
            Text(
                "Add time Toggl 👉 Float",
                style = MaterialTheme.typography.h4,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = { dialogState.show() },
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(from?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: "Select date...")
                }
                AnimatedVisibility(from != null) {

                    Button(
                        onClick = { addTimeEntries(from) },
                        enabled = from != null,
                        contentPadding = PaddingValues(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Add time entries")
                    }
                }
            }


            AnimatedVisibility(missingEntryDates.isNotEmpty()) {
                Column {

                    Divider(Modifier.padding(top = 12.dp))
                    Text(
                        "Dates with entries in Toggl but not yet in Float. Click to quick-add:",
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    FlowRow(modifier = Modifier.fillMaxWidth(), mainAxisSpacing = 16.dp) {
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
            }


        }
    }


}

@Composable
private fun Logs(list: List<Pair<String, LogLevel>>, clearLogs: () -> Unit, modifier: Modifier = Modifier) {
    val logs = remember(list) { list.reversed() }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = modifier.fillMaxHeight()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Logs", style = MaterialTheme.typography.h4)
            OutlinedButton(
                onClick = clearLogs,
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Clear Logs", style = MaterialTheme.typography.caption)
            }
        }
        LazyColumn(
            reverseLayout = true,
            modifier = Modifier.fillMaxWidth()
                .border(1.dp, Color.Black, RoundedCornerShape(4.dp)).padding(8.dp)
        ) {
            itemsIndexed(logs) { index, item ->
                if (index > 0) {
                    Divider()
                }
                Text(
                    item.first,
                    color = if (item.second == LogLevel.Error) Color.Red else LocalContentColor.current,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}


@Preview
@Composable
fun EmptyPreview() {
    FloaterTheme {
        MainContent(MainState(), {}, {}, {}, { _, _, _ -> }, {}, {})
    }
}

@Preview
@Composable
fun ValidPreview() {
    FloaterTheme {
        MainContent(
            MainState(floatApiKey = "sdljf", togglApiKey = "sdf", peopleId = 123),
            {},
            {},
            {},
            { _, _, _ -> },
            {}, {})
    }
}