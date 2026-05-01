package com.miyu.reader.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.miyu.reader.domain.model.*
import com.miyu.reader.ui.theme.DefaultReaderThemeId
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
        ThemeMode.valueOf(prefs[THEME_MODE] ?: ThemeMode.SYSTEM.name)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[THEME_MODE] = mode.name }
    }

    // Reader theme
    val readerThemeId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_READER_THEME_ID] ?: DefaultReaderThemeId
    }

    suspend fun setReaderThemeId(id: String) {
        context.dataStore.edit { it[KEY_READER_THEME_ID] = id }
    }

    // Typography
    val typography: Flow<TypographySettings> = context.dataStore.data.map { prefs ->
        val defaults = TypographySettings()
        TypographySettings(
            fontFamily = prefs[KEY_FONT_FAMILY] ?: defaults.fontFamily,
            fontSize = prefs[KEY_FONT_SIZE] ?: defaults.fontSize,
            lineHeight = prefs[KEY_LINE_HEIGHT] ?: defaults.lineHeight,
            letterSpacing = prefs[KEY_LETTER_SPACING] ?: defaults.letterSpacing,
            paragraphSpacing = prefs[KEY_PARAGRAPH_SPACING] ?: defaults.paragraphSpacing,
            textAlign = TextAlign.valueOf(prefs[KEY_TEXT_ALIGN] ?: defaults.textAlign.name),
            fontWeight = BodyFontWeight.entries.firstOrNull { it.value == (prefs[KEY_FONT_WEIGHT] ?: 400) }
                ?: defaults.fontWeight,
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
        val defaults = ReadingSettings()
        ReadingSettings(
            pageAnimation = PageAnimation.valueOf(prefs[KEY_PAGE_ANIMATION] ?: defaults.pageAnimation.name),
            tapZonesEnabled = prefs[KEY_TAP_ZONES_ENABLED] ?: defaults.tapZonesEnabled,
            tapScrollPageRatio = prefs[KEY_TAP_SCROLL_RATIO] ?: defaults.tapScrollPageRatio,
            tapZoneNavMode = TapZoneNavMode.valueOf(prefs[KEY_TAP_ZONE_MODE] ?: defaults.tapZoneNavMode.name),
            volumeButtonPageTurn = prefs[KEY_VOLUME_TURN] ?: defaults.volumeButtonPageTurn,
            autoScrollSpeed = prefs[KEY_AUTO_SCROLL] ?: defaults.autoScrollSpeed,
            immersiveMode = prefs[KEY_IMMERSIVE] ?: defaults.immersiveMode,
            brightnessOverride = prefs[KEY_BRIGHTNESS],
            blueLightFilter = prefs[KEY_BLUE_LIGHT] ?: defaults.blueLightFilter,
            reducedMotion = prefs[KEY_REDUCED_MOTION] ?: defaults.reducedMotion,
            keepScreenOn = prefs[KEY_KEEP_SCREEN_ON] ?: defaults.keepScreenOn,
            bionicReading = prefs[KEY_BIONIC_READING] ?: defaults.bionicReading,
            autoAdvanceChapter = prefs[KEY_AUTO_ADVANCE_CHAPTER] ?: defaults.autoAdvanceChapter,
            sleepTimerMinutes = prefs[KEY_SLEEP_TIMER_MINUTES] ?: defaults.sleepTimerMinutes,
            readingFlowMode = ReaderFlowMode.valueOf(prefs[KEY_READING_FLOW_MODE] ?: defaults.readingFlowMode.name),
            marginPreset = MarginPreset.valueOf(prefs[KEY_MARGIN_PRESET] ?: defaults.marginPreset.name),
            contentColumnWidth = prefs[KEY_COLUMN_WIDTH] ?: defaults.contentColumnWidth,
            readerColumnLayout = ReaderColumnLayout.valueOf(prefs[KEY_COLUMN_LAYOUT] ?: defaults.readerColumnLayout.name),
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
            it[KEY_KEEP_SCREEN_ON] = settings.keepScreenOn
            it[KEY_BIONIC_READING] = settings.bionicReading
            it[KEY_AUTO_ADVANCE_CHAPTER] = settings.autoAdvanceChapter
            it[KEY_SLEEP_TIMER_MINUTES] = settings.sleepTimerMinutes
            it[KEY_READING_FLOW_MODE] = settings.readingFlowMode.name
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

    val dailyGoalMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_DAILY_GOAL_MINUTES] ?: 30
    }

    suspend fun setDailyGoalMinutes(minutes: Int) {
        context.dataStore.edit { it[KEY_DAILY_GOAL_MINUTES] = minutes.coerceIn(15, 240) }
    }

    val storageDirectoryUri: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_STORAGE_DIRECTORY_URI]
    }

    suspend fun setStorageDirectoryUri(uri: String?) {
        context.dataStore.edit {
            if (uri.isNullOrBlank()) it.remove(KEY_STORAGE_DIRECTORY_URI)
            else it[KEY_STORAGE_DIRECTORY_URI] = uri
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
        private val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        private val KEY_BIONIC_READING = booleanPreferencesKey("bionic_reading")
        private val KEY_AUTO_ADVANCE_CHAPTER = booleanPreferencesKey("auto_advance_chapter")
        private val KEY_SLEEP_TIMER_MINUTES = intPreferencesKey("sleep_timer_minutes")
        private val KEY_READING_FLOW_MODE = stringPreferencesKey("reading_flow_mode")
        private val KEY_MARGIN_PRESET = stringPreferencesKey("margin_preset")
        private val KEY_COLUMN_WIDTH = intPreferencesKey("column_width")
        private val KEY_COLUMN_LAYOUT = stringPreferencesKey("column_layout")
        private val KEY_LAST_BOOK_ID = stringPreferencesKey("last_book_id")
        // v2 intentionally ignores stale values from early broken builds where
        // the picker could persist Night Mode as the apparent first-run theme.
        private val KEY_READER_THEME_ID = stringPreferencesKey("reader_theme_id_v2")
        private val KEY_DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
        private val KEY_STORAGE_DIRECTORY_URI = stringPreferencesKey("storage_directory_uri")
    }
}
