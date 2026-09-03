package com.freegymtrainer.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = OrangeContainerLight,
    onPrimaryContainer = OrangeOnContainerLight,
    secondary = DeepNavy,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = NavyContainerLight,
    onSecondaryContainer = NavyOnContainerLight,
    tertiary = SuccessGreen,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = GreenContainerLight,
    onTertiaryContainer = GreenOnContainerLight,
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
    inversePrimary = OrangePrimaryDark,
    surfaceTint = OrangePrimary,
    error = ErrorRed,
    errorContainer = ErrorContainerLight,
    onErrorContainer = ErrorOnContainerLight
)

private val DarkColors = darkColorScheme(
    primary = OrangePrimaryDark,
    onPrimary = DeepNavy,
    primaryContainer = OrangeContainerDark,
    onPrimaryContainer = OrangeOnContainerDark,
    secondary = OrangePrimaryDark,
    onSecondary = DeepNavy,
    secondaryContainer = NavyContainerDark,
    onSecondaryContainer = NavyOnContainerDark,
    tertiary = SuccessGreen,
    onTertiary = DeepNavy,
    tertiaryContainer = GreenContainerDark,
    onTertiaryContainer = GreenOnContainerDark,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary = OrangePrimary,
    surfaceTint = OrangePrimaryDark,
    error = ErrorRed,
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorOnContainerDark
)

/** Lets a composable ask "is dark active right now" without re-deriving it from themeMode/system
 * state itself - used by [orangeSoftContainer] and any future non-ColorScheme brand token. */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

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

    CompositionLocalProvider(LocalIsDarkTheme provides useDark) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}

/** A fainter, solid orange tone for subtle fills (a row/tile that should feel on-brand without
 * drawing the eye like a badge) - see [OrangeSoftLight]/[OrangeSoftDark]. */
@Composable
fun orangeSoftContainer(): Color = if (LocalIsDarkTheme.current) OrangeSoftDark else OrangeSoftLight
