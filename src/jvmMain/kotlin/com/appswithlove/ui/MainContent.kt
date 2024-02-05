package com.appswithlove.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.appswithlove.floaat.FloatPeopleItem
import com.appswithlove.ui.components.PrimaryButton
import com.appswithlove.ui.feature.atlassian.AddTimeAtlassian
import com.appswithlove.ui.feature.snackbar.SnackbarPublisher
import com.appswithlove.ui.feature.update.LatestRelease
import com.appswithlove.ui.feature.yourweek.YourWeek
import com.appswithlove.ui.setup.SetupForm
import com.appswithlove.ui.theme.FloaterTheme
import com.appswithlove.ui.utils.openInBrowser
import com.appswithlove.version
import com.google.accompanist.flowlayout.FlowRow
import com.vanpra.composematerialdialogs.DesktopWindowPosition
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.MaterialDialogProperties
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun MainContent(viewModel: MainViewModel) {

    val state = viewModel.state.collectAsState()

    val focusRequester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    MainContent(
        state = state.value,
        syncProjects = viewModel::fetchProjects,
        addTimeEntries = viewModel::addTimeEntries,
        save = viewModel::save,
        loadLastWeek = viewModel::loadTimeLastWeek,
        clearLogs = viewModel::clearLogs,
        modifier = Modifier
            .focusRequester(focusRequester)
            .onFocusChanged {
                hasFocus = it.hasFocus
            }
            .focusable(),
        startTimer = { id, description ->
            viewModel.startTimer(id, description)
        }
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
    addTimeEntries: (LocalDate?) -> Unit,
    save: (String?, String?, FloatPeopleItem?) -> Unit,
    loadLastWeek: (Int) -> Unit,
    clearLogs: () -> Unit,
    modifier: Modifier = Modifier,
    startTimer: (Int, String) -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(modifier = modifier) {
        Box {
            Column(Modifier.verticalScroll(scrollState)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when {
                        state.isValid -> {
                            LastRelease(state.latestRelease)
                            Welcome(syncProjects)
                            AddTime(
                                addTimeEntries = addTimeEntries,
                                missingEntryDates = state.missingEntryDates
                            )
                            Divider()
                            AnimatedVisibility(state.isValid) {
                                YourWeek(state, loadLastWeek, startTimer)
                            }
                            Divider()
                            AddTimeAtlassian()
                            Divider()
                            Logs(
                                list = state.logs,
                                clearLogs = clearLogs,
                                modifier = Modifier
                            )
                        }

                        state.loading -> {
                            Loading()
                        }

                        else -> {
                            SetupForm(state, save)
                        }
                    }
                    Version()
                }

            }
            SnackbarPublisher(Modifier.align(Alignment.BottomCenter))
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LastRelease(lastRelease: LatestRelease?) {
    if (lastRelease != null) {
        Card(
            onClick = {
                openInBrowser(URI.create(lastRelease.downloadUrl))
            },
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colors.surface,
            elevation = 10.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Update available! ✨", style = MaterialTheme.typography.subtitle2)
                    Text(text = lastRelease.tagName, style = MaterialTheme.typography.body2)
                }
                Icon(
                    imageVector = Icons.Rounded.ArrowForward, contentDescription = null,
                    modifier = Modifier.size(20.dp).alpha(0.2f)
                )
            }
        }
    }
}

@Composable
private fun Welcome(syncProjects: () -> Unit) {
    Column {

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Happy ${LocalDate.now().dayOfWeek.toString().lowercase().capitalize()}! 🎉",
                style = MaterialTheme.typography.h4
            )

            PrimaryButton(onClick = syncProjects) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text(
                    "Sync",
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        Text(
            "Add time to float:",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.body2
        )
    }
}

@Composable
fun Loading() {
    Box(modifier = Modifier.fillMaxWidth()) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).size(20.dp))
    }
}

@Composable
private fun AddTime(addTimeEntries: (LocalDate?) -> Unit, missingEntryDates: List<LocalDate>) {
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
                addTimeEntries(LocalDate.of(date.year, date.month, date.dayOfMonth))
            }
        }

        Column {
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
    }
}

@Composable
private fun Logs(
    list: List<Pair<String, LogLevel>>,
    clearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            modifier = Modifier.fillMaxWidth().heightIn(0.dp, 300.dp)
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
        MainContent(MainState(), {}, {}, { _, _, _ -> }, {}, {}, Modifier, { _, _ -> })
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
            { _, _, _ -> },
            {}, {}, Modifier, { _, _ -> }
        )
    }
}