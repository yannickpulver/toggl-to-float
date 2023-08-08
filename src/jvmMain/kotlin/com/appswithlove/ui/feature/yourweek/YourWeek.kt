package com.appswithlove.ui.feature.yourweek

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.appswithlove.floaat.FloatOverview
import com.appswithlove.floaat.FloatProject
import com.appswithlove.floaat.rgbColor
import com.appswithlove.ui.Loading
import com.appswithlove.ui.MainState
import com.appswithlove.ui.feature.snackbar.SnackbarStateHolder
import com.appswithlove.ui.theme.FloaterTheme
import com.appswithlove.ui.totalHours
import kotlin.math.pow

@Composable
fun YourWeek(state: MainState, loadLastWeek: (Int) -> Unit, startTimer: (Int, String) -> Unit) {
    Column {
        YourWeekContent(
            state = state,
            loadLastWeek = loadLastWeek,
            modifier = Modifier.fillMaxWidth(),
            startTimer = startTimer
        )
    }
}

@Composable
fun YourWeekContent(
    state: MainState,
    loadLastWeek: (Int) -> Unit,
    modifier: Modifier = Modifier,
    startTimer: (Int, String) -> Unit
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Your Week",
                style = MaterialTheme.typography.h4,
                modifier = Modifier.weight(1f)
            )
            if (state.weeklyOverview.isNotEmpty()) {
                Text(
                    "Planned hours: ${state.weeklyOverview.totalHours}h",
                    style = MaterialTheme.typography.body2
                )
            }
        }
        Text(
            "Click to start timer",
            style = MaterialTheme.typography.body2
        )
        if (state.weeklyOverview.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                //Text("Your Week: (Total hours: ${state.weeklyOverview.totalHours}) ")
                state.weeklyOverview.forEach { (project, items) ->
                    ProjectItem(project, items, loadLastWeek, startTimer)
                }
            }
            Divider()
        } else {
            Loading()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProjectItem(
    project: FloatProject?,
    items: List<FloatOverview>,
    loadLastWeek: (Int) -> Unit,
    startTimer: (Int, String) -> Unit = { _, _ -> }
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val showNotes = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colors.surface)

    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEachIndexed { index, item ->
                val color = item.phase?.rgbColor ?: project?.rgbColor ?: Color.Gray
                Column {

                    Card(
                        backgroundColor = color,
                        contentColor = suitableContentColor(color),
                        shape = if (showNotes.value) RoundedCornerShape(
                            4.dp,
                            4.dp,
                            0.dp,
                            0.dp
                        ) else RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                onClick = {
                                    startTimer(
                                        item.phase?.phase_id ?: item.project?.project_id ?: -1,
                                        item.task.name
                                    )
                                })
                            .onClick(
                                matcher = PointerMatcher.mouse(PointerButton.Secondary),
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(item.id.toString()))
                                    SnackbarStateHolder.success("Copied ${item.id} to clipboard")
                                }
                            )
                            .onClick(
                                matcher = PointerMatcher.mouse(PointerButton.Tertiary),
                                onClick = {
                                    loadLastWeek(item.project?.project_id ?: -1)
                                }
                            ),
                        elevation = 0.dp
                    ) {
                        Column(Modifier.padding(8.dp, 4.dp)) {
                            Text(
                                text = item.title,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(color),
                                color = contentColorFor(color),
                                style = MaterialTheme.typography.subtitle2,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            item.project?.name?.let {
                                Text(it, style = MaterialTheme.typography.body2)
                            }

                            Row(
                                Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(Modifier.weight(1f))

                                if (item.task.notes.isNotEmpty()) {
                                    IconButton(
                                        modifier = Modifier.size(20.dp),
                                        onClick = {
                                            showNotes.value = !showNotes.value
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Show notes",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Text(
                                    "[${item.id}]",
                                    style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Normal),
                                    modifier = Modifier.alpha(0.8f),
                                )
                                Text(
                                    "${item.weekHours}h",
                                    style = MaterialTheme.typography.caption,
                                    modifier = Modifier,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                    if (showNotes.value) {
                        Surface(
                            Modifier.border(
                                1.dp,
                                MaterialTheme.colors.onSurface.copy(0.1f),
                                RoundedCornerShape(0.dp, 0.dp, 4.dp, 4.dp)
                            ).fillMaxWidth()
                                .onClick(
                                matcher = PointerMatcher.mouse(PointerButton.Secondary),
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(item.task.notes))
                                    SnackbarStateHolder.success("Copied notes to clipboard")
                                }
                            )
                        ) {
                            // SelectionContainer {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Row {

                                    Text(
                                        "Notes",
                                        style = MaterialTheme.typography.caption,
                                        modifier = Modifier.weight(1f)
                                    )
                                    // copy to clipboard
                                    IconButton(
                                        modifier = Modifier.size(20.dp),
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(item.task.notes))
                                            SnackbarStateHolder.success("Copied notes to clipboard")
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy notes to clipboard",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                LinkifiedText(
                                    text = item.task.notes,
                                )
                            }
                            // }
                        }
                    }
                }

            }
        }
    }
}

@Preview
@Composable
fun YourWeekPreview() {
    FloaterTheme {
        YourWeekContent(
            state = MainState.Preview,
            loadLastWeek = {},
            startTimer = { _, _ -> }
        )
    }
}

@Preview
@Composable
fun ProjectItemPreview() {
    FloaterTheme {
        ProjectItem(
            FloatProject.Preview,
            listOf(FloatOverview.Preview, FloatOverview.Preview),
            loadLastWeek = {})
    }
}

fun calculateLuminance(color: Color): Float {
    val red = color.red
    val green = color.green
    val blue = color.blue

    val rLinear = if (red > 0.03928f) ((red + 0.055f) / 1.055f).pow(2.4f) else red / 12.92f
    val gLinear = if (green > 0.03928f) ((green + 0.055f) / 1.055f).pow(2.4f) else green / 12.92f
    val bLinear = if (blue > 0.03928f) ((blue + 0.055f) / 1.055f).pow(2.4f) else blue / 12.92f

    return 0.2126f * rLinear + 0.7152f * gLinear + 0.0722f * bLinear
}

fun suitableContentColor(color: Color): Color {
    val luminance = calculateLuminance(color)
    return if (luminance > 0.35f) Color.Black else Color.White
}

