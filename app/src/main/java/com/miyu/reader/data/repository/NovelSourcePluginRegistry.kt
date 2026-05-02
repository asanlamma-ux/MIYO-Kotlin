package com.miyu.reader.data.repository

import com.miyu.reader.domain.model.GeneratedOnlineNovelEpub
import com.miyu.reader.domain.model.NovelSourceInstallState
import com.miyu.reader.domain.model.NovelSourceKind
import com.miyu.reader.domain.model.NovelSourcePlugin
import com.miyu.reader.domain.model.NovelSourcePluginItem
import com.miyu.reader.domain.model.OnlineNovelDetails
import com.miyu.reader.domain.model.OnlineNovelProvider
import com.miyu.reader.domain.model.OnlineNovelProviderId
import com.miyu.reader.domain.model.OnlineNovelSearchResult
import com.miyu.reader.domain.model.OnlineNovelSummary
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NovelSourcePluginRegistry @Inject constructor(
    private val onlineNovelRepository: OnlineNovelRepository,
) {
    private val builtInPlugins: Map<String, NovelSourcePlugin> =
        onlineNovelRepository.providers
            .associate { provider ->
                provider.toSourcePluginId() to BuiltInNovelSourcePlugin(provider, onlineNovelRepository)
            }

    private val availablePlugins = listOf(
        NovelSourcePluginItem(
            id = "external:ao3",
            name = "Archive of Our Own",
            site = "archiveofourown.org",
            language = "Fanfiction",
            version = "repository",
            repositoryUrl = "https://github.com/lnreader/lnreader-plugins",
            kind = NovelSourceKind.EXTERNAL_JS,
            installState = NovelSourceInstallState.AVAILABLE,
            description = "External-source slot for fanfiction plugin manifests.",
        ),
        NovelSourcePluginItem(
            id = "external:royalroad",
            name = "Royal Road",
            site = "royalroad.com",
            language = "EN",
            version = "repository",
            repositoryUrl = "https://github.com/lnreader/lnreader-plugins",
            kind = NovelSourceKind.EXTERNAL_JS,
            installState = NovelSourceInstallState.AVAILABLE,
            description = "External-source slot for web serial plugin manifests.",
        ),
        NovelSourcePluginItem(
            id = "external:scribblehub",
            name = "Scribble Hub",
            site = "scribblehub.com",
            language = "EN",
            version = "repository",
            repositoryUrl = "https://github.com/lnreader/lnreader-plugins",
            kind = NovelSourceKind.EXTERNAL_JS,
            installState = NovelSourceInstallState.AVAILABLE,
            description = "External-source slot for light novel plugin manifests.",
        ),
    )

    val installedSources: List<NovelSourcePluginItem>
        get() = builtInPlugins.values.map { it.item }

    val availableSources: List<NovelSourcePluginItem>
        get() = availablePlugins

    val allSources: List<NovelSourcePluginItem>
        get() = installedSources + availableSources

    fun source(sourceId: String): NovelSourcePluginItem? =
        allSources.firstOrNull { it.id == sourceId }

    suspend fun search(sourceId: String, query: String, page: Int): OnlineNovelSearchResult =
        plugin(sourceId).searchNovels(query, page.coerceAtLeast(1))

    suspend fun popular(sourceId: String, page: Int): OnlineNovelSearchResult =
        plugin(sourceId).popularNovels(page.coerceAtLeast(1))

    suspend fun details(summary: OnlineNovelSummary): OnlineNovelDetails =
        plugin(summary.providerId.toSourcePluginId()).parseNovel(summary)

    suspend fun downloadAsEpub(
        sourceId: String,
        novel: OnlineNovelDetails,
        startChapter: Int,
        endChapter: Int,
        concurrency: Int,
    ): GeneratedOnlineNovelEpub =
        plugin(sourceId).downloadAsEpub(novel, startChapter, endChapter, concurrency)

    private fun plugin(sourceId: String): NovelSourcePlugin =
        builtInPlugins[sourceId] ?: error("Source plugin is not installed or cannot run yet.")
}

private class BuiltInNovelSourcePlugin(
    private val provider: OnlineNovelProvider,
    private val repository: OnlineNovelRepository,
) : NovelSourcePlugin {
    override val item: NovelSourcePluginItem = NovelSourcePluginItem(
        id = provider.toSourcePluginId(),
        name = provider.label,
        site = provider.baseUrl.removePrefix("https://").removePrefix("http://").trimEnd('/'),
        language = provider.sourceLanguageLabel(),
        version = "built-in",
        kind = NovelSourceKind.BUILT_IN,
        installState = NovelSourceInstallState.INSTALLED,
        requiresVerification = provider.requiresBrowserVerification,
        verificationUrl = provider.startUrl,
        description = provider.description,
    )

    override suspend fun popularNovels(page: Int): OnlineNovelSearchResult =
        repository.search(provider.id, query = "", page = page)

    override suspend fun searchNovels(query: String, page: Int): OnlineNovelSearchResult =
        repository.search(provider.id, query = query, page = page)

    override suspend fun parseNovel(summary: OnlineNovelSummary): OnlineNovelDetails =
        repository.getDetails(summary)

    override suspend fun downloadAsEpub(
        novel: OnlineNovelDetails,
        startChapter: Int,
        endChapter: Int,
        concurrency: Int,
    ): GeneratedOnlineNovelEpub =
        repository.downloadAsEpub(novel, startChapter, endChapter, concurrency)
}

private fun OnlineNovelProvider.toSourcePluginId(): String =
    id.toSourcePluginId()

private fun OnlineNovelProviderId.toSourcePluginId(): String =
    "builtin:${name.lowercase().replace('_', '-')}"

private fun OnlineNovelProvider.sourceLanguageLabel(): String = when (id) {
    OnlineNovelProviderId.FANMTL,
    OnlineNovelProviderId.WUXIASPOT,
    OnlineNovelProviderId.MCREADER -> "EN MTL"
    OnlineNovelProviderId.NOVELCOOL,
    OnlineNovelProviderId.FREEWEBNOVEL,
    OnlineNovelProviderId.LIGHTNOVELPUB -> "EN"
    OnlineNovelProviderId.WTR_LAB -> "EN MTL"
}
