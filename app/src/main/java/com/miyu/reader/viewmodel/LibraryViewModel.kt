package com.miyu.reader.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyu.reader.data.repository.BookRepository
import com.miyu.reader.domain.model.*
import com.miyu.reader.engine.bridge.EpubEngineBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.util.UUID
import javax.inject.Inject

enum class ImportFeedbackType { SUCCESS, ERROR, WARNING }

data class ImportFeedback(
    val type: ImportFeedbackType,
    val title: String,
    val message: String,
    val bookId: String? = null,
)

data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val searchQuery: String = "",
    val viewMode: ViewMode = ViewMode.GRID,
    val sortOption: SortOption = SortOption.RECENT,
    val filterOption: FilterOption = FilterOption.ALL,
    val isImporting: Boolean = false,
    val importFeedback: ImportFeedback? = null,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val epubEngine: EpubEngineBridge,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val allBooks: StateFlow<List<Book>> = bookRepository.getAllBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            allBooks.collect { books ->
                _uiState.update { it.copy(books = books) }
            }
        }
    }

    val displayedBooks: StateFlow<List<Book>> = _uiState.map { state ->
        var list = state.books

        // Filter
        list = when (state.filterOption) {
            FilterOption.ALL -> list
            FilterOption.UNREAD -> list.filter { it.readingStatus == ReadingStatus.UNREAD }
            FilterOption.READING -> list.filter { it.readingStatus == ReadingStatus.READING }
            FilterOption.FINISHED -> list.filter { it.readingStatus == ReadingStatus.FINISHED }
        }

        // Search
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) || it.author.lowercase().contains(q)
            }
        }

        // Sort
        list = when (state.sortOption) {
            SortOption.RECENT -> list.sortedByDescending { it.lastReadAt ?: it.dateAdded }
            SortOption.TITLE -> list.sortedBy { it.title.lowercase() }
            SortOption.AUTHOR -> list.sortedBy { it.author.lowercase() }
            SortOption.PROGRESS -> list.sortedByDescending { it.progress }
            SortOption.DATE_ADDED -> list.sortedByDescending { it.dateAdded }
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleViewMode() {
        _uiState.update {
            it.copy(viewMode = if (it.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID)
        }
    }

    fun setSortOption(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }

    fun setFilterOption(option: FilterOption) {
        _uiState.update { it.copy(filterOption = option) }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            bookRepository.deleteBook(bookId)
        }
    }

    fun updateBookStatus(bookId: String, status: ReadingStatus) {
        viewModelScope.launch {
            val book = bookRepository.getBook(bookId) ?: return@launch
            bookRepository.saveBook(book.copy(readingStatus = status))
        }
    }

    fun clearImportFeedback() {
        _uiState.update { it.copy(importFeedback = null) }
    }

    fun importBookFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importFeedback = null) }
            try {
                // Copy the file to internal storage
                val originalName = resolveDisplayName(uri) ?: "book_${System.currentTimeMillis()}.epub"
                val fileName = sanitizeFileName(originalName)
                val destFile = File(appContext.filesDir, "books/$fileName")
                destFile.parentFile?.mkdirs()

                val inputStream = appContext.contentResolver.openInputStream(uri)
                    ?: error("Could not open the selected EPUB.")
                inputStream.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // Parse with native engine
                val metadataJson = epubEngine.parseEpub(destFile.absolutePath)
                val parsed = JSONObject(metadataJson)
                val metadata = parsed.optJSONObject("metadata")
                val totalChapters = parsed.optInt("totalChapters", 0)
                if (totalChapters <= 0) {
                    error("The EPUB imported, but no readable chapters were found.")
                }

                val title = metadata?.optString("title")
                    ?.takeIf { it.isNotBlank() }
                    ?: destFile.nameWithoutExtension.replace("_", " ")
                val author = metadata?.optString("author")
                    ?.takeIf { it.isNotBlank() }
                    ?: "Unknown"
                val bookId = UUID.randomUUID().toString()
                val coverUri = epubEngine.extractCoverImage(destFile.absolutePath)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { if (it.startsWith("data:", ignoreCase = true)) it else "data:image/jpeg;base64,$it" }
                    ?.let { persistCoverImage(bookId, it) ?: it }

                val book = Book(
                    id = bookId,
                    title = title,
                    author = author,
                    coverUri = coverUri,
                    filePath = destFile.absolutePath,
                    fileName = fileName,
                    epubIdentifier = metadata?.optString("identifier")?.takeIf { it.isNotBlank() },
                    language = metadata?.optString("language")?.takeIf { it.isNotBlank() },
                    totalChapters = totalChapters,
                    dateAdded = java.time.Instant.now().toString(),
                )

                bookRepository.importBook(book)
                _uiState.update {
                    it.copy(
                        importFeedback = ImportFeedback(
                            type = ImportFeedbackType.SUCCESS,
                            title = "Import complete",
                            message = "\"${book.title}\" is ready with $totalChapters chapter${if (totalChapters == 1) "" else "s"}.",
                            bookId = book.id,
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        importFeedback = ImportFeedback(
                            type = ImportFeedbackType.ERROR,
                            title = "Import failed",
                            message = e.message ?: "The selected EPUB could not be imported.",
                        )
                    )
                }
            } finally {
                _uiState.update { it.copy(isImporting = false) }
            }
        }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
    }

    private fun sanitizeFileName(fileName: String): String {
        val cleaned = fileName.replace(Regex("""[<>:"/\\|?*]"""), "_").trim()
        val withFallback = cleaned.ifBlank { "book_${System.currentTimeMillis()}.epub" }
        return if (withFallback.endsWith(".epub", ignoreCase = true)) withFallback else "$withFallback.epub"
    }

    private fun persistCoverImage(bookId: String, dataUri: String): String? {
        val match = Regex(
            pattern = "^data:([^;,]+);base64,(.*)$",
            options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).find(dataUri.trim())
        val mime = match?.groupValues?.getOrNull(1).orEmpty()
        val base64Payload = match?.groupValues?.getOrNull(2)?.replace(Regex("\\s"), "").orEmpty()
        if (base64Payload.isBlank()) return null

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
}
