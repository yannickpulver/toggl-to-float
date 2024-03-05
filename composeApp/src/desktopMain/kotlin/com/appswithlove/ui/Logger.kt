package com.appswithlove.ui

import com.appswithlove.ui.feature.snackbar.SnackbarStateHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

object Logger {

    val logs = MutableStateFlow(emptyList<Pair<String, LogLevel>>())

    fun log(message: String) {
        logs.update { it + listOf("${it.size} - $message" to LogLevel.Default) }
        if (message != SPACER) {
            SnackbarStateHolder.success(message)
        }
    }

    fun err(message: String) {
        logs.update { it + listOf("${it.size} - $message" to LogLevel.Error) }
        SnackbarStateHolder.error(message)
    }

    fun clear() {
        logs.update { emptyList() }
    }

    const val SPACER = "---"
}

enum class LogLevel {
    Default,
    Error
}