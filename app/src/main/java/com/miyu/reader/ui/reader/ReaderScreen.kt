package com.miyu.reader.ui.reader

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import com.miyu.reader.ui.components.ThemePickerBottomSheet
import com.miyu.reader.ui.theme.ReaderColors
import com.miyu.reader.viewmodel.ReaderViewModel
import com.miyu.reader.domain.model.MarginPreset
import com.miyu.reader.domain.model.TextAlign

import android.webkit.JavascriptInterface
import com.miyu.reader.ui.reader.components.SelectionToolbar
import com.miyu.reader.ui.reader.components.SelectionData
import org.json.JSONObject

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

    DisposableEffect(readingSettings.immersiveMode, context) {
        val activity = context.findActivity()
        val window = activity?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, it.decorView) }

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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = accentColor)
                    Spacer(Modifier.height(16.dp))
                    Text("Loading book…", color = readerTheme.secondaryText)
                }
            }
        } else {
            // ── WebView ─────────────────────────────────────────────
            val htmlContent = buildReaderHtml(
                chapterHtml = uiState.chapterHtml,
                bgColor = colorToHex(bgColor),
                textColor = colorToHex(textColor),
                accentColor = colorToHex(accentColor),
                fontSize = typography.fontSize,
                lineHeight = typography.lineHeight,
                textAlign = typography.textAlign,
                marginPreset = readingSettings.marginPreset,
            )

            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        @SuppressLint("SetJavaScriptEnabled")
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        settings.allowFileAccessFromFileURLs = true
                        settings.allowUniversalAccessFromFileURLs = true
                        settings.loadsImagesAutomatically = true
                        settings.defaultTextEncodingName = "utf-8"
                        setBackgroundColor(bgColor.toArgb())
                        @SuppressLint("JavascriptInterface")
                        addJavascriptInterface(jsInterface, "AndroidBridge")
                        webViewClient = WebViewClient()
                    }
                },
                update = { webView ->
                    webView.setBackgroundColor(bgColor.toArgb())
                    val baseUrl = uiState.book?.filePath
                        ?.substringBeforeLast('/', missingDelimiterValue = "")
                        ?.takeIf { it.isNotBlank() }
                        ?.let { "file://$it/" }
                    webView.loadDataWithBaseURL(baseUrl, htmlContent, "text/html", "UTF-8", null)
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
                            Icon(Icons.Outlined.Settings, "Layout", tint = textColor)
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

private fun colorToHex(color: Color): String {
    val argb = color.toArgb()
    return String.format("#%06X", 0xFFFFFF and argb)
}

private fun buildReaderHtml(
    chapterHtml: String,
    bgColor: String,
    textColor: String,
    accentColor: String,
    fontSize: Float,
    lineHeight: Float,
    textAlign: TextAlign,
    marginPreset: MarginPreset,
): String {
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
}
body :not(a) { color: $textColor !important; }
a { color: $accentColor !important; text-decoration: none; }
img { max-width: 100%; height: auto; }
h1, h2, h3, h4, h5, h6 { margin: 1em 0 0.5em; line-height: 1.3; }
p { margin-bottom: 0.8em; }
blockquote { border-left: 3px solid $accentColor; padding-left: 12px; margin: 1em 0; opacity: 0.85; }
.miyu-empty-chapter {
  border: 1px solid rgba(154, 119, 71, 0.25);
  border-radius: 18px;
  padding: 22px;
  background: rgba(255, 251, 245, 0.42);
}
""".trimIndent()

    if (chapterHtml.contains("<html", ignoreCase = true)) {
        val themedHtml = if (Regex("<style[^>]*id=[\"']miyu-reader-theme[\"'][^>]*>", RegexOption.IGNORE_CASE).containsMatchIn(chapterHtml)) {
            chapterHtml.replace(
                Regex("<style[^>]*id=[\"']miyu-reader-theme[\"'][^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
                "<style id=\"miyu-reader-theme\">\n$themeCss\n</style>",
            )
        } else {
            chapterHtml.replace(
                Regex("</head>", RegexOption.IGNORE_CASE),
                "<style id=\"miyu-reader-theme\">\n$themeCss\n</style></head>",
            )
        }
        val withBody = if (extractBodyContent(themedHtml).isBlank()) {
            themedHtml.replace(
                Regex("<body[^>]*>", RegexOption.IGNORE_CASE),
                "$0<section class=\"miyu-empty-chapter\"><h2>Chapter content unavailable</h2><p>This chapter imported without readable text. Try rescanning the book from Library.</p></section>",
            )
        } else {
            themedHtml
        }
        return if (Regex("</body>", RegexOption.IGNORE_CASE).containsMatchIn(withBody)) {
            withBody.replace(Regex("</body>", RegexOption.IGNORE_CASE), "${readerBridgeScript()}</body>")
        } else {
            "$withBody${readerBridgeScript()}"
        }
    }

    val bodyContent = extractBodyContent(chapterHtml).takeIf { it.isNotBlank() }
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
$bodyContent
${readerBridgeScript()}
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

private fun readerBridgeScript(): String = """
<script>
document.addEventListener('click', function() {
    if (window.AndroidBridge) window.AndroidBridge.onReaderTap();
});
document.addEventListener('selectionchange', function() {
    var sel = window.getSelection();
    if (!sel || sel.isCollapsed) {
        if (window.AndroidBridge) window.AndroidBridge.onSelectionChanged("null");
        return;
    }
    var text = sel.toString().trim();
    if (text.length === 0) {
        if (window.AndroidBridge) window.AndroidBridge.onSelectionChanged("null");
        return;
    }
    try {
        var rect = sel.getRangeAt(0).getBoundingClientRect();
        var data = {
            text: text,
            x: rect.left,
            y: rect.top,
            width: rect.width,
            height: rect.height
        };
        if (window.AndroidBridge) window.AndroidBridge.onSelectionChanged(JSON.stringify(data));
    } catch (e) {}
});
</script>
""".trimIndent()

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

// Named class so Android lint can see @JavascriptInterface annotations
private class ReaderJsInterface(private val viewModel: ReaderViewModel) {
    @JavascriptInterface
    fun onSelectionChanged(jsonStr: String) {
        if (jsonStr.isBlank() || jsonStr == "null") {
            viewModel.handleSelection(null)
            return
        }
        try {
            val json = JSONObject(jsonStr)
            val text = json.getString("text")
            if (text.isBlank()) {
                viewModel.handleSelection(null)
                return
            }
            val selection = SelectionData(
                text = text,
                x = json.getDouble("x").toFloat(),
                y = json.getDouble("y").toFloat(),
                width = json.getDouble("width").toFloat(),
                height = json.getDouble("height").toFloat()
            )
            viewModel.handleSelection(selection)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun onReaderTap() {
        viewModel.handleReaderTap()
    }
}
