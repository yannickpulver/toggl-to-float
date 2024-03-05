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
    val only_admins_may_create_projects: Boolean,
    val only_admins_may_create_tags: Boolean,
    val only_admins_see_billable_rates: Boolean,
    val only_admins_see_team_dashboard: Boolean,
    val organization_id: Int,
    val premium: Boolean,
    val profile: Int,
    val projects_billable_by_default: Boolean,
    val rate_last_updated: String? = null,
    val reports_collapse: Boolean,
    val rounding: Int,
    val rounding_minutes: Int,
    val server_deleted_at: String? = null,
    val subscription: String? = null,
    val suspended_at: String? = null
)