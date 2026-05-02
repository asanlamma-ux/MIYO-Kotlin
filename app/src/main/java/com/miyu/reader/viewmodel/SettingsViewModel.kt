package com.miyu.reader.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyu.reader.data.preferences.UserPreferences
import com.miyu.reader.data.repository.BookRepository
import com.miyu.reader.data.repository.DictionaryRepository
import com.miyu.reader.domain.model.*
import com.miyu.reader.engine.bridge.EpubEngineBridge
import com.miyu.reader.storage.MiyoStorage
import com.miyu.reader.ui.theme.DefaultReaderThemeId
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class SettingsUiState(
    val typography: TypographySettings = TypographySettings(),
    val readingSettings: ReadingSettings = ReadingSettings(),
    val readerThemeId: String = DefaultReaderThemeId,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val bookCount: Int = 0,
    val dailyGoalMinutes: Int = 30,
    val storageDirectoryUri: String? = null,
    val downloadConcurrency: Int = UserPreferences.DEFAULT_DOWNLOAD_CONCURRENCY,
    val libraryBytes: Long = 0L,
    val starterDictionaries: List<DownloadedDictionary> = emptyList(),
    val downloadedDictionaries: List<DownloadedDictionary> = emptyList(),
    val dictionaryBusy: Boolean = false,
)

data class RescanSummary(
    val imported: Int,
    val removed: Int,
    val tracked: Int,
)

data class DuplicateAuditSummary(
    val exactGroups: Int,
    val samples: List<String>,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferences,
    private val bookRepository: BookRepository,
    private val dictionaryRepository: DictionaryRepository,
    private val epubEngine: EpubEngineBridge,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.typography.collect { typo ->
                _uiState.update { it.copy(typography = typo) }
            }
        }
        viewModelScope.launch {
            preferences.readingSettings.collect { rs ->
                _uiState.update { it.copy(readingSettings = rs) }
            }
        }
        viewModelScope.launch {
            preferences.readerThemeId.collect { id ->
                _uiState.update { it.copy(readerThemeId = id) }
            }
        }
        viewModelScope.launch {
            preferences.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            preferences.dailyGoalMinutes.collect { goal ->
                _uiState.update { it.copy(dailyGoalMinutes = goal) }
            }
        }
        viewModelScope.launch {
            preferences.storageDirectoryUri.collect { uri ->
                _uiState.update { it.copy(storageDirectoryUri = uri) }
            }
        }
        viewModelScope.launch {
            preferences.downloadConcurrency.collect { concurrency ->
                _uiState.update { it.copy(downloadConcurrency = concurrency) }
            }
        }
        viewModelScope.launch {
            bookRepository.getAllBooks().collect { books ->
                val libraryBytes = withContext(Dispatchers.IO) {
                    books.sumOf { book ->
                        runCatching { File(book.filePath).takeIf { it.exists() }?.length() ?: 0L }
                            .getOrDefault(0L)
                    }
                }
                _uiState.update { it.copy(bookCount = books.size, libraryBytes = libraryBytes) }
            }
        }
        viewModelScope.launch {
            dictionaryRepository.getDownloadedDictionaries().collect { dictionaries ->
                _uiState.update {
                    it.copy(
                        starterDictionaries = dictionaryRepository.getStarterDictionaries(),
                        downloadedDictionaries = dictionaries,
                    )
                }
            }
        }
    }

    fun setFontSize(size: Float) {
        viewModelScope.launch {
            val current = _uiState.value.typography
            preferences.setTypography(current.copy(fontSize = size.coerceIn(12f, 28f)))
        }
    }

    fun setLineHeight(height: Float) {
        viewModelScope.launch {
            val current = _uiState.value.typography
            preferences.setTypography(current.copy(lineHeight = height.coerceIn(1.2f, 2.0f)))
        }
    }

    fun setTextAlign(align: TextAlign) {
        viewModelScope.launch {
            val current = _uiState.value.typography
            preferences.setTypography(current.copy(textAlign = align))
        }
    }

    fun setPageAnimation(animation: PageAnimation) {
        viewModelScope.launch {
            val current = _uiState.value.readingSettings
            preferences.setReadingSettings(current.copy(pageAnimation = animation))
        }
    }

    fun setTapZonesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.readingSettings
            preferences.setReadingSettings(current.copy(tapZonesEnabled = enabled))
        }
    }

    fun setVolumeButtonPageTurn(enabled: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.readingSettings
            preferences.setReadingSettings(current.copy(volumeButtonPageTurn = enabled))
        }
    }

    fun setReducedMotion(enabled: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.readingSettings
            preferences.setReadingSettings(current.copy(reducedMotion = enabled))
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.readingSettings
            preferences.setReadingSettings(current.copy(keepScreenOn = enabled))
        }
    }

    fun setBionicReading(enabled: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.readingSettings
            preferences.setReadingSettings(current.copy(bionicReading = enabled))
        }
    }

    fun setAutoAdvanceChapter(enabled: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.readingSettings
            preferences.setReadingSettings(current.copy(autoAdvanceChapter = enabled))
        }
    }

    fun setSleepTimer(minutes: Int) {
        viewModelScope.launch {
            val current = _uiState.value.readingSettings
            preferences.setReadingSettings(current.copy(sleepTimerMinutes = minutes.coerceIn(0, 240)))
        }
    }

    fun setTapZoneNavMode(mode: TapZoneNavMode) {
        viewModelScope.launch {
            val current = _uiState.value.readingSettings
            preferences.setReadingSettings(current.copy(tapZoneNavMode = mode))
        }
    }

    fun setImmersiveMode(enabled: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.readingSettings
            preferences.setReadingSettings(current.copy(immersiveMode = enabled))
        }
    }

    fun setMarginPreset(preset: MarginPreset) {
        viewModelScope.launch {
            val current = _uiState.value.readingSettings
            preferences.setReadingSettings(current.copy(marginPreset = preset))
        }
    }

    fun setReaderThemeId(id: String) {
        viewModelScope.launch { preferences.setReaderThemeId(id) }
    }

    fun setDailyGoalMinutes(minutes: Int) {
        viewModelScope.launch { preferences.setDailyGoalMinutes(minutes) }
    }

    fun setStorageDirectoryUri(uri: String?) {
        viewModelScope.launch { preferences.setStorageDirectoryUri(uri) }
    }

    fun setDownloadConcurrency(concurrency: Int) {
        viewModelScope.launch { preferences.setDownloadConcurrency(concurrency) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    suspend fun exportSettingsSnapshot(): String = withContext(Dispatchers.Default) {
        val state = _uiState.value
        val typography = state.typography
        val reading = state.readingSettings
        JSONObject()
            .put("schema", 1)
            .put("exportedAt", System.currentTimeMillis())
            .put("themeMode", state.themeMode.name)
            .put("readerThemeId", state.readerThemeId)
            .put("dailyGoalMinutes", state.dailyGoalMinutes)
            .put("downloadConcurrency", state.downloadConcurrency)
            .put(
                "typography",
                JSONObject()
                    .put("fontFamily", typography.fontFamily)
                    .put("fontSize", typography.fontSize)
                    .put("lineHeight", typography.lineHeight)
                    .put("letterSpacing", typography.letterSpacing)
                    .put("paragraphSpacing", typography.paragraphSpacing)
                    .put("textAlign", typography.textAlign.name)
                    .put("fontWeight", typography.fontWeight.value),
            )
            .put(
                "reading",
                JSONObject()
                    .put("pageAnimation", reading.pageAnimation.name)
                    .put("tapZonesEnabled", reading.tapZonesEnabled)
                    .put("tapScrollPageRatio", reading.tapScrollPageRatio)
                    .put("tapZoneNavMode", reading.tapZoneNavMode.name)
                    .put("volumeButtonPageTurn", reading.volumeButtonPageTurn)
                    .put("autoScrollSpeed", reading.autoScrollSpeed)
                    .put("immersiveMode", reading.immersiveMode)
                    .put("brightnessOverride", reading.brightnessOverride ?: JSONObject.NULL)
                    .put("blueLightFilter", reading.blueLightFilter)
                    .put("reducedMotion", reading.reducedMotion)
                    .put("keepScreenOn", reading.keepScreenOn)
                    .put("bionicReading", reading.bionicReading)
                    .put("autoAdvanceChapter", reading.autoAdvanceChapter)
                    .put("sleepTimerMinutes", reading.sleepTimerMinutes)
                    .put("readingFlowMode", reading.readingFlowMode.name)
                    .put("marginPreset", reading.marginPreset.name)
                    .put("contentColumnWidth", reading.contentColumnWidth ?: JSONObject.NULL)
                    .put("readerColumnLayout", reading.readerColumnLayout.name),
            )
            .toString(2)
    }

    suspend fun importSettingsSnapshot(rawJson: String): String {
        val snapshot = withContext(Dispatchers.Default) { JSONObject(rawJson) }
        val schema = snapshot.optInt("schema", 0)
        require(schema == 1) { "Unsupported settings snapshot schema: $schema" }

        val typographyJson = snapshot.optJSONObject("typography") ?: JSONObject()
        val readingJson = snapshot.optJSONObject("reading") ?: JSONObject()
        val defaultTypography = TypographySettings()
        val defaultReading = ReadingSettings()

        val importedTypography = TypographySettings(
            fontFamily = typographyJson.optString("fontFamily", defaultTypography.fontFamily),
            fontSize = typographyJson.optDouble("fontSize", defaultTypography.fontSize.toDouble()).toFloat().coerceIn(12f, 28f),
            lineHeight = typographyJson.optDouble("lineHeight", defaultTypography.lineHeight.toDouble()).toFloat().coerceIn(1.2f, 2.0f),
            letterSpacing = typographyJson.optDouble("letterSpacing", defaultTypography.letterSpacing.toDouble()).toFloat(),
            paragraphSpacing = typographyJson.optDouble("paragraphSpacing", defaultTypography.paragraphSpacing.toDouble()).toFloat(),
            textAlign = enumOrDefault(typographyJson.optString("textAlign"), defaultTypography.textAlign),
            fontWeight = BodyFontWeight.entries.firstOrNull {
                it.value == typographyJson.optInt("fontWeight", defaultTypography.fontWeight.value)
            } ?: defaultTypography.fontWeight,
        )

        val importedReading = ReadingSettings(
            pageAnimation = enumOrDefault(readingJson.optString("pageAnimation"), defaultReading.pageAnimation),
            tapZonesEnabled = readingJson.optBoolean("tapZonesEnabled", defaultReading.tapZonesEnabled),
            tapScrollPageRatio = readingJson.optDouble("tapScrollPageRatio", defaultReading.tapScrollPageRatio.toDouble()).toFloat(),
            tapZoneNavMode = enumOrDefault(readingJson.optString("tapZoneNavMode"), defaultReading.tapZoneNavMode),
            volumeButtonPageTurn = readingJson.optBoolean("volumeButtonPageTurn", defaultReading.volumeButtonPageTurn),
            autoScrollSpeed = readingJson.optDouble("autoScrollSpeed", defaultReading.autoScrollSpeed.toDouble()).toFloat(),
            immersiveMode = readingJson.optBoolean("immersiveMode", defaultReading.immersiveMode),
            brightnessOverride = if (readingJson.has("brightnessOverride") && !readingJson.isNull("brightnessOverride")) {
                readingJson.optDouble("brightnessOverride").toFloat()
            } else {
                defaultReading.brightnessOverride
            },
            blueLightFilter = readingJson.optBoolean("blueLightFilter", defaultReading.blueLightFilter),
            reducedMotion = readingJson.optBoolean("reducedMotion", defaultReading.reducedMotion),
            keepScreenOn = readingJson.optBoolean("keepScreenOn", defaultReading.keepScreenOn),
            bionicReading = readingJson.optBoolean("bionicReading", defaultReading.bionicReading),
            autoAdvanceChapter = readingJson.optBoolean("autoAdvanceChapter", defaultReading.autoAdvanceChapter),
            sleepTimerMinutes = readingJson.optInt("sleepTimerMinutes", defaultReading.sleepTimerMinutes).coerceIn(0, 240),
            readingFlowMode = enumOrDefault(readingJson.optString("readingFlowMode"), defaultReading.readingFlowMode),
            marginPreset = enumOrDefault(readingJson.optString("marginPreset"), defaultReading.marginPreset),
            contentColumnWidth = if (readingJson.has("contentColumnWidth") && !readingJson.isNull("contentColumnWidth")) {
                readingJson.optInt("contentColumnWidth").coerceIn(320, 960)
            } else {
                defaultReading.contentColumnWidth
            },
            readerColumnLayout = enumOrDefault(readingJson.optString("readerColumnLayout"), defaultReading.readerColumnLayout),
        )

        preferences.setThemeMode(enumOrDefault(snapshot.optString("themeMode"), ThemeMode.SYSTEM))
        preferences.setReaderThemeId(snapshot.optString("readerThemeId", DefaultReaderThemeId))
        preferences.setDailyGoalMinutes(snapshot.optInt("dailyGoalMinutes", 30))
        preferences.setDownloadConcurrency(snapshot.optInt("downloadConcurrency", UserPreferences.DEFAULT_DOWNLOAD_CONCURRENCY))
        preferences.setTypography(importedTypography)
        preferences.setReadingSettings(importedReading)
        return "Settings snapshot imported. Reader, typography, theme, daily goal, and download preferences were restored."
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        appContext.cacheDir.listFiles()?.forEach { child ->
            if (child.isDirectory) child.deleteRecursively() else child.delete()
        }
    }

    suspend fun rescanLibrary(): RescanSummary = withContext(Dispatchers.IO) {
        val existingBooks = bookRepository.getAllBooksOnce()
        val booksDirs = listOf(MiyoStorage.booksDir(appContext), MiyoStorage.legacyBooksDir(appContext))
            .filter { it.exists() && it.isDirectory }
            .distinctBy { it.absolutePath }
        val epubFiles = booksDirs.flatMap { dir ->
            dir.listFiles { file -> file.isFile && file.extension.equals("epub", ignoreCase = true) }
                ?.toList()
                ?: emptyList()
        }.distinctBy { it.absolutePath }

        val existingPaths = existingBooks.associateBy { File(it.filePath).absolutePath }
        var imported = 0
        epubFiles.forEach { file ->
            if (!existingPaths.containsKey(file.absolutePath)) {
                importBookFile(file)
                imported += 1
            }
        }

        var removed = 0
        existingBooks.forEach { book ->
            val file = File(book.filePath)
            if (!file.exists()) {
                bookRepository.deleteBook(book.id)
                removed += 1
            } else if (book.coverUri.isNullOrBlank()) {
                val coverUri = epubEngine.extractCoverImage(file.absolutePath)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { if (it.startsWith("data:", ignoreCase = true)) it else "data:image/jpeg;base64,$it" }
                    ?.let { persistCoverImage(book.id, it) ?: it }
                if (!coverUri.isNullOrBlank()) {
                    bookRepository.saveBook(book.copy(coverUri = coverUri))
                }
            }
        }

        val tracked = bookRepository.getAllBooksOnce().size
        RescanSummary(imported = imported, removed = removed, tracked = tracked)
    }

    suspend fun auditDuplicates(): DuplicateAuditSummary = withContext(Dispatchers.IO) {
        val books = bookRepository.getAllBooksOnce()
        val exactGroups = books
            .groupBy { (it.epubIdentifier?.trim()?.lowercase()).takeUnless { key -> key.isNullOrBlank() } ?: "${it.title.trim().lowercase()}|${it.author.trim().lowercase()}" }
            .values
            .filter { it.size > 1 }

        DuplicateAuditSummary(
            exactGroups = exactGroups.size,
            samples = exactGroups.take(4).map { group ->
                group.joinToString(" / ") { it.title }
            },
        )
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            preferences.setTypography(TypographySettings())
            preferences.setReadingSettings(ReadingSettings())
            preferences.setReaderThemeId(DefaultReaderThemeId)
            preferences.setThemeMode(ThemeMode.SYSTEM)
            preferences.setDailyGoalMinutes(30)
            preferences.setDownloadConcurrency(UserPreferences.DEFAULT_DOWNLOAD_CONCURRENCY)
            preferences.setInitialSetupComplete(false)
            preferences.setStoragePermissionAutoRedirectComplete(false)
        }
    }

    suspend fun installStarterDictionary(dictionaryId: String): String = withContext(Dispatchers.IO) {
        _uiState.update { it.copy(dictionaryBusy = true) }
        try {
            if (dictionaryRepository.installStarterDictionary(dictionaryId)) {
                "Dictionary installed."
            } else {
                "Dictionary pack was not found."
            }
        } finally {
            _uiState.update { it.copy(dictionaryBusy = false) }
        }
    }

    suspend fun removeDictionary(dictionaryId: String): String = withContext(Dispatchers.IO) {
        _uiState.update { it.copy(dictionaryBusy = true) }
        try {
            dictionaryRepository.removeDictionary(dictionaryId)
            "Dictionary removed."
        } finally {
            _uiState.update { it.copy(dictionaryBusy = false) }
        }
    }

    suspend fun importDictionaryFromUri(uri: Uri): String = withContext(Dispatchers.IO) {
        _uiState.update { it.copy(dictionaryBusy = true) }
        try {
            val fileName = resolveDisplayName(uri) ?: uri.lastPathSegment ?: "dictionary.json"
            val bytes = appContext.contentResolver.openInputStream(uri)
                ?.use { input -> input.readBoundedBytes(MAX_DICTIONARY_PACKAGE_BYTES) }
                ?: error("Could not open this dictionary package.")
            val dictionary = dictionaryRepository.importDictionaryFromBytes(bytes, fileName)
            "Imported ${dictionary.name} with ${dictionary.entriesCount} entries."
        } finally {
            _uiState.update { it.copy(dictionaryBusy = false) }
        }
    }

    private fun resolveDisplayName(uri: Uri): String? =
        appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }

    private fun java.io.InputStream.readBoundedBytes(maxBytes: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read == -1) break
            total += read
            if (total > maxBytes) error("Dictionary packages are limited to ${maxBytes / 1024 / 1024} MB.")
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    suspend fun importDictionaryFromUrl(url: String): String = withContext(Dispatchers.IO) {
        _uiState.update { it.copy(dictionaryBusy = true) }
        try {
            val dictionary = dictionaryRepository.importDictionaryFromUrl(url)
            "Imported ${dictionary.name} with ${dictionary.entriesCount} entries."
        } finally {
            _uiState.update { it.copy(dictionaryBusy = false) }
        }
    }

    private suspend fun importBookFile(file: File) {
        val metadataJson = epubEngine.parseEpub(file.absolutePath)
        val parsed = JSONObject(metadataJson)
        val metadata = parsed.optJSONObject("metadata")
        val totalChapters = parsed.optInt("totalChapters", 0)
        if (totalChapters <= 0) return

        val title = metadata?.optString("title")?.takeIf { it.isNotBlank() }
            ?: file.nameWithoutExtension.replace("_", " ")
        val author = metadata?.optString("author")?.takeIf { it.isNotBlank() } ?: "Unknown"
        val bookId = UUID.randomUUID().toString()
        val coverUri = epubEngine.extractCoverImage(file.absolutePath)
            ?.takeIf { it.isNotBlank() }
            ?.let { if (it.startsWith("data:", ignoreCase = true)) it else "data:image/jpeg;base64,$it" }
            ?.let { persistCoverImage(bookId, it) ?: it }

        val book = Book(
            id = bookId,
            title = title,
            author = author,
            coverUri = coverUri,
            filePath = file.absolutePath,
            fileName = file.name,
            epubIdentifier = metadata?.optString("identifier")?.takeIf { it.isNotBlank() },
            language = metadata?.optString("language")?.takeIf { it.isNotBlank() },
            totalChapters = totalChapters,
            dateAdded = java.time.Instant.now().toString(),
        )
        bookRepository.importBook(book)
    }

    private fun persistCoverImage(bookId: String, dataUri: String): String? {
        val match = Regex(
            pattern = "^data:([^;,]+);base64,(.*)$",
            options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).find(dataUri.trim())
        val mime = match?.groupValues?.getOrNull(1).orEmpty()
        val base64Payload = match?.groupValues?.getOrNull(2)?.replace(Regex("\\s"), "").orEmpty()
        if (base64Payload.isBlank()) return null
        if (!mime.startsWith("image/", ignoreCase = true)) return null
        if (mime.contains("svg", ignoreCase = true)) return null
        if (base64Payload.length > MAX_COVER_BASE64_CHARS) return null

        val extension = when {
            mime.contains("png", ignoreCase = true) -> "png"
            mime.contains("webp", ignoreCase = true) -> "webp"
            mime.contains("gif", ignoreCase = true) -> "gif"
            else -> "jpg"
        }

        return runCatching {
            val coverFile = MiyoStorage.safeChild(MiyoStorage.coversDir(appContext), "$bookId.$extension")
            coverFile.writeBytes(Base64.decode(base64Payload, Base64.DEFAULT))
            Uri.fromFile(coverFile).toString()
        }.getOrNull()
    }

    private companion object {
        const val MAX_COVER_BASE64_CHARS = 14 * 1024 * 1024
        const val MAX_DICTIONARY_PACKAGE_BYTES = 12 * 1024 * 1024
    }
}

private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default
