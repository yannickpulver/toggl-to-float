package com.appswithlove.ui.feature.snackbar

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Global holder of snackbar state
 */
object SnackbarStateHolder {

    private val _snackbarState = MutableSharedFlow<SnackbarState>(replay = 0)
    val snackbarState: SharedFlow<SnackbarState> = _snackbarState

    suspend fun put(snackbar: SnackbarState) {
        _snackbarState.emit(snackbar)
    }

    suspend fun error(message: String? = null, resArgs: List<Any> = emptyList()) {
        put(SnackbarState.Error(message = message))
    }

    suspend fun success(message: String? = null, resArgs: List<Any> = emptyList()) {
        put(SnackbarState.Success(message = message))
    }
}

// String messages may be replaced with StringRes, depending on what is more convenient.
sealed class SnackbarState {
    data class Success(val message: String? = null) : SnackbarState()
    data class Error(val message: String? = null) : SnackbarState()
}