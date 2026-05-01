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

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        appContext.cacheDir.listFiles()?.forEach { child ->
            if (child.isDirectory) child.deleteRecursively() else child.delete()
        }
    }

    suspend fun rescanLibrary(): RescanSummary = withContext(Dispatchers.IO) {
        val existingBooks = bookRepository.getAllBooksOnce()
        val booksDir = File(appContext.filesDir, "books").apply { mkdirs() }
        val epubFiles = booksDir.listFiles { file -> file.isFile && file.extension.equals("epub", ignoreCase = true) }
            ?.toList()
            ?: emptyList()

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
            preferences.setInitialSetupComplete(false)
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
            val coverDir = File(appContext.filesDir, "covers").apply { mkdirs() }
            val coverFile = File(coverDir, "$bookId.$extension")
            coverFile.writeBytes(Base64.decode(base64Payload, Base64.DEFAULT))
            Uri.fromFile(coverFile).toString()
        }.getOrNull()
    }

    private companion object {
        const val MAX_COVER_BASE64_CHARS = 14 * 1024 * 1024
        const val MAX_DICTIONARY_PACKAGE_BYTES = 12 * 1024 * 1024
    }
}
