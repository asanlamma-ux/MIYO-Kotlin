package com.miyu.reader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyu.reader.data.preferences.UserPreferences
import com.miyu.reader.data.repository.BookRepository
import com.miyu.reader.domain.model.Book
import com.miyu.reader.domain.model.OnlineReadingHistoryEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val isSelecting: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
)

sealed interface ReadingHistoryItem {
    val id: String
    val title: String
    val author: String
    val coverUrl: String?
    val lastReadAt: String

    data class Local(
        val book: Book,
    ) : ReadingHistoryItem {
        override val id: String = book.id
        override val title: String = book.title
        override val author: String = book.author
        override val coverUrl: String? = book.coverUri
        override val lastReadAt: String = book.lastReadAt.orEmpty()
    }

    data class Online(
        val entry: OnlineReadingHistoryEntry,
    ) : ReadingHistoryItem {
        override val id: String = entry.id
        override val title: String = entry.title
        override val author: String = entry.author
        override val coverUrl: String? = entry.coverUrl
        override val lastReadAt: String = entry.lastReadAt
    }
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val preferences: UserPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    val readingHistory: StateFlow<List<ReadingHistoryItem>> = combine(
        bookRepository.getAllBooks(),
        preferences.onlineReadingHistory,
    ) { books, onlineEntries ->
        val local = books
            .filter { it.lastReadAt != null }
            .map { ReadingHistoryItem.Local(it) }
        val online = onlineEntries.map { ReadingHistoryItem.Online(it) }
        (local + online).sortedByDescending { it.lastReadAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleSelection(itemId: String) {
        _uiState.update { state ->
            val newIds = state.selectedIds.toMutableSet()
            if (newIds.contains(itemId)) newIds.remove(itemId) else newIds.add(itemId)
            state.copy(selectedIds = newIds)
        }
    }

    fun startSelecting(itemId: String) {
        _uiState.update {
            it.copy(isSelecting = true, selectedIds = setOf(itemId))
        }
    }

    fun cancelSelection() {
        _uiState.update { it.copy(isSelecting = false, selectedIds = emptySet()) }
    }

    fun clearSelectedHistory() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedIds
            val localItems = readingHistory.value.filterIsInstance<ReadingHistoryItem.Local>().filter { it.id in selectedIds }
            val onlineIds = readingHistory.value.filterIsInstance<ReadingHistoryItem.Online>().map { it.id }.filter { it in selectedIds }.toSet()
            localItems.forEach { item ->
                bookRepository.saveBook(item.book.copy(lastReadAt = null))
            }
            if (onlineIds.isNotEmpty()) {
                preferences.removeOnlineReadingHistory(onlineIds)
            }
            _uiState.update { it.copy(isSelecting = false, selectedIds = emptySet()) }
        }
    }
}
