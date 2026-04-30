package com.miyu.reader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyu.reader.data.repository.BookRepository
import com.miyu.reader.domain.model.Book
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val isSelecting: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val bookRepository: BookRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    val readingHistory: StateFlow<List<Book>> = bookRepository.getAllBooks()
        .map { books ->
            books.filter { it.lastReadAt != null }
                .sortedByDescending { it.lastReadAt }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleSelection(bookId: String) {
        _uiState.update { state ->
            val newIds = state.selectedIds.toMutableSet()
            if (newIds.contains(bookId)) newIds.remove(bookId) else newIds.add(bookId)
            state.copy(selectedIds = newIds)
        }
    }

    fun startSelecting(bookId: String) {
        _uiState.update {
            it.copy(isSelecting = true, selectedIds = setOf(bookId))
        }
    }

    fun cancelSelection() {
        _uiState.update { it.copy(isSelecting = false, selectedIds = emptySet()) }
    }

    fun clearSelectedHistory() {
        viewModelScope.launch {
            _uiState.value.selectedIds.forEach { bookId ->
                val book = bookRepository.getBook(bookId) ?: return@forEach
                bookRepository.saveBook(book.copy(lastReadAt = null))
            }
            _uiState.update { it.copy(isSelecting = false, selectedIds = emptySet()) }
        }
    }
}
