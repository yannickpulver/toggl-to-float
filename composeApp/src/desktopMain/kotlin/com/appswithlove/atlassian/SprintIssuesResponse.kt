package com.appswithlove.atlassian

import kotlinx.serialization.Serializable

@Serializable
data class SprintIssuesResponse(
    val issues: List<JiraIssue>
)

@Serializable
data class JiraIssue(
    val key: String,
    val fields: JiraFields
)

@Serializable
data class JiraFields(
    val summary: String,
    val status: JiraStatus,
    val assignee: JiraUser? = null,
    val issuetype: JiraIssueType? = null
)

@Serializable
data class JiraStatus(
    val name: String
)

@Serializable
data class JiraUser(
    val emailAddress: String,
    val displayName: String
)

@Serializable
data class JiraIssueType(
    val name: String
)