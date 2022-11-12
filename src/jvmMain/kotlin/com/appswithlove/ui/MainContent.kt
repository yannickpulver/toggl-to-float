package com.appswithlove.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.appswithlove.floaat.FloatPeopleItem
import com.appswithlove.floaat.totalHours
import com.appswithlove.ui.setup.SetupForm
import com.appswithlove.ui.theme.FloaterTheme
import com.appswithlove.ui.theme.LightGray
import com.appswithlove.version
import com.vanpra.composematerialdialogs.DesktopWindowPosition
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogProperties
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val viewModel = MainViewModel()

@Composable
fun MainContent() {
    val state = viewModel.state.collectAsState()

    MainContent(
        state = state.value,
        clear = viewModel::clear,
        syncProjects = viewModel::fetchProjects,
        archiveProjects = viewModel::archiveProjects,
        addTimeEntries = viewModel::addTimeEntries,
        save = viewModel::save,
        syncColors = viewModel::updateColors
    )
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
    clear: () -> Unit,
    syncProjects: () -> Unit,
    archiveProjects: () -> Unit,
    addTimeEntries: (LocalDate?) -> Unit,
    save: (String?, String?, FloatPeopleItem?) -> Unit,
    syncColors: () -> Unit,
) {
    Scaffold {
        Box {
            OutlinedButton(onClick = { clear() }, modifier = Modifier.align(Alignment.BottomEnd)) {
                Text("Reset T2F", style = MaterialTheme.typography.caption)
            }

            Version(modifier = Modifier.align(Alignment.BottomStart))

            Row {
                AnimatedVisibility(state.isValid) {
                    YourWeek(state)
                }
                Divider(modifier = Modifier.width(1.dp).fillMaxHeight())


                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (state.isValid) {
                        Welcome(syncProjects)
                        Divider()
                        AddTime(addTimeEntries = addTimeEntries)
                    }

                    if (state.loading) {
                        Loading()
                    } else {
                        SetupForm(state, save)
                    }
                    Divider()

                }
            }
        }
    }
}

@Composable
private fun YourWeek(state: MainState) {
    Column(Modifier.width(300.dp).padding(16.dp)) {
        val title = "Your Week" + if (state.weeklyOverview.isNotEmpty()) { " (${state.weeklyOverview.totalHours}h)"  } else ""
        Text(title, style = MaterialTheme.typography.h4)
        if (state.weeklyOverview.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                //Text("Your Week: (Total hours: ${state.weeklyOverview.totalHours}) ")
                state.weeklyOverview.forEach { (project, items) ->
                    Card(backgroundColor = LightGray) {
                        Column(Modifier.padding(8.dp)) {
                            Row {
                                project?.name?.let {
                                    Text(it, modifier = Modifier.weight(1f))
                                }
                                Text("${items.totalHours}h")
                            }

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
        } else {
            Loading()
        }
    }
}

@Composable
private fun Welcome(syncProjects: () -> Unit) {
    Text("Welcome to Toggl 2 Float! 🎉")
    Text(
        "If you need to add all of your Float projects to Toggl - Use the button below. You can also run it again to get the latest projects updated.",
        modifier = Modifier.fillMaxWidth(0.8f)
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth()
    ) {

//                        OutlinedButton(onClick = { openInBrowser(URI("https://track.toggl.com/projects")) }) {
//                            Text("🌍 Visit Toggl Projects")
//                        }

        Button(onClick = syncProjects) {
            Text("⬇️ Sync Projects")
        }

//                        Button(onClick = { syncColors() }) {
//                            Text("🎨 Sync Colors")
//                        }
    }
}

@Composable
private fun Loading() {
    Box(modifier = Modifier.fillMaxWidth()) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).size(20.dp))
    }
}

@Composable
private fun AddTime(addTimeEntries: (LocalDate?) -> Unit) {
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

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Time Entry", style = MaterialTheme.typography.h4)

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {

                OutlinedButton(onClick = { dialogState.show() }) {
                    Text(from?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: "Select date...")
                }

                AnimatedVisibility(from != null) {
                    Button(onClick = { addTimeEntries(from) }) {
                        Text("Add time entries 🚀")
                    }
                }
            }
        }
    }


}

@Composable
private fun Logs(list: List<Pair<String, LogLevel>>) {
    val logs = remember(list) { list.reversed() }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Logs")
        LazyColumn(
            reverseLayout = true,
            modifier = Modifier.heightIn(max = 200.dp).fillMaxWidth(0.8f)
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
        MainContent(MainState(), {}, {}, {}, { }, { _, _, _ -> }, {})
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
            { },
            { _, _, _ -> },
            {})
    }
}