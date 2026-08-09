package com.chenyang.cywms.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Navy900 = Color(0xFF0B1220)
val Navy800 = Color(0xFF121A2B)
val Navy700 = Color(0xFF1A2438)
val Slate200 = Color(0xFFE8EEF7)
val Slate400 = Color(0xFF9AA8BF)
val Amber500 = Color(0xFFF5A623)
val Amber200 = Color(0xFFFFD78A)
val GlassFill = Color(0x66121A2B)
val GlassStroke = Color(0x55FFFFFF)
val SuccessGreen = Color(0xFF3DDC97)
val DangerRed = Color(0xFFFF6B6B)

private val ColorScheme = darkColorScheme(
    primary = Amber500,
    onPrimary = Navy900,
    secondary = Amber200,
    background = Navy900,
    surface = Navy800,
    onBackground = Slate200,
    onSurface = Slate200,
    error = DangerRed
)

@Composable
fun CywmsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = MaterialTheme.typography.copy(
            displayLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                letterSpacing = (-0.5).sp,
                color = Slate200
            ),
            headlineMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                color = Slate200
            ),
            titleLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = Slate200
            ),
            bodyLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = Slate200
            ),
            bodyMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = Slate400
            ),
            labelLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        ),
        content = content
    )
}
