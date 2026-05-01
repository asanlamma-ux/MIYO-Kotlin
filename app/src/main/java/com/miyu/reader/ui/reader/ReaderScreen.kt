package com.miyu.reader.ui.reader

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ActionMode
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miyu.reader.security.ReaderHtmlSanitizer
import com.miyu.reader.ui.components.ThemePickerBottomSheet
import com.miyu.reader.ui.theme.ReaderColors
import com.miyu.reader.ui.theme.ReaderThemeColors
import com.miyu.reader.ui.theme.SpecialThemeBackdrop
import com.miyu.reader.ui.theme.ThemeBackdropVariant
import com.miyu.reader.ui.theme.specialThemeSceneCopy
import com.miyu.reader.viewmodel.ReaderViewModel
import com.miyu.reader.domain.model.MarginPreset
import com.miyu.reader.domain.model.PageAnimation
import com.miyu.reader.domain.model.ReaderColumnLayout
import com.miyu.reader.domain.model.TapZoneNavMode
import com.miyu.reader.domain.model.TextAlign
import com.miyu.reader.viewmodel.ReaderTermDetail

import android.webkit.JavascriptInterface
import coil.compose.AsyncImage
import com.miyu.reader.ui.reader.components.SelectionToolbar
import com.miyu.reader.ui.reader.components.SelectionData
import org.json.JSONObject
import java.io.ByteArrayInputStream

private const val READER_BASE_URL = "https://miyo-reader.local/reader/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: String,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val readerThemeId by viewModel.readerThemeId.collectAsStateWithLifecycle()
    val readingSettings by viewModel.readingSettings.collectAsStateWithLifecycle()
    val typography by viewModel.typography.collectAsStateWithLifecycle()
    val readerTheme = ReaderColors.findById(readerThemeId)

    val bgColor = readerTheme.background
    val textColor = readerTheme.text
    val accentColor = readerTheme.accent

    // Javascript interface to receive selection coordinates and text
    val jsInterface: ReaderJsInterface = remember(viewModel) {
        ReaderJsInterface(viewModel)
    }
    val readerWebViewRef = remember { java.util.concurrent.atomic.AtomicReference<WebView?>() }
    var lastAppendId by remember { mutableStateOf(-1L) }

    DisposableEffect(Unit) {
        onDispose {
            readerWebViewRef.get()?.evaluateJavascript(
                "if (window.MIYU_POST_PROGRESS) window.MIYU_POST_PROGRESS();",
                null,
            )
            readerWebViewRef.set(null)
        }
    }

    DisposableEffect(readingSettings.immersiveMode, readingSettings.keepScreenOn, context) {
        val activity = context.findActivity()
        val window = activity?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, it.decorView) }

        if (readingSettings.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        if (window != null && controller != null && readingSettings.immersiveMode) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else if (window != null && controller != null) {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            controller.show(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            if (window != null && controller != null) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                WindowCompat.setDecorFitsSystemWindows(window, true)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
    ) {
        // ── Loading state ───────────────────────────────────────────
        if (uiState.isLoading && uiState.chapterHtml.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SpecialThemeBackdrop(
                    theme = readerTheme,
                    modifier = Modifier.matchParentSize(),
                    variant = ThemeBackdropVariant.LOADING,
                )
                BookLoadingIndicator(readerTheme = readerTheme, title = uiState.book?.title)
            }
        } else if (uiState.errorMessage != null && uiState.chapterHtml.isBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(28.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = readerTheme.cardBackground,
                    ) {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.padding(22.dp).size(34.dp),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Reader could not open this chapter",
                        color = textColor,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        uiState.errorMessage.orEmpty(),
                        color = readerTheme.secondaryText,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            // ── WebView ─────────────────────────────────────────────
            val htmlContent = buildReaderHtml(
                chapterHtml = uiState.chapterHtml,
                chapterIndex = uiState.renderedChapterIndex,
                totalChapters = uiState.totalChapters,
                bgColor = colorToHex(bgColor),
                textColor = colorToHex(textColor),
                accentColor = colorToHex(accentColor),
                fontSize = typography.fontSize,
                lineHeight = typography.lineHeight,
                textAlign = typography.textAlign,
                marginPreset = readingSettings.marginPreset,
                contentColumnWidth = readingSettings.contentColumnWidth,
                readerColumnLayout = readingSettings.readerColumnLayout,
                pageAnimation = readingSettings.pageAnimation,
                tapZonesEnabled = readingSettings.tapZonesEnabled,
                tapScrollPageRatio = readingSettings.tapScrollPageRatio,
                tapZoneNavMode = readingSettings.tapZoneNavMode,
                bionicReading = readingSettings.bionicReading,
                autoAdvanceChapter = readingSettings.autoAdvanceChapter,
            )

            AndroidView(
                factory = { context ->
                    ReaderWebView(context).apply {
                        @SuppressLint("SetJavaScriptEnabled")
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = false
                        settings.databaseEnabled = false
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.allowFileAccessFromFileURLs = false
                        settings.allowUniversalAccessFromFileURLs = false
                        settings.blockNetworkLoads = true
                        settings.blockNetworkImage = true
                        settings.javaScriptCanOpenWindowsAutomatically = false
                        settings.mediaPlaybackRequiresUserGesture = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        settings.safeBrowsingEnabled = true
                        settings.setGeolocationEnabled(false)
                        settings.setSupportMultipleWindows(false)
                        settings.cacheMode = WebSettings.LOAD_NO_CACHE
                        settings.loadsImagesAutomatically = true
                        settings.defaultTextEncodingName = "utf-8"
                        setBackgroundColor(bgColor.toArgb())
                        @SuppressLint("JavascriptInterface")
                        addJavascriptInterface(jsInterface, "AndroidBridge")
                        webViewClient = SecureReaderWebViewClient()
                    }.also { readerWebViewRef.set(it) }
                },
                update = { webView ->
                    webView.setBackgroundColor(bgColor.toArgb())
                    val restorePercent = uiState.chapterScrollPercent.coerceIn(0f, 1f)
                    webView.webViewClient = object : SecureReaderWebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            view?.evaluateJavascript(restoreReaderScrollScript(restorePercent), null)
                        }
                    }
                    val baseUrl = READER_BASE_URL
                    val loadKey = "${baseUrl.orEmpty()}#${htmlContent.hashCode()}"
                    if (webView.tag != loadKey) {
                        webView.tag = loadKey
                        webView.loadDataWithBaseURL(baseUrl, htmlContent, "text/html", "UTF-8", null)
                    }
                    uiState.pendingChapterAppend?.let { append ->
                        if (append.id != lastAppendId) {
                            lastAppendId = append.id
                            val quotedHtml = JSONObject.quote(ReaderHtmlSanitizer.sanitize(append.bodyHtml))
                            webView.evaluateJavascript(
                                "if (window.MIYU_APPEND_CHAPTER) window.MIYU_APPEND_CHAPTER($quotedHtml, ${append.chapterIndex});",
                                null,
                            )
                            viewModel.consumePendingChapterAppend(append.id)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // ── Selection Toolbar ───────────────────────────────────────
        if (uiState.selection != null) {
            SelectionToolbar(
                selection = uiState.selection,
                readerTheme = readerTheme,
                onClose = { viewModel.clearSelection() },
                onHighlight = { data -> viewModel.saveHighlight(data) },
                onNote = { data -> viewModel.saveHighlight(data) },
                onCopy = { text ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Selected text", text))
                },
                onShare = { text ->
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TEXT, text)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share"))
                },
                onDictionary = { viewModel.showDictionary(it) },
                onTranslate = { viewModel.showTranslation(it) },
                onBookmarkSelection = { viewModel.addBookmark(it) },
                onAddTerm = { viewModel.showAddTerm(it) },
            )
        }

        uiState.addTermText?.let { selectedText ->
            com.miyu.reader.ui.reader.components.AddTermBottomSheet(
                selectedText = selectedText,
                groups = uiState.activeTermGroups.ifEmpty { emptyList() },
                readerTheme = readerTheme,
                onDismiss = { viewModel.clearAddTerm() },
                onSaveToGroup = viewModel::saveTerm,
                onCreateGroupAndSave = viewModel::createTermGroupAndSave,
            )
        }

        // ── Translation Sheet ───────────────────────────────────────
        if (uiState.translationText != null) {
            com.miyu.reader.ui.reader.components.TranslationSheetBottomSheet(
                sourceText = uiState.translationText!!,
                status = uiState.translationStatus,
                readerTheme = readerTheme,
                onOpenExternal = {
                    val uri = Uri.parse(
                        "https://translate.google.com/?sl=auto&tl=en&text=${Uri.encode(uiState.translationText!!)}&op=translate",
                    )
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                },
                onDismiss = { viewModel.clearTranslation() },
            )
        }

        // ── Dictionary Sheet ────────────────────────────────────────
        if (uiState.dictionaryWord != null) {
            com.miyu.reader.ui.reader.components.DictionaryLookupBottomSheet(
                word = uiState.dictionaryWord!!,
                result = uiState.dictionaryResult,
                downloadedDictionaryCount = uiState.downloadedDictionaryCount,
                isLoading = uiState.dictionaryLoading,
                readerTheme = readerTheme,
                onDismiss = { viewModel.clearDictionary() },
            )
        }

        uiState.selectedTermDetail?.let { detail ->
            TermDetailBottomSheet(
                detail = detail,
                readerTheme = readerTheme,
                onDismiss = { viewModel.clearTermDetail() },
            )
        }

        // ── Top controls overlay ────────────────────────────────────
        AnimatedVisibility(
            visible = uiState.showControls && uiState.selection == null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Surface(
                color = bgColor.copy(alpha = 0.95f),
                shadowElevation = 4.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = textColor)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                    ) {
                        Text(
                            uiState.book?.title ?: "",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = textColor,
                        )
                        Text(
                            "Chapter ${uiState.chapterIndex + 1} of ${uiState.totalChapters}",
                            style = MaterialTheme.typography.labelSmall,
                            color = readerTheme.secondaryText,
                        )
                    }
                    IconButton(onClick = { viewModel.addBookmark("") }) {
                        Icon(Icons.Outlined.BookmarkBorder, "Bookmark", tint = textColor)
                    }
                    IconButton(onClick = { viewModel.toggleAnnotationsDrawer() }) {
                        Icon(Icons.Outlined.Bookmarks, "Annotations", tint = textColor)
                    }
                    IconButton(onClick = { viewModel.toggleStatsModal() }) {
                        Icon(Icons.Outlined.Info, "Stats", tint = textColor)
                    }
                    IconButton(onClick = { viewModel.toggleSearchModal() }) {
                        Icon(Icons.Outlined.Search, "Search", tint = textColor)
                    }
                    IconButton(onClick = { viewModel.toggleChapterDrawer() }) {
                        Icon(Icons.Outlined.FormatListBulleted, "Chapters", tint = textColor)
                    }
                }
            }
        }

        // ── Search Modal ────────────────────────────────────────────
        if (uiState.showSearchModal) {
            com.miyu.reader.ui.reader.components.SearchInBookBottomSheet(
                currentChapterIndex = uiState.chapterIndex,
                readerTheme = readerTheme,
                onSearch = viewModel::searchInBook,
                onGoToChapter = { viewModel.goToChapter(it) },
                onDismiss = { viewModel.toggleSearchModal() },
            )
        }

        // ── Bottom controls overlay ─────────────────────────────────
        AnimatedVisibility(
            visible = uiState.showControls && uiState.selection == null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Surface(
                color = bgColor.copy(alpha = 0.95f),
                shadowElevation = 4.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    // Progress slider
                    if (uiState.totalChapters > 0) {
                        Slider(
                            value = (uiState.chapterIndex.toFloat()),
                            onValueChange = { viewModel.goToChapter(it.toInt()) },
                            valueRange = 0f..(uiState.totalChapters - 1).toFloat().coerceAtLeast(0f),
                            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        // Previous chapter
                        IconButton(
                            onClick = { viewModel.navigateChapter(-1) },
                            enabled = uiState.chapterIndex > 0,
                        ) {
                            Icon(Icons.Default.SkipPrevious, "Previous", tint = if (uiState.chapterIndex > 0) textColor else textColor.copy(alpha = 0.3f))
                        }

                        // Theme picker
                        IconButton(onClick = { viewModel.toggleThemePicker() }) {
                            Icon(Icons.Outlined.Palette, "Theme", tint = textColor)
                        }

                        // Settings panel
                        IconButton(onClick = { viewModel.toggleLayoutPanel() }) {
                            Icon(Icons.Outlined.Tune, "Layout", tint = textColor)
                        }

                        // Next chapter
                        IconButton(
                            onClick = { viewModel.navigateChapter(1) },
                            enabled = uiState.chapterIndex < uiState.totalChapters - 1,
                        ) {
                            Icon(Icons.Default.SkipNext, "Next", tint = if (uiState.chapterIndex < uiState.totalChapters - 1) textColor else textColor.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }

        // ── Theme Picker ────────────────────────────────────────────
        if (uiState.showThemePicker) {
            ThemePickerBottomSheet(
                selectedThemeId = readerThemeId,
                onThemeSelected = { viewModel.setReaderThemeId(it) },
                onDismiss = { viewModel.toggleThemePicker() },
            )
        }

        // ── Layout Panel ────────────────────────────────────────────
        if (uiState.showLayoutPanel) {
            com.miyu.reader.ui.reader.components.ReaderLayoutPanelBottomSheet(
                settings = readingSettings,
                readerTheme = readerTheme,
                onSettingsChanged = { viewModel.updateReadingSettings(it) },
                onDismiss = { viewModel.toggleLayoutPanel() },
            )
        }

        // ── Stats Modal ─────────────────────────────────────────────
        if (uiState.showStatsModal && uiState.book != null) {
            com.miyu.reader.ui.reader.components.ReadingStatsBottomSheet(
                book = uiState.book!!,
                chapterIndex = uiState.chapterIndex,
                totalChapters = uiState.totalChapters,
                totalHighlights = uiState.highlights.size,
                totalBookmarks = uiState.bookmarks.size,
                readerTheme = readerTheme,
                onDismiss = { viewModel.toggleStatsModal() },
            )
        }

        // ── Annotations drawer ──────────────────────────────────────
        if (uiState.showAnnotationsDrawer) {
            com.miyu.reader.ui.reader.components.AnnotationsDrawerBottomSheet(
                highlights = uiState.highlights,
                bookmarks = uiState.bookmarks,
                readerTheme = readerTheme,
                onDismiss = { viewModel.toggleAnnotationsDrawer() },
                onHighlightClick = { highlight ->
                    viewModel.goToChapter(highlight.chapterIndex)
                    viewModel.toggleAnnotationsDrawer()
                },
                onBookmarkClick = { bookmark ->
                    viewModel.goToChapter(bookmark.chapterIndex)
                    viewModel.toggleAnnotationsDrawer()
                },
                onDeleteHighlight = { viewModel.deleteHighlight(it) },
                onDeleteBookmark = { viewModel.deleteBookmark(it) },
            )
        }

        // ── Chapter drawer ──────────────────────────────────────────
        if (uiState.showChapterDrawer) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.toggleChapterDrawer() },
                containerColor = readerTheme.cardBackground,
            ) {
                Text(
                    "Chapters",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    color = textColor,
                )
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 32.dp),
                ) {
                    val chapCount = uiState.totalChapters.coerceAtLeast(1)
                    itemsIndexed(List(chapCount) { it }) { index, _ ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    "Chapter ${index + 1}",
                                    fontWeight = if (index == uiState.chapterIndex) FontWeight.Bold else FontWeight.Normal,
                                    color = if (index == uiState.chapterIndex) accentColor else textColor,
                                )
                            },
                            leadingContent = {
                                if (index == uiState.chapterIndex) {
                                    Surface(shape = CircleShape, color = accentColor, modifier = Modifier.size(8.dp)) {}
                                }
                            },
                            modifier = Modifier.clickable { viewModel.goToChapter(index) },
                        )
                    }
                }
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TermDetailBottomSheet(
    detail: ReaderTermDetail,
    readerTheme: ReaderThemeColors,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = readerTheme.cardBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                detail.groupName ?: "Term detail",
                color = readerTheme.secondaryText,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                detail.correctedText,
                color = readerTheme.text,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.height(16.dp))
            detail.imageUri?.let { imageUri ->
                AsyncImage(
                    model = imageUri,
                    contentDescription = detail.correctedText,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(18.dp)),
                )
                Spacer(Modifier.height(16.dp))
            }
            TermDetailRow("Original", detail.originalText, readerTheme)
            TermDetailRow("Replacement", detail.correctedText, readerTheme)
            detail.translationText?.let { TermDetailRow("Translation", it, readerTheme) }
            detail.context?.let { TermDetailRow("Context", it, readerTheme) }
        }
    }
}

@Composable
private fun TermDetailRow(
    label: String,
    value: String,
    readerTheme: ReaderThemeColors,
) {
    if (value.isBlank()) return
    Text(
        label.uppercase(),
        color = readerTheme.secondaryText,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
    )
    Spacer(Modifier.height(4.dp))
    Text(
        value,
        color = readerTheme.text,
        style = MaterialTheme.typography.bodyMedium,
    )
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun BookLoadingIndicator(
    readerTheme: ReaderThemeColors,
    title: String?,
) {
    val transition = rememberInfiniteTransition(label = "book-loading")
    val pulse by transition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "loading-card-pulse",
    )
    val pageAngle by transition.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 760),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "page-angle",
    )
    val progress by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.94f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2100),
        ),
        label = "loading-progress",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 28.dp),
    ) {
        Surface(
            color = readerTheme.cardBackground.copy(alpha = if (readerTheme.isDark) 0.94f else 0.96f),
            shape = RoundedCornerShape(30.dp),
            shadowElevation = 14.dp,
            modifier = Modifier
                .size(width = 188.dp, height = 198.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                },
        ) {
            Box(
                modifier = Modifier.padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(readerTheme.accent.copy(alpha = 0.14f)),
                )
                Column(
                    modifier = Modifier.align(Alignment.BottomStart),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.74f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(50))
                            .background(readerTheme.accent.copy(alpha = 0.34f)),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.56f)
                            .height(9.dp)
                            .clip(RoundedCornerShape(50))
                            .background(readerTheme.secondaryText.copy(alpha = 0.20f)),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.42f)
                            .height(9.dp)
                            .clip(RoundedCornerShape(50))
                            .background(readerTheme.secondaryText.copy(alpha = 0.14f)),
                    )
                }
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 42.dp, height = 58.dp)
                            .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                            .background(readerTheme.accent.copy(alpha = 0.56f)),
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 42.dp, height = 58.dp)
                            .graphicsLayer {
                                rotationY = pageAngle
                                cameraDistance = 12f * density
                            }
                            .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                            .background(readerTheme.accent),
                    )
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(
            if (title.isNullOrBlank()) "Opening book" else "Opening \"$title\"",
            color = readerTheme.text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            specialThemeSceneCopy(readerTheme),
            color = readerTheme.secondaryText,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .fillMaxWidth(0.72f)
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(readerTheme.secondaryText.copy(alpha = 0.14f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(readerTheme.accent),
            )
        }
    }
}

private fun colorToHex(color: Color): String {
    val argb = color.toArgb()
    return String.format("#%06X", 0xFFFFFF and argb)
}

private fun buildReaderHtml(
    chapterHtml: String,
    chapterIndex: Int,
    totalChapters: Int,
    bgColor: String,
    textColor: String,
    accentColor: String,
    fontSize: Float,
    lineHeight: Float,
    textAlign: TextAlign,
    marginPreset: MarginPreset,
    contentColumnWidth: Int?,
    readerColumnLayout: ReaderColumnLayout,
    pageAnimation: PageAnimation,
    tapZonesEnabled: Boolean,
    tapScrollPageRatio: Float,
    tapZoneNavMode: TapZoneNavMode,
    bionicReading: Boolean,
    autoAdvanceChapter: Boolean,
): String {
    val scrollBehavior = if (pageAnimation == PageAnimation.NONE) "auto" else "smooth"
    val maxWidthCss = contentColumnWidth?.let { "${it.coerceIn(360, 1200)}px" } ?: "none"
    val columnCount = if (readerColumnLayout == ReaderColumnLayout.TWO) 2 else 1
    val themeCss = """
html, body {
  background: $bgColor !important;
  color: $textColor !important;
  font-family: 'Georgia', serif;
  font-size: ${fontSize.toInt()}px;
  line-height: $lineHeight;
  text-align: ${if (textAlign == TextAlign.JUSTIFY) "justify" else "left"};
  padding: 20px ${readerMarginPx(marginPreset)}px 72px ${readerMarginPx(marginPreset)}px;
  word-wrap: break-word;
  overflow-wrap: break-word;
  -webkit-text-size-adjust: 100%;
  -webkit-user-select: text;
  user-select: text;
  scroll-behavior: $scrollBehavior;
}
body {
  max-width: $maxWidthCss;
  margin-left: auto;
  margin-right: auto;
  column-count: $columnCount;
  column-gap: 36px;
  transform: translateY(0px);
}
body :not(a) { color: $textColor !important; }
a { color: $accentColor !important; text-decoration: none; }
img { max-width: 100%; height: auto; }
.miyu-term {
  border-bottom: 2px dotted $accentColor;
  cursor: pointer;
  padding-bottom: 0.04em;
}
.miyu-term:active {
  background: rgba(154, 119, 71, 0.16);
}
h1, h2, h3, h4, h5, h6 { margin: 1em 0 0.5em; line-height: 1.3; }
p { margin-bottom: 0.8em; }
blockquote { border-left: 3px solid $accentColor; padding-left: 12px; margin: 1em 0; opacity: 0.85; }
.miyu-chapter {
  display: block;
  min-height: calc(100vh - 92px);
}
.miyu-continuous-divider {
  margin: 2.4em 0 1.35em;
  padding: 0.82em 0;
  border-top: 1px solid rgba(154, 119, 71, 0.28);
  border-bottom: 1px solid rgba(154, 119, 71, 0.18);
  color: $accentColor !important;
  font-family: system-ui, sans-serif;
  font-size: 0.72em;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-align: center;
  text-transform: uppercase;
  opacity: 0.82;
}
.miyu-continuous-loader {
  margin: 2.1em auto 1.4em;
  padding: 0.7em 1em;
  width: fit-content;
  border: 1px solid rgba(154, 119, 71, 0.22);
  border-radius: 999px;
  color: $accentColor !important;
  font-family: system-ui, sans-serif;
  font-size: 0.72em;
  font-weight: 700;
  letter-spacing: 0.06em;
  opacity: 0.82;
}
.miyu-pull-next {
  position: fixed;
  left: 50%;
  bottom: 26px;
  transform: translateX(-50%);
  z-index: 20;
  padding: 0.7em 1.05em;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.72);
  color: white !important;
  font-family: system-ui, sans-serif;
  font-size: 0.72em;
  font-weight: 800;
  letter-spacing: 0.06em;
  opacity: 0;
  pointer-events: none;
  transition: opacity 160ms ease, transform 160ms ease;
}
.miyu-pull-next.visible {
  opacity: 1;
  transform: translateX(-50%) translateY(-4px);
}
@media (max-width: 720px) {
  body { max-width: none; column-count: 1; }
}
.miyu-empty-chapter {
  border: 1px solid rgba(154, 119, 71, 0.25);
  border-radius: 18px;
  padding: 22px;
  background: rgba(255, 251, 245, 0.42);
}
""".trimIndent()

    val bodyContent = ReaderHtmlSanitizer.sanitize(extractBodyContent(chapterHtml)).takeIf { it.isNotBlank() }
        ?: "<section class=\"miyu-empty-chapter\"><h2>Chapter content unavailable</h2><p>This chapter imported without readable text. Try rescanning the book from Library.</p></section>"
    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
* { box-sizing: border-box; margin: 0; padding: 0; }
$themeCss
</style>
</head>
<body>
	<section class="miyu-chapter" data-miyu-chapter-index="$chapterIndex">
	$bodyContent
	</section>
	${readerBridgeScript(tapZonesEnabled, tapScrollPageRatio, tapZoneNavMode, bionicReading, autoAdvanceChapter, chapterIndex, totalChapters)}
	</body>
</html>
""".trimIndent()
}

private fun readerMarginPx(marginPreset: MarginPreset): Int = when (marginPreset) {
    MarginPreset.NARROW -> 12
    MarginPreset.MEDIUM -> 18
    MarginPreset.WIDE -> 28
}

private fun extractBodyContent(html: String): String {
    val bodyMatch = Regex("<body[^>]*>([\\s\\S]*?)</body>", RegexOption.IGNORE_CASE).find(html)
    return bodyMatch?.groupValues?.getOrNull(1) ?: html
}

private fun restoreReaderScrollScript(percent: Float): String {
    val safePercent = percent.coerceIn(0f, 1f)
    return "if (window.MIYU_RESTORE_SCROLL) window.MIYU_RESTORE_SCROLL($safePercent);"
}

private fun readerBridgeScript(
    tapZonesEnabled: Boolean,
    tapScrollPageRatio: Float,
    tapZoneNavMode: TapZoneNavMode,
    bionicReading: Boolean,
    autoAdvanceChapter: Boolean,
    chapterIndex: Int,
    totalChapters: Int,
): String {
    val tapZones = if (tapZonesEnabled) "true" else "false"
    val bionic = if (bionicReading) "true" else "false"
    val autoAdvance = if (autoAdvanceChapter) "true" else "false"
    val scrollRatio = tapScrollPageRatio.coerceIn(0.25f, 1f)
    val navMode = tapZoneNavMode.name
    val initialChapterIndex = chapterIndex.coerceAtLeast(0)
    val safeTotalChapters = totalChapters.coerceAtLeast(0)
    return """
<script>
document.addEventListener('click', function(event) {
    if (event.target && event.target.closest && event.target.closest('a, button, input, textarea, select')) return;
    if (window.__miyuSelectionActiveUntil && Date.now() < window.__miyuSelectionActiveUntil) return;
    var term = event.target && event.target.closest ? event.target.closest('.miyu-term') : null;
    if (term) {
        event.preventDefault();
        var detail = {
            originalText: term.getAttribute('data-original') || '',
            correctedText: term.getAttribute('data-corrected') || term.textContent || '',
            translationText: term.getAttribute('data-translation') || '',
            context: term.getAttribute('data-context') || '',
            imageUri: term.getAttribute('data-image-uri') || '',
            groupName: term.getAttribute('data-group') || ''
        };
        if (window.AndroidBridge) window.AndroidBridge.onTermTapped(JSON.stringify(detail));
        return;
    }
    var tapZonesEnabled = $tapZones;
    var navMode = "$navMode";
    if (tapZonesEnabled) {
        var x = event.clientX || 0;
        var width = window.innerWidth || document.documentElement.clientWidth || 1;
        if (x < width * 0.24 || x > width * 0.76) {
            var delta = x < width * 0.24 ? -1 : 1;
            if (navMode === "CHAPTER") {
                if (window.AndroidBridge) window.AndroidBridge.onReaderEdgeTap(delta);
            } else {
                window.scrollBy({ top: delta * window.innerHeight * $scrollRatio, behavior: "${if (tapZoneNavMode == TapZoneNavMode.SCROLL) "smooth" else "auto"}" });
            }
            return;
        }
    }
    var selection = window.getSelection ? window.getSelection() : null;
    var hasSelection = !!(selection && !selection.isCollapsed && normalizeSelectionText(selection.toString()).length > 0);
    if (!hasSelection && window.AndroidBridge) {
        window.AndroidBridge.onReaderTap();
    }
});

function normalizeSelectionText(value) {
    return (value || '').replace(/\s+/g, ' ').trim();
}

function originalAwareText(node) {
    if (!node) return '';
    if (node.nodeType === Node.TEXT_NODE) return node.nodeValue || '';
    if (node.nodeType !== Node.ELEMENT_NODE && node.nodeType !== Node.DOCUMENT_FRAGMENT_NODE) return '';
    if (node.nodeType === Node.ELEMENT_NODE && node.classList && node.classList.contains('miyu-term')) {
        var selected = node.textContent || '';
        var corrected = node.getAttribute('data-corrected') || selected;
        var original = node.getAttribute('data-original') || '';
        return original && normalizeSelectionText(selected) === normalizeSelectionText(corrected)
            ? original
            : selected;
    }
    var output = '';
    for (var i = 0; i < node.childNodes.length; i++) {
        output += originalAwareText(node.childNodes[i]);
    }
    return output;
}

function originalTextForSelection(selection) {
    try {
        if (!selection || selection.rangeCount === 0) return '';
        return normalizeSelectionText(originalAwareText(selection.getRangeAt(0).cloneContents()));
    } catch (e) {
        return '';
    }
}

document.addEventListener('selectionchange', function() {
    if (window.__miyuSelectionTimer) {
        clearTimeout(window.__miyuSelectionTimer);
    }
    window.__miyuSelectionTimer = setTimeout(function() {
        var sel = window.getSelection ? window.getSelection() : null;
        if (!sel || sel.isCollapsed) {
            if (window.AndroidBridge) window.AndroidBridge.onSelectionChanged("null");
            return;
        }
        var text = normalizeSelectionText(sel.toString());
        if (text.length === 0) {
            if (window.AndroidBridge) window.AndroidBridge.onSelectionChanged("null");
            return;
        }
        try {
            var rect = sel.getRangeAt(0).getBoundingClientRect();
            if (!rect || (!rect.width && !rect.height)) return;
            var originalText = originalTextForSelection(sel);
            window.__miyuSelectionActiveUntil = Date.now() + 680;
            var data = {
                text: text,
                originalText: originalText,
                x: rect.left + rect.width / 2,
                y: rect.top,
                width: rect.width,
                height: rect.height
            };
            if (window.AndroidBridge) window.AndroidBridge.onSelectionChanged(JSON.stringify(data));
        } catch (e) {}
    }, 280);
});
	(function() {
	    var lastProgressPost = 0;
	    var loadedThroughChapterIndex = $initialChapterIndex;
	    var totalChapters = $safeTotalChapters;
	    var appendRequestInFlight = false;
	    var appendLoader = null;
	    var pullHint = null;
	    var touchStartY = 0;
	    var pullDistance = 0;
	    function currentProgress() {
	        var doc = document.documentElement;
	        var scrollTop = window.scrollY || doc.scrollTop || 0;
	        var sections = Array.prototype.slice.call(document.querySelectorAll('.miyu-chapter[data-miyu-chapter-index]'));
	        if (sections.length === 0) {
	            var maxScroll = Math.max(1, doc.scrollHeight - window.innerHeight);
	            return {
	                chapterIndex: loadedThroughChapterIndex,
	                progress: Math.max(0, Math.min(1, scrollTop / maxScroll))
	            };
	        }
	        var probeY = Math.max(42, window.innerHeight * 0.18);
	        var active = sections[0];
	        var nearest = Number.POSITIVE_INFINITY;
	        sections.forEach(function(section) {
	            var rect = section.getBoundingClientRect();
	            var distance = rect.top <= probeY && rect.bottom >= probeY
	                ? -1
	                : Math.min(Math.abs(rect.top - probeY), Math.abs(rect.bottom - probeY));
	            if (distance < nearest) {
	                nearest = distance;
	                active = section;
	            }
	        });
	        var chapterIndex = parseInt(active.getAttribute('data-miyu-chapter-index') || loadedThroughChapterIndex, 10);
	        if (isNaN(chapterIndex)) chapterIndex = loadedThroughChapterIndex;
	        var activeRect = active.getBoundingClientRect();
	        var sectionTop = scrollTop + activeRect.top;
	        var sectionScrollable = Math.max(1, active.offsetHeight - Math.min(window.innerHeight, active.offsetHeight));
	        var progress = Math.max(0, Math.min(1, (scrollTop - sectionTop) / sectionScrollable));
	        if (activeRect.bottom <= window.innerHeight + 2) progress = 1;
	        return { chapterIndex: chapterIndex, progress: progress };
	    }
	    function postProgress(force) {
	        var now = Date.now();
	        if (!force && now - lastProgressPost < 900) return;
	        lastProgressPost = now;
	        var progress = currentProgress();
	        if (window.AndroidBridge) {
	            if (window.AndroidBridge.onChapterScrollProgress) {
	                window.AndroidBridge.onChapterScrollProgress(progress.chapterIndex, progress.progress);
	            } else {
	                window.AndroidBridge.onScrollProgress(progress.progress);
	            }
	        }
	    }
	    function remainingScroll() {
	        var doc = document.documentElement;
	        return doc.scrollHeight - ((window.scrollY || doc.scrollTop || 0) + window.innerHeight);
	    }
	    function showContinuousLoader() {
	        if (appendLoader) return;
	        appendLoader = document.createElement('div');
	        appendLoader.className = 'miyu-continuous-loader';
	        appendLoader.textContent = 'Loading next chapter...';
	        document.body.appendChild(appendLoader);
	    }
	    function hideContinuousLoader() {
	        if (appendLoader && appendLoader.parentNode) appendLoader.parentNode.removeChild(appendLoader);
	        appendLoader = null;
	    }
	    function showPullHint(distance) {
	        if (!pullHint) {
	            pullHint = document.createElement('div');
	            pullHint.className = 'miyu-pull-next';
	            document.body.appendChild(pullHint);
	        }
	        var percent = Math.max(0, Math.min(1, distance / 88));
	        pullHint.textContent = percent >= 1
	            ? ($autoAdvance ? 'Release to load next chapter' : 'Release for next chapter')
	            : ($autoAdvance ? 'Pull for next chapter' : 'Pull for next chapter');
	        pullHint.className = 'miyu-pull-next visible';
	    }
	    function resetPullInteraction() {
	        if (pullHint) pullHint.className = 'miyu-pull-next';
	        pullDistance = 0;
	        document.body.style.transform = 'translateY(0px)';
	    }
	    function maybeRequestNextChapter(force) {
	        if (!$autoAdvance || appendRequestInFlight) return;
	        if (totalChapters > 0 && loadedThroughChapterIndex >= totalChapters - 1) return;
	        if (force) {
	            if (remainingScroll() > 28) return;
	        } else if (remainingScroll() > 14) {
	            return;
	        }
	        appendRequestInFlight = true;
	        resetPullInteraction();
	        showContinuousLoader();
	        postProgress(true);
	        if (window.AndroidBridge) window.AndroidBridge.onReaderAppendNextChapter();
	        setTimeout(function() {
	            if (appendRequestInFlight) {
	                appendRequestInFlight = false;
	                hideContinuousLoader();
	            }
	        }, 9000);
	    }
	    window.MIYU_POST_PROGRESS = function() { postProgress(true); };
	    window.MIYU_RESTORE_SCROLL = function(percent) {
	        var safePercent = Math.max(0, Math.min(1, Number(percent) || 0));
	        var attempts = 0;
	        function applyRestore() {
	            var doc = document.documentElement;
	            var maxScroll = Math.max(0, doc.scrollHeight - window.innerHeight);
	            window.scrollTo(0, Math.round(maxScroll * safePercent));
	            attempts += 1;
	            if (attempts < 6) {
	                setTimeout(applyRestore, attempts * 120);
	            } else {
	                postProgress(true);
	            }
	        }
	        requestAnimationFrame(applyRestore);
	    };
	    window.MIYU_APPEND_CHAPTER = function(bodyHtml, chapterIndex) {
	        var parsedIndex = parseInt(chapterIndex, 10);
	        if (isNaN(parsedIndex)) {
	            appendRequestInFlight = false;
	            hideContinuousLoader();
	            return;
	        }
	        loadedThroughChapterIndex = Math.max(loadedThroughChapterIndex, parsedIndex);
	        if (!document.querySelector('.miyu-chapter[data-miyu-chapter-index="' + parsedIndex + '"]')) {
	            var section = document.createElement('section');
	            section.className = 'miyu-chapter miyu-appended-chapter';
	            section.setAttribute('data-miyu-chapter-index', String(parsedIndex));
	            section.innerHTML = '<div class="miyu-continuous-divider">Chapter ' + (parsedIndex + 1) + '</div>' + bodyHtml;
	            document.body.appendChild(section);
	        }
	        appendRequestInFlight = false;
	        hideContinuousLoader();
	        postProgress(true);
	    };
	    window.addEventListener('scroll', function() {
	        postProgress(false);
	        maybeRequestNextChapter(false);
	    }, { passive: true });
	    window.addEventListener('touchstart', function(event) {
	        touchStartY = event.touches && event.touches.length ? event.touches[0].clientY : 0;
	        pullDistance = 0;
	    }, { passive: true });
	    window.addEventListener('touchmove', function(event) {
	        if (appendRequestInFlight || remainingScroll() > 28) return;
	        var currentY = event.touches && event.touches.length ? event.touches[0].clientY : touchStartY;
	        pullDistance = Math.max(0, touchStartY - currentY);
	        if (pullDistance > 18) {
	            showPullHint(pullDistance);
	            var resistance = Math.min(26, pullDistance * 0.16);
	            document.body.style.transform = 'translateY(' + (-resistance) + 'px)';
	        } else {
	            document.body.style.transform = 'translateY(0px)';
	        }
	    }, { passive: true });
	    window.addEventListener('touchend', function() {
	        if (pullDistance > 88) {
	            if ($autoAdvance) {
	                maybeRequestNextChapter(true);
	            } else if (window.AndroidBridge && (!totalChapters || loadedThroughChapterIndex < totalChapters - 1)) {
	                resetPullInteraction();
	                window.AndroidBridge.onReaderEdgeTap(1);
	                return;
	            }
	        } else {
	            resetPullInteraction();
	        }
	        resetPullInteraction();
	    }, { passive: true });
	    window.addEventListener('pagehide', function() { postProgress(true); });
	    window.addEventListener('beforeunload', function() { postProgress(true); });
	    document.addEventListener('visibilitychange', function() {
	        if (document.visibilityState === 'hidden') postProgress(true);
	    });
	    setTimeout(function() {
	        postProgress(false);
	    }, 500);
	    if ($bionic) {
	        var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, {
	            acceptNode: function(node) {
	                var parent = node.parentElement;
	                if (!parent) return NodeFilter.FILTER_REJECT;
	                var tag = parent.tagName ? parent.tagName.toLowerCase() : '';
	                if (tag === 'script' || tag === 'style' || tag === 'textarea' || tag === 'input') return NodeFilter.FILTER_REJECT;
	                return node.nodeValue && node.nodeValue.trim().length > 2 ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_REJECT;
	            }
	        });
	        var nodes = [];
	        while (walker.nextNode()) nodes.push(walker.currentNode);
	        nodes.forEach(function(node) {
	            var parts = node.nodeValue.split(/(\s+)/);
	            var frag = document.createDocumentFragment();
	            parts.forEach(function(part) {
	                if (/^\s+$/.test(part) || part.length < 4) {
	                    frag.appendChild(document.createTextNode(part));
	                    return;
	                }
	                var strongCount = Math.max(1, Math.ceil(part.length * 0.42));
	                var span = document.createElement('span');
	                var strong = document.createElement('strong');
	                strong.textContent = part.slice(0, strongCount);
	                span.appendChild(strong);
	                span.appendChild(document.createTextNode(part.slice(strongCount)));
	                frag.appendChild(span);
	            });
	            if (node.parentNode) node.parentNode.replaceChild(frag, node);
	        });
	    }
	})();
	</script>
""".trimIndent()
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

private class ReaderWebView(context: Context) : WebView(context) {
    override fun startActionMode(callback: ActionMode.Callback?): ActionMode? = null

    override fun startActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? = null
}

private open class SecureReaderWebViewClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        val scheme = request?.url?.scheme?.lowercase() ?: return null
        return when (scheme) {
            "about", "data" -> null
            else -> emptyWebResourceResponse()
        }
    }

    private fun emptyWebResourceResponse(): WebResourceResponse =
        WebResourceResponse(
            "text/plain",
            "UTF-8",
            ByteArrayInputStream(ByteArray(0)),
        )
}

// Named class so Android lint can see @JavascriptInterface annotations
private class ReaderJsInterface(private val viewModel: ReaderViewModel) {
    @JavascriptInterface
    fun onSelectionChanged(jsonStr: String) {
        if (jsonStr.isBlank() || jsonStr == "null") {
            viewModel.handleSelection(null)
            return
        }
        if (jsonStr.length > MAX_BRIDGE_PAYLOAD_CHARS) {
            viewModel.handleSelection(null)
            return
        }
        try {
            val json = JSONObject(jsonStr)
            val text = sanitizeBridgeText(json.getString("text"))
            if (text.isBlank()) {
                viewModel.handleSelection(null)
                return
            }
            val selection = SelectionData(
                text = text,
                originalText = sanitizeBridgeText(json.optString("originalText")).takeIf { it.isNotBlank() },
                x = json.getDouble("x").toFloat(),
                y = json.getDouble("y").toFloat(),
                width = json.getDouble("width").toFloat(),
                height = json.getDouble("height").toFloat()
            )
            viewModel.handleSelection(selection)
        } catch (e: Exception) {
            viewModel.handleSelection(null)
        }
    }

    @JavascriptInterface
    fun onReaderTap() {
        viewModel.handleReaderTap()
    }

    @JavascriptInterface
    fun onReaderEdgeTap(delta: Int) {
        viewModel.navigateChapter(delta)
    }

    @JavascriptInterface
    fun onReaderAppendNextChapter() {
        viewModel.appendNextChapter()
    }

    @JavascriptInterface
    fun onScrollProgress(progress: Double) {
        viewModel.updateScrollProgress(progress.toFloat())
    }

    @JavascriptInterface
    fun onChapterScrollProgress(chapterIndex: Int, progress: Double) {
        viewModel.updateChapterScrollProgress(chapterIndex, progress.toFloat())
    }

    @JavascriptInterface
    fun onTermTapped(jsonStr: String) {
        if (jsonStr.isBlank() || jsonStr.length > MAX_BRIDGE_PAYLOAD_CHARS) return
        try {
            val json = JSONObject(jsonStr)
            viewModel.showTermDetail(
                ReaderTermDetail(
                    originalText = sanitizeBridgeText(json.optString("originalText")),
                    correctedText = sanitizeBridgeText(json.optString("correctedText")),
                    translationText = sanitizeBridgeText(json.optString("translationText")).takeIf { it.isNotBlank() },
                    context = sanitizeBridgeText(json.optString("context")).takeIf { it.isNotBlank() },
                    imageUri = json.optString("imageUri").takeIf { it.isNotBlank() },
                    groupName = sanitizeBridgeText(json.optString("groupName")).takeIf { it.isNotBlank() },
                )
            )
        } catch (e: Exception) {
            // Ignore malformed bridge payloads from reader content.
        }
    }

    private fun sanitizeBridgeText(value: String): String =
        value
            .replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]"), "")
            .take(MAX_SELECTION_TEXT_CHARS)

    private companion object {
        const val MAX_BRIDGE_PAYLOAD_CHARS = 32_000
        const val MAX_SELECTION_TEXT_CHARS = 8_000
    }
}
