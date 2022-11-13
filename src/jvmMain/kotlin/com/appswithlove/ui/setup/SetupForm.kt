package com.appswithlove.ui.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.appswithlove.floaat.FloatPeopleItem
import com.appswithlove.ui.MainState
import com.appswithlove.ui.utils.openInBrowser
import java.net.URI
import java.time.LocalDate

@Composable
fun SetupForm(
    state: MainState,
    save: (String?, String?, FloatPeopleItem?) -> Unit
) {
    val togglApiKey = remember { mutableStateOf(state.togglApiKey) }
    val floatApiKey = remember { mutableStateOf(state.floatApiKey) }
    val client = remember { mutableStateOf(state.people.find { it.people_id == state.peopleId }) }
    LaunchedEffect(state.togglApiKey) {
        togglApiKey.value = state.togglApiKey
    }
    LaunchedEffect(state.floatApiKey) {
        floatApiKey.value = state.floatApiKey
    }
    LaunchedEffect(state.peopleId) {
        client.value = state.people.find { it.people_id == state.peopleId }
    }

    Text("Happy ${LocalDate.now().dayOfWeek.toString().lowercase().capitalize()}! 🎉", style = MaterialTheme.typography.h4)

    when {
        state.togglApiKey.isNullOrEmpty() -> {
            TogglSetup(key = togglApiKey.value.orEmpty(), onChange = { togglApiKey.value = it })
        }
        state.floatApiKey.isNullOrEmpty() -> {
            FloatSetup(key = floatApiKey.value.orEmpty(), onChange = { floatApiKey.value = it })
        }
        state.peopleId == null || state.peopleId == -1 -> {
            PeopleSelection(people = state.people, client = client.value, onChange = { client.value = it })
        }
    }

    if (!state.isValid) {
        Button(onClick = { save(togglApiKey.value, floatApiKey.value, client.value) }) {
            Text("Save")
        }
    }
}

@Composable
private fun FloatSetup(key: String, onChange: (String) -> Unit) {
    Text(
        "🔑 Setup Float API Key: Get the API key (e.g. from 1Password) & paste it in the field below:",
        modifier = Modifier.fillMaxWidth(0.8f)
    )
    OutlinedTextField(
        value = key,
        onValueChange = onChange,
        label = {
            Text("Float API Key")
        },
        visualTransformation = PasswordVisualTransformation()
    )
}

@Composable
private fun TogglSetup(key: String, onChange: (String) -> Unit) {
    Text(
        "🔑 Setup Toggl API Key: Please visit https://track.toggl.com/profile (click the button below) and copy the key from the 'API Token' section here & paste it in the field below.",
        modifier = Modifier.fillMaxWidth(0.8f)
    )
    OutlinedButton({ openInBrowser(URI("https://track.toggl.com/profile")) }) {
        Text("🌍 Open Toggl Website")
    }
    OutlinedTextField(
        value = key,
        onValueChange = onChange,
        label = {
            Text("Toggl API Key")
        },
        visualTransformation = PasswordVisualTransformation()
    )
}

@Composable
private fun PeopleSelection(
    people: List<FloatPeopleItem>,
    client: FloatPeopleItem?,
    onChange: (FloatPeopleItem) -> Unit
) {
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
            people.forEachIndexed { index, floatPeopleItem ->
                val isSelected = floatPeopleItem == client
                item {
                    Text(
                        "${floatPeopleItem.name} (${floatPeopleItem.people_id})",
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.clickable { onChange(floatPeopleItem) }
                            .background(if (isSelected) Color.LightGray else Color.Transparent)
                            .padding(8.dp)
                            .fillMaxWidth()
                    )
                    if (index < people.size - 1) {
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

    AnimatedVisibility(client != null) {
        Text("Are you ${client?.name}? Then press save!")
    }
}
