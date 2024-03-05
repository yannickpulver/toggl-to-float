package com.appswithlove.floaat

import kotlinx.serialization.Serializable

@Serializable
data class FloatPeopleItem(
    val name: String,
    val people_id: Int,
    val active: Int? = null,
    val auto_email: Int? = null,
    val avatar_file: String? = null,
    val contractor: Int? = null,
    val created: String? = null,
    val default_hourly_rate: String? = null,
    val department: FloatDepartment? = null,
    val email: String? = null,
    val employee_type: Int? = null,
    val end_date: String? = null,
    val job_title: String? = null,
    val modified: String? = null,
    val non_work_days: Array<Double>? = null,
    val notes: String? = null,
    val people_type_id: Int? = null,
    val start_date: String? = null,
    val tags: List<FloatPeopleTag>? = null,
    val work_days_hours: Array<Double>? = null,
)

@Serializable
data class FloatDepartment(
    val department_id: Int? = null,
    val name: String
)

@Serializable
data class FloatPeopleTag(
    val name: String
)