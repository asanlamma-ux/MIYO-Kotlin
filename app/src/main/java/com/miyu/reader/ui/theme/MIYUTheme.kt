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

@Composable
fun MIYUTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    readerThemeId: String = DefaultReaderThemeId,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val readerTheme = ReaderColors.findById(readerThemeId)
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val shellTheme = when {
        themeMode == ThemeMode.LIGHT && readerTheme.isDark -> ReaderColors.SepiaClassic
        themeMode == ThemeMode.DARK && !readerTheme.isDark -> ReaderColors.NightMode
        themeMode == ThemeMode.SYSTEM && darkTheme != readerTheme.isDark -> {
            if (darkTheme) ReaderColors.NightMode else ReaderColors.SepiaClassic
        }
        else -> readerTheme
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = shellTheme.accent,
            onPrimary = Color.White,
            secondary = Secondary,
            background = shellTheme.background,
            surface = shellTheme.cardBackground,
            onSurface = shellTheme.text,
            error = Error,
        )
    } else {
        lightColorScheme(
            primary = shellTheme.accent,
            onPrimary = Color.White,
            secondary = Secondary,
            onSecondary = Color.White,
            background = shellTheme.background,
            onBackground = shellTheme.text,
            surface = shellTheme.cardBackground,
            onSurface = shellTheme.text,
            surfaceVariant = shellTheme.cardBackground,
            onSurfaceVariant = shellTheme.secondaryText,
            outline = shellTheme.secondaryText.copy(alpha = 0.35f),
            error = Error,
        )
    }

    val miyuColors = MIYUColors(
        primary = shellTheme.accent,
        background = shellTheme.background,
        surface = shellTheme.background,
        onBackground = shellTheme.text,
        onSurface = shellTheme.text,
        secondaryText = shellTheme.secondaryText,
        cardBackground = shellTheme.cardBackground,
        accent = shellTheme.accent,
        isDark = darkTheme,
    )

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
