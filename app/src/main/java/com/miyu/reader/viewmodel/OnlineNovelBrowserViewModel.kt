package com.miyu.reader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyu.reader.data.repository.OnlineNovelRepository
import com.miyu.reader.domain.model.GeneratedOnlineNovelEpub
import com.miyu.reader.domain.model.OnlineChapterContent
import com.miyu.reader.domain.model.OnlineChapterSummary
import com.miyu.reader.domain.model.OnlineNovelDetails
import com.miyu.reader.domain.model.OnlineNovelProvider
import com.miyu.reader.domain.model.OnlineNovelProviderId
import com.miyu.reader.domain.model.OnlineNovelSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

data class WtrBridgeCommand(
    val id: String,
    val script: String,
)

data class OnlineNovelBrowserUiState(
    val providers: List<OnlineNovelProvider> = emptyList(),
    val selectedProviderId: OnlineNovelProviderId = OnlineNovelProviderId.WTR_LAB,
    val query: String = "",
    val results: List<OnlineNovelSummary> = emptyList(),
    val selectedNovel: OnlineNovelDetails? = null,
    val chapterStart: String = "1",
    val chapterEnd: String = "50",
    val page: Int = 1,
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val loading: Boolean = false,
    val downloading: Boolean = false,
    val statusMessage: String = "Open WTR-LAB and complete verification if prompted.",
    val error: String? = null,
    val wtrBridgeReady: Boolean = false,
    val captchaRequired: Boolean = false,
    val captchaBody: String = "",
    val bridgeCommand: WtrBridgeCommand? = null,
    val generatedEpub: GeneratedOnlineNovelEpub? = null,
)

@HiltViewModel
class OnlineNovelBrowserViewModel @Inject constructor(
    private val onlineNovelRepository: OnlineNovelRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        OnlineNovelBrowserUiState(providers = onlineNovelRepository.providers)
    )
    val uiState: StateFlow<OnlineNovelBrowserUiState> = _uiState.asStateFlow()

    private val pendingRequests = mutableMapOf<String, PendingRequest>()
    private var downloadPlan: WtrDownloadPlan? = null

    fun setQuery(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    fun selectProvider(providerId: OnlineNovelProviderId) {
        val provider = onlineNovelRepository.providers.firstOrNull { it.id == providerId } ?: return
        pendingRequests.clear()
        downloadPlan = null
        _uiState.update {
            it.copy(
                selectedProviderId = providerId,
                results = emptyList(),
                selectedNovel = null,
                page = 1,
                nextCursor = null,
                hasMore = false,
                error = null,
                captchaRequired = false,
                captchaBody = "",
                statusMessage = if (provider.requiresBrowserVerification) {
                    "Complete ${provider.label} verification in the browser card, then search."
                } else {
                    "${provider.label} direct parser ready."
                },
            )
        }
    }

    fun search(loadMore: Boolean = false) {
        val state = _uiState.value
        val providerId = state.selectedProviderId
        val nextPage = if (loadMore) state.page + 1 else 1
        if (providerId == OnlineNovelProviderId.WTR_LAB) {
            if (!state.wtrBridgeReady) {
                _uiState.update {
                    it.copy(
                        error = "WTR-LAB is not ready yet. Complete the in-app verification card first.",
                        statusMessage = "Waiting for WTR-LAB verification.",
                    )
                }
                return
            }
            val payload = JSONObject()
                .put("providerId", "wtr-lab")
                .put("query", state.query.trim())
                .put("page", nextPage)
                .put("cursor", if (loadMore) state.nextCursor else JSONObject.NULL)
                .put("latestOnly", false)
                .put("orderBy", "update")
                .put("order", "desc")
                .put("status", "all")
                .put("minChapters", JSONObject.NULL)
                .put("maxChapters", JSONObject.NULL)
                .put("minRating", JSONObject.NULL)
                .put("minReviewCount", JSONObject.NULL)
            sendWtrRequest(
                type = "search",
                payload = payload,
                pendingRequest = PendingRequest.Search(loadMore),
                statusMessage = if (loadMore) "Loading more from WTR-LAB..." else "Searching WTR-LAB...",
            )
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, statusMessage = "Searching provider...") }
            runCatching {
                onlineNovelRepository.search(
                    providerId = providerId,
                    query = state.query,
                    page = nextPage,
                    cursor = if (loadMore) state.nextCursor else null,
                )
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        results = if (loadMore) (it.results + result.items).distinctBy { item -> item.path } else result.items,
                        page = result.page,
                        hasMore = result.hasMore,
                        nextCursor = result.nextCursor,
                        loading = false,
                        statusMessage = if (result.items.isEmpty()) "No novels matched that search." else "Search complete.",
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        error = error.message ?: "Search failed.",
                        statusMessage = "Search failed.",
                    )
                }
            }
        }
    }

    fun openNovel(summary: OnlineNovelSummary) {
        if (summary.providerId == OnlineNovelProviderId.WTR_LAB) {
            val payload = summary.toWtrPayload().put("includeChapters", true)
            sendWtrRequest(
                type = "details",
                payload = payload,
                pendingRequest = PendingRequest.Details,
                statusMessage = "Loading ${summary.title} from WTR-LAB...",
            )
            _uiState.update {
                it.copy(
                    selectedNovel = summary.toDetails(chapters = emptyList()),
                    error = null,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loading = true,
                    error = null,
                    selectedNovel = summary.toDetails(chapters = emptyList()),
                    statusMessage = "Loading ${summary.title}...",
                )
            }
            runCatching { onlineNovelRepository.getDetails(summary) }
                .onSuccess { details ->
                    _uiState.update {
                        it.copy(
                            selectedNovel = details,
                            chapterStart = "1",
                            chapterEnd = (details.chapterCount ?: details.chapters.size).coerceAtLeast(1).toString(),
                            loading = false,
                            statusMessage = "Novel loaded.",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = error.message ?: "Could not load this novel.",
                            statusMessage = "Novel load failed.",
                        )
                    }
                }
        }
    }

    fun backToResults() {
        _uiState.update { it.copy(selectedNovel = null, error = null) }
    }

    fun setChapterStart(value: String) {
        _uiState.update { it.copy(chapterStart = value.filter(Char::isDigit).take(5)) }
    }

    fun setChapterEnd(value: String) {
        _uiState.update { it.copy(chapterEnd = value.filter(Char::isDigit).take(5)) }
    }

    fun downloadSelectedNovel() {
        val state = _uiState.value
        val novel = state.selectedNovel ?: return
        val start = state.chapterStart.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val chapterTotal = novel.chapterCount ?: novel.chapters.size
        val end = (state.chapterEnd.toIntOrNull() ?: chapterTotal.coerceAtLeast(start)).coerceAtLeast(start)
        val selectedChapters = novel.chapters.filter { it.order in start..end }
        if (selectedChapters.isEmpty()) {
            _uiState.update { it.copy(error = "No chapters were found in that range.") }
            return
        }

        if (novel.providerId == OnlineNovelProviderId.WTR_LAB) {
            val chaptersPayload = JSONArray().apply {
                selectedChapters.forEach { chapter ->
                    put(
                        JSONObject()
                            .put("order", chapter.order)
                            .put("title", chapter.title)
                            .put("path", chapter.path)
                    )
                }
            }
            val payload = novel.toWtrPayload()
                .put("chapters", chaptersPayload)
                .put("maxConcurrency", 4)
            _uiState.update {
                it.copy(
                    downloading = true,
                    error = null,
                    statusMessage = "Fetching ${selectedChapters.size} WTR-LAB chapters...",
                )
            }
            sendWtrRequest(
                type = "chapters",
                payload = payload,
                pendingRequest = PendingRequest.Chapters(novel),
                statusMessage = "Fetching ${selectedChapters.size} WTR-LAB chapters...",
            )
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(downloading = true, error = null, statusMessage = "Downloading chapters...") }
            runCatching {
                onlineNovelRepository.downloadAsEpub(novel, start, end)
            }.onSuccess { generated ->
                _uiState.update {
                    it.copy(
                        downloading = false,
                        generatedEpub = generated,
                        statusMessage = "EPUB generated. Importing into library...",
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        downloading = false,
                        error = error.message ?: "Download failed.",
                        statusMessage = "Download failed.",
                    )
                }
            }
        }
    }

    fun consumeGeneratedEpub() {
        _uiState.update { it.copy(generatedEpub = null) }
    }

    fun consumeBridgeCommand(id: String) {
        _uiState.update { state ->
            if (state.bridgeCommand?.id == id) state.copy(bridgeCommand = null) else state
        }
    }

    fun handleWtrBridgeMessage(rawMessage: String) {
        if (rawMessage.length > MAX_BRIDGE_MESSAGE_CHARS) return
        val message = runCatching { JSONObject(rawMessage) }.getOrNull() ?: return
        if (message.optString("scope") != "wtr-lab") return
        when (message.optString("type")) {
            "ready" -> {
                _uiState.update {
                    it.copy(
                        wtrBridgeReady = true,
                        captchaRequired = false,
                        captchaBody = "",
                        error = null,
                        statusMessage = "WTR-LAB is verified and ready.",
                    )
                }
            }
            "challenge" -> {
                pendingRequests.clear()
                downloadPlan = null
                _uiState.update {
                    it.copy(
                        wtrBridgeReady = false,
                        captchaRequired = true,
                        captchaBody = message.optString("body").take(900),
                        loading = false,
                        downloading = false,
                        statusMessage = "Complete WTR-LAB verification in the browser card.",
                    )
                }
            }
            "error" -> {
                val id = message.optString("id")
                pendingRequests.remove(id)
                _uiState.update {
                    it.copy(
                        loading = false,
                        downloading = false,
                        error = message.optString("error").ifBlank { "WTR-LAB request failed." },
                        statusMessage = "WTR-LAB request failed.",
                    )
                }
            }
            "result" -> handleWtrResult(message)
        }
    }

    private fun handleWtrResult(message: JSONObject) {
        val id = message.optString("id")
        val request = pendingRequests.remove(id) ?: return
        val payload = message.optJSONObject("payload") ?: JSONObject()
        when (request) {
            is PendingRequest.Search -> {
                val parsed = parseSearchPayload(payload)
                _uiState.update {
                    it.copy(
                        results = if (request.loadMore) (it.results + parsed.items).distinctBy { item -> item.path } else parsed.items,
                        page = parsed.page,
                        hasMore = parsed.hasMore,
                        nextCursor = parsed.nextCursor,
                        loading = false,
                        statusMessage = if (parsed.items.isEmpty()) "No WTR-LAB novels matched that search." else "WTR-LAB search complete.",
                    )
                }
            }
            PendingRequest.Details -> {
                val details = parseDetailsPayload(payload)
                _uiState.update {
                    it.copy(
                        selectedNovel = details,
                        chapterStart = "1",
                        chapterEnd = (details.chapterCount ?: details.chapters.size).coerceAtLeast(1).toString(),
                        loading = false,
                        statusMessage = "Novel loaded.",
                    )
                }
            }
            PendingRequest.Chapter -> {
                val chapter = parseChapterPayload(payload)
                val plan = downloadPlan ?: return
                downloadPlan = plan.copy(
                    fetched = plan.fetched + chapter,
                    nextIndex = plan.nextIndex + 1,
                )
                requestNextWtrChapter()
            }
            is PendingRequest.Chapters -> {
                val chapters = payload.optJSONArray("chapters").toChapterContents()
                viewModelScope.launch {
                    runCatching {
                        onlineNovelRepository.createGeneratedEpub(request.novel, chapters)
                    }.onSuccess { generated ->
                        _uiState.update {
                            it.copy(
                                loading = false,
                                downloading = false,
                                generatedEpub = generated,
                                statusMessage = "WTR-LAB EPUB generated. Importing into library...",
                            )
                        }
                    }.onFailure { error ->
                        _uiState.update {
                            it.copy(
                                loading = false,
                                downloading = false,
                                error = error.message ?: "Could not build the WTR-LAB EPUB.",
                                statusMessage = "EPUB build failed.",
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestNextWtrChapter() {
        val plan = downloadPlan ?: return
        if (plan.nextIndex >= plan.chapters.size) {
            finishWtrDownload(plan)
            return
        }
        val chapter = plan.chapters[plan.nextIndex]
        val payload = JSONObject()
            .put("providerId", "wtr-lab")
            .put("rawId", plan.novel.rawId)
            .put("slug", plan.novel.slug)
            .put("path", chapter.path)
            .put("chapterNo", chapter.order)
            .put("chapterTitle", chapter.title)
        sendWtrRequest(
            type = "chapter",
            payload = payload,
            pendingRequest = PendingRequest.Chapter,
            statusMessage = "Fetching WTR-LAB chapter ${plan.nextIndex + 1} of ${plan.chapters.size}...",
        )
    }

    private fun finishWtrDownload(plan: WtrDownloadPlan) {
        val chapters = plan.fetched
        downloadPlan = null
        viewModelScope.launch {
            runCatching {
                onlineNovelRepository.createGeneratedEpub(plan.novel, chapters)
            }.onSuccess { generated ->
                _uiState.update {
                    it.copy(
                        downloading = false,
                        generatedEpub = generated,
                        statusMessage = "WTR-LAB EPUB generated. Importing into library...",
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        downloading = false,
                        error = error.message ?: "Could not build the WTR-LAB EPUB.",
                        statusMessage = "EPUB build failed.",
                    )
                }
            }
        }
    }

    private fun sendWtrRequest(
        type: String,
        payload: JSONObject,
        pendingRequest: PendingRequest,
        statusMessage: String,
    ) {
        val id = "wtr_${UUID.randomUUID()}"
        val request = JSONObject()
            .put("id", id)
            .put("type", type)
            .put("payload", payload)
        pendingRequests[id] = pendingRequest
        val script = """
(function(){
  if (window.__MIYO_WTR_BRIDGE && window.__MIYO_WTR_BRIDGE.run) {
    window.__MIYO_WTR_BRIDGE.run(${request});
  } else if (window.AndroidWtrBridge) {
    window.AndroidWtrBridge.postMessage(JSON.stringify({scope:'wtr-lab', type:'error', id:'$id', providerId:'wtr-lab', error:'WTR bridge unavailable. Complete verification and retry.'}));
  }
})();
true;
""".trimIndent()
        _uiState.update {
            it.copy(
                bridgeCommand = WtrBridgeCommand(id = id, script = script),
                loading = pendingRequest !is PendingRequest.Chapter,
                error = null,
                statusMessage = statusMessage,
            )
        }
    }

    private fun parseSearchPayload(payload: JSONObject): ParsedSearchPayload {
        val items = payload.optJSONArray("items").toNovelSummaries()
        return ParsedSearchPayload(
            items = items,
            page = payload.optInt("page", 1),
            hasMore = payload.optBoolean("hasMore", false),
            nextCursor = payload.optString("nextCursor").takeIf { it.isNotBlank() && it != "null" },
        )
    }

    private fun parseDetailsPayload(payload: JSONObject): OnlineNovelDetails {
        val summary = payload.toNovelSummary()
        val chapters = payload.optJSONArray("chapters").toChapterSummaries()
        return summary.toDetails(
            genres = payload.optJSONArray("genres").toStringList(),
            tags = payload.optJSONArray("tags").toStringList(),
            chapters = chapters,
        )
    }

    private fun parseChapterPayload(payload: JSONObject): OnlineChapterContent =
        OnlineChapterContent(
            order = payload.optInt("order", 1),
            title = payload.optString("title", "Chapter ${payload.optInt("order", 1)}").sanitizeBridgeText(220),
            html = payload.optString("html").take(MAX_CHAPTER_HTML_CHARS),
        )

    private fun JSONArray?.toChapterContents(): List<OnlineChapterContent> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                optJSONObject(index)?.let { add(parseChapterPayload(it)) }
            }
        }.sortedBy { it.order }
    }

    private fun JSONArray?.toNovelSummaries(): List<OnlineNovelSummary> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                optJSONObject(index)?.toNovelSummary()?.let(::add)
            }
        }
    }

    private fun JSONObject.toNovelSummary(): OnlineNovelSummary =
        OnlineNovelSummary(
            providerId = OnlineNovelProviderId.WTR_LAB,
            providerLabel = optString("providerLabel", "WTR-LAB").sanitizeBridgeText(80),
            rawId = opt("rawId")?.toString().orEmpty().sanitizeBridgeText(80),
            slug = optString("slug").sanitizeBridgeText(180),
            path = optString("path").sanitizeBridgeText(260),
            title = optString("title", "Untitled").sanitizeBridgeText(220).ifBlank { "Untitled" },
            coverUrl = optString("coverUrl").takeIf { it.startsWith("https://", ignoreCase = true) },
            author = optString("author", "Unknown Author").sanitizeBridgeText(160).ifBlank { "Unknown Author" },
            summary = optString("summary").sanitizeBridgeText(1_000),
            status = optString("status", "Unknown").sanitizeBridgeText(80).ifBlank { "Unknown" },
            chapterCount = optNullableInt("chapterCount"),
            rating = optNullableFloat("rating"),
        )

    private fun JSONArray?.toChapterSummaries(): List<OnlineChapterSummary> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val order = item.optInt("order", index + 1).coerceAtLeast(1)
                add(
                    OnlineChapterSummary(
                        order = order,
                        title = item.optString("title", "Chapter $order").sanitizeBridgeText(220).ifBlank { "Chapter $order" },
                        path = item.optString("path").sanitizeBridgeText(260),
                        updatedAt = item.optString("updatedAt").sanitizeBridgeText(80).takeIf { it.isNotBlank() },
                    )
                )
            }
        }.distinctBy { it.path.ifBlank { it.order.toString() } }.sortedBy { it.order }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val value = optString(index).sanitizeBridgeText(80)
                if (value.isNotBlank()) add(value)
            }
        }.distinct().take(30)
    }

    private fun JSONObject.optNullableInt(key: String): Int? =
        if (isNull(key)) null else optInt(key).takeIf { it > 0 }

    private fun JSONObject.optNullableFloat(key: String): Float? =
        if (isNull(key)) null else optDouble(key).toFloat().takeIf { it > 0f }

    private fun OnlineNovelSummary.toWtrPayload(): JSONObject =
        buildWtrPayload(
            rawId = rawId,
            slug = slug,
            path = path,
            title = title,
            coverUrl = coverUrl,
            author = author,
            summary = summary,
            status = status,
            chapterCount = chapterCount,
        )

    private fun OnlineNovelDetails.toWtrPayload(): JSONObject =
        buildWtrPayload(
            rawId = rawId,
            slug = slug,
            path = path,
            title = title,
            coverUrl = coverUrl,
            author = author,
            summary = summary,
            status = status,
            chapterCount = chapterCount,
        )

    private fun buildWtrPayload(
        rawId: String,
        slug: String,
        path: String,
        title: String,
        coverUrl: String?,
        author: String,
        summary: String,
        status: String,
        chapterCount: Int?,
    ): JSONObject =
        JSONObject()
            .put("providerId", "wtr-lab")
            .put("rawId", rawId)
            .put("slug", slug)
            .put("path", path)
            .put("fallbackTitle", title)
            .put("fallbackCoverUrl", coverUrl ?: JSONObject.NULL)
            .put("fallbackAuthor", author)
            .put("fallbackSummary", summary)
            .put("fallbackStatus", status)
            .put("fallbackChapterCount", chapterCount ?: JSONObject.NULL)

    private fun OnlineNovelSummary.toDetails(
        genres: List<String> = emptyList(),
        tags: List<String> = emptyList(),
        chapters: List<OnlineChapterSummary>,
    ): OnlineNovelDetails =
        OnlineNovelDetails(
            providerId = providerId,
            providerLabel = providerLabel,
            rawId = rawId,
            slug = slug,
            path = path,
            title = title,
            coverUrl = coverUrl,
            author = author,
            summary = summary,
            status = status,
            chapterCount = chapterCount ?: chapters.size.takeIf { it > 0 },
            rating = rating,
            genres = genres,
            tags = tags,
            chapters = chapters,
        )

    private fun String.sanitizeBridgeText(maxChars: Int): String =
        replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]"), "")
            .trim()
            .take(maxChars)

    private sealed interface PendingRequest {
        data class Search(val loadMore: Boolean) : PendingRequest
        object Details : PendingRequest
        object Chapter : PendingRequest
        data class Chapters(val novel: OnlineNovelDetails) : PendingRequest
    }

    private data class WtrDownloadPlan(
        val novel: OnlineNovelDetails,
        val chapters: List<OnlineChapterSummary>,
        val fetched: List<OnlineChapterContent>,
        val nextIndex: Int,
    )

    private data class ParsedSearchPayload(
        val items: List<OnlineNovelSummary>,
        val page: Int,
        val hasMore: Boolean,
        val nextCursor: String?,
    )

    private companion object {
        const val MAX_BRIDGE_MESSAGE_CHARS = 2_000_000
        const val MAX_CHAPTER_HTML_CHARS = 800_000
    }
}
