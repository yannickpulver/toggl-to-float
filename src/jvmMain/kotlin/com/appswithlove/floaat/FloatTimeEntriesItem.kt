package com.appswithlove.floaat

import kotlinx.serialization.Serializable

@Serializable
data class FloatTimeEntriesItem(
    val date: String,
    val hours: Double,
    val people_id: Int,
    val project_id: Int,
    val billable: Int? = null,
    val created: String? = null,
    val created_by: Int? = null,
    val logged_time_id: String? = null,
    val locked_date: String? = null,
    val modified: String? = null,
    val modified_by: Int? = null,
    val notes: String? = null,
    val phase_id: Int? = null,
    val priority: Int? = null,
    val task_id: Int? = null,
    val task_meta_id: Int? = null,
    val task_name: String? = null,
)