package com.miyu.reader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.miyu.reader.domain.model.ThemeMode

@Immutable
data class MIYUColors(
    val primary: Color = Primary,
    val background: Color = BackgroundLight,
    val surface: Color = SurfaceLight,
    val onBackground: Color = Color(0xFF3A3228),
    val onSurface: Color = Color(0xFF3A3228),
    val secondaryText: Color = Color(0xFF6B5D4D),
    val cardBackground: Color = Color(0xFFFFFBF5),
    val accent: Color = Primary,
    val isDark: Boolean = false,
)

val LocalMIYUColors = staticCompositionLocalOf { MIYUColors() }

/**
 * The currently-selected reader theme ID, provided through CompositionLocal
 * so any composable can read it without drilling props.
 */
val LocalReaderThemeId = staticCompositionLocalOf { DefaultReaderThemeId }

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    secondary = Secondary,
    onSecondary = Color.White,
    background = BackgroundLight,
    onBackground = Color(0xFF3A3228),
    surface = SurfaceLight,
    onSurface = Color(0xFF3A3228),
    surfaceVariant = Color(0xFFE9E1D7),
    onSurfaceVariant = Color(0xFF6B5D4D),
    outline = Color(0xFFD8CEC2),
    error = Error,
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    secondary = Secondary,
    background = BackgroundDark,
    surface = SurfaceDark,
    error = Error,
)

@Composable
fun MIYUTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    readerThemeId: String = DefaultReaderThemeId,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val miyuColors = if (darkTheme) {
        MIYUColors(
            background = BackgroundDark,
            surface = SurfaceDark,
            onBackground = Color(0xFFEDE5DA),
            onSurface = Color(0xFFEDE5DA),
            secondaryText = Color(0xFFB8A897),
            cardBackground = Color(0xFF2A2420),
            accent = Primary,
            isDark = true,
        )
    } else {
        MIYUColors()
    }

    CompositionLocalProvider(
        LocalMIYUColors provides miyuColors,
        LocalReaderThemeId provides readerThemeId,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            content = content,
        )
    }
}
