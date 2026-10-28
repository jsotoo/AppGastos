package com.minimalgastos.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F6F5C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5EEE5),
    onPrimaryContainer = Color(0xFF083D31),
    background = Color(0xFFF7F9F8),
    onBackground = Color(0xFF191C1B),
    surface = Color.White,
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFE9EEEC),
    onSurfaceVariant = Color(0xFF4D5654),
    outline = Color(0xFFB8C2BF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD6BC),
    onPrimary = Color(0xFF00382B),
    primaryContainer = Color(0xFF14463A),
    onPrimaryContainer = Color(0xFFCDEFE4),
    background = Color(0xFF0F1312),
    onBackground = Color(0xFFE1E5E3),
    surface = Color(0xFF171B1A),
    onSurface = Color(0xFFE1E5E3),
    surfaceVariant = Color(0xFF252B2A),
    onSurfaceVariant = Color(0xFFB4BDBA),
    outline = Color(0xFF47504D)
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 42.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
)

@Composable
fun GastosTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}

data class LevelColors(val container: Color, val content: Color)

/** Colores del panel según el nivel de presupuesto: 0 normal, 80 rojo suave, 100 rojo intenso. */
@Composable
fun budgetColors(level: Int): LevelColors {
    val dark = isSystemInDarkTheme()
    val scheme = MaterialTheme.colorScheme
    return when (level) {
        100 -> if (dark) LevelColors(Color(0xFFB3261E), Color.White) else LevelColors(Color(0xFFC62828), Color.White)
        80 -> if (dark) LevelColors(Color(0xFF5C1F1B), Color(0xFFFFDAD6)) else LevelColors(Color(0xFFFFDAD6), Color(0xFF7A1410))
        else -> LevelColors(scheme.primaryContainer, scheme.onPrimaryContainer)
    }
}
