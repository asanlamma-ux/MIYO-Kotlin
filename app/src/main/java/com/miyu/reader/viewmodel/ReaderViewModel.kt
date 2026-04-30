package com.miyu.reader.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyu.reader.data.preferences.UserPreferences
import com.miyu.reader.data.repository.BookRepository
import com.miyu.reader.domain.model.*
import com.miyu.reader.engine.bridge.EpubEngineBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.miyu.reader.ui.reader.components.SelectionData
import com.miyu.reader.ui.reader.components.HighlightData

data class ReaderUiState(
    val book: Book? = null,
    val chapterHtml: String = "",
    val chapterIndex: Int = 0,
    val totalChapters: Int = 0,
    val showControls: Boolean = false,
    val showChapterDrawer: Boolean = false,
    val showAnnotationsDrawer: Boolean = false,
    val showLayoutPanel: Boolean = false,
    val showStatsModal: Boolean = false,
    val showSearchModal: Boolean = false,
    val isLoading: Boolean = true,
    val css: String = "",
    val selection: SelectionData? = null,
    val translationText: String? = null,
    val dictionaryWord: String? = null,
    val highlights: List<Highlight> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val epubEngine: EpubEngineBridge,
    private val preferences: UserPreferences,
) : ViewModel() {

    private val bookId: String = savedStateHandle["bookId"] ?: ""

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    val readerThemeId: StateFlow<String> = preferences.readerThemeId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "sepia-classic")

    val readingSettings: StateFlow<ReadingSettings> = preferences.readingSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingSettings())

    init {
        loadBook()
    }

    private fun loadBook() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val book = bookRepository.getBook(bookId) ?: return@launch
                val position = bookRepository.getReadingPosition(bookId)
                val chapterIndex = position?.chapterIndex ?: 0

                _uiState.update {
                    it.copy(
                        book = book,
                        chapterIndex = chapterIndex,
                        totalChapters = book.totalChapters,
                    )
                }

                // Load first chapter and CSS
                val css = try { epubEngine.extractStylesheet(book.filePath) } catch (_: Exception) { "" }
                _uiState.update { it.copy(css = css) }

                loadChapter(chapterIndex, book.filePath)

                // Observe annotations
                viewModelScope.launch {
                    bookRepository.getBookmarks(bookId).collect { bookmarks ->
                        _uiState.update { it.copy(bookmarks = bookmarks) }
                    }
                }
                viewModelScope.launch {
                    bookRepository.getHighlights(bookId).collect { highlights ->
                        _uiState.update { it.copy(highlights = highlights) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun loadChapter(index: Int, filePath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val html = epubEngine.renderChapter(filePath, index)
                _uiState.update {
                    it.copy(
                        chapterHtml = html,
                        chapterIndex = index,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun navigateChapter(delta: Int) {
        val state = _uiState.value
        val book = state.book ?: return
        val newIndex = (state.chapterIndex + delta).coerceIn(0, (state.totalChapters - 1).coerceAtLeast(0))
        if (newIndex != state.chapterIndex) {
            loadChapter(newIndex, book.filePath)
            savePosition(newIndex, 0f)
        }
    }

    fun toggleControls() {
        _uiState.update { it.copy(showControls = !it.showControls) }
    }

    fun toggleChapterDrawer() {
        _uiState.update { it.copy(showChapterDrawer = !it.showChapterDrawer) }
    }

    fun toggleAnnotationsDrawer() {
        _uiState.update { it.copy(showAnnotationsDrawer = !it.showAnnotationsDrawer) }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            bookRepository.removeBookmark(bookmark)
        }
    }

    fun deleteHighlight(highlightId: String) {
        viewModelScope.launch {
            bookRepository.removeHighlight(highlightId)
        }
    }

    fun goToChapter(index: Int) {
        val book = _uiState.value.book ?: return
        loadChapter(index, book.filePath)
        savePosition(index, 0f)
        _uiState.update { it.copy(showChapterDrawer = false) }
    }

    private fun savePosition(chapterIndex: Int, scrollPercent: Float) {
        viewModelScope.launch {
            val book = _uiState.value.book ?: return@launch
            bookRepository.saveReadingPosition(
                ReadingPosition(
                    bookId = book.id,
                    chapterIndex = chapterIndex,
                    scrollPosition = 0f,
                    chapterScrollPercent = scrollPercent,
                    timestamp = java.time.Instant.now().toString(),
                )
            )
            // Update overall progress
            if (book.totalChapters > 0) {
                val progress = ((chapterIndex + 1).toFloat() / book.totalChapters * 100f).coerceIn(0f, 100f)
                bookRepository.updateProgress(book.id, chapterIndex, progress)
            }
        }
    }

    fun addBookmark(text: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val book = state.book ?: return@launch
            bookRepository.addBookmark(
                Bookmark(
                    id = java.util.UUID.randomUUID().toString(),
                    bookId = book.id,
                    chapterIndex = state.chapterIndex,
                    position = 0f,
                    text = text.take(200),
                    createdAt = java.time.Instant.now().toString(),
                )
            )
        }
    }

    fun handleSelection(selection: SelectionData?) {
        _uiState.update { it.copy(selection = selection) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selection = null) }
    }

    fun saveHighlight(data: HighlightData, startOffset: Int = 0, endOffset: Int = 0) {
        viewModelScope.launch {
            val state = _uiState.value
            val book = state.book ?: return@launch
            bookRepository.addHighlight(
                Highlight(
                    id = java.util.UUID.randomUUID().toString(),
                    bookId = book.id,
                    chapterIndex = state.chapterIndex,
                    startOffset = startOffset,
                    endOffset = endOffset,
                    text = data.text,
                    color = data.color,
                    textColor = data.textColor,
                    note = data.note,
                    createdAt = java.time.Instant.now().toString(),
                )
            )
        }
    }

    fun showTranslation(text: String) {
        _uiState.update { it.copy(translationText = text) }
    }

    fun clearTranslation() {
        _uiState.update { it.copy(translationText = null) }
    }

    fun showDictionary(word: String) {
        _uiState.update { it.copy(dictionaryWord = word) }
    }

    fun clearDictionary() {
        _uiState.update { it.copy(dictionaryWord = null) }
    }

    fun updateReadingSettings(settings: ReadingSettings) {
        viewModelScope.launch {
            preferences.setReadingSettings(settings)
        }
    }

    fun toggleLayoutPanel() {
        _uiState.update { it.copy(showLayoutPanel = !it.showLayoutPanel) }
    }

    fun toggleStatsModal() {
        _uiState.update { it.copy(showStatsModal = !it.showStatsModal) }
    }

    fun toggleSearchModal() {
        _uiState.update { it.copy(showSearchModal = !it.showSearchModal) }
    }
}
