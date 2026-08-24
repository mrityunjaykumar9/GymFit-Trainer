package com.freegymtrainer.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = DeepNavy,
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface,
    error = ErrorRed
)

private val DarkColors = darkColorScheme(
    primary = OrangePrimaryDark,
    onPrimary = DeepNavy,
    secondary = OrangePrimaryDark,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    error = ErrorRed
)

/**
 * @param themeMode "system" | "light" | "dark" - drives every screen's colors from one place.
 */
@Composable
fun FreeGymTrainerTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }
    val colorScheme = if (useDark) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
