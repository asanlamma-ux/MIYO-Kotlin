package com.miyu.reader.domain.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class ReaderTheme(
    val id: String,
    val name: String,
    val background: String,
    val text: String,
    val accent: String,
    val secondaryText: String,
    val cardBackground: String,
    val isDark: Boolean,
    val isCustom: Boolean = false,
    val category: ThemeCategory = ThemeCategory.NORMAL,
    val effectPreset: ThemeEffect? = null,
    val assetPackId: String? = null,
    val performanceHint: PerformanceHint = PerformanceHint.STANDARD,
)

@Keep
enum class ThemeCategory { NORMAL, SPECIAL }

@Keep
enum class ThemeEffect { BLOSSOM, COFFEE, COMFORT, MATCHA }

@Keep
enum class PerformanceHint { STANDARD, DECORATIVE }

@Keep
enum class ThemeMode { LIGHT, DARK }

@Keep
@Serializable
data class TypographySettings(
    val fontFamily: String = "default",
    val fontSize: Float = 16f, // 12-28
    val lineHeight: Float = 1.6f, // 1.2-2.0
    val letterSpacing: Float = 0f, // -0.05 to 0.2
    val paragraphSpacing: Float = 0f,
    val textAlign: TextAlign = TextAlign.JUSTIFY,
    val fontWeight: BodyFontWeight = BodyFontWeight.W400,
)

@Keep
enum class TextAlign { LEFT, JUSTIFY }

@Keep
enum class BodyFontWeight(val value: Int) { W400(400), W500(500), W600(600), W700(700) }

@Keep
enum class MarginPreset { NARROW, MEDIUM, WIDE }

@Keep
enum class ReaderColumnLayout { SINGLE, TWO }

@Keep
enum class TapZoneNavMode { SCROLL, CHAPTER }

@Keep
@Serializable
data class ReadingSettings(
    val pageAnimation: PageAnimation = PageAnimation.SLIDE,
    val tapZonesEnabled: Boolean = true,
    val tapScrollPageRatio: Float = 0.9f,
    val tapZoneNavMode: TapZoneNavMode = TapZoneNavMode.SCROLL,
    val volumeButtonPageTurn: Boolean = false,
    val autoScrollSpeed: Float = 0f,
    val immersiveMode: Boolean = false,
    val brightnessOverride: Float? = null,
    val blueLightFilter: Boolean = false,
    val reducedMotion: Boolean = false,
    val marginPreset: MarginPreset = MarginPreset.MEDIUM,
    val contentColumnWidth: Int? = 640,
    val readerColumnLayout: ReaderColumnLayout = ReaderColumnLayout.SINGLE,
)

@Keep
enum class PageAnimation { SLIDE, FADE, CURL }