package com.miyu.reader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyu.reader.data.repository.OpdsRepository
import com.miyu.reader.domain.model.OpdsCatalog
import com.miyu.reader.domain.model.OpdsEntry
import com.miyu.reader.domain.model.OpdsFeed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OpdsCatalogUiState(
    val catalogs: List<OpdsCatalog> = emptyList(),
    val activeCatalogId: String? = null,
    val feed: OpdsFeed? = null,
    val feedStack: List<String> = emptyList(),
    val query: String = "",
    val catalogUrl: String = "",
    val loading: Boolean = false,
    val savingCatalog: Boolean = false,
    val importingEntryId: String? = null,
    val error: String? = null,
)

@HiltViewModel
class OpdsCatalogViewModel @Inject constructor(
    private val opdsRepository: OpdsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OpdsCatalogUiState())
    val uiState: StateFlow<OpdsCatalogUiState> = _uiState.asStateFlow()

    val filteredEntries: List<OpdsEntry>
        get() {
            val state = _uiState.value
            val entries = state.feed?.entries.orEmpty()
            val query = state.query.trim().lowercase()
            if (query.isBlank()) return entries
            return entries.filter { entry ->
                entry.title.lowercase().contains(query) ||
                    entry.author.lowercase().contains(query) ||
                    entry.summary.orEmpty().lowercase().contains(query)
            }
        }

    fun open() {
        viewModelScope.launch {
            val catalogs = opdsRepository.getSavedCatalogs()
            _uiState.update { it.copy(catalogs = catalogs) }
            if (_uiState.value.feed == null && catalogs.isNotEmpty()) {
                loadFeed(catalogs.first().url, pushHistory = false, catalogId = catalogs.first().id)
            }
        }
    }

    fun setQuery(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    fun setCatalogUrl(value: String) {
        _uiState.update { it.copy(catalogUrl = value) }
    }

    fun addCatalog() {
        val url = _uiState.value.catalogUrl.trim()
        if (url.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(savingCatalog = true, error = null) }
            runCatching {
                opdsRepository.addCatalog(url)
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        catalogs = result.catalogs,
                        catalogUrl = "",
                        activeCatalogId = result.addedCatalog.id,
                        error = if (result.alreadySaved) "That catalog was already saved. Opened the existing entry." else null,
                    )
                }
                loadFeed(result.addedCatalog.url, pushHistory = false, catalogId = result.addedCatalog.id)
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.message ?: "Could not save this catalog.") }
            }
            _uiState.update { it.copy(savingCatalog = false) }
        }
    }

    fun removeCatalog(catalogId: String) {
        viewModelScope.launch {
            val catalogs = opdsRepository.removeCatalog(catalogId)
            _uiState.update { it.copy(catalogs = catalogs) }
            if (_uiState.value.activeCatalogId == catalogId) {
                catalogs.firstOrNull()?.let { loadFeed(it.url, pushHistory = false, catalogId = it.id) }
            }
        }
    }

    fun loadFeed(url: String, pushHistory: Boolean = true, catalogId: String? = null) {
        viewModelScope.launch {
            val current = _uiState.value.feed
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { opdsRepository.fetchFeed(url) }
                .onSuccess { feed ->
                    _uiState.update { state ->
                        state.copy(
                            feed = feed,
                            activeCatalogId = catalogId ?: state.activeCatalogId,
                            feedStack = if (pushHistory && current != null && current.url != feed.url) {
                                state.feedStack + current.url
                            } else {
                                state.feedStack
                            },
                            loading = false,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = error.message ?: "Failed to load catalog.",
                        )
                    }
                }
        }
    }

    fun goBackFeed() {
        val previous = _uiState.value.feedStack.lastOrNull() ?: return
        _uiState.update { it.copy(feedStack = it.feedStack.dropLast(1)) }
        loadFeed(previous, pushHistory = false)
    }

    fun markImporting(entryId: String?) {
        _uiState.update { it.copy(importingEntryId = entryId) }
    }
}
