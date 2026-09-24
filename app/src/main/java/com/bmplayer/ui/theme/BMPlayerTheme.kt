package com.bmplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightBackground = Color(0xFFEEF0ED)
private val LightText = Color(0xFF1B1918)
private val LightSecondary = Color(0xFF6B6560)
private val LightAccent = Color(0xFFB9750F)
private val DarkBackground = Color(0xFF17151A)
private val DarkText = Color(0xFFF5F1EA)
private val DarkSecondary = Color(0xFF9C948A)
private val DarkAccent = Color(0xFFE8A33D)

@Composable
fun BMPlayerTheme(darkTheme: Boolean, accent: Color? = null, content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = accent ?: DarkAccent,
            onPrimary = DarkBackground,
            background = DarkBackground,
            onBackground = DarkText,
            surface = DarkBackground,
            onSurface = DarkText,
            onSurfaceVariant = DarkSecondary
        )
    } else {
        lightColorScheme(
            primary = accent ?: LightAccent,
            onPrimary = Color.White,
            background = LightBackground,
            onBackground = LightText,
            surface = LightBackground,
            onSurface = LightText,
            onSurfaceVariant = LightSecondary
        )
    }
    MaterialTheme(colorScheme = colors, typography = BMPlayerTypography, content = content)
}

private val BMPlayerTypography = androidx.compose.material3.Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
)
