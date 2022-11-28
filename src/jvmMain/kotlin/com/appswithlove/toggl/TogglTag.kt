package com.appswithlove.toggl

import kotlinx.serialization.Serializable

@Serializable
data class TogglTagCreate(
    val workspace_id: Int,
    val name: String
)

@Serializable
data class TogglTag(
    val at: String,
    val id: Int,
    val name: String,
    val workspace_id: Int
)