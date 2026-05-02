package com.miyu.reader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyu.reader.data.preferences.UserPreferences
import com.miyu.reader.data.repository.NovelSourcePluginRegistry
import com.miyu.reader.domain.model.GeneratedOnlineNovelEpub
import com.miyu.reader.domain.model.NovelSourceInstallState
import com.miyu.reader.domain.model.NovelSourceKind
import com.miyu.reader.domain.model.NovelSourcePluginItem
import com.miyu.reader.domain.model.OnlineNovelSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BrowseSourceTab(val label: String) {
    INSTALLED("Installed"),
    AVAILABLE("Available"),
}

enum class GlobalSourceFilter(val label: String) {
    PINNED("Pinned"),
    ALL("All"),
}

data class GlobalSearchSourceResult(
    val source: NovelSourcePluginItem,
    val novels: List<OnlineNovelSummary> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

data class BrowseUiState(
    val installedSources: List<NovelSourcePluginItem> = emptyList(),
    val availableSources: List<NovelSourcePluginItem> = emptyList(),
    val selectedTab: BrowseSourceTab = BrowseSourceTab.INSTALLED,
    val sourceQuery: String = "",
    val novelQuery: String = "",
    val pinnedSourceIds: Set<String> = emptySet(),
    val languageFilter: Set<String> = emptySet(),
    val lastUsedSourceId: String? = null,
    val repositoryUrls: Set<String> = emptySet(),
    val browseSearchHistory: List<String> = emptyList(),
    val downloadConcurrency: Int = UserPreferences.DEFAULT_DOWNLOAD_CONCURRENCY,
    val results: List<OnlineNovelSummary> = emptyList(),
    val searchedSourceId: String? = null,
    val globalSearchQuery: String = "",
    val globalSourceFilter: GlobalSourceFilter = GlobalSourceFilter.PINNED,
    val globalOnlyHasResults: Boolean = false,
    val globalSearchResults: List<GlobalSearchSourceResult> = emptyList(),
    val globalSearchProgress: Int = 0,
    val globalSearchTotal: Int = 0,
    val page: Int = 1,
    val hasMore: Boolean = false,
    val loading: Boolean = false,
    val downloadingKey: String? = null,
    val generatedEpub: GeneratedOnlineNovelEpub? = null,
    val error: String? = null,
) {
    val visibleSources: List<NovelSourcePluginItem>
        get() {
            val pool = when (selectedTab) {
                BrowseSourceTab.INSTALLED -> installedSources
                BrowseSourceTab.AVAILABLE -> availableSources
            }
            val languageFiltered = if (languageFilter.isEmpty()) {
                pool
            } else {
                pool.filter { it.language in languageFilter }
            }
            val queried = if (sourceQuery.isBlank()) {
                languageFiltered
            } else {
                val q = sourceQuery.trim()
                languageFiltered.filter {
                    it.name.contains(q, ignoreCase = true) ||
                        it.site.contains(q, ignoreCase = true) ||
                        it.language.contains(q, ignoreCase = true)
                }
            }
            return queried.sortedWith(
                compareByDescending<NovelSourcePluginItem> { it.id in pinnedSourceIds }
                    .thenByDescending { it.id == lastUsedSourceId }
                    .thenBy { it.name.lowercase() },
            )
        }

    val availableLanguages: List<String>
        get() = (installedSources + availableSources)
            .map { it.language }
            .distinct()
            .sorted()

    val recommendedSources: List<NovelSourcePluginItem>
        get() = installedSources
            .sortedWith(
                compareByDescending<NovelSourcePluginItem> { recommendationScore(it) }
                    .thenBy { it.name.lowercase() },
            )
            .take(4)

    val recommendedQueries: List<String>
        get() = listOf("Fantasy", "Action", "Adventure", "Drama", "Romance", "Isekai", "Cultivation", "Regression")

    val visibleGlobalSearchResults: List<GlobalSearchSourceResult>
        get() = if (globalOnlyHasResults) {
            globalSearchResults.filter { it.novels.isNotEmpty() || it.loading || it.error != null }
        } else {
            globalSearchResults
        }

    private fun recommendationScore(source: NovelSourcePluginItem): Int {
        var score = 0
        if (source.id == lastUsedSourceId) score += 80
        if (source.id in pinnedSourceIds) score += 60
        if (!source.requiresVerification) score += 30
        if (source.kind == NovelSourceKind.BUILT_IN) score += 20
        if (languageFilter.isNotEmpty() && source.language in languageFilter) score += 15
        if (source.hasUpdate) score += 10
        return score
    }
}

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val sourceRegistry: NovelSourcePluginRegistry,
    private val preferences: UserPreferences,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        BrowseUiState(
            installedSources = sourceRegistry.installedSources,
            availableSources = sourceRegistry.availableSources,
        ),
    )
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.sourcePinnedIds.collect { pinned ->
                _uiState.update { it.copy(pinnedSourceIds = pinned) }
            }
        }
        viewModelScope.launch {
            preferences.sourceLanguageFilter.collect { languages ->
                _uiState.update { it.copy(languageFilter = languages) }
            }
        }
        viewModelScope.launch {
            preferences.lastUsedSourceId.collect { sourceId ->
                _uiState.update { it.copy(lastUsedSourceId = sourceId) }
            }
        }
        viewModelScope.launch {
            preferences.sourceRepositoryUrls.collect { urls ->
                _uiState.update { it.copy(repositoryUrls = urls) }
            }
        }
        viewModelScope.launch {
            preferences.browseSearchHistory.collect { history ->
                _uiState.update { it.copy(browseSearchHistory = history) }
            }
        }
        viewModelScope.launch {
            preferences.downloadConcurrency.collect { concurrency ->
                _uiState.update { it.copy(downloadConcurrency = concurrency) }
            }
        }
    }

    fun setTab(tab: BrowseSourceTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setSourceQuery(query: String) {
        _uiState.update { it.copy(sourceQuery = query) }
    }

    fun setNovelQuery(query: String) {
        _uiState.update { it.copy(novelQuery = query) }
    }

    fun setGlobalSearchQuery(query: String) {
        _uiState.update { it.copy(globalSearchQuery = query) }
    }

    fun setGlobalSourceFilter(filter: GlobalSourceFilter) {
        _uiState.update { it.copy(globalSourceFilter = filter) }
        val query = _uiState.value.globalSearchQuery
        if (query.isNotBlank() && !_uiState.value.loading) {
            searchAllSources(query)
        }
    }

    fun toggleGlobalOnlyHasResults() {
        _uiState.update { it.copy(globalOnlyHasResults = !it.globalOnlyHasResults) }
    }

    fun removeBrowseSearchQuery(query: String) {
        viewModelScope.launch { preferences.removeBrowseSearchQuery(query) }
    }

    fun clearBrowseSearchHistory() {
        viewModelScope.launch { preferences.clearBrowseSearchHistory() }
    }

    fun source(sourceId: String): NovelSourcePluginItem? =
        sourceRegistry.source(sourceId)

    fun recordSourceOpened(sourceId: String) {
        viewModelScope.launch {
            preferences.setLastUsedSourceId(sourceId)
        }
    }

    fun togglePinSource(sourceId: String) {
        viewModelScope.launch {
            val next = _uiState.value.pinnedSourceIds.toMutableSet()
            if (!next.add(sourceId)) next.remove(sourceId)
            preferences.setSourcePinnedIds(next)
        }
    }

    fun toggleLanguageFilter(language: String) {
        viewModelScope.launch {
            val next = _uiState.value.languageFilter.toMutableSet()
            if (!next.add(language)) next.remove(language)
            preferences.setSourceLanguageFilter(next)
        }
    }

    fun clearLanguageFilter() {
        viewModelScope.launch {
            preferences.setSourceLanguageFilter(emptySet())
        }
    }

    fun addRepositoryUrl(url: String) {
        val cleanUrl = url.trim()
        if (!cleanUrl.startsWith("https://", ignoreCase = true)) {
            _uiState.update { it.copy(error = "Repository URLs must use HTTPS.") }
            return
        }
        viewModelScope.launch {
            preferences.setSourceRepositoryUrls(_uiState.value.repositoryUrls + cleanUrl)
            _uiState.update { it.copy(error = null) }
        }
    }

    fun removeRepositoryUrl(url: String) {
        viewModelScope.launch {
            preferences.setSourceRepositoryUrls(_uiState.value.repositoryUrls - url)
        }
    }

    fun searchSource(sourceId: String, loadMore: Boolean = false) {
        val source = sourceRegistry.source(sourceId)
        if (source == null) {
            _uiState.update { it.copy(error = "Source not found.") }
            return
        }
        if (source.installState != NovelSourceInstallState.INSTALLED) {
            _uiState.update { it.copy(error = "${source.name} is not installed yet.") }
            return
        }
        if (source.requiresVerification) {
            _uiState.update {
                it.copy(
                    error = "${source.name} needs in-app verification before parser requests can run. Open the verifier from the source page.",
                    searchedSourceId = sourceId,
                    results = emptyList(),
                    generatedEpub = null,
                    hasMore = false,
                )
            }
            return
        }

        val state = _uiState.value
        val nextPage = if (loadMore && state.searchedSourceId == sourceId) state.page + 1 else 1
        viewModelScope.launch {
            if (!loadMore) preferences.recordBrowseSearchQuery(state.novelQuery)
            _uiState.update {
                it.copy(
                    loading = true,
                    error = null,
                    generatedEpub = null,
                    searchedSourceId = sourceId,
                    page = nextPage,
                    results = if (nextPage == 1) emptyList() else it.results,
                )
            }
            runCatching {
                sourceRegistry.search(sourceId, state.novelQuery, nextPage)
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        results = if (nextPage == 1) {
                            result.items
                        } else {
                            (it.results + result.items).distinctBy { item -> "${item.providerId}:${item.path}" }
                        },
                        page = result.page,
                        hasMore = result.hasMore,
                        error = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        hasMore = false,
                        error = error.message ?: "Source search failed.",
                    )
                }
            }
        }
    }

    fun searchAllSources(query: String = _uiState.value.globalSearchQuery) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return
        val state = _uiState.value
        val installed = state.installedSources.filter {
            it.installState == NovelSourceInstallState.INSTALLED && !it.requiresVerification
        }
        val filtered = when (state.globalSourceFilter) {
            GlobalSourceFilter.PINNED -> installed.filter { it.id in state.pinnedSourceIds }.takeIf { it.isNotEmpty() } ?: installed
            GlobalSourceFilter.ALL -> installed
        }

        viewModelScope.launch {
            preferences.recordBrowseSearchQuery(cleanQuery)
            _uiState.update {
                it.copy(
                    globalSearchQuery = cleanQuery,
                    loading = true,
                    error = null,
                    globalSearchProgress = 0,
                    globalSearchTotal = filtered.size,
                    globalSearchResults = filtered.map { source -> GlobalSearchSourceResult(source = source, loading = true) },
                )
            }
            filtered.forEachIndexed { index, source ->
                runCatching {
                    sourceRegistry.search(source.id, cleanQuery, page = 1).items
                }.onSuccess { novels ->
                    _uiState.update { current ->
                        current.copy(
                            globalSearchProgress = index + 1,
                            globalSearchResults = current.globalSearchResults.map {
                                if (it.source.id == source.id) {
                                    it.copy(novels = novels.take(10), loading = false, error = null)
                                } else {
                                    it
                                }
                            },
                        )
                    }
                }.onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            globalSearchProgress = index + 1,
                            globalSearchResults = current.globalSearchResults.map {
                                if (it.source.id == source.id) {
                                    it.copy(loading = false, error = error.message ?: "Search failed.")
                                } else {
                                    it
                                }
                            },
                        )
                    }
                }
            }
            _uiState.update { it.copy(loading = false) }
        }
    }

    fun downloadNovel(sourceId: String, summary: OnlineNovelSummary) {
        val source = sourceRegistry.source(sourceId)
        if (source == null) {
            _uiState.update { it.copy(error = "Source not found.") }
            return
        }
        if (source.installState != NovelSourceInstallState.INSTALLED || source.requiresVerification) {
            _uiState.update { it.copy(error = "${source.name} cannot download until its plugin runtime is available.") }
            return
        }
        val downloadKey = "${summary.providerId}:${summary.path}"
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    downloadingKey = downloadKey,
                    generatedEpub = null,
                    error = null,
                )
            }
            runCatching {
                val details = sourceRegistry.details(summary)
                val chapters = details.chapters.sortedBy { chapter -> chapter.order }
                if (chapters.isEmpty()) error("No chapters were found for ${details.title}.")
                val start = chapters.first().order
                val end = chapters.take(250).last().order
                sourceRegistry.downloadAsEpub(sourceId, details, start, end, _uiState.value.downloadConcurrency)
            }.onSuccess { generated ->
                _uiState.update {
                    it.copy(
                        downloadingKey = null,
                        generatedEpub = generated,
                        error = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        downloadingKey = null,
                        generatedEpub = null,
                        error = error.message ?: "Novel download failed.",
                    )
                }
            }
        }
    }
}
