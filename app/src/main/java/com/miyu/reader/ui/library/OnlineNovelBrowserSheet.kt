package com.miyu.reader.ui.library

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.miyu.reader.data.repository.WtrLabBridgeScript
import com.miyu.reader.domain.model.GeneratedOnlineNovelEpub
import com.miyu.reader.domain.model.OnlineNovelDetails
import com.miyu.reader.domain.model.OnlineNovelProvider
import com.miyu.reader.domain.model.OnlineNovelProviderId
import com.miyu.reader.domain.model.OnlineNovelSummary
import com.miyu.reader.ui.theme.LocalMIYUColors
import com.miyu.reader.viewmodel.OnlineNovelBrowserViewModel
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineNovelBrowserWorkspace(
    onDismiss: () -> Unit,
    onImportGeneratedEpub: (GeneratedOnlineNovelEpub) -> Unit,
    viewModel: OnlineNovelBrowserViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalMIYUColors.current
    val webViewRef = remember { AtomicReference<WebView?>() }
    val selectedProvider = state.providers.firstOrNull { it.id == state.selectedProviderId }

    LaunchedEffect(state.bridgeCommand?.id) {
        val command = state.bridgeCommand ?: return@LaunchedEffect
        webViewRef.get()?.evaluateJavascript(command.script, null)
        viewModel.consumeBridgeCommand(command.id)
    }

    LaunchedEffect(state.generatedEpub?.filePath) {
        val generated = state.generatedEpub ?: return@LaunchedEffect
        onImportGeneratedEpub(generated)
        viewModel.consumeGeneratedEpub()
    }

    LibraryWorkspaceSurface {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                WorkspaceExitButton(label = "Exit online browser", onClick = onDismiss)
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Online MTL Browser",
                            color = colors.onBackground,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            "RN-style providers, protected WTR verification, and EPUB export.",
                            color = colors.secondaryText,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (state.loading || state.downloading) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), color = colors.accent)
                    }
                }
            }
            item {
                ProviderRow(
                    providers = state.providers,
                    selectedProviderId = state.selectedProviderId,
                    onSelectProvider = viewModel::selectProvider,
                )
            }
            if (selectedProvider?.requiresBrowserVerification == true) {
                item {
                    WtrLabVerificationCard(
                        bridgeReady = state.wtrBridgeReady,
                        captchaRequired = state.captchaRequired,
                        captchaBody = state.captchaBody,
                        webViewRef = webViewRef,
                        onBridgeMessage = viewModel::handleWtrBridgeMessage,
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = viewModel::setQuery,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        placeholder = { Text("Search novels...") },
                    )
                    Button(
                        onClick = { viewModel.search(loadMore = false) },
                        enabled = !state.loading && !state.downloading,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("Search")
                    }
                }
            }
            item {
                StatusCard(
                    message = state.statusMessage,
                    error = state.error,
                    provider = selectedProvider,
                )
            }
            if (state.selectedNovel == null) {
                if (state.results.isEmpty() && !state.loading) {
                    item {
                        Text(
                            "No loaded novels yet. Search a provider to begin.",
                            color = colors.secondaryText,
                            modifier = Modifier.padding(vertical = 18.dp),
                        )
                    }
                }
                items(state.results, key = { "${it.providerId}:${it.path}:${it.rawId}" }) { item ->
                    OnlineNovelCard(item = item, onClick = { viewModel.openNovel(item) })
                }
                if (state.hasMore) {
                    item {
                        OutlinedButton(
                            onClick = { viewModel.search(loadMore = true) },
                            enabled = !state.loading,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Load more")
                        }
                    }
                }
            } else {
                item {
                    NovelDetailPanel(
                        novel = state.selectedNovel!!,
                        chapterStart = state.chapterStart,
                        chapterEnd = state.chapterEnd,
                        downloading = state.downloading,
                        downloadCompletedChapters = state.downloadCompletedChapters,
                        downloadTotalChapters = state.downloadTotalChapters,
                        statusMessage = state.statusMessage,
                        onBack = viewModel::backToResults,
                        onChapterStartChange = viewModel::setChapterStart,
                        onChapterEndChange = viewModel::setChapterEnd,
                        onDownload = viewModel::downloadSelectedNovel,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderRow(
    providers: List<OnlineNovelProvider>,
    selectedProviderId: OnlineNovelProviderId,
    onSelectProvider: (OnlineNovelProviderId) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 4.dp),
    ) {
        items(providers, key = { it.id.name }) { provider ->
            FilterChip(
                selected = provider.id == selectedProviderId,
                onClick = { onSelectProvider(provider.id) },
                label = { Text(provider.label, maxLines = 1) },
                leadingIcon = {
                    Icon(
                        if (provider.id == selectedProviderId) Icons.Filled.Check else Icons.Outlined.Public,
                        contentDescription = null,
                    )
                },
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WtrLabVerificationCard(
    bridgeReady: Boolean,
    captchaRequired: Boolean,
    captchaBody: String,
    webViewRef: AtomicReference<WebView?>,
    onBridgeMessage: (String) -> Unit,
) {
    val colors = LocalMIYUColors.current
    var showBrowser by rememberSaveable { mutableStateOf(!bridgeReady) }

    LaunchedEffect(bridgeReady, captchaRequired) {
        showBrowser = captchaRequired || !bridgeReady
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (bridgeReady) "WTR-LAB Browser Verified" else "WTR-LAB Verification",
                        color = colors.onBackground,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (captchaRequired) {
                            "Complete Cloudflare below. The browser is clipped to this card so it cannot cover the app."
                        } else {
                            "This WebView keeps the protected WTR session inside Miyo."
                        },
                        color = colors.secondaryText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (bridgeReady && !captchaRequired) {
                    TextButton(onClick = { showBrowser = !showBrowser }) {
                        Text(
                            if (showBrowser) "Hide verifier" else "Show verifier",
                            color = colors.accent,
                        )
                    }
                }
                IconButton(onClick = {
                    showBrowser = true
                    webViewRef.get()?.reload()
                }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Reload WTR-LAB", tint = colors.accent)
                }
            }
            if (captchaBody.isNotBlank()) {
                Text(
                    captchaBody,
                    color = colors.secondaryText,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Spacer(Modifier.height(if (showBrowser) 8.dp else 1.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        when {
                            !showBrowser -> 1.dp
                            bridgeReady && !captchaRequired -> 150.dp
                            else -> 360.dp
                        }
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = if (showBrowser) 0.04f else 0f)),
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = false
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.allowFileAccessFromFileURLs = false
                            settings.allowUniversalAccessFromFileURLs = false
                            settings.javaScriptCanOpenWindowsAutomatically = false
                            settings.mediaPlaybackRequiresUserGesture = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            settings.safeBrowsingEnabled = true
                            settings.setGeolocationEnabled(false)
                            settings.setSupportMultipleWindows(false)
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            addJavascriptInterface(WtrBridgeInterface(onBridgeMessage), "AndroidWtrBridge")
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    view?.evaluateJavascript(WtrLabBridgeScript.bootstrap, null)
                                }
                            }
                            loadUrl(WtrLabBridgeScript.START_URL)
                        }.also(webViewRef::set)
                    },
                    update = { webView ->
                        webViewRef.set(webView)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            when {
                                !showBrowser -> 1.dp
                                bridgeReady && !captchaRequired -> 150.dp
                                else -> 360.dp
                            }
                        ),
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    message: String,
    error: String?,
    provider: OnlineNovelProvider?,
) {
    val colors = LocalMIYUColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Public, contentDescription = null, tint = colors.accent)
            Column(modifier = Modifier.weight(1f)) {
                Text(provider?.label ?: "Provider", color = colors.onBackground, fontWeight = FontWeight.SemiBold)
                Text(message, color = colors.secondaryText, style = MaterialTheme.typography.bodySmall)
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun OnlineNovelCard(
    item: OnlineNovelSummary,
    onClick: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CoverThumb(url = item.coverUrl, title = item.title, modifier = Modifier.size(62.dp, 86.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, color = colors.onBackground, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(item.author, color = colors.secondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (item.summary.isNotBlank()) {
                    Text(
                        item.summary,
                        color = colors.secondaryText,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                    AssistChip(onClick = {}, label = { Text(item.providerLabel) }, enabled = false)
                    item.chapterCount?.let { AssistChip(onClick = {}, label = { Text("$it chapters") }, enabled = false) }
                }
            }
        }
    }
}

@Composable
private fun NovelDetailPanel(
    novel: OnlineNovelDetails,
    chapterStart: String,
    chapterEnd: String,
    downloading: Boolean,
    downloadCompletedChapters: Int,
    downloadTotalChapters: Int,
    statusMessage: String,
    onBack: () -> Unit,
    onChapterStartChange: (String) -> Unit,
    onChapterEndChange: (String) -> Unit,
    onDownload: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
            Text("Back to results")
        }
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CoverThumb(url = novel.coverUrl, title = novel.title, modifier = Modifier.size(92.dp, 132.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(novel.title, color = colors.onBackground, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Text(novel.author, color = colors.secondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${novel.providerLabel} · ${novel.status}",
                        color = colors.secondaryText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    novel.chapterCount?.let {
                        Text("$it chapters", color = colors.secondaryText, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (novel.summary.isNotBlank()) {
                Text(
                    novel.summary,
                    color = colors.secondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 14.dp),
                )
            }
        }
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Chapter Range", color = colors.onBackground, fontWeight = FontWeight.Bold)
                Text(
                    "Select a safe range to export. Large provider downloads are intentionally chunked.",
                    color = colors.secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = chapterStart,
                        onValueChange = onChapterStartChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Start") },
                    )
                    OutlinedTextField(
                        value = chapterEnd,
                        onValueChange = onChapterEndChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("End") },
                    )
                }
                if (downloadTotalChapters > 0) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { (downloadCompletedChapters.toFloat() / downloadTotalChapters.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$downloadCompletedChapters / $downloadTotalChapters chapters · $statusMessage",
                        color = colors.secondaryText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onDownload,
                    enabled = !downloading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    if (downloading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (downloading) "Downloading..." else "Export EPUB")
                }
            }
        }
        Text(
            "Loaded chapters: ${novel.chapters.size}",
            color = colors.secondaryText,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun CoverThumb(
    url: String?,
    title: String,
    modifier: Modifier,
) {
    val colors = LocalMIYUColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                title.take(1).ifBlank { "M" },
                color = colors.accent,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private class WtrBridgeInterface(
    private val onMessage: (String) -> Unit,
) {
    @JavascriptInterface
    fun postMessage(message: String) {
        onMessage(message)
    }
}
