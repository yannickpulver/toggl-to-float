package com.appswithlove.toggl

import kotlinx.serialization.Serializable
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Serializable
    data class TimeEntryCreate(
        val created_with: String = "T2F",
        val duration: Int = -1,
        val project_id: Int,
        val start: String = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        val tags: List<String>,  // add your own tags here
        val workspace_id: Int
    )