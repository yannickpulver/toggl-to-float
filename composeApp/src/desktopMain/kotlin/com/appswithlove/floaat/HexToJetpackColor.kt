package com.appswithlove.floaat

import androidx.compose.ui.graphics.Color


fun hex2Rgb(colorStr: String?): Color? {
    colorStr ?: return null
    return Color(Integer.valueOf(colorStr, 16))
}