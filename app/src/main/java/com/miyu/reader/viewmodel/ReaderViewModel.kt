package com.miyu.reader.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyu.reader.data.preferences.UserPreferences
import com.miyu.reader.data.repository.BookRepository
import com.miyu.reader.data.repository.DictionaryRepository
import com.miyu.reader.data.repository.TermRepository
import com.miyu.reader.data.repository.TranslationRepository
import com.miyu.reader.domain.model.*
import com.miyu.reader.engine.bridge.EpubEngineBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import com.miyu.reader.ui.theme.DefaultReaderThemeId
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.miyu.reader.ui.reader.components.SelectionData
import com.miyu.reader.ui.reader.components.HighlightData
import com.miyu.reader.ui.reader.components.SearchResult
import org.json.JSONArray

data class ReaderUiState(
    val book: Book? = null,
    val chapterHtml: String = "",
    val chapterIndex: Int = 0,
    val renderedChapterIndex: Int = 0,
    val chapterScrollPercent: Float = 0f,
    val continuousLoadedThroughIndex: Int = 0,
    val totalChapters: Int = 0,
    val showControls: Boolean = false,
    val showChapterDrawer: Boolean = false,
    val showAnnotationsDrawer: Boolean = false,
    val showLayoutPanel: Boolean = false,
    val showStatsModal: Boolean = false,
    val showSearchModal: Boolean = false,
    val showThemePicker: Boolean = false,
    val isLoading: Boolean = true,
    val selection: SelectionData? = null,
    val addTermText: String? = null,
    val activeTermGroups: List<TermGroup> = emptyList(),
    val translationText: String? = null,
    val translationStatus: String? = null,
    val dictionaryWord: String? = null,
    val dictionaryLoading: Boolean = false,
    val dictionaryResult: DictionaryLookupResult? = null,
    val downloadedDictionaryCount: Int = 0,
    val highlights: List<Highlight> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val errorMessage: String? = null,
    val selectedTermDetail: ReaderTermDetail? = null,
    val pendingChapterAppend: PendingChapterAppend? = null,
)

data class PendingChapterAppend(
    val id: Long,
    val chapterIndex: Int,
    val bodyHtml: String,
)

data class ReaderTermDetail(
    val originalText: String,
    val correctedText: String,
    val translationText: String? = null,
    val context: String? = null,
    val imageUri: String? = null,
    val groupName: String? = null,
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val termRepository: TermRepository,
    private val dictionaryRepository: DictionaryRepository,
    private val translationRepository: TranslationRepository,
    private val epubEngine: EpubEngineBridge,
    private val preferences: UserPreferences,
) : ViewModel() {

    private val bookId: String = savedStateHandle["bookId"] ?: ""

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()
    private var chapterAppendInFlight = false

    val readerThemeId: StateFlow<String> = preferences.readerThemeId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DefaultReaderThemeId)

    val readingSettings: StateFlow<ReadingSettings> = preferences.readingSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingSettings())

    val typography: StateFlow<TypographySettings> = preferences.typography
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TypographySettings())

    init {
        loadBook()
    }

    private fun loadBook() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val book = bookRepository.getBook(bookId)
                if (book == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "This book is no longer in the library.",
                        )
                    }
                    return@launch
                }
                val position = bookRepository.getReadingPosition(bookId)
                val chapterIndex = position?.chapterIndex ?: 0
                val chapterScrollPercent = (
                    position?.chapterScrollPercent ?: position?.scrollPosition ?: 0f
                ).coerceIn(0f, 1f)

                _uiState.update {
                    it.copy(
                        book = book,
                        chapterIndex = chapterIndex,
                        renderedChapterIndex = chapterIndex,
                        chapterScrollPercent = chapterScrollPercent,
                        continuousLoadedThroughIndex = chapterIndex,
                        totalChapters = book.totalChapters,
                        activeTermGroups = termRepository.getAllGroupsOnce(),
                        errorMessage = null,
                    )
                }

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
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Could not open this book.",
                    )
                }
            }
        }
    }

    private fun loadChapter(index: Int, filePath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val book = _uiState.value.book
                val replacements = if (book != null) {
                    termRepository.getReplacementMarkupForBook(book.id)
                } else {
                    emptyMap()
                }
                val html = epubEngine.renderChapter(filePath, index, replacements)
                if (html.isBlank()) {
                    _uiState.update {
                        it.copy(
                            chapterHtml = "",
                            chapterIndex = index,
                            renderedChapterIndex = index,
                            continuousLoadedThroughIndex = index,
                            pendingChapterAppend = null,
                            isLoading = false,
                            errorMessage = "This chapter imported without readable content.",
                        )
                    }
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        chapterHtml = html,
                        chapterIndex = index,
                        renderedChapterIndex = index,
                        chapterScrollPercent = _uiState.value.chapterScrollPercent.takeIf { _uiState.value.chapterIndex == index } ?: 0f,
                        continuousLoadedThroughIndex = index,
                        pendingChapterAppend = null,
                        activeTermGroups = termRepository.getAllGroupsOnce(),
                        isLoading = false,
                        errorMessage = null,
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Could not render this chapter.",
                    )
                }
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
            _uiState.update { it.copy(chapterScrollPercent = 0f) }
        }
    }

    fun appendNextChapter() {
        if (chapterAppendInFlight) return
        viewModelScope.launch {
            chapterAppendInFlight = true
            val state = _uiState.value
            val book = state.book ?: run {
                chapterAppendInFlight = false
                return@launch
            }
            val nextIndex = state.continuousLoadedThroughIndex + 1
            if (nextIndex >= state.totalChapters) {
                chapterAppendInFlight = false
                return@launch
            }

            try {
                val replacements = termRepository.getReplacementMarkupForBook(book.id)
                val html = epubEngine.renderChapter(book.filePath, nextIndex, replacements)
                val bodyHtml = extractBodyContent(html).takeIf { it.isNotBlank() } ?: run {
                    chapterAppendInFlight = false
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        continuousLoadedThroughIndex = nextIndex,
                        pendingChapterAppend = PendingChapterAppend(
                            id = System.nanoTime(),
                            chapterIndex = nextIndex,
                            bodyHtml = bodyHtml,
                        ),
                        errorMessage = null,
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Could not append the next chapter.")
                }
            } finally {
                chapterAppendInFlight = false
            }
        }
    }

    fun consumePendingChapterAppend(id: Long) {
        _uiState.update { state ->
            if (state.pendingChapterAppend?.id == id) {
                state.copy(pendingChapterAppend = null)
            } else {
                state
            }
        }
    }

    fun toggleControls() {
        _uiState.update { it.copy(showControls = !it.showControls) }
    }

    fun handleReaderTap() {
        _uiState.update { state ->
            if (state.selection != null) state.copy(selection = null) else state.copy(showControls = !state.showControls)
        }
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
        _uiState.update { it.copy(showChapterDrawer = false, chapterScrollPercent = 0f) }
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
                val progress = ((chapterIndex + scrollPercent.coerceIn(0f, 1f)) / book.totalChapters * 100f)
                    .coerceIn(0f, 100f)
                bookRepository.updateProgress(book.id, chapterIndex, progress)
            }
        }
    }

    fun updateScrollProgress(chapterScrollPercent: Float) {
        updateChapterScrollProgress(_uiState.value.chapterIndex, chapterScrollPercent)
    }

    fun updateChapterScrollProgress(chapterIndex: Int, chapterScrollPercent: Float) {
        viewModelScope.launch {
            val state = _uiState.value
            val book = state.book ?: return@launch
            val safeChapterIndex = chapterIndex.coerceIn(0, (state.totalChapters - 1).coerceAtLeast(0))
            val safeChapterPercent = chapterScrollPercent.coerceIn(0f, 1f)
            _uiState.update { current ->
                current.copy(
                    chapterIndex = safeChapterIndex,
                    chapterScrollPercent = safeChapterPercent,
                )
            }
            val overallProgress = if (state.totalChapters > 0) {
                ((safeChapterIndex + safeChapterPercent) / state.totalChapters * 100f).coerceIn(0f, 100f)
            } else {
                safeChapterPercent * 100f
            }
            bookRepository.saveReadingPosition(
                ReadingPosition(
                    bookId = book.id,
                    chapterIndex = safeChapterIndex,
                    scrollPosition = safeChapterPercent,
                    chapterScrollPercent = safeChapterPercent,
                    timestamp = java.time.Instant.now().toString(),
                )
            )
            bookRepository.updateProgress(book.id, safeChapterIndex, overallProgress)
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

    fun showAddTerm(text: String) {
        _uiState.update { it.copy(addTermText = text, selection = null) }
    }

    fun clearAddTerm() {
        _uiState.update { it.copy(addTermText = null) }
    }

    fun saveTerm(
        originalText: String,
        correctedText: String,
        translationText: String?,
        context: String?,
        groupId: String,
        imageUri: String?,
    ) {
        viewModelScope.launch {
            val state = _uiState.value
            val book = state.book ?: return@launch
            val now = java.time.Instant.now().toString()
            termRepository.addTermToGroupAndApplyToBook(
                groupId = groupId,
                term = Term(
                    id = java.util.UUID.randomUUID().toString(),
                    originalText = originalText.trim(),
                    correctedText = correctedText.trim(),
                    translationText = translationText?.trim()?.takeIf { it.isNotBlank() },
                    context = context?.trim()?.takeIf { it.isNotBlank() },
                    imageUri = imageUri?.trim()?.takeIf { it.isNotBlank() },
                    createdAt = now,
                    updatedAt = now,
                ),
                bookId = book.id,
            )
            clearAddTerm()
            loadChapter(state.chapterIndex, book.filePath)
        }
    }

    fun createTermGroupAndSave(
        groupName: String,
        originalText: String,
        correctedText: String,
        translationText: String?,
        context: String?,
        imageUri: String?,
    ) {
        viewModelScope.launch {
            val state = _uiState.value
            val book = state.book ?: return@launch
            val now = java.time.Instant.now().toString()
            val group = TermGroup(
                id = java.util.UUID.randomUUID().toString(),
                name = groupName.trim(),
                appliedToBooks = listOf(book.id),
                createdAt = now,
                updatedAt = now,
            )
            termRepository.saveGroup(group)
            termRepository.addTermToGroupAndApplyToBook(
                groupId = group.id,
                term = Term(
                    id = java.util.UUID.randomUUID().toString(),
                    originalText = originalText.trim(),
                    correctedText = correctedText.trim(),
                    translationText = translationText?.trim()?.takeIf { it.isNotBlank() },
                    context = context?.trim()?.takeIf { it.isNotBlank() },
                    imageUri = imageUri?.trim()?.takeIf { it.isNotBlank() },
                    createdAt = now,
                    updatedAt = now,
                ),
                bookId = book.id,
            )
            clearAddTerm()
            loadChapter(state.chapterIndex, book.filePath)
        }
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
        _uiState.update {
            it.copy(
                translationText = text,
                translationStatus = "Translating...",
                selection = null,
            )
        }
        viewModelScope.launch {
            val result = translationRepository.translateToEnglish(text)
            _uiState.update {
                it.copy(
                    translationStatus = result.statusMessage,
                )
            }
        }
    }

    fun clearTranslation() {
        _uiState.update { it.copy(translationText = null, translationStatus = null) }
    }

    fun showDictionary(word: String) {
        val lookupWord = word.trim().split(Regex("\\s+")).firstOrNull()?.take(80)?.trim().orEmpty()
        if (lookupWord.isBlank()) return
        _uiState.update {
            it.copy(
                dictionaryWord = lookupWord,
                dictionaryLoading = true,
                dictionaryResult = null,
                selection = null,
            )
        }
        viewModelScope.launch {
            val downloadedCount = dictionaryRepository.getDownloadedDictionaryCount()
            val result = dictionaryRepository.lookupWord(lookupWord)
            _uiState.update {
                it.copy(
                    dictionaryLoading = false,
                    dictionaryResult = result,
                    downloadedDictionaryCount = downloadedCount,
                )
            }
        }
    }

    fun clearDictionary() {
        _uiState.update {
            it.copy(
                dictionaryWord = null,
                dictionaryLoading = false,
                dictionaryResult = null,
            )
        }
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

    suspend fun searchInBook(query: String, fullBook: Boolean): List<SearchResult> {
        val state = _uiState.value
        val book = state.book ?: return emptyList()
        if (query.length < 2) return emptyList()
        return runCatching {
            val json = epubEngine.searchInBook(book.filePath, query)
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val chapterOrder = item.optInt("chapterIndex", -1)
                    val chapterIndex = if (chapterOrder >= 0) chapterOrder else findChapterIndex(item.optString("chapterId"))
                    if (fullBook || chapterIndex == state.chapterIndex) {
                        val excerpt = item.optString("excerpt")
                        val matchIndex = excerpt.indexOf(query, ignoreCase = true).coerceAtLeast(0)
                        val matchEnd = (matchIndex + query.length).coerceAtMost(excerpt.length)
                        add(
                            SearchResult(
                                chapterIndex = chapterIndex.coerceAtLeast(0),
                                chapterTitle = item.optString("chapterTitle", "Chapter ${chapterIndex + 1}"),
                                excerptBefore = excerpt.substring(0, matchIndex),
                                excerptMatch = excerpt.substring(matchIndex, matchEnd),
                                excerptAfter = excerpt.substring(matchEnd),
                            )
                        )
                    }
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun findChapterIndex(chapterId: String): Int {
        val suffixNumber = Regex("(\\d+)").findAll(chapterId).lastOrNull()?.value?.toIntOrNull()
        return suffixNumber?.minus(1) ?: _uiState.value.chapterIndex
    }

    fun toggleThemePicker() {
        _uiState.update { it.copy(showThemePicker = !it.showThemePicker) }
    }

    fun setReaderThemeId(id: String) {
        viewModelScope.launch { preferences.setReaderThemeId(id) }
    }

    fun showTermDetail(detail: ReaderTermDetail) {
        _uiState.update { it.copy(selectedTermDetail = detail) }
    }

    fun clearTermDetail() {
        _uiState.update { it.copy(selectedTermDetail = null) }
    }

    private fun extractBodyContent(html: String): String {
        val bodyMatch = Regex("<body[^>]*>([\\s\\S]*?)</body>", RegexOption.IGNORE_CASE).find(html)
        return bodyMatch?.groupValues?.getOrNull(1) ?: html
    }
}
