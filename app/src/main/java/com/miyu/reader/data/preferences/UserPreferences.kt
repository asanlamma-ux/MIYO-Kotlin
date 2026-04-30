package com.miyu.reader.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.miyu.reader.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "miyu_settings")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // Theme
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        ThemeMode.valueOf(prefs[THEME_MODE] ?: ThemeMode.LIGHT.name)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[THEME_MODE] = mode.name }
    }

<<<<<<< HEAD
=======
    // Reader theme
    val readerThemeId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_READER_THEME_ID] ?: "sepia-classic"
    }

    suspend fun setReaderThemeId(id: String) {
        context.dataStore.edit { it[KEY_READER_THEME_ID] = id }
    }

>>>>>>> debug
    // Typography
    val typography: Flow<TypographySettings> = context.dataStore.data.map { prefs ->
        TypographySettings(
            fontFamily = prefs[KEY_FONT_FAMILY] ?: "default",
            fontSize = prefs[KEY_FONT_SIZE] ?: 16f,
            lineHeight = prefs[KEY_LINE_HEIGHT] ?: 1.6f,
            letterSpacing = prefs[KEY_LETTER_SPACING] ?: 0f,
            paragraphSpacing = prefs[KEY_PARAGRAPH_SPACING] ?: 0f,
            textAlign = TextAlign.valueOf(prefs[KEY_TEXT_ALIGN] ?: TextAlign.JUSTIFY.name),
            fontWeight = BodyFontWeight.entries.firstOrNull { it.value == (prefs[KEY_FONT_WEIGHT] ?: 400) }
                ?: BodyFontWeight.W400,
        )
    }

    suspend fun setTypography(settings: TypographySettings) {
        context.dataStore.edit {
            it[KEY_FONT_FAMILY] = settings.fontFamily
            it[KEY_FONT_SIZE] = settings.fontSize
            it[KEY_LINE_HEIGHT] = settings.lineHeight
            it[KEY_LETTER_SPACING] = settings.letterSpacing
            it[KEY_PARAGRAPH_SPACING] = settings.paragraphSpacing
            it[KEY_TEXT_ALIGN] = settings.textAlign.name
            it[KEY_FONT_WEIGHT] = settings.fontWeight.value
        }
    }

    // Reading settings
    val readingSettings: Flow<ReadingSettings> = context.dataStore.data.map { prefs ->
        ReadingSettings(
            pageAnimation = PageAnimation.valueOf(prefs[KEY_PAGE_ANIMATION] ?: PageAnimation.SLIDE.name),
            tapZonesEnabled = prefs[KEY_TAP_ZONES_ENABLED] ?: true,
            tapScrollPageRatio = prefs[KEY_TAP_SCROLL_RATIO] ?: 0.9f,
            tapZoneNavMode = TapZoneNavMode.valueOf(prefs[KEY_TAP_ZONE_MODE] ?: TapZoneNavMode.SCROLL.name),
            volumeButtonPageTurn = prefs[KEY_VOLUME_TURN] ?: false,
            autoScrollSpeed = prefs[KEY_AUTO_SCROLL] ?: 0f,
            immersiveMode = prefs[KEY_IMMERSIVE] ?: false,
            brightnessOverride = prefs[KEY_BRIGHTNESS],
            blueLightFilter = prefs[KEY_BLUE_LIGHT] ?: false,
            reducedMotion = prefs[KEY_REDUCED_MOTION] ?: false,
            marginPreset = MarginPreset.valueOf(prefs[KEY_MARGIN_PRESET] ?: MarginPreset.MEDIUM.name),
            contentColumnWidth = prefs[KEY_COLUMN_WIDTH],
            readerColumnLayout = ReaderColumnLayout.valueOf(prefs[KEY_COLUMN_LAYOUT] ?: ReaderColumnLayout.SINGLE.name),
        )
    }

    suspend fun setReadingSettings(settings: ReadingSettings) {
        context.dataStore.edit {
            it[KEY_PAGE_ANIMATION] = settings.pageAnimation.name
            it[KEY_TAP_ZONES_ENABLED] = settings.tapZonesEnabled
            it[KEY_TAP_SCROLL_RATIO] = settings.tapScrollPageRatio
            it[KEY_TAP_ZONE_MODE] = settings.tapZoneNavMode.name
            it[KEY_VOLUME_TURN] = settings.volumeButtonPageTurn
            it[KEY_AUTO_SCROLL] = settings.autoScrollSpeed
            it[KEY_IMMERSIVE] = settings.immersiveMode
            if (settings.brightnessOverride != null) it[KEY_BRIGHTNESS] = settings.brightnessOverride
            else it.remove(KEY_BRIGHTNESS)
            it[KEY_BLUE_LIGHT] = settings.blueLightFilter
            it[KEY_REDUCED_MOTION] = settings.reducedMotion
            it[KEY_MARGIN_PRESET] = settings.marginPreset.name
            if (settings.contentColumnWidth != null) it[KEY_COLUMN_WIDTH] = settings.contentColumnWidth
            else it.remove(KEY_COLUMN_WIDTH)
            it[KEY_COLUMN_LAYOUT] = settings.readerColumnLayout.name
        }
    }

    // Last active book
    val lastActiveBookId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_BOOK_ID]
    }

    suspend fun setLastActiveBookId(bookId: String?) {
        context.dataStore.edit {
            if (bookId != null) it[KEY_LAST_BOOK_ID] = bookId
            else it.remove(KEY_LAST_BOOK_ID)
        }
    }

    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_FONT_FAMILY = stringPreferencesKey("font_family")
        private val KEY_FONT_SIZE = floatPreferencesKey("font_size")
        private val KEY_LINE_HEIGHT = floatPreferencesKey("line_height")
        private val KEY_LETTER_SPACING = floatPreferencesKey("letter_spacing")
        private val KEY_PARAGRAPH_SPACING = floatPreferencesKey("paragraph_spacing")
        private val KEY_TEXT_ALIGN = stringPreferencesKey("text_align")
        private val KEY_FONT_WEIGHT = intPreferencesKey("font_weight")
        private val KEY_PAGE_ANIMATION = stringPreferencesKey("page_animation")
        private val KEY_TAP_ZONES_ENABLED = booleanPreferencesKey("tap_zones")
        private val KEY_TAP_SCROLL_RATIO = floatPreferencesKey("tap_scroll_ratio")
        private val KEY_TAP_ZONE_MODE = stringPreferencesKey("tap_zone_mode")
        private val KEY_VOLUME_TURN = booleanPreferencesKey("volume_turn")
        private val KEY_AUTO_SCROLL = floatPreferencesKey("auto_scroll")
        private val KEY_IMMERSIVE = booleanPreferencesKey("immersive")
        private val KEY_BRIGHTNESS = floatPreferencesKey("brightness")
        private val KEY_BLUE_LIGHT = booleanPreferencesKey("blue_light")
        private val KEY_REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        private val KEY_MARGIN_PRESET = stringPreferencesKey("margin_preset")
        private val KEY_COLUMN_WIDTH = intPreferencesKey("column_width")
        private val KEY_COLUMN_LAYOUT = stringPreferencesKey("column_layout")
        private val KEY_LAST_BOOK_ID = stringPreferencesKey("last_book_id")
<<<<<<< HEAD
=======
        private val KEY_READER_THEME_ID = stringPreferencesKey("reader_theme_id")
>>>>>>> debug
    }
}