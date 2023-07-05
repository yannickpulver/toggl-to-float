package com.appswithlove.ui.feature.yourweek

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.appswithlove.floaat.FloatOverview
import com.appswithlove.floaat.FloatProject
import com.appswithlove.floaat.rgbColor
import com.appswithlove.ui.Loading
import com.appswithlove.ui.MainState
import com.appswithlove.ui.theme.FloaterTheme
import com.appswithlove.ui.totalHours
import kotlin.math.pow

@Composable
fun YourWeek(state: MainState, loadLastWeek: (Int) -> Unit, startTimer: (Int, String) -> Unit) {

    Column(
        Modifier.background(MaterialTheme.colors.onSurface.copy(0.05f)).fillMaxHeight()
            .padding(8.dp)
    ) {
        YourWeekContent(
            state = state,
            loadLastWeek = loadLastWeek,
            modifier = Modifier.width(250.dp),
            startTimer
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
        Text(
            "Your Week",
            style = MaterialTheme.typography.h4,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (state.weeklyOverview.isNotEmpty()) {
            Text(
                "Planned hours: ${state.weeklyOverview.totalHours}h",
                style = MaterialTheme.typography.caption
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                //Text("Your Week: (Total hours: ${state.weeklyOverview.totalHours}) ")
                state.weeklyOverview.forEach { (project, items) ->
                    ProjectItem(project, items, loadLastWeek, startTimer)
                }
            }
        } else {
            Loading()
        }
    }
}

@Composable
private fun ProjectItem(
    project: FloatProject?,
    items: List<FloatOverview>,
    loadLastWeek: (Int) -> Unit,
    startTimer: (Int, String) -> Unit = { _, _ -> }
) {
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
                Card(
                    backgroundColor = color,
                    contentColor = suitableContentColor(color),
                    modifier = Modifier.fillMaxWidth().clickable(onClick = {
                        startTimer(
                            item.phase?.phase_id ?: item.project?.project_id ?: -1,
                            item.task.name
                        )
                    }),
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
                        Row(Modifier.padding(top = 8.dp)) {
                            Text(
                                "[${item.id}]",
                                style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Normal),
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${item.weekHours}h",
                                style = MaterialTheme.typography.caption,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
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
