package com.appswithlove.store

import java.util.prefs.Preferences

class DataStore {

    private val field_togglApiKey = "togglApiKey"
    private val field_floatApiKey = "floatApiKey"
    private val field_floatClientId = "floatClientId"
    private val field_amountTimeEntries = "amountTimeEntries"
    private val field_atlassianEmail = "atlassianEmail"
    private val field_atlassianApiKey = "atlassianApiKey"
    private val field_atlassianUrl = "atlassianUrl"
    private val field_atlassianPrefix = "atlassianPrefix"
    private val preferences = Preferences.userNodeForPackage(javaClass)

    val getStore: Store
        get() {
            val togglApiKey = preferences[field_togglApiKey, ""]
            val floatApiKey = preferences[field_floatApiKey, ""]
            val floatClientId = preferences.getInt(field_floatClientId, -1)
            val atlassianEmail = preferences[field_atlassianEmail, ""]
            val atlassianApiKey = preferences[field_atlassianApiKey, ""]
            val atlassianUrl = preferences[field_atlassianUrl, ""]
            val atlassianPrefix = preferences[field_atlassianPrefix, ""]
            return Store(togglApiKey, floatApiKey, floatClientId, atlassianEmail, atlassianApiKey, atlassianUrl, atlassianPrefix)
        }

    fun setTogglApiKey(apiKey: String?) {
        preferences.put(field_togglApiKey, apiKey.orEmpty())
    }

    fun setFloatApiKey(apiKey: String?) {
        preferences.put(field_floatApiKey, apiKey.orEmpty())
    }

    fun setFloatClientId(clientId: Int?) {
        preferences.putInt(field_floatClientId, clientId ?: -1)
    }

    fun setAtlassianEmail(email: String?) {
        preferences.put(field_atlassianEmail, email.orEmpty())
    }

    fun setAtlassianApiKey(apiKey: String?) {
        preferences.put(field_atlassianApiKey, apiKey.orEmpty())
    }

    fun setAtlassianHost(url: String?) {
        preferences.put(field_atlassianUrl, url.orEmpty())
    }

    fun setAtlassianPrefix(prefix: String?) {
        preferences.put(field_atlassianPrefix, prefix.orEmpty())
    }

    fun addAndGetTimeEntryCount(entriesUploaded: Int): Int {
        val count = preferences.getInt(field_amountTimeEntries, 0)
        val newCount = count + entriesUploaded
        preferences.putInt(field_amountTimeEntries, newCount)
        return newCount
    }

    fun clear() {
        preferences.clear()
    }
}