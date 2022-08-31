package com.appswithlove.floaat

import kotlinx.serialization.Serializable

@Serializable
data class FloatProject(
    val project_id: Int,
    val name: String,
    val active: Int? = null,
    val all_pms_schedule: Int? = null,
    val budget_per_phase: Int? = null,
    val budget_total: Double? = null,
    val budget_type: Int? = null,
    val client_id: Int? = null,
    val color: String? = null,
    val created: String? = null,
    val default_hourly_rate: Float? = null,
    val locked_task_list: Int? = null,
    val modified: String? = null,
    val non_billable: Int? = null,
    val notes: String? = null,
    val project_manager: Int? = null,
    val tags: List<String>? = null,
    val tentative: Int? = null
)