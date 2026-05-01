package com.miyu.reader.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyu.reader.data.preferences.UserPreferences
import com.miyu.reader.data.repository.BookRepository
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
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val bookCount: Int = 0,
    val dailyGoalMinutes: Int = 30,
    val storageDirectoryUri: String? = null,
    val libraryBytes: Long = 0L,
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
            if (!File(book.filePath).exists()) {
                bookRepository.deleteBook(book.id)
                removed += 1
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
            preferences.setDailyGoalMinutes(30)
        }
    }

    private suspend fun importBookFile(file: File) {
        val metadataJson = epubEngine.parseEpub(file.absolutePath)
        val parsed = JSONObject(metadataJson)
        val metadata = parsed.optJSONObject("metadata")
        val totalChapters = parsed.optInt("totalChapters", 0)
        if (totalChapters <= 0) return

        val coverBase64 = epubEngine.extractCoverImage(file.absolutePath)
            ?.takeIf { it.isNotBlank() }
            ?.let { if (it.startsWith("data:", ignoreCase = true)) it else "data:image/jpeg;base64,$it" }

        val title = metadata?.optString("title")?.takeIf { it.isNotBlank() }
            ?: file.nameWithoutExtension.replace("_", " ")
        val author = metadata?.optString("author")?.takeIf { it.isNotBlank() } ?: "Unknown"

        val book = Book(
            id = UUID.randomUUID().toString(),
            title = title,
            author = author,
            coverUri = coverBase64,
            filePath = file.absolutePath,
            fileName = file.name,
            epubIdentifier = metadata?.optString("identifier")?.takeIf { it.isNotBlank() },
            language = metadata?.optString("language")?.takeIf { it.isNotBlank() },
            totalChapters = totalChapters,
            dateAdded = java.time.Instant.now().toString(),
        )
        bookRepository.importBook(book)
    }
}
