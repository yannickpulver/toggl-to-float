package com.appswithlove.toggl

import kotlinx.serialization.Serializable

@Serializable
data class TogglWorkspaceItem(
    val admin: Boolean,
    val api_token: String,
    val at: String,
    val business_ws: Boolean,
    val csv_upload: String? = null,
    val default_currency: String,
    val default_hourly_rate: String? = null,
    val ical_enabled: Boolean,
    val ical_url: String,
    val id: Int,
    val logo_url: String,
    val name: String,
)