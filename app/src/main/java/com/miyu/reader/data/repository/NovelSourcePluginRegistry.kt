package com.miyu.reader.data.repository

import com.miyu.reader.domain.model.GeneratedOnlineNovelEpub
import com.miyu.reader.domain.model.ExternalSourcePackageDescriptor
import com.miyu.reader.domain.model.NovelSourcePlugin
import com.miyu.reader.domain.model.NovelSourcePluginItem
import com.miyu.reader.domain.model.OnlineChapterContent
import com.miyu.reader.domain.model.OnlineChapterSummary
import com.miyu.reader.domain.model.OnlineNovelDetails
import com.miyu.reader.domain.model.OnlineNovelProviderId
import com.miyu.reader.domain.model.OnlineNovelSearchResult
import com.miyu.reader.domain.model.OnlineNovelSummary
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NovelSourcePluginRegistry @Inject constructor(
    private val onlineNovelRepository: OnlineNovelRepository,
    private val externalPackageManager: ExternalSourcePackageManager,
    private val externalRuntime: ExternalJsPluginRuntime,
) {
    private val externalPlugins: Map<String, NovelSourcePlugin>
        get() = externalPackageManager.installedPackages()
            .associate { externalPackage ->
                externalPackage.sourceId to ExternalPackageNovelSourcePlugin(
                    externalPackage = externalPackage,
                    repository = onlineNovelRepository,
                    runtime = externalRuntime,
                )
            }

    val installedSources: List<NovelSourcePluginItem>
        get() = externalPlugins.values
            .map { it.item }
            .sortedBy { it.name.lowercase() }

    val availableSources: List<NovelSourcePluginItem>
        get() = emptyList()

    val installedExternalPackages: List<ExternalSourcePackageDescriptor>
        get() = externalPackageManager.installedPackageDescriptors()

    val allSources: List<NovelSourcePluginItem>
        get() = installedSources + availableSources

    fun source(sourceId: String): NovelSourcePluginItem? =
        allSources.firstOrNull { it.id == sourceId }

    fun sourceIdForProvider(providerId: OnlineNovelProviderId): String =
        externalPackageManager.packageForProvider(providerId)?.sourceId
            ?: "missing:${providerId.name.lowercase().replace('_', '-')}"

    suspend fun installExternalPackage(uri: android.net.Uri): ExternalSourcePackageDescriptor =
        externalPackageManager.importPackage(uri)

    suspend fun removeExternalPackage(packageId: String) {
        externalPackageManager.removePackage(packageId)
    }

    suspend fun search(sourceId: String, query: String, page: Int): OnlineNovelSearchResult =
        plugin(sourceId).searchNovels(query, page.coerceAtLeast(1))

    suspend fun popular(sourceId: String, page: Int): OnlineNovelSearchResult =
        plugin(sourceId).popularNovels(page.coerceAtLeast(1))

    suspend fun details(summary: OnlineNovelSummary): OnlineNovelDetails =
        plugin(sourceIdForProvider(summary.providerId)).parseNovel(summary)

    suspend fun chapterContent(
        sourceId: String,
        novel: OnlineNovelDetails,
        chapter: OnlineChapterSummary,
    ): OnlineChapterContent =
        plugin(sourceId).fetchChapterContent(novel, chapter)

    suspend fun downloadAsEpub(
        sourceId: String,
        novel: OnlineNovelDetails,
        startChapter: Int,
        endChapter: Int,
        concurrency: Int,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null,
    ): GeneratedOnlineNovelEpub =
        plugin(sourceId).downloadAsEpub(novel, startChapter, endChapter, concurrency, onProgress)

    private fun plugin(sourceId: String): NovelSourcePlugin =
        externalPlugins[sourceId]
            ?: error("Source plugin is not installed or cannot run yet.")
}
