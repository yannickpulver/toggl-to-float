package com.appswithlove// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
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
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}

val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }