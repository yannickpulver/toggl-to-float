package com.appswithlove.floaat

import kotlinx.serialization.Serializable

@Serializable
data class FloatTask(
    val billable: Int,
    val created: String,
    val created_by: Int? = null,
    val end_date: String,
    val hours: Double,
    val modified: String,
    val modified_by: Int,
    val name: String,
    val notes: String,
    val parent_task_id: Int? = null,
    val people_id: Int? = null,
    val people_ids: List<Int>? = null,
    val phase_id: Int,
    val project_id: Int,
    val repeat_end_date: String? = null,
    val repeat_state: Int,
    val root_task_id: Int? = null,
    val start_date: String,
    val start_time: String? = null,
    val status: Int,
    val task_id: Int,
    val task_meta_id: Int
) {
    companion object {
        val Preview = FloatTask(
            billable = 1,
            created = "2021-01-01",
            end_date = "2021-01-01",
            hours = 1.0,
            modified = "2021-01-01",
            modified_by = 1,
            name = "Task 1",
            notes = "Notes 1",
            phase_id = 1,
            project_id = 1,
            repeat_state = 1,
            start_date = "2021-01-01",
            status = 1,
            task_id = 1,
            task_meta_id = 1
        )
    }
}