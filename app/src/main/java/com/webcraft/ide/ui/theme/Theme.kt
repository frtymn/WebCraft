package com.webcraft.ide.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF7AA2FF),
    onPrimary = Color(0xFF0B0D10),
    primaryContainer = Color(0xFF2B3F6B),
    onPrimaryContainer = Color(0xFFD6E2FF),
    secondary = Color(0xFF4ECDC4),
    onSecondary = Color(0xFF0B0D10),
    secondaryContainer = Color(0xFF1B4B47),
    onSecondaryContainer = Color(0xFFBFF0EB),
    tertiary = Color(0xFFFFD166),
    onTertiary = Color(0xFF0B0D10),
    tertiaryContainer = Color(0xFF5C4A14),
    onTertiaryContainer = Color(0xFFFFE9A8),
    error = Color(0xFFFF7B54),
    onError = Color(0xFF0B0D10),
    errorContainer = Color(0xFF6B2410),
    onErrorContainer = Color(0xFFFFD6C2),
    background = Color(0xFF0F1114),
    onBackground = Color(0xFFE8E8E8),
    surface = Color(0xFF14181D),
    onSurface = Color(0xFFE8E8E8),
    surfaceVariant = Color(0xFF1A1E24),
    onSurfaceVariant = Color(0xFFB9BFC9),
    outline = Color(0xFF3A4048),
    outlineVariant = Color(0xFF2B3138),
    surfaceContainer = Color(0xFF171B21),
    surfaceContainerLow = Color(0xFF13161B),
    surfaceContainerHigh = Color(0xFF1B2027),
    surfaceContainerHighest = Color(0xFF222831),
    inverseSurface = Color(0xFFE8E8E8),
    inverseOnSurface = Color(0xFF14181D),
    inversePrimary = Color(0xFF2B3F6B),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF2B3F6B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E2FF),
    onPrimaryContainer = Color(0xFF0F1B3D),
    secondary = Color(0xFF1B6E68),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBFF0EB),
    onSecondaryContainer = Color(0xFF0A332F),
    tertiary = Color(0xFF6B5414),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE9A8),
    onTertiaryContainer = Color(0xFF221A03),
    error = Color(0xFF93290D),
    onError = Color.White,
    errorContainer = Color(0xFFFFD6C2),
    onErrorContainer = Color(0xFF380B00),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFEEEFF2),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),
)

@Composable
fun WebCraftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkScheme else LightScheme
    MaterialTheme(
        colorScheme = colors,
        typography = WebCraftTypography,
        content = content
    )
}
