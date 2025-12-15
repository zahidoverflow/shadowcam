package com.shadowcam.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = AccentCyan,
    secondary = AccentPurple,
    tertiary = AccentLime,
    background = SurfaceDark,
    surface = SurfaceElevated,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onPrimary = SurfaceDark,
    onSecondary = SurfaceDark
)

@Composable
fun ShadowCamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = ShadowTypography,
        content = content
    )
}
