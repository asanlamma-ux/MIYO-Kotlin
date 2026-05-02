package com.miyu.reader.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyu.reader.data.repository.BookRepository
import com.miyu.reader.domain.model.Book
import com.miyu.reader.domain.model.ReadingPosition
import com.miyu.reader.domain.model.ReadingStatus
import com.miyu.reader.engine.bridge.EpubEngineBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import javax.inject.Inject

data class BookDetailsChapter(
    val index: Int,
    val title: String,
    val wordCount: Int,
)

data class BookDetailsUiState(
    val book: Book? = null,
    val description: String = "",
    val publisher: String? = null,
    val publishDate: String? = null,
    val language: String? = null,
    val subjects: List<String> = emptyList(),
    val chapters: List<BookDetailsChapter> = emptyList(),
    val fileSizeBytes: Long = 0L,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
class BookDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val epubEngine: EpubEngineBridge,
) : ViewModel() {

    private val bookId: String = savedStateHandle["bookId"] ?: ""
    private val _uiState = MutableStateFlow(BookDetailsUiState())
    val uiState: StateFlow<BookDetailsUiState> = _uiState.asStateFlow()

    private var parsedFilePath: String? = null

    init {
        observeBook()
    }

    private fun observeBook() {
        viewModelScope.launch {
            bookRepository.observeBook(bookId).collectLatest { book ->
                if (book == null) {
                    _uiState.update {
                        it.copy(
                            book = null,
                            isLoading = false,
                            errorMessage = "This book is no longer in the library.",
                        )
                    }
                    return@collectLatest
                }

                val fileSizeBytes = withContext(Dispatchers.IO) {
                    runCatching { File(book.filePath).takeIf { it.exists() }?.length() ?: 0L }.getOrDefault(0L)
                }
                _uiState.update {
                    it.copy(
                        book = book,
                        fileSizeBytes = fileSizeBytes,
                        errorMessage = null,
                    )
                }

                if (parsedFilePath != book.filePath) {
                    parsedFilePath = book.filePath
                    loadMetadata(book)
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private suspend fun parseChapterOutline(chaptersJson: JSONArray?): List<BookDetailsChapter> {
        if (chaptersJson == null) return emptyList()
        return buildList {
            for (index in 0 until chaptersJson.length()) {
                val chapter = chaptersJson.optJSONObject(index) ?: continue
                val order = chapter.optInt("order", index).coerceAtLeast(0)
                add(
                    BookDetailsChapter(
                        index = order,
                        title = chapter.optString("title", "Chapter ${order + 1}").ifBlank { "Chapter ${order + 1}" },
                        wordCount = chapter.optInt("wordCount", 0).coerceAtLeast(0),
                    ),
                )
            }
        }.sortedBy { it.index }
    }

    private fun loadMetadata(book: Book) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    val parsed = JSONObject(epubEngine.parseEpub(book.filePath))
                    val metadata = parsed.optJSONObject("metadata")
                    val chapters = parseChapterOutline(parsed.optJSONArray("chapters"))
                    Triple(metadata, chapters, parsed)
                }
            }.onSuccess { (metadata, chapters, _) ->
                _uiState.update {
                    it.copy(
                        description = metadata?.optString("description").orEmpty().trim(),
                        publisher = metadata?.optString("publisher")?.takeIf(String::isNotBlank),
                        publishDate = metadata?.optString("publishDate")?.takeIf(String::isNotBlank),
                        language = metadata?.optString("language")?.takeIf(String::isNotBlank) ?: book.language,
                        subjects = metadata?.optJSONArray("subjects").toStringList(),
                        chapters = chapters,
                        isLoading = false,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Could not load book details.",
                    )
                }
            }
        }
    }

    fun setReadingStatus(status: ReadingStatus) {
        viewModelScope.launch {
            val book = _uiState.value.book ?: return@launch
            bookRepository.saveBook(book.copy(readingStatus = status))
        }
    }

    fun deleteBook() {
        viewModelScope.launch {
            val book = _uiState.value.book ?: return@launch
            bookRepository.deleteBook(book.id)
        }
    }

    fun updateBookCover(coverUri: Uri) {
        viewModelScope.launch {
            val book = _uiState.value.book ?: return@launch
            bookRepository.saveBook(book.copy(coverUri = coverUri.toString()))
        }
    }

    fun markChaptersRead(chapterIndexes: Set<Int>) {
        updateSelectedChapterProgress(chapterIndexes, markRead = true)
    }

    fun markChaptersUnread(chapterIndexes: Set<Int>) {
        updateSelectedChapterProgress(chapterIndexes, markRead = false)
    }

    private fun updateSelectedChapterProgress(chapterIndexes: Set<Int>, markRead: Boolean) {
        viewModelScope.launch {
            val state = _uiState.value
            val book = state.book ?: return@launch
            val totalChapters = (book.totalChapters.takeIf { it > 0 } ?: state.chapters.size).coerceAtLeast(1)
            val maxChapterIndex = totalChapters - 1
            val normalized = chapterIndexes
                .map { it.coerceIn(0, maxChapterIndex) }
                .distinct()
                .sorted()
            if (normalized.isEmpty()) return@launch

            val completedChapters = if (markRead) {
                (normalized.last() + 1).coerceIn(0, totalChapters)
            } else {
                normalized.first().coerceIn(0, totalChapters)
            }
            val targetChapter = completedChapters.coerceIn(0, maxChapterIndex)
            val progress = (completedChapters / totalChapters.toFloat() * 100f).coerceIn(0f, 100f)
            val timestamp = Instant.now().toString()
            bookRepository.saveReadingPosition(
                ReadingPosition(
                    bookId = book.id,
                    chapterIndex = targetChapter,
                    scrollPosition = 0f,
                    chapterScrollPercent = 0f,
                    timestamp = timestamp,
                ),
            )
            bookRepository.updateProgress(book.id, targetChapter, progress)
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val value = optString(index).trim()
                if (value.isNotBlank()) add(value)
            }
        }.distinct()
    }
}
