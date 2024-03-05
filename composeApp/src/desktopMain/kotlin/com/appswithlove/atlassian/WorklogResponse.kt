package com.appswithlove.atlassian

import kotlinx.serialization.Serializable

@Serializable
data class WorklogResponse(
    val total: Int
)
