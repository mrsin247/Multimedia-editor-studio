package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = StudioCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF00363D),
    onPrimaryContainer = StudioCyan,
    secondary = StudioAmber,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3E2B00),
    onSecondaryContainer = StudioAmber,
    tertiary = StudioMagenta,
    onTertiary = Color.White,
    background = StudioBackground,
    onBackground = StudioTextPrimary,
    surface = StudioSurface,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = StudioTextSecondary,
    outline = StudioBorder,
    outlineVariant = Color(0xFF21262D)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // Media production suites thrive in a focused dark studio environment
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
