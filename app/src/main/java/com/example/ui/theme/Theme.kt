package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ElectricVioletLight,
    onPrimary = Color.White,
    primaryContainer = ElectricViolet,
    onPrimaryContainer = Color.White,
    secondary = NeonCyan,
    onSecondary = Color.Black,
    secondaryContainer = ObsidianSurfaceVariant,
    onSecondaryContainer = Color.White,
    tertiary = NeonPink,
    background = ObsidianDark,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = ObsidianBorder
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricViolet,
    onPrimary = Color.White,
    primaryContainer = ElectricVioletLight,
    onPrimaryContainer = Color.Black,
    secondary = NeonCyan,
    onSecondary = Color.Black,
    secondaryContainer = CinemaLightSurfaceVariant,
    onSecondaryContainer = Color.Black,
    tertiary = NeonPink,
    background = CinemaLightBg,
    onBackground = TextPrimaryLight,
    surface = CinemaLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = CinemaLightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = CinemaLightBorder
)

@Composable
fun VideoLibraryTheme(
    themePreference: String = "dark",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
