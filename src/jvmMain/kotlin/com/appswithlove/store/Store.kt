package com.appswithlove.store

data class Store(
    val togglKey: String?,
    val floatKey: String?,
    val floatClientId: Int?
) {
    val shouldLoadPeople = (floatClientId == null || floatClientId == -1) && !floatKey.isNullOrEmpty()
}