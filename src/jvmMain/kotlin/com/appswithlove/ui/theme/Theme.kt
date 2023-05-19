package com.appswithlove.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color.Companion.White

private val DarkColorPalette = darkColors(
    primary = White,
    primaryVariant = White,
    secondary = White,
    surface = DarkGray,
    onPrimary = Black,
    onSecondary = Black
)

private val LightColorPalette = lightColors(
    primary = Black,
    primaryVariant = Black,
    secondary = Black,
    surface = LightGray,
    onPrimary = White,
    onSecondary = White

    /* Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    */
)

@Composable
fun FloaterTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
     val colors = if (darkTheme) {
         DarkColorPalette
     } else {
         LightColorPalette
     }

    MaterialTheme(
        colors = LightColorPalette,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
