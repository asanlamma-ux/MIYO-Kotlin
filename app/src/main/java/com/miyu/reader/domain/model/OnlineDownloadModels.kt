package com.miyu.reader.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class OnlineDownloadRequest(
    val key: String,
    val summary: OnlineNovelSummary,
    val selectedChapterOrders: List<Int> = emptyList(),
)

enum class OnlineDownloadStatus {
    RUNNING,
    PAUSED,
    COMPLETED,
    ERROR,
    CANCELED,
}

data class OnlineDownloadTaskState(
    val key: String,
    val title: String,
    val completed: Int,
    val total: Int,
    val status: OnlineDownloadStatus,
    val generatedEpub: GeneratedOnlineNovelEpub? = null,
    val error: String? = null,
) {
    val percent: Int
        get() = if (total <= 0) 0 else ((completed.coerceIn(0, total) * 100f) / total.toFloat()).toInt()
}

@Serializable
data class OnlineReadingHistoryEntry(
    val id: String,
    val providerId: OnlineNovelProviderId,
    val path: String,
    val title: String,
    val author: String = "Unknown Author",
    val coverUrl: String? = null,
    val lastChapterTitle: String? = null,
    val lastReadAt: String,
)

@Serializable
data class OnlineDownloadHistoryEntry(
    val id: String,
    val key: String,
    val title: String,
    val chapterCount: Int,
    val completedAt: String,
    val dayKey: String,
)
