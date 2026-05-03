package com.miyu.reader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyu.reader.data.repository.TermRepository
import com.miyu.reader.domain.model.Term
import com.miyu.reader.domain.model.TermGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class TermsUiState(
    val termGroups: List<TermGroup> = emptyList(),
    val searchQuery: String = "",
    val selectedGroupId: String? = null,
    val showCreateInput: Boolean = false,
)

@HiltViewModel
class TermsViewModel @Inject constructor(
    private val termRepository: TermRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TermsUiState())
    val uiState: StateFlow<TermsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            termRepository.getAllGroups().collect { groups ->
                _uiState.update { it.copy(termGroups = groups) }
            }
        }
    }

    val filteredGroups: StateFlow<List<TermGroup>> = _uiState.map { state ->
        val query = state.searchQuery.trim().lowercase()
        if (query.isBlank()) {
            state.termGroups
        } else {
            state.termGroups.filter { group ->
                group.name.lowercase().contains(query) ||
                    group.terms.any { term ->
                        term.originalText.lowercase().contains(query) ||
                            term.correctedText.lowercase().contains(query) ||
                            term.translationText.orEmpty().lowercase().contains(query) ||
                            term.context.orEmpty().lowercase().contains(query)
                    }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSelectedGroupId(id: String?) {
        _uiState.update { it.copy(selectedGroupId = id) }
    }

    fun toggleCreateInput() {
        _uiState.update { it.copy(showCreateInput = !it.showCreateInput) }
    }

    fun createGroup(name: String) {
        viewModelScope.launch {
            val group = TermGroup(
                id = UUID.randomUUID().toString(),
                name = name,
                createdAt = java.time.Instant.now().toString(),
                updatedAt = java.time.Instant.now().toString(),
            )
            termRepository.saveGroup(group)
            _uiState.update { it.copy(showCreateInput = false, selectedGroupId = group.id) }
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            termRepository.deleteGroup(groupId)
            _uiState.update {
                it.copy(selectedGroupId = if (it.selectedGroupId == groupId) null else it.selectedGroupId)
            }
        }
    }

    fun addTerm(groupId: String, originalText: String, correctedText: String) {
        viewModelScope.launch {
            val now = java.time.Instant.now().toString()
            termRepository.addTermToGroup(
                groupId,
                Term(
                    id = UUID.randomUUID().toString(),
                    originalText = originalText.trim(),
                    correctedText = correctedText.trim(),
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }
}
