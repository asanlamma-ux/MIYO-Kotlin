package com.miyu.reader.ui.browse

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.miyu.reader.domain.model.GeneratedOnlineNovelEpub
import com.miyu.reader.domain.model.NovelSourceInstallState
import com.miyu.reader.domain.model.NovelSourceKind
import com.miyu.reader.domain.model.NovelSourcePluginItem
import com.miyu.reader.domain.model.OnlineChapterSummary
import com.miyu.reader.domain.model.OnlineNovelDetails
import com.miyu.reader.domain.model.OnlineNovelProviderId
import com.miyu.reader.domain.model.OnlineNovelSummary
import com.miyu.reader.ui.core.components.MiyoEmptyScreen
import com.miyu.reader.ui.core.components.MiyoIconActionButton
import com.miyu.reader.ui.core.components.MiyoMainOverflowMenu
import com.miyu.reader.ui.core.components.MiyoWorkspaceExitButton
import com.miyu.reader.ui.core.components.MiyoWorkspaceSurface
import com.miyu.reader.ui.core.theme.MiyoSpacing
import com.miyu.reader.ui.theme.LocalMIYUColors
import com.miyu.reader.viewmodel.BrowseDownloadProgress
import com.miyu.reader.viewmodel.BrowseSourceTab
import com.miyu.reader.viewmodel.BrowseViewModel
import com.miyu.reader.viewmodel.GlobalSearchSourceResult
import com.miyu.reader.viewmodel.GlobalSourceFilter

@Composable
fun BrowseWorkflowScreen(
    onOpenSource: (String) -> Unit,
    onOpenGlobalSearch: () -> Unit,
    onOpenRepositories: () -> Unit,
    onOpenMigration: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenUpdates: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenThemePicker: () -> Unit = {},
    onSaveAndExport: () -> Unit = {},
    onAbout: () -> Unit = {},
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalMIYUColors.current
    val visibleSources = state.visibleSources
    val showInstalledSections = state.selectedTab == BrowseSourceTab.INSTALLED && state.sourceQuery.isBlank()
    val pinnedSources = if (showInstalledSections) {
        visibleSources.filter { it.id in state.pinnedSourceIds }
    } else {
        emptyList()
    }
    val lastUsedSource = if (showInstalledSections) {
        visibleSources.firstOrNull { it.id == state.lastUsedSourceId && it.id !in state.pinnedSourceIds }
    } else {
        null
    }
    val mainSources = if (showInstalledSections) {
        visibleSources.filter { it.id !in state.pinnedSourceIds && it.id != lastUsedSource?.id }
    } else {
        visibleSources
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        BrowseSearchLauncher(
            onClick = onOpenGlobalSearch,
            actions = {
                MiyoMainOverflowMenu(
                    onOpenSettings = onOpenSettings,
                    onOpenThemePicker = onOpenThemePicker,
                    onExportData = onSaveAndExport,
                    onImportData = onSaveAndExport,
                    onAbout = onAbout,
                    extraItems = { dismiss ->
                        DropdownMenuItem(
                            text = { Text("Global search") },
                            onClick = {
                                dismiss()
                                onOpenGlobalSearch()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = { Text("Downloads") },
                            onClick = {
                                dismiss()
                                onOpenDownloads()
                            },
                            leadingIcon = { Icon(Icons.Outlined.CloudDownload, contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = { Text("Manage sources") },
                            onClick = {
                                dismiss()
                                onOpenRepositories()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = { Text("Migrate sources") },
                            onClick = {
                                dismiss()
                                onOpenMigration()
                            },
                            leadingIcon = { Icon(Icons.Outlined.SwapHoriz, contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = { Text("Source updates") },
                            onClick = {
                                dismiss()
                                onOpenUpdates()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Update, contentDescription = null) },
                        )
                        HorizontalDivider()
                    },
                )
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(MiyoSpacing.medium),
        ) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = MiyoSpacing.large),
                    horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.small),
                ) {
                    items(BrowseSourceTab.entries) { option ->
                        FilterChip(
                            selected = state.selectedTab == option,
                            onClick = { viewModel.setTab(option) },
                            label = { Text(option.label, fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(14.dp),
                        )
                    }
                }
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = MiyoSpacing.large),
                    horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.small),
                ) {
                    item {
                        FilterChip(
                            selected = state.languageFilter.isEmpty(),
                            onClick = viewModel::clearLanguageFilter,
                            label = { Text("All languages", fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(14.dp),
                        )
                    }
                    items(state.availableLanguages) { language ->
                        FilterChip(
                            selected = language in state.languageFilter,
                            onClick = { viewModel.toggleLanguageFilter(language) },
                            label = { Text(language, fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(14.dp),
                        )
                    }
                }
            }

            item {
                BrowseStatusStrip(
                    onOpenDownloads = onOpenDownloads,
                    onOpenUpdates = onOpenUpdates,
                )
            }

            if (state.recommendedSources.isNotEmpty()) {
                item {
                    SourceRecommendationStrip(
                        title = "Recommended routes",
                        sources = state.recommendedSources,
                        onOpenSource = { source ->
                            viewModel.recordSourceOpened(source.id)
                            onOpenSource(source.id)
                        },
                    )
                }
            }

            item {
                Text(
                    text = "Sources expose structured metadata: id, site, language, version, manifest URL, parser capabilities, and source-specific settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryText,
                    modifier = Modifier.padding(horizontal = MiyoSpacing.large),
                )
            }

            if (pinnedSources.isNotEmpty()) {
                item { SourceSectionHeader("Pinned sources") }
                items(pinnedSources, key = { "pinned:${it.id}" }) { source ->
                    SourcePluginCard(
                        source = source,
                        pinned = true,
                        lastUsed = false,
                        onTogglePin = { viewModel.togglePinSource(source.id) },
                        onOpen = {
                            viewModel.recordSourceOpened(source.id)
                            onOpenSource(source.id)
                        },
                    )
                }
            }

            lastUsedSource?.let { source ->
                item { SourceSectionHeader("Last used") }
                item(key = "last-used:${source.id}") {
                    SourcePluginCard(
                        source = source,
                        pinned = false,
                        lastUsed = true,
                        onTogglePin = { viewModel.togglePinSource(source.id) },
                        onOpen = {
                            viewModel.recordSourceOpened(source.id)
                            onOpenSource(source.id)
                        },
                    )
                }
            }

            item {
                SourceSectionHeader(
                    when {
                        state.sourceQuery.isNotBlank() -> "Search results"
                        state.selectedTab == BrowseSourceTab.AVAILABLE -> "Available plugin slots"
                        else -> "Installed sources"
                    },
                )
            }

            items(mainSources, key = { it.id }) { source ->
                SourcePluginCard(
                    source = source,
                    pinned = source.id in state.pinnedSourceIds,
                    lastUsed = source.id == state.lastUsedSourceId,
                    onTogglePin = { viewModel.togglePinSource(source.id) },
                    onOpen = {
                        viewModel.recordSourceOpened(source.id)
                        onOpenSource(source.id)
                    },
                )
            }

            if (visibleSources.isEmpty()) {
                item {
                    MiyoEmptyScreen(
                        icon = Icons.Outlined.Extension,
                        title = "No sources found",
                        message = "Change the filter or add a plugin repository.",
                        actionLabel = "Repositories",
                        onAction = onOpenRepositories,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceSectionHeader(title: String) {
    val colors = LocalMIYUColors.current
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
        color = colors.secondaryText,
        modifier = Modifier.padding(horizontal = MiyoSpacing.large),
    )
}

@Composable
private fun BrowseSearchLauncher(
    onClick: () -> Unit,
    actions: @Composable () -> Unit,
) {
    val colors = LocalMIYUColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MiyoSpacing.medium, vertical = MiyoSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.small),
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(28.dp),
            color = colors.cardBackground,
            border = BorderStroke(1.dp, colors.secondaryText.copy(alpha = 0.28f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = MiyoSpacing.large),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.medium),
            ) {
                Icon(Icons.Outlined.TravelExplore, contentDescription = null, tint = colors.secondaryText)
                Text(
                    "Search novels, sources...",
                    color = colors.secondaryText,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        actions()
    }
}

@Composable
fun SourceWorkflowDetailScreen(
    sourceId: String,
    onBack: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenVerifier: () -> Unit,
    onOpenNovel: (OnlineNovelSummary) -> Unit,
    onImportGeneratedEpub: (GeneratedOnlineNovelEpub) -> Unit,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val source = viewModel.source(sourceId)
    val colors = LocalMIYUColors.current

    MiyoWorkspaceSurface {
        MiyoWorkspaceExitButton(label = "Exit source", onClick = onBack)

        if (source == null) {
            MiyoEmptyScreen(
                icon = Icons.Outlined.ErrorOutline,
                title = "Source not found",
                message = "Return to Browse and choose an installed source.",
                modifier = Modifier.fillMaxSize(),
            )
            return@MiyoWorkspaceSurface
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(MiyoSpacing.medium),
        ) {
            item {
                SourceHeroCard(source = source)
            }

            if (source.installState != NovelSourceInstallState.INSTALLED) {
                item {
                    SourceNoticeCard(
                        icon = Icons.Outlined.Extension,
                        title = "Plugin install flow",
                        message = "This source is an external plugin slot. Repository manifests are modeled now; runtime install/update execution is isolated behind the plugin registry.",
                    )
                }
            } else if (source.requiresVerification) {
                item {
                    SourceNoticeCard(
                        icon = Icons.Outlined.Verified,
                        title = "Verification required",
                        message = "${source.name} is protected by Cloudflare. Complete verification in Miyo, then return here before searching or downloading.",
                        actionLabel = "Open verifier",
                        onAction = onOpenVerifier,
                    )
                }
            } else {
                item {
                    SourceSearchPanel(
                        query = state.novelQuery,
                        loading = state.loading,
                        onQueryChange = viewModel::setNovelQuery,
                        onSearch = { viewModel.searchSource(source.id, loadMore = false) },
                    )
                }
                state.error?.let { error ->
                    item {
                        SourceNoticeCard(
                            icon = Icons.Outlined.ErrorOutline,
                            title = "Source error",
                            message = error,
                            isError = true,
                        )
                    }
                }
                state.generatedEpub?.let { generated ->
                    item {
                        GeneratedEpubCard(
                            generated = generated,
                            onImport = {},
                        )
                    }
                }
                items(state.results.chunked(3), key = { row -> row.joinToString { "${it.providerId}:${it.path}" } }) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MiyoSpacing.small),
                        horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.extraSmall),
                    ) {
                        row.forEach { novel ->
                            NovelCoverTile(
                                novel = novel,
                                onOpen = { onOpenNovel(novel) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - row.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
                if (state.results.isEmpty() && state.searchedSourceId == source.id && !state.loading && state.error == null) {
                    item {
                        MiyoEmptyScreen(
                            icon = Icons.Outlined.MenuBook,
                            title = "No novels found",
                            message = "Try a broader title, author, or source search.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 28.dp),
                        )
                    }
                }
                if (state.hasMore) {
                    item {
                        OutlinedButton(
                            onClick = { viewModel.searchSource(source.id, loadMore = true) },
                            enabled = !state.loading,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Load more")
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onOpenDownloads,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open download queue")
                }
            }
        }
    }
}

@Composable
fun GlobalSourceSearchScreen(
    onBack: () -> Unit,
    onOpenSource: (String) -> Unit,
    onOpenVerifier: (String) -> Unit,
    onOpenNovel: (OnlineNovelSummary) -> Unit,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalMIYUColors.current
    val protectedSources = state.installedSources.filter { it.requiresVerification }
    val progress = if (state.globalSearchTotal > 0) {
        state.globalSearchProgress / state.globalSearchTotal.toFloat()
    } else {
        0f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(colors.background),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MiyoSpacing.small, vertical = MiyoSpacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.onBackground)
                }
                OutlinedTextField(
                    value = state.globalSearchQuery,
                    onValueChange = viewModel::setGlobalSearchQuery,
                    placeholder = { Text("Search...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { viewModel.searchAllSources() },
                    ),
                    trailingIcon = {
                        if (state.globalSearchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setGlobalSearchQuery("") }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MiyoSpacing.small),
                horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.small),
            ) {
                FilterChip(
                    selected = state.globalSourceFilter == GlobalSourceFilter.PINNED,
                    onClick = { viewModel.setGlobalSourceFilter(GlobalSourceFilter.PINNED) },
                    leadingIcon = { Icon(Icons.Outlined.PushPin, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text("Pinned") },
                )
                FilterChip(
                    selected = state.globalSourceFilter == GlobalSourceFilter.ALL,
                    onClick = { viewModel.setGlobalSourceFilter(GlobalSourceFilter.ALL) },
                    leadingIcon = { Icon(Icons.Outlined.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text("All") },
                )
                FilterChip(
                    selected = state.globalOnlyHasResults,
                    onClick = viewModel::toggleGlobalOnlyHasResults,
                    leadingIcon = { Icon(Icons.Outlined.FilterList, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text("Has results") },
                )
            }
            if (progress > 0f && progress < 1f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.accent,
                )
            }
            HorizontalDivider()
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 44.dp),
            verticalArrangement = Arrangement.spacedBy(MiyoSpacing.small),
        ) {
            if (state.globalSearchQuery.isBlank() && state.globalSearchResults.isEmpty()) {
                item {
                    SearchSpaceLanding(
                        recommendedQueries = state.recommendedQueries,
                        history = state.browseSearchHistory,
                        sources = state.recommendedSources,
                        onQuerySelected = { query ->
                            viewModel.setGlobalSearchQuery(query)
                            viewModel.searchAllSources(query)
                        },
                        onRemoveHistory = viewModel::removeBrowseSearchQuery,
                        onClearHistory = viewModel::clearBrowseSearchHistory,
                        onOpenSource = { source ->
                            viewModel.recordSourceOpened(source.id)
                            onOpenSource(source.id)
                        },
                    )
                }
                if (protectedSources.isNotEmpty()) {
                    item { SourceSectionHeader("Needs verification") }
                    items(protectedSources, key = { "verify:${it.id}" }) { source ->
                        SourceVerificationPromptCard(
                            source = source,
                            onOpenSource = { onOpenSource(source.id) },
                            onOpenVerifier = { onOpenVerifier(source.id) },
                        )
                    }
                }
            } else {
                items(state.visibleGlobalSearchResults, key = { "global:${it.source.id}" }) { result ->
                    GlobalSearchSourceResultBlock(
                        result = result,
                        onOpenSource = {
                            viewModel.recordSourceOpened(result.source.id)
                            onOpenSource(result.source.id)
                        },
                        onOpenNovel = onOpenNovel,
                    )
                }
                if (state.visibleGlobalSearchResults.isEmpty() && !state.loading) {
                    item {
                        MiyoEmptyScreen(
                            icon = Icons.Outlined.Search,
                            title = "No results found",
                            message = "Try all sources, a shorter title, or another provider.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnlineNovelDetailsScreen(
    providerId: String,
    path: String,
    fallbackTitle: String?,
    onBack: () -> Unit,
    onImportGeneratedEpub: (GeneratedOnlineNovelEpub) -> Unit,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val provider = remember(providerId) {
        runCatching { OnlineNovelProviderId.valueOf(providerId) }.getOrNull()
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalMIYUColors.current

    LaunchedEffect(provider, path, fallbackTitle) {
        if (provider != null) {
            viewModel.loadNovelDetails(provider, path, fallbackTitle)
        }
    }

    MiyoWorkspaceSurface {
        MiyoWorkspaceExitButton(label = "Exit novel details", onClick = onBack)

        if (provider == null) {
            MiyoEmptyScreen(
                icon = Icons.Outlined.ErrorOutline,
                title = "Provider not found",
                message = "Return to Browse and open the result again.",
                modifier = Modifier.fillMaxSize(),
            )
            return@MiyoWorkspaceSurface
        }

        val details = state.selectedNovelDetails
        val summary = state.selectedNovelSummary
        val downloadKey = details?.let { "${it.providerId}:${it.path}" }
        val downloading = downloadKey?.let { state.downloadingKey == it } == true
        val downloadProgress = state.downloadProgress?.takeIf { it.key == downloadKey }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(MiyoSpacing.medium),
        ) {
            item {
                OnlineNovelHeroCard(
                    details = details,
                    summary = summary,
                    fallbackTitle = fallbackTitle,
                    loading = state.detailsLoading,
                    downloading = downloading,
                    downloadProgress = downloadProgress,
                    onDownload = {
                        details?.let(viewModel::downloadNovelDetails)
                    },
                )
            }
            state.error?.let { error ->
                item {
                    SourceNoticeCard(
                        icon = Icons.Outlined.ErrorOutline,
                        title = "Provider error",
                        message = error,
                        isError = true,
                    )
                }
            }
            state.generatedEpub?.let { generated ->
                item {
                    GeneratedEpubCard(
                        generated = generated,
                        onImport = {},
                    )
                }
            }
            if (details == null && state.detailsLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = colors.accent)
                    }
                }
            }
            details?.let { novel ->
                item { OnlineNovelDescriptionCard(novel) }
                item { OnlineNovelTagCard(novel) }
                item { OnlineNovelChapterCard(novel.chapters) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnlineNovelHeroCard(
    details: OnlineNovelDetails?,
    summary: OnlineNovelSummary?,
    fallbackTitle: String?,
    loading: Boolean,
    downloading: Boolean,
    downloadProgress: BrowseDownloadProgress?,
    onDownload: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    val title = details?.title ?: summary?.title ?: fallbackTitle ?: "Novel"
    val author = details?.author ?: summary?.author ?: "Unknown Author"
    val coverUrl = details?.coverUrl ?: summary?.coverUrl
    val providerLabel = details?.providerLabel ?: summary?.providerLabel ?: "Provider"
    val chapterCount = details?.chapterCount ?: summary?.chapterCount ?: details?.chapters?.size
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier = Modifier.padding(MiyoSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(MiyoSpacing.medium),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.medium)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colors.secondaryText.copy(alpha = 0.12f),
                    modifier = Modifier.size(width = 108.dp, height = 150.dp),
                ) {
                    if (coverUrl != null) {
                        AsyncImage(
                            model = coverUrl,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = colors.accent)
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(MiyoSpacing.small),
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = colors.onBackground,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.small),
                        verticalArrangement = Arrangement.spacedBy(MiyoSpacing.small),
                    ) {
                        SourcePill(providerLabel)
                        details?.status?.takeIf { it.isNotBlank() && it != "Unknown" }?.let { SourcePill(it) }
                        chapterCount?.let { SourcePill("$it chapters") }
                    }
                    if (downloadProgress != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                progress = { downloadProgress.fraction },
                                color = colors.accent,
                                trackColor = colors.secondaryText.copy(alpha = 0.18f),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                "${downloadProgress.completed}/${downloadProgress.total} chapters",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = colors.secondaryText,
                            )
                        }
                    }
                    Button(
                        onClick = onDownload,
                        enabled = details != null && details.chapters.isNotEmpty() && !downloading,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        if (downloading || loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                downloading -> downloadProgress?.let { "Exporting ${it.completed}/${it.total}" } ?: "Exporting"
                                details == null -> "Loading"
                                details.chapters.isEmpty() -> "No chapters"
                                else -> "Download EPUB"
                            },
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineNovelDescriptionCard(novel: OnlineNovelDetails) {
    val colors = LocalMIYUColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(MiyoSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(MiyoSpacing.small),
        ) {
            Text(
                "Description",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = colors.onBackground,
            )
            Text(
                novel.summary.ifBlank { "No description was found for this provider result." },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryText,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnlineNovelTagCard(novel: OnlineNovelDetails) {
    val colors = LocalMIYUColors.current
    val tags = (novel.genres + novel.tags).distinct().take(32)
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(MiyoSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(MiyoSpacing.small),
        ) {
            Text(
                "Tags",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = colors.onBackground,
            )
            if (tags.isEmpty()) {
                Text("No tags found.", color = colors.secondaryText, style = MaterialTheme.typography.bodyMedium)
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.small),
                    verticalArrangement = Arrangement.spacedBy(MiyoSpacing.small),
                ) {
                    tags.forEach { tag ->
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    tag,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            enabled = false,
                            shape = RoundedCornerShape(14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineNovelChapterCard(chapters: List<OnlineChapterSummary>) {
    val colors = LocalMIYUColors.current
    val visibleChapters = chapters.sortedBy { it.order }.take(120)
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = MiyoSpacing.small)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MiyoSpacing.medium, vertical = MiyoSpacing.small),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Chapters",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = colors.onBackground,
                )
                Text(
                    chapters.size.toString(),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.secondaryText,
                )
            }
            if (chapters.isEmpty()) {
                Text(
                    "No chapters were found. Try another provider result or refresh the source later.",
                    modifier = Modifier.padding(horizontal = MiyoSpacing.medium, vertical = MiyoSpacing.small),
                    color = colors.secondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                visibleChapters.forEach { chapter ->
                    OnlineChapterRow(chapter)
                }
                if (visibleChapters.size < chapters.size) {
                    Text(
                        "Showing first ${visibleChapters.size} chapters.",
                        modifier = Modifier.padding(horizontal = MiyoSpacing.medium, vertical = MiyoSpacing.small),
                        color = colors.secondaryText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun OnlineChapterRow(chapter: OnlineChapterSummary) {
    val colors = LocalMIYUColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MiyoSpacing.medium, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            chapter.order.toString(),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
            color = colors.accent,
            modifier = Modifier.width(40.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                chapter.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            chapter.updatedAt?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SearchSpaceLanding(
    recommendedQueries: List<String>,
    history: List<String>,
    sources: List<NovelSourcePluginItem>,
    onQuerySelected: (String) -> Unit,
    onRemoveHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onOpenSource: (NovelSourcePluginItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MiyoSpacing.large),
        verticalArrangement = Arrangement.spacedBy(MiyoSpacing.large),
    ) {
        SourceSectionHeader("Recommendations")
        LazyRow(
            contentPadding = PaddingValues(horizontal = MiyoSpacing.large),
            horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.small),
        ) {
            items(recommendedQueries, key = { "query:$it" }) { query ->
                FilterChip(
                    selected = false,
                    onClick = { onQuerySelected(query) },
                    label = { Text(query, fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(12.dp),
                )
            }
        }

        if (sources.isNotEmpty()) {
            SourceRecommendationStrip(
                title = "Recommended sources",
                sources = sources,
                onOpenSource = onOpenSource,
            )
        }

        if (history.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MiyoSpacing.large),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Search history",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                    color = LocalMIYUColors.current.secondaryText,
                )
                TextButton(onClick = onClearHistory) {
                    Text("Clear")
                }
            }
            history.forEach { query ->
                SearchHistoryRow(
                    query = query,
                    onClick = { onQuerySelected(query) },
                    onRemove = { onRemoveHistory(query) },
                )
            }
        }
    }
}

@Composable
private fun SearchHistoryRow(
    query: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = MiyoSpacing.large, end = MiyoSpacing.small, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.History, contentDescription = null, tint = colors.secondaryText)
        Spacer(Modifier.width(MiyoSpacing.large))
        Text(
            query,
            modifier = Modifier.weight(1f),
            color = colors.onBackground,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Outlined.Close, contentDescription = "Remove", tint = colors.secondaryText)
        }
    }
}

@Composable
private fun GlobalSearchSourceResultBlock(
    result: GlobalSearchSourceResult,
    onOpenSource: () -> Unit,
    onOpenNovel: (OnlineNovelSummary) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenSource)
                .padding(start = MiyoSpacing.large, end = MiyoSpacing.large, top = 18.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    result.source.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = LocalMIYUColors.current.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    result.source.language,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalMIYUColors.current.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onOpenSource) {
                Text("Show all", color = LocalMIYUColors.current.accent, fontWeight = FontWeight.Bold)
            }
        }

        when {
            result.loading -> GlobalSearchLoadingResultItem()
            result.error != null -> GlobalSearchErrorResultItem(result.error)
            result.novels.isEmpty() -> Text(
                text = "No results found",
                modifier = Modifier.padding(horizontal = MiyoSpacing.large, vertical = MiyoSpacing.small),
                color = LocalMIYUColors.current.secondaryText,
            )
            else -> LazyRow(
                contentPadding = PaddingValues(horizontal = MiyoSpacing.large, vertical = MiyoSpacing.small),
                horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.medium),
            ) {
                items(result.novels, key = { "${result.source.id}:${it.path}" }) { novel ->
                    GlobalNovelCoverTile(
                        novel = novel,
                        onOpen = { onOpenNovel(novel) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GlobalSearchLoadingResultItem() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MiyoSpacing.large),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun GlobalSearchErrorResultItem(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MiyoSpacing.large, vertical = MiyoSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(MiyoSpacing.large))
        Text(message, color = LocalMIYUColors.current.secondaryText, style = MaterialTheme.typography.titleMedium)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SourceVerifierScreen(
    sourceId: String,
    onBack: () -> Unit,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val source = viewModel.source(sourceId)
    val colors = LocalMIYUColors.current
    var loading by remember { mutableStateOf(true) }
    var pageTitle by remember { mutableStateOf("Verifier") }
    val verificationUrl = source?.verificationUrl ?: source?.site?.let { "https://$it" }

    MiyoWorkspaceSurface {
        MiyoWorkspaceExitButton(label = "Exit verifier", onClick = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 44.dp),
            verticalArrangement = Arrangement.spacedBy(MiyoSpacing.medium),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(MiyoSpacing.large),
                        verticalArrangement = Arrangement.spacedBy(MiyoSpacing.medium),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SourceIconHeader(icon = Icons.Outlined.Verified)
                            Spacer(Modifier.width(MiyoSpacing.medium))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${source?.name ?: "Source"} verification",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                                    color = colors.onBackground,
                                )
                                Text(
                                    if (loading) "Loading protected page..." else pageTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.secondaryText,
                                )
                            }
                            if (loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = colors.accent,
                                    strokeWidth = 3.dp,
                                )
                            }
                        }
                        Text(
                            "Complete the provider challenge here. The WebView is contained inside Miyo so it does not break the Browse layout.",
                            color = colors.secondaryText,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.20f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (verificationUrl.isNullOrBlank()) {
                        MiyoEmptyScreen(
                            icon = Icons.Outlined.ErrorOutline,
                            title = "No verifier URL",
                            message = "This source does not expose a verification URL.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp),
                        )
                    } else {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(520.dp),
                            factory = { context ->
                                WebView(context).apply {
                                    CookieManager.getInstance().setAcceptCookie(true)
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.databaseEnabled = true
                                    settings.mediaPlaybackRequiresUserGesture = true
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                            loading = true
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            loading = false
                                            pageTitle = view?.title?.takeIf { it.isNotBlank() } ?: url.orEmpty()
                                        }
                                    }
                                    loadUrl(verificationUrl)
                                }
                            },
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProviderRepositoriesScreen(
    onBack: () -> Unit,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalMIYUColors.current
    var repositoryUrl by remember { mutableStateOf("") }

    MiyoWorkspaceSurface {
        MiyoWorkspaceExitButton(label = "Exit repositories", onClick = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 44.dp),
            verticalArrangement = Arrangement.spacedBy(MiyoSpacing.medium),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(MiyoSpacing.large),
                        verticalArrangement = Arrangement.spacedBy(MiyoSpacing.medium),
                    ) {
                        SourceIconHeader(icon = Icons.Outlined.Extension)
                        Text(
                            "Plugin Repositories",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                            color = colors.onBackground,
                        )
                        Text(
                            "Source repositories are stored as HTTPS manifest URLs. Built-in parsers ship locally, while external source code stays isolated behind repository plugins.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.secondaryText,
                        )
                        OutlinedTextField(
                            value = repositoryUrl,
                            onValueChange = { repositoryUrl = it },
                            placeholder = { Text("https://example.com/plugins.json") },
                            leadingIcon = { Icon(Icons.Outlined.Public, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                        )
                        Button(
                            onClick = {
                                viewModel.addRepositoryUrl(repositoryUrl)
                                repositoryUrl = ""
                            },
                            enabled = repositoryUrl.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Add repository", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            state.error?.let { error ->
                item {
                    SourceNoticeCard(
                        icon = Icons.Outlined.ErrorOutline,
                        title = "Repository error",
                        message = error,
                        isError = true,
                    )
                }
            }

            item {
                SourceSectionHeader("Saved repositories")
            }

            if (state.repositoryUrls.isEmpty()) {
                item {
                    MiyoEmptyScreen(
                        icon = Icons.Outlined.Extension,
                        title = "No repositories added",
                        message = "Add an HTTPS source manifest URL when you are ready to install third-party source plugins.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                    )
                }
            } else {
                items(state.repositoryUrls.toList().sorted(), key = { it }) { url ->
                    RepositoryUrlCard(
                        url = url,
                        onRemove = { viewModel.removeRepositoryUrl(url) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RepositoryUrlCard(
    url: String,
    onRemove: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(MiyoSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SourceIconHeader(icon = Icons.Outlined.Public)
            Spacer(Modifier.width(MiyoSpacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Repository manifest",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                    color = colors.onBackground,
                )
                Text(
                    url,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            MiyoIconActionButton(
                icon = Icons.Outlined.Delete,
                contentDescription = "Remove repository",
                onClick = onRemove,
                modifier = Modifier.size(46.dp),
            )
        }
    }
}

@Composable
fun MigrationWorkflowScreen(
    onBack: () -> Unit,
) {
    WorkflowWorkspaceScreen(
        exitLabel = "Exit migration",
        title = "Source Migration",
        subtitle = "Move a library novel from one source plugin to another without losing progress.",
        icon = Icons.Outlined.SwapHoriz,
        onBack = onBack,
        steps = listOf(
            "Pick an existing library novel.",
            "Search matching titles across installed source plugins.",
            "Compare metadata, chapter counts, and latest updates.",
            "Swap the source binding while keeping terms, progress, and history.",
        ),
    )
}

@Composable
fun DownloadsWorkflowScreen(
    onBack: () -> Unit,
) {
    WorkflowWorkspaceScreen(
        exitLabel = "Exit downloads",
        title = "Downloads",
        subtitle = "Queued chapter downloads, EPUB export, retry state, and Android notifications.",
        icon = Icons.Outlined.CloudDownload,
        onBack = onBack,
        steps = listOf(
            "Pending, running, completed, and failed tasks stay separated.",
            "Each task owns chapter progress and EPUB export progress.",
            "Provider retry/backoff will be connected through the source registry.",
            "Completed EPUBs flow into the local library import pipeline.",
        ),
    )
}

@Composable
fun SourceUpdatesWorkflowScreen(
    onBack: () -> Unit,
) {
    WorkflowWorkspaceScreen(
        exitLabel = "Exit updates",
        title = "Updates",
        subtitle = "Track fresh chapters from followed source plugins in one place.",
        icon = Icons.Outlined.Update,
        onBack = onBack,
        steps = listOf(
            "Track followed online novels by source id.",
            "Group new chapters by today, yesterday, and older updates.",
            "Queue unread chapters directly from the update list.",
            "Update checks run through the same plugin interface as Browse.",
        ),
    )
}

@Composable
private fun BrowseStatusStrip(
    onOpenDownloads: () -> Unit,
    onOpenUpdates: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MiyoSpacing.large),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(MiyoSpacing.medium),
            horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QueueMetric(
                icon = Icons.Outlined.Update,
                title = "Updates",
                value = "Ready",
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenUpdates),
            )
            QueueMetric(
                icon = Icons.Outlined.CloudDownload,
                title = "Downloads",
                value = "0 queued",
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenDownloads),
            )
        }
    }
}

@Composable
private fun SourceRecommendationStrip(
    title: String,
    sources: List<NovelSourcePluginItem>,
    onOpenSource: (NovelSourcePluginItem) -> Unit,
) {
    val colors = LocalMIYUColors.current
    Column(verticalArrangement = Arrangement.spacedBy(MiyoSpacing.small)) {
        SourceSectionHeader(title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = MiyoSpacing.large),
            horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.small),
        ) {
            items(sources, key = { "recommended:${it.id}" }) { source ->
                Card(
                    modifier = Modifier
                        .width(244.dp)
                        .clickable { onOpenSource(source) },
                    colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.18f)),
                ) {
                    Column(
                        modifier = Modifier.padding(MiyoSpacing.medium),
                        verticalArrangement = Arrangement.spacedBy(MiyoSpacing.small),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SourceIcon(source = source)
                            Spacer(Modifier.width(MiyoSpacing.small))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    source.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                    color = colors.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    if (source.requiresVerification) "Verify before search" else "Direct parser route",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.secondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SourcePill(source.language)
                            SourcePill(source.version)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueMetric(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMIYUColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = colors.accent.copy(alpha = 0.10f),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(MiyoSpacing.small))
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall, color = colors.secondaryText)
                Text(value, style = MaterialTheme.typography.titleSmall, color = colors.onBackground, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SourcePluginCard(
    source: NovelSourcePluginItem,
    pinned: Boolean,
    lastUsed: Boolean,
    onTogglePin: () -> Unit,
    onOpen: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    val installed = source.installState == NovelSourceInstallState.INSTALLED
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = MiyoSpacing.large, vertical = MiyoSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SourceIcon(source = source)
        Spacer(Modifier.width(MiyoSpacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = source.name,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = source.language,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (installed) {
            TextButton(onClick = onOpen) {
                Text("Latest", color = colors.accent, fontWeight = FontWeight.Bold)
            }
        }
        IconButton(onClick = onTogglePin) {
            Icon(
                if (pinned) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                contentDescription = if (pinned) "Unpin source" else "Pin source",
                tint = if (pinned || lastUsed) colors.accent else colors.secondaryText,
            )
        }
    }
}

@Composable
private fun SourceHeroCard(source: NovelSourcePluginItem) {
    val colors = LocalMIYUColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(MiyoSpacing.large)) {
            SourceIcon(source = source, large = true)
            Spacer(Modifier.height(MiyoSpacing.large))
            Text(
                source.name,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = colors.onBackground,
            )
            Text(source.site, style = MaterialTheme.typography.titleMedium, color = colors.secondaryText)
            Spacer(Modifier.height(MiyoSpacing.medium))
            Text(source.description, style = MaterialTheme.typography.bodyLarge, color = colors.secondaryText)
            Spacer(Modifier.height(MiyoSpacing.medium))
            Row(horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.small)) {
                SourcePill(source.language)
                SourcePill(source.version)
                SourcePill(if (source.kind == NovelSourceKind.BUILT_IN) "Built-in" else "External")
            }
        }
    }
}

@Composable
private fun SourceIcon(source: NovelSourcePluginItem, large: Boolean = false) {
    val colors = LocalMIYUColors.current
    Surface(
        shape = RoundedCornerShape(if (large) 24.dp else 16.dp),
        color = colors.accent.copy(alpha = 0.13f),
        modifier = Modifier.size(if (large) 76.dp else 54.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = when {
                    source.requiresVerification -> Icons.Outlined.Verified
                    source.kind == NovelSourceKind.EXTERNAL_JS -> Icons.Outlined.Extension
                    else -> Icons.Outlined.Public
                },
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(if (large) 34.dp else 26.dp),
            )
        }
    }
}

@Composable
private fun SourceSearchPanel(
    query: String,
    loading: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(MiyoSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(MiyoSpacing.medium),
        ) {
            Text(
                "Search novels",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = colors.onBackground,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.small)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Title, author, keyword...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
                Button(
                    onClick = onSearch,
                    enabled = !loading,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(56.dp),
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Search", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneratedEpubCard(
    generated: GeneratedOnlineNovelEpub,
    onImport: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.30f)),
    ) {
        Column(
            modifier = Modifier.padding(MiyoSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(MiyoSpacing.small),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = colors.accent)
                Spacer(Modifier.width(MiyoSpacing.small))
                Text(
                    "EPUB generated",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                    color = colors.onBackground,
                )
            }
            Text(
                "${generated.title} was exported as ${generated.fileName}.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryText,
            )
        }
    }
}

@Composable
private fun GlobalNovelCoverTile(
    novel: OnlineNovelSummary,
    onOpen: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    Column(
        modifier = Modifier
            .width(128.dp)
            .clickable(onClick = onOpen),
    ) {
        Box {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.secondaryText.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f),
            ) {
                if (novel.coverUrl != null) {
                    AsyncImage(
                        model = novel.coverUrl,
                        contentDescription = novel.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = colors.accent)
                    }
                }
            }
            Surface(
                color = colors.accent,
                shape = RoundedCornerShape(7.dp),
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopStart),
            ) {
                Text(
                    novel.providerLabel,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            novel.title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = colors.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NovelCoverTile(
    novel: OnlineNovelSummary,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp? = null,
) {
    val colors = LocalMIYUColors.current
    Column(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
            .clickable(onClick = onOpen)
            .padding(4.dp),
    ) {
        Box {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = colors.accent.copy(alpha = 0.10f),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f),
            ) {
                if (novel.coverUrl != null) {
                    AsyncImage(
                        model = novel.coverUrl,
                        contentDescription = novel.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = colors.accent)
                    }
                }
            }
            Surface(
                color = colors.accent,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .padding(6.dp)
                    .align(Alignment.TopStart),
            ) {
                Text(
                    novel.providerLabel,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            novel.title,
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = colors.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            novel.author,
            style = MaterialTheme.typography.labelSmall,
            color = colors.secondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SourceVerificationPromptCard(
    source: NovelSourcePluginItem,
    onOpenSource: () -> Unit,
    onOpenVerifier: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    Card(
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.28f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(MiyoSpacing.medium),
            verticalArrangement = Arrangement.spacedBy(MiyoSpacing.small),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SourceIcon(source = source)
                Spacer(Modifier.width(MiyoSpacing.medium))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "${source.name} needs verification",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = colors.onBackground,
                    )
                    Text(
                        "Complete the provider challenge before WTR-LAB results can load.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.secondaryText,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.small)) {
                OutlinedButton(
                    onClick = onOpenSource,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("See source", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onOpenVerifier,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Verify", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SourceNoticeCard(
    icon: ImageVector,
    title: String,
    message: String,
    isError: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = LocalMIYUColors.current
    val tint = if (isError) MaterialTheme.colorScheme.error else colors.accent
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.10f) else colors.cardBackground,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.24f)),
    ) {
        Row(
            modifier = Modifier.padding(MiyoSpacing.medium),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(MiyoSpacing.medium))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = colors.onBackground)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.secondaryText)
                if (actionLabel != null && onAction != null) {
                    Spacer(Modifier.height(MiyoSpacing.small))
                    Button(
                        onClick = onAction,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(actionLabel, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SourcePill(text: String) {
    val colors = LocalMIYUColors.current
    Surface(
        shape = RoundedCornerShape(50),
        color = colors.accent.copy(alpha = 0.10f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = colors.accent,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WorkflowWorkspaceScreen(
    exitLabel: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    onBack: () -> Unit,
    steps: List<String>,
) {
    MiyoWorkspaceSurface {
        MiyoWorkspaceExitButton(label = exitLabel, onClick = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(MiyoSpacing.medium),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LocalMIYUColors.current.cardBackground),
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.padding(MiyoSpacing.large)) {
                        SourceIconHeader(icon = icon)
                        Spacer(Modifier.height(MiyoSpacing.large))
                        Text(title, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black))
                        Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = LocalMIYUColors.current.secondaryText)
                        Spacer(Modifier.height(MiyoSpacing.large))
                        steps.forEachIndexed { index, step ->
                            WorkflowStepCard(
                                step = (index + 1).toString(),
                                text = step,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceIconHeader(icon: ImageVector) {
    val colors = LocalMIYUColors.current
    Surface(
        shape = CircleShape,
        color = colors.accent.copy(alpha = 0.14f),
        modifier = Modifier.size(72.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun WorkflowStepCard(
    step: String,
    text: String,
) {
    val colors = LocalMIYUColors.current
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            shape = CircleShape,
            color = colors.accent.copy(alpha = 0.14f),
            modifier = Modifier.size(30.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(step, color = colors.accent, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.width(MiyoSpacing.medium))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.secondaryText)
    }
}
