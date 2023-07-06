package com.appswithlove.ui.feature.snackbar

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/**
 * Global holder of snackbar state
 */
object SnackbarStateHolder {

    private val _snackbarState = MutableSharedFlow<SnackbarState>(replay = 0)
    val snackbarState: SharedFlow<SnackbarState> = _snackbarState

    suspend fun put(snackbar: SnackbarState) {
        _snackbarState.emit(snackbar)
    }

    fun error(message: String? = null, resArgs: List<Any> = emptyList()) {
        CoroutineScope(Dispatchers.IO).launch {
            put(SnackbarState.Error(message = message))
        }
    }

    fun success(message: String? = null, resArgs: List<Any> = emptyList()) {
        CoroutineScope(Dispatchers.IO).launch {
            put(SnackbarState.Success(message = message))
        }
    }
}

// String messages may be replaced with StringRes, depending on what is more convenient.
sealed class SnackbarState {
    data class Success(val message: String? = null) : SnackbarState()
    data class Error(val message: String? = null) : SnackbarState()
}

enum class SnackbarActionLabel {
    SUCCESS,
    ERROR
}