package com.appswithlove.ui.feature.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LatestRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val url: String,
    @SerialName("assets") val assets: List<ReleaseAsset>
) {
    val name get() = tagName.drop(1)

    val downloadUrl: String
        get() {
            val suffix = if (isMacOs()) ".dmg" else ".exe"
            return assets.find { it.name.endsWith(suffix) }?.downloadUrl ?: url
        }

    private fun isMacOs(): Boolean {
        val osName = System.getProperty("os.name")
        return osName.contains(other = "mac", ignoreCase = true)
    }
}
