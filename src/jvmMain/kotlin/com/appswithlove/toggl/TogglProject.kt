package com.appswithlove.toggl

import kotlinx.serialization.Serializable
import java.awt.Color
import kotlin.random.Random

@Serializable
data class TogglProject(
    val project_id: Int,
    val active: Boolean = true,
    val auto_estimates: Boolean = false,
    val currency: String = "CHF",
    val estimated_hours: Int = 1,
    val is_private: Boolean = false,
    val name: String,
    val color: String? = null,
)

@Serializable
data class TogglProjectCreate(
    val active: Boolean = true,
    val auto_estimates: Boolean = false,
    val currency: String = "CHF",
    val estimated_hours: Int = 1,
    val is_private: Boolean = false,
    val name: String,
    val color: String? = null,
)

@Serializable
data class TogglProjectUpdate(
    val color: String? = null,
)

fun randomColor() : String {
    val r: Int = Random.nextInt(255)
    val g: Int = Random.nextInt(255)
    val b: Int = Random.nextInt(255)
    val color = Color(r, g, b)
    var hex = Integer.toHexString(color.rgb and 0xffffff)
    while (hex.length < 6) {
        hex = "0$hex"
    }
    hex = "#$hex"
    return hex
}