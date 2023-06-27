package com.appswithlove// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import com.appswithlove.ui.MainContent
import com.appswithlove.ui.MainViewModel
import com.appswithlove.ui.theme.FloaterTheme
import io.kanro.compose.jetbrains.expui.control.ActionButton
import io.kanro.compose.jetbrains.expui.control.Icon
import io.kanro.compose.jetbrains.expui.control.Tooltip
import io.kanro.compose.jetbrains.expui.theme.DarkTheme
import io.kanro.compose.jetbrains.expui.theme.LightTheme
import io.kanro.compose.jetbrains.expui.window.JBWindow
import kotlinx.serialization.json.Json

@Composable
@Preview
fun App(viewModel: MainViewModel) {
    FloaterTheme {
        MainContent(viewModel)
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun main() = application {

    val viewModel = MainViewModel()

    var isDark by remember { mutableStateOf(false) }
    val theme = if (isDark) {
        DarkTheme
    } else {
        LightTheme
    }


    JBWindow(
        onCloseRequest = ::exitApplication,
        title = "Toggl 👉 Float",
        showTitle = true, // If you want to render your own component in the center of the title bar like Intellij do, disable this to hide the title of the MainToolBar (TitleBar).
        theme = theme, // Change the theme here, LightTheme and DarkTheme are provided.
        icon = painterResource("icons/icon_24.svg"),
        mainToolBar = {
            // Render your own component in the MainToolBar (TitleBar).
            Row(
                Modifier.mainToolBarItem(Alignment.End, true)
            ) {
                Tooltip("Reset Toggle2Float if something is wrong.") {
                    ActionButton(
                        { viewModel.reset() }, Modifier.size(40.dp), shape = RectangleShape
                    ) {
                        Icon("icons/logout.svg")
                    }
                }
            }
        }) {
        App(viewModel)
    }
}

val version = "1.1.0" // todo: replace with bundle version

val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
val jsonNoDefaults = Json { ignoreUnknownKeys = true }