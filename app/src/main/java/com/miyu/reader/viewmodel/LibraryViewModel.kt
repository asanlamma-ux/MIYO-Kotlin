package com.miyu.reader.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyu.reader.data.repository.BookRepository
import com.miyu.reader.domain.model.*
import com.miyu.reader.engine.bridge.EpubEngineBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val searchQuery: String = "",
    val viewMode: ViewMode = ViewMode.GRID,
    val sortOption: SortOption = SortOption.RECENT,
    val filterOption: FilterOption = FilterOption.ALL,
    val isImporting: Boolean = false,
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

    fun importBookFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) }
            try {
                // Copy the file to internal storage
                val fileName = "book_${System.currentTimeMillis()}.epub"
                val destFile = java.io.File(appContext.filesDir, "books/$fileName")
                destFile.parentFile?.mkdirs()

                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // Parse with native engine
                val metadataJson = epubEngine.parseEpub(destFile.absolutePath)

                // Extract cover
                val coverBase64 = epubEngine.extractCoverImage(destFile.absolutePath)

                // Create Book domain object (simplified — real parser would extract title/author from JSON)
                val book = Book(
                    id = UUID.randomUUID().toString(),
                    title = destFile.nameWithoutExtension.replace("_", " "),
                    author = "Unknown",
                    coverUri = coverBase64,
                    filePath = destFile.absolutePath,
                    fileName = fileName,
                    dateAdded = java.time.Instant.now().toString(),
                )

                bookRepository.importBook(book)
            } catch (e: Exception) {
                // TODO: surface error to UI
                e.printStackTrace()
            } finally {
                _uiState.update { it.copy(isImporting = false) }
            }
        }
    }
}
