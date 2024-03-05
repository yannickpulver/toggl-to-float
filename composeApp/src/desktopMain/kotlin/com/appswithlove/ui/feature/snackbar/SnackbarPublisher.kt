package com.appswithlove.ui.feature.snackbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarDefaults
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun SnackbarPublisher(modifier: Modifier = Modifier) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var snackbarJob: Job = Job()

    LaunchedEffect(Unit) {
        SnackbarStateHolder.snackbarState.collect { snackbarState ->
            snackbarJob.cancel()
            snackbarJob = scope.launch {
                when (snackbarState) {
                    is SnackbarState.Error -> snackbarHostState.showSnackbar(
                        snackbarState.message.orEmpty(),
                        SnackbarActionLabel.ERROR.name,
                        duration = SnackbarDuration.Long
                    )

                    is SnackbarState.Success -> snackbarHostState.showSnackbar(
                        snackbarState.message.orEmpty(),
                        SnackbarActionLabel.SUCCESS.name
                    )
                }
            }
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier.padding(bottom = 24.dp)
    ) { data ->
        val isError = data.actionLabel == SnackbarActionLabel.ERROR.name
        AppSnackbar(data.message, isError)
    }
}

@Composable
private fun AppSnackbar(message: String, isError: Boolean) {
    Surface(
        modifier = Modifier.padding(horizontal = 64.dp),
        color = if (isError) MaterialTheme.colors.error else SnackbarDefaults.backgroundColor,
        shape = CircleShape
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon(
            //     if (isError) Icons.Default.Close else Icons.Default.Check,
            //     contentDescription = null,
            //     tint = Color.White,
            //     modifier = Modifier.size(16.dp)
            // )
            Text(
                text = message,
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.caption
            )
        }
    }
}
