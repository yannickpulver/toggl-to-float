package com.appswithlove.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet

object Logger {

    val logs = MutableStateFlow(emptyList<Pair<String, LogLevel>>())

    fun log(string: String) {
        logs.update { it + listOf("${it.size} - $string" to LogLevel.Default) }
    }


    fun err(string: String) {
        logs.update { it + listOf("${it.size} - $string" to LogLevel.Error) }
    }

    fun clear() {
        logs.update { emptyList() }
    }


}

enum class LogLevel {
    Default,
    Error
}