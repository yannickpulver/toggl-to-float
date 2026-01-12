package com.appswithlove.ui.feature.timeentry

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.appswithlove.timetracking.ActiveTimer
import com.appswithlove.timetracking.LocalTimeEntry
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt

private const val START_HOUR = 6
private const val END_HOUR = 22
private val HOUR_HEIGHT = 60.dp
private val TIME_LABEL_WIDTH = 50.dp
private const val MIN_ENTRY_MINUTES = 15

@Composable
fun TimelineView(
    selectedDate: java.time.LocalDate,
    entries: List<LocalTimeEntry>,
    activeTimer: ActiveTimer?,
    currentTime: Instant,
    getProjectName: (Int?, Int?) -> String?,
    onCreateEntry: (startTime: Instant, endTime: Instant) -> Unit,
    onUpdateEntry: (id: Long, newStartTime: Instant, newEndTime: Instant) -> Unit = { _, _, _ -> },
    onUpdateTimer: (newStartTime: Instant) -> Unit = {},
    onEntryClick: (LocalTimeEntry) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val hourHeightPx = with(density) { HOUR_HEIGHT.toPx() }

    var dragStartY by remember { mutableStateOf<Float?>(null) }
    var dragCurrentY by remember { mutableStateOf<Float?>(null) }
    var isDragging by remember { mutableStateOf(false) }

    val totalHours = END_HOUR - START_HOUR
    val totalHeight = HOUR_HEIGHT * totalHours

    // Calculate current time position
    val isToday = selectedDate == java.time.LocalDate.now()
    val tz = TimeZone.currentSystemDefault()
    val currentLocalTime = currentTime.toLocalDateTime(tz).time
    val currentTimeMinutes = (currentLocalTime.hour - START_HOUR) * 60 + currentLocalTime.minute
    val currentTimeY = (currentTimeMinutes / 60f) * hourHeightPx

    // Scroll to current time on launch (with offset so it's visible near top)
    LaunchedEffect(Unit) {
        if (isToday && currentTimeMinutes > 0) {
            val scrollOffset = (currentTimeY - 100f).coerceAtLeast(0f).toInt()
            scrollState.scrollTo(scrollOffset)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Timeline",
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .verticalScroll(scrollState)
        ) {
            Row(modifier = Modifier.height(totalHeight)) {
                // Hour labels column
                Column(
                    modifier = Modifier
                        .width(TIME_LABEL_WIDTH)
                        .fillMaxHeight()
                ) {
                    for (hour in START_HOUR until END_HOUR) {
                        Box(
                            modifier = Modifier
                                .height(HOUR_HEIGHT)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Text(
                                text = formatTime(hour, 0),
                                style = MaterialTheme.typography.caption,
                                color = Color.Gray,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                }

                // Timeline content area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(totalHeight)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp))
                        .pointerInput(selectedDate) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    dragStartY = offset.y
                                    dragCurrentY = offset.y
                                    isDragging = true
                                },
                                onDrag = { change, _ ->
                                    dragCurrentY = change.position.y.coerceIn(0f, totalHours * hourHeightPx)
                                },
                                onDragEnd = {
                                    val startY = dragStartY
                                    val endY = dragCurrentY
                                    if (startY != null && endY != null) {
                                        val (minY, maxY) = if (startY < endY) startY to endY else endY to startY
                                        val startTime = yToInstant(minY, hourHeightPx, selectedDate)
                                        val endTime = yToInstant(maxY, hourHeightPx, selectedDate)
                                        onCreateEntry(startTime, endTime)
                                    }
                                    isDragging = false
                                    dragStartY = null
                                    dragCurrentY = null
                                },
                                onDragCancel = {
                                    isDragging = false
                                    dragStartY = null
                                    dragCurrentY = null
                                }
                            )
                        }
                ) {
                    // Hour grid lines
                    Canvas(modifier = Modifier.matchParentSize()) {
                        for (i in 0..totalHours) {
                            val y = i * hourHeightPx
                            drawLine(
                                color = Color.LightGray,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f
                            )
                        }
                        // Half-hour lines
                        for (i in 0 until totalHours) {
                            val y = (i + 0.5f) * hourHeightPx
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.5f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 0.5f
                            )
                        }
                    }

                    // Existing time entries
                    entries.forEach { entry ->
                        TimeEntryBlock(
                            entry = entry,
                            selectedDate = selectedDate,
                            hourHeightPx = hourHeightPx,
                            totalHours = totalHours,
                            density = density,
                            onUpdateEntry = onUpdateEntry,
                            onClick = { onEntryClick(entry) }
                        )
                    }

                    // Active timer (only show on today)
                    if (activeTimer != null && selectedDate == java.time.LocalDate.now()) {
                        val tz = TimeZone.currentSystemDefault()
                        val timerStartDate = activeTimer.startTime.toLocalDateTime(tz).date
                        val today = LocalDate(selectedDate.year, selectedDate.monthValue, selectedDate.dayOfMonth)

                        if (timerStartDate == today) {
                            ActiveTimerBlock(
                                timer = activeTimer,
                                currentTime = currentTime,
                                projectName = getProjectName(activeTimer.projectId, activeTimer.phaseId),
                                selectedDate = selectedDate,
                                hourHeightPx = hourHeightPx,
                                totalHours = totalHours,
                                density = density,
                                onUpdateTimer = onUpdateTimer
                            )
                        }
                    }

                    // Drag preview
                    if (isDragging && dragStartY != null && dragCurrentY != null) {
                        val (minY, maxY) = if (dragStartY!! < dragCurrentY!!) {
                            dragStartY!! to dragCurrentY!!
                        } else {
                            dragCurrentY!! to dragStartY!!
                        }
                        val heightPx = maxY - minY

                        Box(
                            modifier = Modifier
                                .offset { IntOffset(0, minY.roundToInt()) }
                                .fillMaxWidth()
                                .height(with(density) { heightPx.toDp() })
                                .background(
                                    Color(0xFF2196F3).copy(alpha = 0.3f),
                                    RoundedCornerShape(4.dp)
                                )
                        ) {
                            val startTime = yToTime(minY, hourHeightPx)
                            val endTime = yToTime(maxY, hourHeightPx)
                            Text(
                                text = "${formatTime(startTime.first, startTime.second)} - ${formatTime(endTime.first, endTime.second)}",
                                style = MaterialTheme.typography.caption,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }

                    // Current time indicator (on top of everything, only show today)
                    if (isToday && currentTimeMinutes in 0..(totalHours * 60)) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            // Red line
                            drawLine(
                                color = Color.Red,
                                start = Offset(0f, currentTimeY),
                                end = Offset(size.width, currentTimeY),
                                strokeWidth = 2f
                            )
                            // Small circle at the start
                            drawCircle(
                                color = Color.Red,
                                radius = 5f,
                                center = Offset(5f, currentTimeY)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveTimerBlock(
    timer: ActiveTimer,
    currentTime: Instant,
    projectName: String?,
    selectedDate: java.time.LocalDate,
    hourHeightPx: Float,
    totalHours: Int,
    density: Density,
    onUpdateTimer: (newStartTime: Instant) -> Unit
) {
    val tz = TimeZone.currentSystemDefault()
    val startTime = timer.startTime.toLocalDateTime(tz).time
    val currentLocalTime = currentTime.toLocalDateTime(tz).time

    val startMinutes = (startTime.hour - START_HOUR) * 60 + startTime.minute
    val currentMinutes = (currentLocalTime.hour - START_HOUR) * 60 + currentLocalTime.minute

    if (startMinutes < 0) return

    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var tempStartMinutes by remember { mutableStateOf(startMinutes) }

    val currentStartMinutes = if (isDragging) tempStartMinutes else startMinutes

    val topPx = (currentStartMinutes / 60f) * hourHeightPx
    val durationMinutes = ((if (isDragging) currentMinutes - tempStartMinutes else currentMinutes - startMinutes)).coerceAtLeast(MIN_ENTRY_MINUTES)
    val heightPx = (durationMinutes / 60f) * hourHeightPx
    val resizeHandleHeight = 8.dp
    val resizeHandleHeightPx = with(density) { resizeHandleHeight.toPx() }

    // Pulsing animation
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .offset { IntOffset(4, topPx.roundToInt()) }
            .padding(end = 8.dp)
            .fillMaxWidth()
            .height(with(density) { heightPx.toDp() })
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { heightPx.toDp() })
                .pointerInput(timer.startTime) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            // Only allow dragging from top handle (adjust start time)
                            if (offset.y < resizeHandleHeightPx) {
                                isDragging = true
                                tempStartMinutes = startMinutes
                                dragOffsetY = 0f
                            }
                        },
                        onDrag = { _, dragAmount ->
                            if (isDragging) {
                                dragOffsetY += dragAmount.y
                                val deltaMinutes = ((dragOffsetY / hourHeightPx) * 60).roundToInt()
                                val snappedDelta = (deltaMinutes / 15) * 15
                                val newStart = (startMinutes + snappedDelta).coerceIn(0, currentMinutes - MIN_ENTRY_MINUTES)
                                tempStartMinutes = newStart
                            }
                        },
                        onDragEnd = {
                            if (isDragging) {
                                val kotlinDate = LocalDate(selectedDate.year, selectedDate.monthValue, selectedDate.dayOfMonth)
                                val newStartHour = START_HOUR + tempStartMinutes / 60
                                val newStartMinute = tempStartMinutes % 60
                                val newStartInstant = kotlinDate.atTime(LocalTime(newStartHour, newStartMinute)).toInstant(tz)
                                onUpdateTimer(newStartInstant)
                            }
                            isDragging = false
                            dragOffsetY = 0f
                        },
                        onDragCancel = {
                            isDragging = false
                            dragOffsetY = 0f
                        }
                    )
                },
            backgroundColor = Color(0xFFFF9800).copy(alpha = if (isDragging) 1f else alpha),
            elevation = if (isDragging) 6.dp else 4.dp,
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                // Top resize handle indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(resizeHandleHeight)
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pulsing dot indicator
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.White, CircleShape)
                    )
                    Spacer(Modifier.width(4.dp))
                    val displayStartTime = if (isDragging) {
                        formatTime(START_HOUR + currentStartMinutes / 60, currentStartMinutes % 60)
                    } else {
                        formatTime(startTime.hour, startTime.minute)
                    }
                    Text(
                        text = "$displayStartTime - now",
                        style = MaterialTheme.typography.caption,
                        color = Color.White
                    )
                }
                timer.description?.let {
                    Text(it, style = MaterialTheme.typography.caption, color = Color.White, maxLines = 1)
                }
                projectName?.let {
                    Text(it, style = MaterialTheme.typography.caption, color = Color.White.copy(alpha = 0.8f), maxLines = 1)
                }
            }
        }
    }
}

private enum class DragMode { NONE, MOVE, RESIZE_TOP, RESIZE_BOTTOM }

@Composable
private fun TimeEntryBlock(
    entry: LocalTimeEntry,
    selectedDate: java.time.LocalDate,
    hourHeightPx: Float,
    totalHours: Int,
    density: Density,
    onUpdateEntry: (id: Long, newStartTime: Instant, newEndTime: Instant) -> Unit,
    onClick: () -> Unit
) {
    val tz = TimeZone.currentSystemDefault()
    val startTime = entry.startTime.toLocalDateTime(tz).time
    val endTime = entry.endTime?.toLocalDateTime(tz)?.time ?: LocalTime(END_HOUR, 0)

    val startMinutes = (startTime.hour - START_HOUR) * 60 + startTime.minute
    val endMinutes = (endTime.hour - START_HOUR) * 60 + endTime.minute

    if (startMinutes < 0 || endMinutes <= startMinutes) return

    var dragMode by remember { mutableStateOf(DragMode.NONE) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var totalDragDistance by remember { mutableStateOf(0f) }
    var tempStartMinutes by remember { mutableStateOf(startMinutes) }
    var tempEndMinutes by remember { mutableStateOf(endMinutes) }

    val currentStartMinutes = if (dragMode != DragMode.NONE) tempStartMinutes else startMinutes
    val currentEndMinutes = if (dragMode != DragMode.NONE) tempEndMinutes else endMinutes

    val topPx = (currentStartMinutes / 60f) * hourHeightPx
    val durationMinutes = currentEndMinutes - currentStartMinutes
    val displayMinutes = durationMinutes.coerceAtLeast(MIN_ENTRY_MINUTES)
    val heightPx = (displayMinutes / 60f) * hourHeightPx
    val resizeHandleHeight = 8.dp
    val resizeHandleHeightPx = with(density) { resizeHandleHeight.toPx() }

    Box(
        modifier = Modifier
            .offset { IntOffset(4, topPx.roundToInt()) }
            .padding(end = 8.dp)
            .fillMaxWidth()
            .height(with(density) { heightPx.toDp() })
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { heightPx.toDp() })
                .pointerInput(entry.id) {
                    detectTapGestures(onTap = { onClick() })
                }
                .pointerInput(entry.id) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragMode = when {
                                offset.y < resizeHandleHeightPx -> DragMode.RESIZE_TOP
                                offset.y > heightPx - resizeHandleHeightPx -> DragMode.RESIZE_BOTTOM
                                else -> DragMode.MOVE
                            }
                            tempStartMinutes = startMinutes
                            tempEndMinutes = endMinutes
                            dragOffsetY = 0f
                            totalDragDistance = 0f
                        },
                        onDrag = { _, dragAmount ->
                            dragOffsetY += dragAmount.y
                            totalDragDistance += kotlin.math.abs(dragAmount.y)
                            val deltaMinutes = ((dragOffsetY / hourHeightPx) * 60).roundToInt()
                            val snappedDelta = (deltaMinutes / 15) * 15

                            when (dragMode) {
                                DragMode.MOVE -> {
                                    val newStart = (startMinutes + snappedDelta).coerceIn(0, totalHours * 60 - (endMinutes - startMinutes))
                                    val newEnd = newStart + (endMinutes - startMinutes)
                                    tempStartMinutes = newStart
                                    tempEndMinutes = newEnd.coerceAtMost(totalHours * 60)
                                }
                                DragMode.RESIZE_TOP -> {
                                    val newStart = (startMinutes + snappedDelta).coerceIn(0, endMinutes - MIN_ENTRY_MINUTES)
                                    tempStartMinutes = newStart
                                }
                                DragMode.RESIZE_BOTTOM -> {
                                    val newEnd = (endMinutes + snappedDelta).coerceIn(startMinutes + MIN_ENTRY_MINUTES, totalHours * 60)
                                    tempEndMinutes = newEnd
                                }
                                DragMode.NONE -> {}
                            }
                        },
                        onDragEnd = {
                            // Treat as click if minimal drag distance
                            if (totalDragDistance < 5f) {
                                onClick()
                            } else if (dragMode != DragMode.NONE) {
                                val kotlinDate = LocalDate(selectedDate.year, selectedDate.monthValue, selectedDate.dayOfMonth)
                                val newStartHour = START_HOUR + tempStartMinutes / 60
                                val newStartMinute = tempStartMinutes % 60
                                val newEndHour = START_HOUR + tempEndMinutes / 60
                                val newEndMinute = tempEndMinutes % 60
                                val newStartInstant = kotlinDate.atTime(LocalTime(newStartHour, newStartMinute)).toInstant(tz)
                                val newEndInstant = kotlinDate.atTime(LocalTime(newEndHour, newEndMinute)).toInstant(tz)
                                onUpdateEntry(entry.id, newStartInstant, newEndInstant)
                            }
                            dragMode = DragMode.NONE
                            dragOffsetY = 0f
                            totalDragDistance = 0f
                        },
                        onDragCancel = {
                            dragMode = DragMode.NONE
                            dragOffsetY = 0f
                            totalDragDistance = 0f
                        }
                    )
                },
            backgroundColor = if (dragMode != DragMode.NONE) Color(0xFF1976D2) else Color(0xFF2196F3),
            elevation = if (dragMode != DragMode.NONE) 6.dp else 2.dp,
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                // Top resize handle indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(resizeHandleHeight)
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                )

                val displayStartTime = if (dragMode != DragMode.NONE) {
                    formatTime(START_HOUR + currentStartMinutes / 60, currentStartMinutes % 60)
                } else {
                    formatTime(startTime.hour, startTime.minute)
                }
                val displayEndTime = if (dragMode != DragMode.NONE) {
                    formatTime(START_HOUR + currentEndMinutes / 60, currentEndMinutes % 60)
                } else {
                    formatTime(endTime.hour, endTime.minute)
                }

                Text(
                    text = "$displayStartTime - $displayEndTime",
                    style = MaterialTheme.typography.caption,
                    color = Color.White
                )
                entry.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.caption,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 2
                    )
                }
            }
        }

        // Bottom resize handle indicator
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(resizeHandleHeight)
                .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
        )
    }
}

private fun yToTime(y: Float, hourHeightPx: Float): Pair<Int, Int> {
    val totalMinutes = ((y / hourHeightPx) * 60).roundToInt() + (START_HOUR * 60)
    val snappedMinutes = (totalMinutes / 15) * 15 // Snap to 15-minute intervals
    val hour = (snappedMinutes / 60).coerceIn(START_HOUR, END_HOUR)
    val minute = snappedMinutes % 60
    return hour to minute
}

private fun yToInstant(y: Float, hourHeightPx: Float, selectedDate: java.time.LocalDate): Instant {
    val (hour, minute) = yToTime(y, hourHeightPx)
    val kotlinDate = LocalDate(selectedDate.year, selectedDate.monthValue, selectedDate.dayOfMonth)
    return kotlinDate.atTime(LocalTime(hour, minute)).toInstant(TimeZone.currentSystemDefault())
}

fun formatTime(hour: Int, minute: Int): String {
    return String.format("%02d:%02d", hour, minute)
}
