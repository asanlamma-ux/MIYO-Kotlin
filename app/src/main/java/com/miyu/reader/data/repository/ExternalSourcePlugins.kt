package com.miyu.reader.data.repository

import com.miyu.reader.domain.model.GeneratedOnlineNovelEpub
import com.miyu.reader.domain.model.NovelSourceInstallState
import com.miyu.reader.domain.model.NovelSourceKind
import com.miyu.reader.domain.model.NovelSourcePlugin
import com.miyu.reader.domain.model.NovelSourcePluginItem
import com.miyu.reader.domain.model.OnlineChapterContent
import com.miyu.reader.domain.model.OnlineChapterSummary
import com.miyu.reader.domain.model.OnlineNovelDetails
import com.miyu.reader.domain.model.OnlineNovelProviderId
import com.miyu.reader.domain.model.OnlineNovelSearchResult
import com.miyu.reader.domain.model.OnlineNovelSummary
import org.json.JSONArray
import org.json.JSONObject

class ExternalPackageNovelSourcePlugin(
    private val externalPackage: InstalledExternalSourcePackage,
    private val repository: OnlineNovelRepository,
    private val runtime: ExternalJsPluginRuntime,
) : NovelSourcePlugin {
    private val definition = ExternalJsPluginDefinition(
        pluginId = externalPackage.sourceId,
        providerScope = externalPackage.manifest.bridgeScope,
        startUrl = externalPackage.manifest.startUrl,
        bootstrapScript = externalPackage.script,
        bridgeObjectName = "__MIYO_SOURCE_BRIDGE",
        androidBridgeName = "AndroidExternalSourceBridge",
    )

    override val item: NovelSourcePluginItem = NovelSourcePluginItem(
        id = externalPackage.sourceId,
        name = externalPackage.manifest.name,
        site = externalPackage.manifest.site,
        language = externalPackage.manifest.language,
        version = externalPackage.manifest.version,
        customJsUrl = externalPackage.installPath?.let { "file://$it/${externalPackage.manifest.entry}" }
            ?: "asset://source-packages/${externalPackage.manifest.packageId}/${externalPackage.manifest.entry}",
        verificationUrl = externalPackage.manifest.startUrl,
        kind = NovelSourceKind.EXTERNAL_JS,
        installState = NovelSourceInstallState.INSTALLED,
        requiresVerification = externalPackage.manifest.requiresVerification,
        description = externalPackage.manifest.description,
    )

    override suspend fun popularNovels(page: Int): OnlineNovelSearchResult =
        runtime.execute(
            definition = definition,
            requestType = "search",
            payload = JSONObject()
                .put("page", page.coerceAtLeast(1))
                .put("latestOnly", true),
        ).toSearchResult(externalPackage.providerId, item.name)

    override suspend fun searchNovels(query: String, page: Int): OnlineNovelSearchResult =
        runtime.execute(
            definition = definition,
            requestType = "search",
            payload = JSONObject()
                .put("page", page.coerceAtLeast(1))
                .put("query", query.trim())
                .put("latestOnly", false),
        ).toSearchResult(externalPackage.providerId, item.name)

    override suspend fun parseNovel(summary: OnlineNovelSummary): OnlineNovelDetails =
        runtime.execute(
            definition = definition,
            requestType = "details",
            payload = summary.toBridgePayload(includeChapters = true),
            timeoutMs = 180_000L,
        ).toNovelDetails(externalPackage.providerId, item.name)

    override suspend fun fetchChapterContent(
        novel: OnlineNovelDetails,
        chapter: OnlineChapterSummary,
    ): OnlineChapterContent =
        runtime.execute(
            definition = definition,
            requestType = "chapter",
            payload = novel.toBridgePayload(chapter),
            timeoutMs = 180_000L,
        ).toChapterContent(chapter.order, chapter.title)

    override suspend fun downloadAsEpub(
        novel: OnlineNovelDetails,
        startChapter: Int,
        endChapter: Int,
        concurrency: Int,
        onProgress: ((completed: Int, total: Int) -> Unit)?,
    ): GeneratedOnlineNovelEpub {
        val chapters = novel.chapters
            .filter { it.order in startChapter..endChapter }
            .sortedBy { it.order }
        if (chapters.isEmpty()) error("No chapters were found in that range.")
        val response = runtime.execute(
            definition = definition,
            requestType = "chapters",
            payload = novel.toBridgePayload(chapters, concurrency.coerceIn(2, 10)),
            timeoutMs = (chapters.size * 3_000L).coerceIn(300_000L, 1_800_000L),
            onProgress = { progress ->
                onProgress?.invoke(
                    progress.optInt("completed", 0),
                    progress.optInt("total", chapters.size),
                )
            },
        )
        val downloadedChapters = response.optJSONArray("chapters")
            .toChapterContentList()
            .sortedBy { it.order }
        if (downloadedChapters.isEmpty()) {
            val failures = response.optJSONArray("failures").toStringList()
            error(failures.firstOrNull() ?: "The external source runtime did not return any chapters.")
        }
        return repository.createGeneratedEpub(novel, downloadedChapters)
    }
}

private fun JSONObject.toSearchResult(
    providerId: OnlineNovelProviderId,
    providerLabel: String,
): OnlineNovelSearchResult {
    val items = optJSONArray("items").toSummaryList(providerId, providerLabel)
    return OnlineNovelSearchResult(
        items = items,
        page = optInt("page", 1).coerceAtLeast(1),
        hasMore = optBoolean("hasMore", items.size >= 20),
        nextCursor = optString("nextCursor").takeIf { it.isNotBlank() },
    )
}

private fun JSONObject.toNovelDetails(
    providerId: OnlineNovelProviderId,
    providerLabel: String,
): OnlineNovelDetails =
    OnlineNovelDetails(
        providerId = providerId,
        providerLabel = providerLabel,
        rawId = optRawId(),
        slug = optString("slug"),
        path = optString("path").normalizePath(),
        title = optString("title").ifBlank { "Untitled" },
        coverUrl = optString("coverUrl").takeIf { it.isNotBlank() },
        author = optString("author").ifBlank { "Unknown Author" },
        summary = optString("summary"),
        status = optString("status").ifBlank { "Unknown" },
        chapterCount = optInt("chapterCount").takeIf { it > 0 },
        rating = optDouble("rating", 0.0).toFloat().takeIf { it > 0f },
        genres = optJSONArray("genres").toStringList(),
        tags = optJSONArray("tags").toStringList(),
        chapters = optJSONArray("chapters").toChapterSummaryList().sortedBy { it.order },
    )

private fun JSONObject.toChapterContent(
    fallbackOrder: Int,
    fallbackTitle: String,
): OnlineChapterContent =
    OnlineChapterContent(
        order = optInt("order", fallbackOrder),
        title = optString("title").ifBlank { fallbackTitle },
        html = optString("html").ifBlank { "<p><em>No chapter content was returned.</em></p>" },
    )

private fun JSONArray?.toSummaryList(
    providerId: OnlineNovelProviderId,
    providerLabel: String,
): List<OnlineNovelSummary> = buildList {
    if (this@toSummaryList == null) return@buildList
    for (index in 0 until this@toSummaryList.length()) {
        val item = this@toSummaryList.optJSONObject(index) ?: continue
        add(
            OnlineNovelSummary(
                providerId = providerId,
                providerLabel = providerLabel,
                rawId = item.optRawId(),
                slug = item.optString("slug"),
                path = item.optString("path").normalizePath(),
                title = item.optString("title").ifBlank { "Untitled" },
                coverUrl = item.optString("coverUrl").takeIf { it.isNotBlank() },
                author = item.optString("author").ifBlank { "Unknown Author" },
                summary = item.optString("summary"),
                status = item.optString("status").ifBlank { "Unknown" },
                chapterCount = item.optInt("chapterCount").takeIf { it > 0 },
                rating = item.optDouble("rating", 0.0).toFloat().takeIf { it > 0f },
            ),
        )
    }
}.distinctBy { "${it.providerId}:${it.path}" }

private fun JSONObject.optRawId(): String =
    opt("rawId")
        ?.takeUnless { it == JSONObject.NULL }
        ?.toString()
        .orEmpty()
        .ifBlank {
            optString("path")
                .normalizePath()
                .let { path ->
                    Regex("/novel/(\\d+)/", RegexOption.IGNORE_CASE)
                        .find(path)
                        ?.groupValues
                        ?.getOrNull(1)
                }
                .orEmpty()
        }

private fun JSONArray?.toChapterSummaryList(): List<OnlineChapterSummary> = buildList {
    if (this@toChapterSummaryList == null) return@buildList
    for (index in 0 until this@toChapterSummaryList.length()) {
        val item = this@toChapterSummaryList.optJSONObject(index) ?: continue
        val order = item.optInt("order", index + 1)
        add(
            OnlineChapterSummary(
                order = order,
                title = item.optString("title").ifBlank { "Chapter $order" },
                path = item.optString("path").normalizePath(),
                updatedAt = item.optString("updatedAt").takeIf { it.isNotBlank() },
            ),
        )
    }
}.distinctBy { it.path.ifBlank { it.order.toString() } }

private fun JSONArray?.toChapterContentList(): List<OnlineChapterContent> = buildList {
    if (this@toChapterContentList == null) return@buildList
    for (index in 0 until this@toChapterContentList.length()) {
        val item = this@toChapterContentList.optJSONObject(index) ?: continue
        add(
            OnlineChapterContent(
                order = item.optInt("order", index + 1),
                title = item.optString("title").ifBlank { "Chapter ${index + 1}" },
                html = item.optString("html").ifBlank { "<p><em>No chapter content was returned.</em></p>" },
            ),
        )
    }
}

private fun JSONArray?.toStringList(): List<String> = buildList {
    if (this@toStringList == null) return@buildList
    for (index in 0 until this@toStringList.length()) {
        val value = this@toStringList.optString(index).trim()
        if (value.isNotBlank() && value !in this) add(value)
    }
}

private fun OnlineNovelSummary.toBridgePayload(includeChapters: Boolean): JSONObject =
    JSONObject()
        .put("rawId", rawId.toIntOrNull() ?: path.extractNumericId())
        .put("slug", slug)
        .put("path", path.normalizePath())
        .put("fallbackTitle", title)
        .put("fallbackCoverUrl", coverUrl)
        .put("fallbackAuthor", author)
        .put("fallbackSummary", summary)
        .put("fallbackStatus", status)
        .put("fallbackChapterCount", chapterCount ?: 0)
        .put("includeChapters", includeChapters)

private fun OnlineNovelDetails.toBridgePayload(chapter: OnlineChapterSummary): JSONObject =
    JSONObject()
        .put("rawId", rawId.toIntOrNull() ?: path.extractNumericId())
        .put("slug", slug)
        .put("path", chapter.path.normalizePath())
        .put("chapterNo", chapter.order)
        .put("chapterTitle", chapter.title)

private fun OnlineNovelDetails.toBridgePayload(
    chapters: List<OnlineChapterSummary>,
    concurrency: Int,
): JSONObject =
    JSONObject()
        .put("rawId", rawId.toIntOrNull() ?: path.extractNumericId())
        .put("slug", slug)
        .put("path", path.normalizePath())
        .put("maxConcurrency", concurrency)
        .put(
            "chapters",
            JSONArray().apply {
                chapters.forEach { chapter ->
                    put(
                        JSONObject()
                            .put("order", chapter.order)
                            .put("title", chapter.title)
                            .put("path", chapter.path.normalizePath()),
                    )
                }
            },
        )

private fun String.normalizePath(): String =
    trim().ifBlank { "/" }.let { value -> if (value.startsWith("/")) value else "/$value" }

private fun String.extractNumericId(): Int =
    Regex("/novel/(\\d+)/", RegexOption.IGNORE_CASE)
        .find(this)
        ?.groupValues
        ?.getOrNull(1)
        ?.toIntOrNull()
        ?: error("Provider result is missing a numeric raw id.")
