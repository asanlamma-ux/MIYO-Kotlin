package com.miyu.reader.domain.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
enum class NovelSourceKind {
    BUILT_IN,
    EXTERNAL_JS,
}

@Keep
@Serializable
enum class NovelSourceInstallState {
    INSTALLED,
    AVAILABLE,
    DISABLED,
}

@Keep
@Serializable
data class NovelSourcePluginItem(
    val id: String,
    val name: String,
    val site: String,
    val language: String,
    val version: String,
    val repositoryUrl: String? = null,
    val iconUrl: String? = null,
    val customJsUrl: String? = null,
    val customCssUrl: String? = null,
    val verificationUrl: String? = null,
    val kind: NovelSourceKind = NovelSourceKind.BUILT_IN,
    val installState: NovelSourceInstallState = NovelSourceInstallState.INSTALLED,
    val hasUpdate: Boolean = false,
    val hasSettings: Boolean = false,
    val requiresVerification: Boolean = false,
    val description: String = "",
)

interface NovelSourcePlugin {
    val item: NovelSourcePluginItem

    suspend fun popularNovels(page: Int): OnlineNovelSearchResult

    suspend fun searchNovels(query: String, page: Int): OnlineNovelSearchResult

    suspend fun parseNovel(summary: OnlineNovelSummary): OnlineNovelDetails

    suspend fun downloadAsEpub(
        novel: OnlineNovelDetails,
        startChapter: Int,
        endChapter: Int,
        concurrency: Int,
    ): GeneratedOnlineNovelEpub
}
