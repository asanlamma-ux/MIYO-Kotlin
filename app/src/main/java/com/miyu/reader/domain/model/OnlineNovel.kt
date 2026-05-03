package com.miyu.reader.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class OnlineNovelProviderId {
    WTR_LAB,
    FANMTL,
    WUXIASPOT,
    NOVELCOOL,
    MCREADER,
    FREEWEBNOVEL,
    LIGHTNOVELPUB,
    SCRIBBLEHUB,
    ROYALROAD,
    NOVELFIRE,
    ARCHIVE_OF_OUR_OWN,
}

@Serializable
data class OnlineNovelProvider(
    val id: OnlineNovelProviderId,
    val label: String,
    val description: String,
    val baseUrl: String,
    val startUrl: String,
    val requiresBrowserVerification: Boolean = false,
)

@Serializable
data class OnlineNovelSummary(
    val providerId: OnlineNovelProviderId,
    val providerLabel: String,
    val rawId: String,
    val slug: String,
    val path: String,
    val title: String,
    val coverUrl: String? = null,
    val author: String = "Unknown Author",
    val summary: String = "",
    val status: String = "Unknown",
    val chapterCount: Int? = null,
    val rating: Float? = null,
)

@Serializable
data class OnlineChapterSummary(
    val order: Int,
    val title: String,
    val path: String,
    val updatedAt: String? = null,
)

@Serializable
data class OnlineNovelDetails(
    val providerId: OnlineNovelProviderId,
    val providerLabel: String,
    val rawId: String,
    val slug: String,
    val path: String,
    val title: String,
    val coverUrl: String? = null,
    val author: String = "Unknown Author",
    val summary: String = "",
    val status: String = "Unknown",
    val chapterCount: Int? = null,
    val rating: Float? = null,
    val genres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val chapters: List<OnlineChapterSummary> = emptyList(),
)

@Serializable
data class OnlineNovelSearchResult(
    val items: List<OnlineNovelSummary>,
    val page: Int,
    val hasMore: Boolean,
    val nextCursor: String? = null,
)

@Serializable
data class OnlineChapterContent(
    val order: Int,
    val title: String,
    val html: String,
)

@Serializable
data class GeneratedOnlineNovelEpub(
    val filePath: String,
    val fileName: String,
    val title: String,
)
