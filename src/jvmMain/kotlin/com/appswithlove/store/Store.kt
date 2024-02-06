package com.appswithlove.store

data class Store(
    val togglKey: String?,
    val floatKey: String?,
    val floatClientId: Int?,
    val atlassianEmail: String?,
    val atlassianApiKey: String?,
    val atlassianHost: String?,
    val atlassianPrefix: String?
) {
    val shouldLoadPeople = (floatClientId == null || floatClientId == -1) && !floatKey.isNullOrEmpty()
}