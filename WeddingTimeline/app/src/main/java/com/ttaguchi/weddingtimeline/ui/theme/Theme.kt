package com.ttaguchi.weddingtimeline.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Light theme color scheme for WeddingTimeline
private val LightColorScheme = lightColorScheme(
    primary = AppPink500,
    onPrimary = AppWhite,
    primaryContainer = AppPink100,
    onPrimaryContainer = AppPink600,

    secondary = AppPurple500,
    onSecondary = AppWhite,
    secondaryContainer = AppPurple100,
    onSecondaryContainer = AppPurple600,

    tertiary = AppPink400,
    onTertiary = AppWhite,

    background = AppWhite,
    onBackground = AppGray900,

    surface = AppBgCard,
    onSurface = AppGray900,
    surfaceVariant = AppGray400,
    onSurfaceVariant = AppGray800,

    error = AppRed500,
    onError = AppWhite,
    errorContainer = AppRed50,
    onErrorContainer = AppRed600,

    outline = AppGray400
)

// Dark theme color scheme for WeddingTimeline
private val DarkColorScheme = darkColorScheme(
    primary = AppPink200,
    onPrimary = AppGray900,
    primaryContainer = AppPink600,
    onPrimaryContainer = AppPink50,

    secondary = AppPurple200,
    onSecondary = AppGray900,
    secondaryContainer = AppPurple600,
    onSecondaryContainer = AppPink50,

    tertiary = AppPink400,
    onTertiary = AppGray900,

    background = AppGray900,
    onBackground = AppWhite,

    surface = AppGray800,
    onSurface = AppWhite,
    surfaceVariant = AppGray600,
    onSurfaceVariant = AppWhite,

    error = AppRed500,
    onError = AppWhite,
    errorContainer = AppRed600,
    onErrorContainer = AppRed50,

    outline = AppGray500
)

@Composable
fun WeddingTimelineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}