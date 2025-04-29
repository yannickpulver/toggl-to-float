package com.appswithlove.atlassian

import kotlinx.serialization.Serializable

@Serializable
data class Permission(
    val id: String,
    val key: String,
    val name: String,
    val type: String,
    val description: String,
    val havePermission: Boolean
)

@Serializable
data class Permissions(
    val WORK_ON_ISSUES: Permission
)

@Serializable
data class PermissionResponse(
    val permissions: Permissions
)