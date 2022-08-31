package com.appswithlove.floaat

import kotlinx.serialization.Serializable

@Serializable
data class FloatPhaseItem(
    val project_id: Int,
    val name: String,
    val start_date: String,
    val end_date: String,
    val active: Int? = null,
    val phase_id: Int? = null,
    val budget_total: String? = null,
    val color: String? = null,
    val created: String? = null,
    val default_hourly_rate: String? = null,
    val modified: String? = null,
    val non_billable: Int? = null,
    val notes: String? = null,
    val tentative: Int? = null
)