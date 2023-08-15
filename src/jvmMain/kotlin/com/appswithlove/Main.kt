package com.appswithlove// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.appswithlove.di.initKoin
import com.appswithlove.ui.MainContent
import com.appswithlove.ui.MainViewModel
import com.appswithlove.ui.components.PrimaryOutlineButton
import com.appswithlove.ui.theme.FloaterTheme
import io.kanro.compose.jetbrains.expui.control.ActionButton
import io.kanro.compose.jetbrains.expui.control.Icon
import io.kanro.compose.jetbrains.expui.control.Tooltip
import io.kanro.compose.jetbrains.expui.theme.DarkTheme
import io.kanro.compose.jetbrains.expui.theme.LightTheme
import io.kanro.compose.jetbrains.expui.window.JBWindow
import io.kanro.compose.jetbrains.expui.window.LocalWindow
import kotlinx.serialization.json.Json
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

@Composable
@Preview
fun App(viewModel: MainViewModel) {

    val window = LocalWindow.current
    LaunchedEffect(Unit) {
        window.addWindowFocusListener(object : java.awt.event.WindowFocusListener {
            override fun windowGainedFocus(e: java.awt.event.WindowEvent?) {
                viewModel.refresh()
            }

            override fun windowLostFocus(e: java.awt.event.WindowEvent?) {}
        })
    }

    MainContent(viewModel)
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
fun main() = application {
    KoinApplication(application = { initKoin() }) {
        val viewModel = koinInject<MainViewModel>()
        val state = viewModel.state.collectAsState()

        var isDark by remember { mutableStateOf(false) }
        val theme = if (isDark) {
            DarkTheme
        } else {
            LightTheme
        }

        val windowState = rememberWindowState(size = DpSize(400.dp, 700.dp))

        val showResetDialog = remember { mutableStateOf(false) }


        JBWindow(
            state = windowState,
            onCloseRequest = ::exitApplication,
            title = "Toggl 👉 Float",
            showTitle = true, // If you want to render your own component in the center of the title bar like Intellij do, disable this to hide the title of the MainToolBar (TitleBar).
            theme = theme, // Change the theme here, LightTheme and DarkTheme are provided.
            icon = painterResource("icons/icon_24.svg"),
            mainToolBar = {
                // Render your own component in the MainToolBar (TitleBar).
                Row(
                    Modifier.mainToolBarItem(Alignment.End, true),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Tooltip("Let's just say we're loading something") {
                        if (state.value.loading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    Tooltip("Reset Toggle2Float if something is wrong.") {
                        ActionButton(
                            { showResetDialog.value = true },
                            Modifier.size(40.dp),
                            shape = RectangleShape
                        ) {
                            Icon("icons/logout.svg")
                        }
                    }
                }
            }) {
            FloaterTheme {
                App(viewModel)

                if (showResetDialog.value) {
                    AlertDialog(
                        onDismissRequest = { showResetDialog.value = false },
                        title = { Text("Reset") },
                        text = { Text("Do you want to reset T2F?") },
                        confirmButton = {
                            com.appswithlove.ui.components.PrimaryButton(
                                onClick = { viewModel.reset() }
                            ) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            PrimaryOutlineButton(
                                onClick = { showResetDialog.value = false }
                            ) {
                                Text("Cancel", style = MaterialTheme.typography.button)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                }
            }
        }
    }
}

val version = "1.3.6" // todo: replace with bundle version

val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
val jsonNoDefaults = Json { ignoreUnknownKeys = true }