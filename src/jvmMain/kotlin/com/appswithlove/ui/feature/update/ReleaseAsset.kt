package com.appswithlove.ui.feature.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReleaseAsset(
    @SerialName("browser_download_url") val downloadUrl: String,
    @SerialName("name") val name: String,
)