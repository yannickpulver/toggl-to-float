package com.appswithlove.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp
import com.appswithlove.ui.theme.Type.Avenir

object Type {
    val Avenir = FontFamily(
        Font(resource = "font/AvenirLTStd-Black.otf", FontWeight.Black),
        Font(resource = "font/AvenirLTStd-Book.otf", FontWeight.Light),
        Font(resource = "font/AvenirLTStd-Roman.otf", FontWeight.Normal),
    )

}

// Set of Material typography styles to start with
val Typography = Typography(
    body1 = TextStyle(
     //   fontFamily = Avenir,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
    caption = TextStyle(
      //  fontFamily = Avenir,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
    ),
    h4 = TextStyle(
      //  fontFamily = Avenir,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
    ),
    button = TextStyle(
       // fontFamily = Avenir,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
    ),
    /* Other default text styles to override
    button = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp
    ),
    caption = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    )
    */
)