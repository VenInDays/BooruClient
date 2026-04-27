package com.booru.client.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = ClayColors.Primary,
    onPrimary = ClayColors.TextOnPrimary,
    primaryContainer = ClayColors.SurfaceElevated,
    onPrimaryContainer = ClayColors.PrimaryDark,
    secondary = ClayColors.Accent,
    onSecondary = ClayColors.TextPrimary,
    secondaryContainer = ClayColors.AccentLight,
    onSecondaryContainer = ClayColors.AccentDark,
    tertiary = ClayColors.PrimaryLight,
    background = ClayColors.Background,
    onBackground = ClayColors.TextPrimary,
    surface = ClayColors.Surface,
    onSurface = ClayColors.TextPrimary,
    surfaceVariant = ClayColors.SurfaceElevated,
    onSurfaceVariant = ClayColors.TextSecondary,
    error = ClayColors.Error,
    onError = Color.White,
    outline = ClayColors.TextTertiary,
    outlineVariant = Color(0xFFD4CFC7)
)

/**
 * Claymorphism theme — warm, tactile, soft UI aesthetic.
 * Only light theme; dark mode follows the same warm tone palette.
 */
@Composable
fun BooruTheme(content: @Composable () -> Unit) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ClayTypography,
        content = content
    )
}
