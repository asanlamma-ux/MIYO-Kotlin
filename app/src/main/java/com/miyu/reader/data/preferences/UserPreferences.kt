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

    val shouldShowInitialSetup: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_INITIAL_SETUP_COMPLETE]?.not() ?: isFreshInstallPreferences(prefs)
    }

    suspend fun setInitialSetupComplete(complete: Boolean) {
        context.dataStore.edit { it[KEY_INITIAL_SETUP_COMPLETE] = complete }
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
            readerMode = ReaderMode.valueOf(prefs[KEY_READER_MODE] ?: defaults.readerMode.name),
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
            hideReaderHeader = prefs[KEY_HIDE_READER_HEADER] ?: defaults.hideReaderHeader,
            hideReaderFooter = prefs[KEY_HIDE_READER_FOOTER] ?: defaults.hideReaderFooter,
            readerNavLocked = prefs[KEY_READER_NAV_LOCKED] ?: defaults.readerNavLocked,
            selectionPopupEnabled = prefs[KEY_SELECTION_POPUP_ENABLED] ?: defaults.selectionPopupEnabled,
            showPageBorder = prefs[KEY_SHOW_PAGE_BORDER] ?: defaults.showPageBorder,
            overwriteLinkStyle = prefs[KEY_OVERWRITE_LINK_STYLE] ?: defaults.overwriteLinkStyle,
            overwriteTextStyle = prefs[KEY_OVERWRITE_TEXT_STYLE] ?: defaults.overwriteTextStyle,
        )
    }

    suspend fun setReadingSettings(settings: ReadingSettings) {
        context.dataStore.edit {
            it[KEY_READER_MODE] = settings.readerMode.name
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
            it[KEY_HIDE_READER_HEADER] = settings.hideReaderHeader
            it[KEY_HIDE_READER_FOOTER] = settings.hideReaderFooter
            it[KEY_READER_NAV_LOCKED] = settings.readerNavLocked
            it[KEY_SELECTION_POPUP_ENABLED] = settings.selectionPopupEnabled
            it[KEY_SHOW_PAGE_BORDER] = settings.showPageBorder
            it[KEY_OVERWRITE_LINK_STYLE] = settings.overwriteLinkStyle
            it[KEY_OVERWRITE_TEXT_STYLE] = settings.overwriteTextStyle
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

    val storagePermissionAutoRedirectComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_STORAGE_PERMISSION_AUTO_REDIRECT_COMPLETE] ?: false
    }

    suspend fun setStoragePermissionAutoRedirectComplete(complete: Boolean) {
        context.dataStore.edit { it[KEY_STORAGE_PERMISSION_AUTO_REDIRECT_COMPLETE] = complete }
    }

    val downloadConcurrency: Flow<Int> = context.dataStore.data.map { prefs ->
        (prefs[KEY_DOWNLOAD_CONCURRENCY] ?: DEFAULT_DOWNLOAD_CONCURRENCY).coerceIn(MIN_DOWNLOAD_CONCURRENCY, MAX_DOWNLOAD_CONCURRENCY)
    }

    suspend fun setDownloadConcurrency(value: Int) {
        context.dataStore.edit {
            it[KEY_DOWNLOAD_CONCURRENCY] = value.coerceIn(MIN_DOWNLOAD_CONCURRENCY, MAX_DOWNLOAD_CONCURRENCY)
        }
    }

    val libraryCategories: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_LIBRARY_CATEGORIES].orEmpty()
    }

    suspend fun setLibraryCategories(categories: Set<String>) {
        context.dataStore.edit {
            it[KEY_LIBRARY_CATEGORIES] = categories
                .mapNotNull { name -> name.trim().takeIf(String::isNotBlank) }
                .toSet()
        }
    }

    val sourcePinnedIds: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_SOURCE_PINNED_IDS].orEmpty()
    }

    suspend fun setSourcePinnedIds(sourceIds: Set<String>) {
        context.dataStore.edit {
            it[KEY_SOURCE_PINNED_IDS] = sourceIds
                .mapNotNull { sourceId -> sourceId.trim().takeIf(String::isNotBlank) }
                .toSet()
        }
    }

    val sourceLanguageFilter: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_SOURCE_LANGUAGE_FILTER].orEmpty()
    }

    suspend fun setSourceLanguageFilter(languages: Set<String>) {
        context.dataStore.edit {
            it[KEY_SOURCE_LANGUAGE_FILTER] = languages
                .mapNotNull { language -> language.trim().takeIf(String::isNotBlank) }
                .toSet()
        }
    }

    val lastUsedSourceId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_USED_SOURCE_ID]
    }

    suspend fun setLastUsedSourceId(sourceId: String?) {
        context.dataStore.edit {
            if (sourceId.isNullOrBlank()) it.remove(KEY_LAST_USED_SOURCE_ID)
            else it[KEY_LAST_USED_SOURCE_ID] = sourceId
        }
    }

    val sourceRepositoryUrls: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_SOURCE_REPOSITORY_URLS].orEmpty()
    }

    suspend fun setSourceRepositoryUrls(urls: Set<String>) {
        context.dataStore.edit {
            it[KEY_SOURCE_REPOSITORY_URLS] = urls
                .mapNotNull { url -> url.trim().takeIf(String::isNotBlank) }
                .toSet()
        }
    }

    val browseSearchHistory: Flow<List<String>> = context.dataStore.data.map { prefs ->
        decodeSearchHistory(prefs[KEY_BROWSE_SEARCH_HISTORY])
    }

    suspend fun recordBrowseSearchQuery(query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return
        context.dataStore.edit { prefs ->
            val next = (listOf(cleanQuery) + decodeSearchHistory(prefs[KEY_BROWSE_SEARCH_HISTORY]))
                .distinctBy { it.lowercase() }
                .take(MAX_SEARCH_HISTORY)
            prefs[KEY_BROWSE_SEARCH_HISTORY] = next.joinToString(HISTORY_SEPARATOR)
        }
    }

    suspend fun removeBrowseSearchQuery(query: String) {
        context.dataStore.edit { prefs ->
            val next = decodeSearchHistory(prefs[KEY_BROWSE_SEARCH_HISTORY])
                .filterNot { it.equals(query.trim(), ignoreCase = true) }
            if (next.isEmpty()) {
                prefs.remove(KEY_BROWSE_SEARCH_HISTORY)
            } else {
                prefs[KEY_BROWSE_SEARCH_HISTORY] = next.joinToString(HISTORY_SEPARATOR)
            }
        }
    }

    suspend fun clearBrowseSearchHistory() {
        context.dataStore.edit { it.remove(KEY_BROWSE_SEARCH_HISTORY) }
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
        private val KEY_READER_MODE = stringPreferencesKey("reader_mode_v1")
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
        private val KEY_HIDE_READER_HEADER = booleanPreferencesKey("hide_reader_header")
        private val KEY_HIDE_READER_FOOTER = booleanPreferencesKey("hide_reader_footer")
        private val KEY_READER_NAV_LOCKED = booleanPreferencesKey("reader_nav_locked")
        private val KEY_SELECTION_POPUP_ENABLED = booleanPreferencesKey("selection_popup_enabled")
        private val KEY_SHOW_PAGE_BORDER = booleanPreferencesKey("show_page_border")
        private val KEY_OVERWRITE_LINK_STYLE = booleanPreferencesKey("overwrite_link_style")
        private val KEY_OVERWRITE_TEXT_STYLE = booleanPreferencesKey("overwrite_text_style")
        private val KEY_LAST_BOOK_ID = stringPreferencesKey("last_book_id")
        // v2 intentionally ignores stale values from early broken builds where
        // the picker could persist Night Mode as the apparent first-run theme.
        private val KEY_READER_THEME_ID = stringPreferencesKey("reader_theme_id_v2")
        private val KEY_DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
        private val KEY_STORAGE_DIRECTORY_URI = stringPreferencesKey("storage_directory_uri")
        private val KEY_STORAGE_PERMISSION_AUTO_REDIRECT_COMPLETE =
            booleanPreferencesKey("storage_permission_auto_redirect_complete_v1")
        private val KEY_INITIAL_SETUP_COMPLETE = booleanPreferencesKey("initial_setup_complete_v1")
        private val KEY_LIBRARY_CATEGORIES = stringSetPreferencesKey("library_categories")
        private val KEY_SOURCE_PINNED_IDS = stringSetPreferencesKey("source_pinned_ids")
        private val KEY_SOURCE_LANGUAGE_FILTER = stringSetPreferencesKey("source_language_filter")
        private val KEY_LAST_USED_SOURCE_ID = stringPreferencesKey("last_used_source_id")
        private val KEY_SOURCE_REPOSITORY_URLS = stringSetPreferencesKey("source_repository_urls")
        private val KEY_BROWSE_SEARCH_HISTORY = stringPreferencesKey("browse_search_history_v1")
        private val KEY_DOWNLOAD_CONCURRENCY = intPreferencesKey("download_concurrency_v1")
        private const val HISTORY_SEPARATOR = "\u001F"
        private const val MAX_SEARCH_HISTORY = 12
        const val MIN_DOWNLOAD_CONCURRENCY = 2
        const val MAX_DOWNLOAD_CONCURRENCY = 10
        const val DEFAULT_DOWNLOAD_CONCURRENCY = 4

        private val SETUP_EXISTING_KEYS = setOf<Preferences.Key<*>>(
            THEME_MODE,
            KEY_READER_THEME_ID,
            KEY_FONT_FAMILY,
            KEY_FONT_SIZE,
            KEY_LINE_HEIGHT,
            KEY_READER_MODE,
            KEY_PAGE_ANIMATION,
            KEY_TAP_ZONES_ENABLED,
            KEY_READING_FLOW_MODE,
            KEY_DAILY_GOAL_MINUTES,
            KEY_STORAGE_DIRECTORY_URI,
            KEY_DOWNLOAD_CONCURRENCY,
        )

        private fun isFreshInstallPreferences(prefs: Preferences): Boolean =
            prefs.asMap().keys.none { key -> key in SETUP_EXISTING_KEYS }

        private fun decodeSearchHistory(raw: String?): List<String> =
            raw
                ?.split(HISTORY_SEPARATOR)
                ?.mapNotNull { it.trim().takeIf(String::isNotBlank) }
                .orEmpty()
    }
}
