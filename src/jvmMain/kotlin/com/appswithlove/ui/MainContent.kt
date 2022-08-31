package com.appswithlove.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.appswithlove.floaat.FloatPeopleItem
import com.appswithlove.version
import java.net.URI
import java.time.LocalDate

val viewModel = MainViewModel()

@Composable
@Preview
fun MainContent() {
    val state = viewModel.state.collectAsState()

    MainContent(
        state = state.value,
        clear = viewModel::clear,
        fetchProjects = viewModel::fetchProjects,
        archiveProjects = viewModel::archiveProjects,
        addTimeEntries = viewModel::addTimeEntries,
        save = viewModel::save
    )
}

@Composable
@Preview
private fun MainContent(
    state: MainState,
    clear: () -> Unit,
    fetchProjects: () -> Unit,
    archiveProjects: () -> Unit,
    addTimeEntries: (String, String) -> Unit,
    save: (String?, String?, FloatPeopleItem?) -> Unit,
) {
    Scaffold {
        Box {
            OutlinedButton(onClick = { clear() }, modifier = Modifier.align(Alignment.BottomEnd)) {
                Text("Reset T2F", style = MaterialTheme.typography.caption)
            }

            Text(
                "Version $version",
                modifier = Modifier.align(Alignment.BottomStart),
                style = MaterialTheme.typography.caption
            )

            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.isValid) {
                    Text("Welcome to Toggl 2 Float! 🎉")
                    Text("If you need to add all of your Float projects to Toggl - Use the button below. You can also run it again to get the latest projects updated.", modifier = Modifier.fillMaxWidth(0.8f))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        OutlinedButton(onClick = { openInBrowser(URI("https://track.toggl.com/projects")) }) {
                            Text("🌍 Visit Toggl Projects")
                        }

                        Button(onClick = { fetchProjects() }) {
                            Text("Sync Float Projects ➡️ Toggl")
                        }
                    }
                    Divider()

                    val from = remember { mutableStateOf(LocalDate.now().toString()) }
                    val to = remember { mutableStateOf(LocalDate.now().toString()) }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            from.value,
                            { from.value = it },
                            leadingIcon = { Text("📆") },
                            label = { Text("From (YYYY-MM-DD)") })
                        OutlinedTextField(
                            to.value,
                            { to.value = it },
                            leadingIcon = { Text("📆") },
                            label = { Text("To (YYYY-MM-DD)") })
                    }

                    if (from.value == to.value) {
                        Text("Want to add time entries from ${from.value} to Float?")
                    } else {
                        Text("Want to add time entries from ${from.value} - ${to.value} to Float?")

                    }

                    Row {
                        Button(onClick = { addTimeEntries(from.value, to.value) }) {
                            Text("Add time entries 🚀")
                        }
                    }
                }

                if (state.loading) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                } else {
                    Form(state, save)
                }
                Divider()

                AnimatedVisibility(state.logs.isNotEmpty()) {
                    Logs(state.logs)
                }
            }
        }
    }
}

@Composable
@Preview
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

@Composable
@Preview
private fun Form(
    state: MainState,
    save: (String?, String?, FloatPeopleItem?) -> Unit
) {
    val togglApiKey = remember { mutableStateOf<String?>(null) }
    val floatApiKey = remember { mutableStateOf<String?>(null) }
    val client = remember { mutableStateOf<FloatPeopleItem?>(null) }

    when {
        state.togglApiKey.isNullOrEmpty() -> {
            Text(
                "🔑 Setup Toggl API Key: Please visit https://track.toggl.com/profile (click the button below) and copy the key from the 'API Token' section here & paste it in the field below.",
                modifier = Modifier.fillMaxWidth(0.8f)
            )
            OutlinedButton({ openInBrowser(URI("https://track.toggl.com/profile")) }) {
                Text("🌍 Open Toggl Website", style = MaterialTheme.typography.caption)
            }
            OutlinedTextField(
                value = togglApiKey.value ?: "",
                onValueChange = { togglApiKey.value = it },
                label = {
                    Text("Toggl API Key")
                },
                visualTransformation = PasswordVisualTransformation()
            )
        }
        state.floatApiKey.isNullOrEmpty() -> {
            Text(
                "🔑 Setup Float API Key: Get the API key (e.g. from 1Password) & paste it in the field below:",
                modifier = Modifier.fillMaxWidth(0.8f)
            )
            OutlinedTextField(value = floatApiKey.value ?: "", onValueChange = { floatApiKey.value = it }, label = {
                Text("Float API Key")
            }, visualTransformation = PasswordVisualTransformation())
        }

        state.peopleId == null || state.peopleId == -1 -> {

            Text(
                "🙆 Select your name from the list below:",
                modifier = Modifier.fillMaxWidth(0.8f)
            )

            val lazyListState = rememberLazyListState()
            Row(Modifier.fillMaxWidth(0.8f).height(300.dp)) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.weight(1f).border(1.dp, Color.Black, RoundedCornerShape(4.dp)).padding(8.dp)
                ) {
                    state.people.forEachIndexed { index, floatPeopleItem ->
                        val isSelected = floatPeopleItem == client.value
                        item {
                            Text(
                                "${floatPeopleItem.name} (${floatPeopleItem.people_id})",
                                style = MaterialTheme.typography.caption,
                                modifier = Modifier.clickable { client.value = floatPeopleItem }
                                    .background(if (isSelected) Color.LightGray else Color.Transparent)
                                    .padding(8.dp)
                                    .fillMaxWidth()
                            )
                            if (index < state.people.size - 1) {
                                Divider()
                            }
                        }
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.fillMaxHeight(),
                    adapter = rememberScrollbarAdapter(lazyListState)
                )
            }

            AnimatedVisibility(client.value != null) {
                Text("Are you ${client.value?.name}? Then press save!")
            }
        }
    }


    if (!state.isValid) {
        Button(onClick = { save(togglApiKey.value, floatApiKey.value, client.value) }) {
            Text("Save")
        }
    }
}