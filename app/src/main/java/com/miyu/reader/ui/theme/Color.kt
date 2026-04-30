package com.miyu.reader.ui.theme

import androidx.compose.ui.graphics.Color

// Base palette
val Primary = Color(0xFF6366F1)       // Indigo
val PrimaryVariant = Color(0xFF4F46E5)
val Secondary = Color(0xFF14B8A6)     // Teal
val SurfaceLight = Color(0xFFFAFAFA)
val SurfaceDark = Color(0xFF0F172A)
val BackgroundLight = Color(0xFFF8FAFC)
val BackgroundDark = Color(0xFF020617)
val Error = Color(0xFFEF4444)

// Reader themes — data class shared by the 16 built-in themes
data class ReaderThemeColors(
    val id: String,
    val name: String,
    val background: Color,
    val text: Color,
    val accent: Color,
    val secondaryText: Color,
    val cardBackground: Color,
    val isDark: Boolean,
    val isSpecial: Boolean = false,
)

/**
 * All 16 built-in reader themes, ported 1-to-1 from the React Native reference
 * (EbookReaderOld → types/theme.ts → defaultThemes[]).
 */
object ReaderColors {

    val SepiaClassic = ReaderThemeColors(
        id = "sepia-classic",
        name = "Sepia Classic",
        background = Color(0xFFF4EFE8),
        text = Color(0xFF3A3228),
        accent = Color(0xFF8B6F47),
        secondaryText = Color(0xFF6B5D4D),
        cardBackground = Color(0xFFFFFBF5),
        isDark = false,
    )

    val NightMode = ReaderThemeColors(
        id = "night-mode",
        name = "Night Mode",
        background = Color(0xFF000000),
        text = Color(0xFFECE7E1),
        accent = Color(0xFF8AB4F8),
        secondaryText = Color(0xFFA3A3A3),
        cardBackground = Color(0xFF090909),
        isDark = true,
    )

    val ForestGreen = ReaderThemeColors(
        id = "forest-green",
        name = "Forest Green",
        background = Color(0xFFE8F0E8),
        text = Color(0xFF2D3E2D),
        accent = Color(0xFF4A7C59),
        secondaryText = Color(0xFF4D6B4D),
        cardBackground = Color(0xFFF0F5F0),
        isDark = false,
    )

    val LavenderDream = ReaderThemeColors(
        id = "lavender-dream",
        name = "Lavender Dream",
        background = Color(0xFFF0EBF4),
        text = Color(0xFF3E3548),
        accent = Color(0xFF9B7EBD),
        secondaryText = Color(0xFF6B5E78),
        cardBackground = Color(0xFFF8F4FB),
        isDark = false,
    )

    val MidnightOled = ReaderThemeColors(
        id = "midnight-oled",
        name = "Midnight OLED",
        background = Color(0xFF000000),
        text = Color(0xFFCCCCCC),
        accent = Color(0xFF00D9FF),
        secondaryText = Color(0xFF888888),
        cardBackground = Color(0xFF0A0A0A),
        isDark = true,
    )

    val ParchmentComfort = ReaderThemeColors(
        id = "parchment",
        name = "Parchment Comfort",
        background = Color(0xFFFFFBF5),
        text = Color(0xFF2C2416),
        accent = Color(0xFF9D7651),
        secondaryText = Color(0xFF5C4D3A),
        cardBackground = Color(0xFFFFF8F0),
        isDark = false,
        isSpecial = true,
    )

    val Monochrome = ReaderThemeColors(
        id = "monochrome",
        name = "Monochrome",
        background = Color(0xFFF5F5F5),
        text = Color(0xFF1A1A1A),
        accent = Color(0xFF666666),
        secondaryText = Color(0xFF4A4A4A),
        cardBackground = Color(0xFFFFFFFF),
        isDark = false,
    )

    val OceanBlue = ReaderThemeColors(
        id = "ocean-blue",
        name = "Ocean Blue",
        background = Color(0xFFE8F1F5),
        text = Color(0xFF1E3A4C),
        accent = Color(0xFF2D7D9A),
        secondaryText = Color(0xFF4A6B7C),
        cardBackground = Color(0xFFF0F7FA),
        isDark = false,
    )

    val WarmSunset = ReaderThemeColors(
        id = "warm-sunset",
        name = "Warm Sunset",
        background = Color(0xFFFFF5ED),
        text = Color(0xFF4A2C1A),
        accent = Color(0xFFD97706),
        secondaryText = Color(0xFF7A5C4A),
        cardBackground = Color(0xFFFFFAF5),
        isDark = false,
    )

    val NordicNight = ReaderThemeColors(
        id = "nordic-night",
        name = "Nordic Night",
        background = Color(0xFF1E2430),
        text = Color(0xFFD8DEE9),
        accent = Color(0xFF88C0D0),
        secondaryText = Color(0xFF8FBCBB),
        cardBackground = Color(0xFF2E3440),
        isDark = true,
    )

    val PeachBlossom = ReaderThemeColors(
        id = "rose-garden",
        name = "Peach Blossom",
        background = Color(0xFFFDF2F4),
        text = Color(0xFF4A2832),
        accent = Color(0xFFD4687A),
        secondaryText = Color(0xFF7A4A58),
        cardBackground = Color(0xFFFFF8F9),
        isDark = false,
        isSpecial = true,
    )

    val DarkCoffee = ReaderThemeColors(
        id = "dark-coffee",
        name = "Dark Coffee",
        background = Color(0xFF1C1816),
        text = Color(0xFFE5DDD5),
        accent = Color(0xFFC4A77D),
        secondaryText = Color(0xFFA89888),
        cardBackground = Color(0xFF2A2420),
        isDark = true,
        isSpecial = true,
    )

    val MatchaPaper = ReaderThemeColors(
        id = "matcha-paper",
        name = "Matcha Paper",
        background = Color(0xFFEEF5E8),
        text = Color(0xFF263628),
        accent = Color(0xFF5E8B4A),
        secondaryText = Color(0xFF58705A),
        cardBackground = Color(0xFFF8FCF5),
        isDark = false,
        isSpecial = true,
    )

    val InkStone = ReaderThemeColors(
        id = "ink-stone",
        name = "Ink Stone",
        background = Color(0xFF171A1F),
        text = Color(0xFFE7E3DA),
        accent = Color(0xFFC28C4C),
        secondaryText = Color(0xFF9A958B),
        cardBackground = Color(0xFF20242A),
        isDark = true,
    )

    val BlueprintDay = ReaderThemeColors(
        id = "blueprint-day",
        name = "Blueprint Day",
        background = Color(0xFFEEF4FA),
        text = Color(0xFF203246),
        accent = Color(0xFF3A78B8),
        secondaryText = Color(0xFF5D7387),
        cardBackground = Color(0xFFF8FBFE),
        isDark = false,
    )

    val EmberNight = ReaderThemeColors(
        id = "ember-night",
        name = "Ember Night",
        background = Color(0xFF151313),
        text = Color(0xFFF0E5DC),
        accent = Color(0xFFE07A3F),
        secondaryText = Color(0xFFB1A197),
        cardBackground = Color(0xFF201B1A),
        isDark = true,
    )

    /** Ordered list matching the React Native reference order. */
    val allThemes: List<ReaderThemeColors> = listOf(
        SepiaClassic,
        NightMode,
        ForestGreen,
        LavenderDream,
        MidnightOled,
        ParchmentComfort,
        Monochrome,
        OceanBlue,
        WarmSunset,
        NordicNight,
        PeachBlossom,
        DarkCoffee,
        MatchaPaper,
        InkStone,
        BlueprintDay,
        EmberNight,
    )

    fun findById(id: String): ReaderThemeColors =
        allThemes.firstOrNull { it.id == id } ?: SepiaClassic
}
