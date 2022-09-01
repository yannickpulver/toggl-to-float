package com.appswithlove// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.appswithlove.ui.MainContent
import kotlinx.serialization.json.Json

@Composable
@Preview
fun App() {
    MaterialTheme {
        MainContent()
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Toggl 2 Float", icon = painterResource("drawable/icon.png")) {
        App()
    }
}

val version = "1.0.1" // todo: replace with bundle version

val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }