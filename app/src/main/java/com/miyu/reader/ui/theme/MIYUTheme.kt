package com.miyu.reader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
<<<<<<< HEAD
=======
import androidx.compose.ui.graphics.Color
>>>>>>> debug
import com.miyu.reader.domain.model.ThemeMode

@Immutable
data class MIYUColors(
<<<<<<< HEAD
    val primary: androidx.compose.ui.graphics.Color = Primary,
    val background: androidx.compose.ui.graphics.Color = BackgroundLight,
    val surface: androidx.compose.ui.graphics.Color = SurfaceLight,
    val onBackground: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF1E293B),
    val onSurface: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF1E293B),
    val secondaryText: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF64748B),
    val cardBackground: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFF1F5F9),
=======
    val primary: Color = Primary,
    val background: Color = BackgroundLight,
    val surface: Color = SurfaceLight,
    val onBackground: Color = Color(0xFF1E293B),
    val onSurface: Color = Color(0xFF1E293B),
    val secondaryText: Color = Color(0xFF64748B),
    val cardBackground: Color = Color(0xFFF1F5F9),
    val accent: Color = Primary,
>>>>>>> debug
    val isDark: Boolean = false,
)

val LocalMIYUColors = staticCompositionLocalOf { MIYUColors() }

<<<<<<< HEAD
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
=======
/**
 * The currently-selected reader theme ID, provided through CompositionLocal
 * so any composable can read it without drilling props.
 */
val LocalReaderThemeId = staticCompositionLocalOf { "sepia-classic" }

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
>>>>>>> debug
    secondary = Secondary,
    background = BackgroundLight,
    surface = SurfaceLight,
    error = Error,
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
<<<<<<< HEAD
    onPrimary = androidx.compose.ui.graphics.Color.White,
=======
    onPrimary = Color.White,
>>>>>>> debug
    secondary = Secondary,
    background = BackgroundDark,
    surface = SurfaceDark,
    error = Error,
)

@Composable
fun MIYUTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
<<<<<<< HEAD
=======
    readerThemeId: String = "sepia-classic",
>>>>>>> debug
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
<<<<<<< HEAD
            onBackground = androidx.compose.ui.graphics.Color(0xFFE2E8F0),
            onSurface = androidx.compose.ui.graphics.Color(0xFFE2E8F0),
            secondaryText = androidx.compose.ui.graphics.Color(0xFF94A3B8),
            cardBackground = androidx.compose.ui.graphics.Color(0xFF1E293B),
=======
            onBackground = Color(0xFFE2E8F0),
            onSurface = Color(0xFFE2E8F0),
            secondaryText = Color(0xFF94A3B8),
            cardBackground = Color(0xFF1E293B),
            accent = Primary,
>>>>>>> debug
            isDark = true,
        )
    } else {
        MIYUColors()
    }

<<<<<<< HEAD
    CompositionLocalProvider(LocalMIYUColors provides miyuColors) {
=======
    CompositionLocalProvider(
        LocalMIYUColors provides miyuColors,
        LocalReaderThemeId provides readerThemeId,
    ) {
>>>>>>> debug
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            content = content,
        )
    }
}