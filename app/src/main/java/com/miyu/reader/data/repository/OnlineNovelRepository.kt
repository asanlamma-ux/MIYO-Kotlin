package com.miyu.reader.data.repository

import android.content.Context
import android.net.Uri
import com.miyu.reader.domain.model.GeneratedOnlineNovelEpub
import com.miyu.reader.domain.model.OnlineChapterContent
import com.miyu.reader.domain.model.OnlineChapterSummary
import com.miyu.reader.domain.model.OnlineNovelDetails
import com.miyu.reader.domain.model.OnlineNovelProvider
import com.miyu.reader.domain.model.OnlineNovelProviderId
import com.miyu.reader.domain.model.OnlineNovelSearchResult
import com.miyu.reader.domain.model.OnlineNovelSummary
import com.miyu.reader.security.ReaderHtmlSanitizer
import com.miyu.reader.storage.MiyoStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.time.Instant
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnlineNovelRepository @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    private val routeCookies = ConcurrentHashMap<String, ConcurrentHashMap<String, String>>()

    val providers: List<OnlineNovelProvider> = listOf(
        OnlineNovelProvider(
            id = OnlineNovelProviderId.WTR_LAB,
            label = "WTR-LAB",
            description = "Protected source from the RN app. Requires the browser verification bridge.",
            baseUrl = "https://wtr-lab.com/",
            startUrl = "https://wtr-lab.com/en/novel-finder",
            requiresBrowserVerification = true,
        ),
        OnlineNovelProvider(
            id = OnlineNovelProviderId.FANMTL,
            label = "FanMTL",
            description = "Fan-fiction-heavy MTL catalog with paged chapter lists.",
            baseUrl = "https://www.fanmtl.com/",
            startUrl = "https://www.fanmtl.com/search.html",
        ),
        OnlineNovelProvider(
            id = OnlineNovelProviderId.WUXIASPOT,
            label = "WuxiaSpot",
            description = "WuxiaSpot HTML source with the same structure as FanMTL.",
            baseUrl = "https://www.wuxiaspot.com/",
            startUrl = "https://www.wuxiaspot.com/search.html",
        ),
        OnlineNovelProvider(
            id = OnlineNovelProviderId.NOVELCOOL,
            label = "NovelCool",
            description = "Large multi-language novel catalog with broad genre coverage.",
            baseUrl = "https://novelcool.com/",
            startUrl = "https://novelcool.com/search/?name=",
        ),
        OnlineNovelProvider(
            id = OnlineNovelProviderId.MCREADER,
            label = "MCReader",
            description = "Large MTL repository compatible with the FanMTL chapter format.",
            baseUrl = "https://www.mcreader.net/",
            startUrl = "https://www.mcreader.net/list/all/all-newstime-0.html",
        ),
        OnlineNovelProvider(
            id = OnlineNovelProviderId.FREEWEBNOVEL,
            label = "FreeWebNovel",
            description = "FreeWebNovel source with translated and web-translated chapters.",
            baseUrl = "https://freewebnovel.com/",
            startUrl = "https://freewebnovel.com/",
        ),
        OnlineNovelProvider(
            id = OnlineNovelProviderId.LIGHTNOVELPUB,
            label = "LightNovelPub",
            description = "Aggregated light novel repository with dense chapter lists.",
            baseUrl = "https://lightnovelpub.vip/",
            startUrl = "https://lightnovelpub.vip/browse/",
        ),
    )

    suspend fun search(
        providerId: OnlineNovelProviderId,
        query: String,
        page: Int = 1,
        cursor: String? = null,
    ): OnlineNovelSearchResult = withContext(Dispatchers.IO) {
        val provider = provider(providerId)
        if (provider.requiresBrowserVerification) {
            error("${provider.label} requires the in-app browser verification card before search can run.")
        }
        if (providerId in siteProviders) {
            searchSite(provider, query, page.coerceAtLeast(1), cursor)
        } else {
            searchGenericSite(provider, query, page.coerceAtLeast(1), cursor)
        }
    }

    suspend fun getDetails(summary: OnlineNovelSummary): OnlineNovelDetails = withContext(Dispatchers.IO) {
        val provider = provider(summary.providerId)
        if (provider.requiresBrowserVerification) {
            error("${provider.label} requires the protected browser bridge before novel details can load.")
        }
        if (summary.providerId in siteProviders) {
            fetchSiteNovelDetails(provider, summary, includeChapters = true)
        } else {
            fetchGenericNovelDetails(provider, summary, includeChapters = true)
        }
    }

    suspend fun downloadAsEpub(
        novel: OnlineNovelDetails,
        startChapter: Int,
        endChapter: Int,
        concurrency: Int,
    ): GeneratedOnlineNovelEpub = withContext(Dispatchers.IO) {
        val provider = provider(novel.providerId)
        val chapters = novel.chapters.filter { it.order in startChapter..endChapter }
        if (chapters.isEmpty()) error("No chapters were found in that range.")
        if (chapters.size > MAX_DOWNLOAD_CHAPTERS) {
            error("Limit downloads to $MAX_DOWNLOAD_CHAPTERS chapters at a time.")
        }
        val boundedConcurrency = concurrency.coerceIn(MIN_DOWNLOAD_CONCURRENCY, MAX_DOWNLOAD_CONCURRENCY)
        val semaphore = Semaphore(boundedConcurrency)

        val chapterResults = coroutineScope {
            chapters.map { chapter ->
                async {
                    semaphore.withPermit {
                        try {
                            Result.success(fetchChapterWithRetry(provider, novel, chapter))
                        } catch (error: Exception) {
                            Result.failure(error)
                        }
                    }
                }
            }.awaitAll()
        }
        val failures = chapterResults.mapIndexedNotNull { index, result ->
            result.exceptionOrNull()?.let { error ->
                val chapter = chapters[index]
                "Chapter ${chapter.order}: ${error.message ?: error::class.java.simpleName}"
            }
        }
        if (failures.isNotEmpty()) {
            error(
                buildString {
                    append("Download failed for ${failures.size}/${chapters.size} chapters. No EPUB was written.")
                    append("\n")
                    append(failures.take(6).joinToString("\n"))
                },
            )
        }

        val payloads = chapterResults.map { it.getOrThrow() }.sortedBy { it.order }

        createGeneratedEpub(novel, payloads)
    }

    suspend fun createGeneratedEpub(
        novel: OnlineNovelDetails,
        chapters: List<OnlineChapterContent>,
    ): GeneratedOnlineNovelEpub = withContext(Dispatchers.IO) {
        if (chapters.isEmpty()) error("No chapters were available to export.")
        val file = buildRemoteNovelEpub(novel, chapters.sortedBy { it.order })
        GeneratedOnlineNovelEpub(file.absolutePath, file.name, novel.title)
    }

    private fun provider(id: OnlineNovelProviderId): OnlineNovelProvider =
        providers.first { it.id == id }

    private fun searchSite(
        provider: OnlineNovelProvider,
        query: String,
        page: Int,
        cursor: String?,
    ): OnlineNovelSearchResult {
        val doc = when {
            !cursor.isNullOrBlank() -> fetchDocument(cursor)
            query.isNotBlank() -> fetchDocument(
                absoluteUrl(provider, "/e/search/index.php"),
                method = Connection.Method.POST,
                form = mapOf(
                    "show" to "title",
                    "tempid" to "1",
                    "tbname" to "news",
                    "keyboard" to query.trim(),
                ),
            )
            else -> fetchDocument(absoluteUrl(provider, "/list/all/all-newstime-${(page - 1).coerceAtLeast(0)}.html"))
        }
        val items = parseSiteNovelList(provider, doc)
            .filter { it.matchesQuery(query) }
            .distinctBy { "${it.providerId}:${it.path}" }
        val next = parseNextCursor(provider, doc, page)
        return OnlineNovelSearchResult(items = items, page = page, hasMore = next != null, nextCursor = next)
    }

    private fun searchGenericSite(
        provider: OnlineNovelProvider,
        query: String,
        page: Int,
        cursor: String?,
    ): OnlineNovelSearchResult {
        val doc = if (!cursor.isNullOrBlank()) {
            fetchDocument(cursor)
        } else {
            when (provider.id) {
                OnlineNovelProviderId.NOVELCOOL -> fetchDocument(
                    if (query.isNotBlank()) {
                        absoluteUrl(provider, "/search/?name=${Uri.encode(query.trim())}&page=$page")
                    } else {
                        absoluteUrl(provider, "/category/page-$page/")
                    }
                )
                OnlineNovelProviderId.FREEWEBNOVEL -> {
                    if (query.isNotBlank()) {
                        fetchDocument(
                            absoluteUrl(provider, "/search"),
                            method = Connection.Method.POST,
                            form = mapOf("searchkey" to query.trim()),
                        )
                    } else {
                        fetchDocument(absoluteUrl(provider, "/novel-list/?page=$page"))
                    }
                }
                OnlineNovelProviderId.LIGHTNOVELPUB -> fetchDocument(
                    if (query.isNotBlank()) {
                        absoluteUrl(provider, "/search?title=${Uri.encode(query.trim())}&page=$page")
                    } else {
                        absoluteUrl(provider, "/browse/?page=$page")
                    }
                )
                else -> fetchDocument(absoluteUrl(provider, "/search/?q=${Uri.encode(query.trim())}"))
            }
        }
        val items = parseGenericNovelList(provider, doc)
            .filter { it.matchesQuery(query) }
            .distinctBy { "${it.providerId}:${it.path}" }
        val next = parseNextCursor(provider, doc, page)
        return OnlineNovelSearchResult(items = items, page = page, hasMore = next != null, nextCursor = next)
    }

    private fun parseSiteNovelList(provider: OnlineNovelProvider, doc: Document): List<OnlineNovelSummary> =
        doc.select("ul.novel-list li.novel-item").mapNotNull { node ->
            val link = node.selectFirst("a[href]") ?: return@mapNotNull null
            val path = parsePath(provider, link.attr("href"))
            val cleanPath = path.substringBefore('?')
            val slug = Regex("/novel/([^/.]+)\\.html$", RegexOption.IGNORE_CASE)
                .find(cleanPath)?.groupValues?.getOrNull(1)
                ?: cleanPath.trim('/').ifBlank { "novel" }
            val title = node.textFrom(".novel-title")
                .ifBlank { link.attr("title") }
                .ifBlank { link.text() }
                .ifBlank { "Untitled" }
            val cover = node.selectFirst("img")
                ?.let { absoluteUrlOrNull(provider, it.attr("data-src").ifBlank { it.attr("src") }) }
            OnlineNovelSummary(
                providerId = provider.id,
                providerLabel = provider.label,
                rawId = slug,
                slug = slug,
                path = path,
                title = title.cleanText(),
                coverUrl = cover,
                author = "Unknown Author",
                summary = node.textFrom("p").cleanText(),
                status = node.textFrom(".status").ifBlank { "Unknown" },
                chapterCount = node.select(".novel-stats").firstOrNull()?.text()?.parseFirstInt(),
            )
        }

    private fun parseGenericNovelList(provider: OnlineNovelProvider, doc: Document): List<OnlineNovelSummary> {
        val selectorSets = listOf(
            SelectorSet(".book-list .book-item", "a[href]", ".book-name, .bookdetail-booktitle, h3, h4", ".book-pic img, img", ".chapter, .book-data-item, .book-data", ".status, .book-type", ".book-intro, .book-summary-content"),
            SelectorSet(".ul-list1 .li-row .li .con, .ul-list1-2 .li-row .li .con", ".txt h3 a[href], .pic a[href], a[href]", ".txt .tit, .txt h3, .tit", ".pic img, img", ".chapter, .chapter .s1", ".chapter .s2, .status", ".desc, p"),
            SelectorSet("ul.novel-list li.novel-item", "a[href]", ".novel-title", "img", ".novel-stats", ".status", "p"),
            SelectorSet(".book-list .bookinfo, .m-book-list .bookinfo", "a[href]", ".bookdetail-booktitle, .book-intro-title, h3, h4", "img", null, null, ".intro"),
            SelectorSet(".m-book-item, .col-novel .novel-item, .col-content .novel-item, .col-content .m-book-item", "a.tit[href], a[href]", ".tit, .name, h3, h4", "img", ".chapter", ".con", ".intro"),
            SelectorSet(".list-of-novels .col-novel, .list-of-novels .novel", "a[href]", ".novel-title, h3, h4", "img", ".total-chapters", ".status", ".description"),
            SelectorSet(".col-12.col-sm-6, .book-item", "a[href]", "h3, h4, .title", "img", null, null, "p"),
        )
        selectorSets.forEach { selectors ->
            val items = doc.select(selectors.container).mapNotNull { node ->
                val link = node.selectFirst(selectors.link) ?: node.closest("a[href]") ?: return@mapNotNull null
                val path = parsePath(provider, link.attr("href"))
                if (path.isBlank() || path == "/") return@mapNotNull null
                val cleanPath = path.substringBefore('?')
                val slug = Regex("/novel/([^/.]+?)(?:\\.html|/|$)", RegexOption.IGNORE_CASE)
                    .find(cleanPath)?.groupValues?.getOrNull(1)
                    ?: cleanPath.trim('/').substringAfterLast('/').removeSuffix(".html").ifBlank { cleanPath.trim('/') }
                val titleNode = selectors.title?.let { node.selectFirst(it) } ?: link
                val coverNode = selectors.cover?.let { node.selectFirst(it) }
                val statsNode = selectors.stats?.let { node.selectFirst(it) }
                val statusNode = selectors.status?.let { node.selectFirst(it) }
                val summaryNode = selectors.summary?.let { node.selectFirst(it) }
                OnlineNovelSummary(
                    providerId = provider.id,
                    providerLabel = provider.label,
                    rawId = slug,
                    slug = slug,
                    path = path,
                    title = titleNode.text().ifBlank { link.attr("title") }.ifBlank { "Untitled" }.cleanText(),
                    coverUrl = coverNode?.let { absoluteUrlOrNull(provider, it.attr("data-src").ifBlank { it.attr("src") }) },
                    author = "Unknown Author",
                    summary = summaryNode?.text().orEmpty().cleanText(),
                    status = statusNode?.text()?.cleanText()?.ifBlank { "Unknown" } ?: "Unknown",
                    chapterCount = statsNode?.text()?.parseFirstInt(),
                )
            }
            if (items.isNotEmpty()) return items
        }
        return emptyList()
    }

    private fun fetchSiteNovelDetails(
        provider: OnlineNovelProvider,
        summary: OnlineNovelSummary,
        includeChapters: Boolean,
    ): OnlineNovelDetails {
        val path = summary.path.ifBlank { "/novel/${summary.slug}.html" }
        val suffix = if (path.contains("?")) "&tab=chapters" else "?tab=chapters"
        val doc = fetchDocument(absoluteUrl(provider, path + suffix))
        val chapters = if (includeChapters) parseSiteChapterList(provider, doc) else emptyList()
        return OnlineNovelDetails(
            providerId = provider.id,
            providerLabel = provider.label,
            rawId = summary.rawId,
            slug = summary.slug,
            path = path,
            title = doc.textFrom(".novel-title").ifBlank { summary.title }.ifBlank { "Untitled" },
            coverUrl = parseSiteCoverUrl(provider, doc) ?: summary.coverUrl,
            author = parseSiteAuthor(doc).ifBlank { summary.author },
            summary = parseSiteSummary(doc).ifBlank { summary.summary },
            status = parseSiteStatus(doc).ifBlank { summary.status },
            chapterCount = parseSiteChapterCount(doc) ?: summary.chapterCount ?: chapters.size.takeIf { it > 0 },
            genres = parseSiteGenres(doc),
            tags = parseSiteTags(doc),
            chapters = chapters,
        )
    }

    private fun fetchGenericNovelDetails(
        provider: OnlineNovelProvider,
        summary: OnlineNovelSummary,
        includeChapters: Boolean,
    ): OnlineNovelDetails {
        val path = summary.path.ifBlank { "/novel/${summary.slug}.html" }
        val doc = fetchDocument(absoluteUrl(provider, path))
        val chapters = if (includeChapters) parseGenericChapterList(provider, doc) else emptyList()
        return OnlineNovelDetails(
            providerId = provider.id,
            providerLabel = provider.label,
            rawId = summary.rawId,
            slug = summary.slug,
            path = path,
            title = parseGenericTitle(doc).ifBlank { summary.title }.ifBlank { "Untitled" },
            coverUrl = parseGenericCoverUrl(provider, doc) ?: summary.coverUrl,
            author = parseGenericAuthor(doc).ifBlank { summary.author },
            summary = parseGenericSummary(doc).ifBlank { summary.summary },
            status = parseGenericStatus(doc).ifBlank { summary.status },
            chapterCount = parseGenericChapterCount(doc) ?: summary.chapterCount ?: chapters.size.takeIf { it > 0 },
            genres = parseSiteGenres(doc),
            tags = parseSiteTags(doc),
            chapters = chapters,
        )
    }

    private fun parseSiteChapterList(provider: OnlineNovelProvider, doc: Document): List<OnlineChapterSummary> =
        doc.select("ul.chapter-list li a[href]").mapIndexed { index, link ->
            val path = parsePath(provider, link.attr("href"))
            val order = parseChapterOrder(path) ?: index + 1
            OnlineChapterSummary(
                order = order,
                title = link.text().cleanText().ifBlank { "Chapter $order" },
                path = path,
            )
        }.distinctBy { it.path }.sortedBy { it.order }

    private fun parseGenericChapterList(provider: OnlineNovelProvider, doc: Document): List<OnlineChapterSummary> {
        val selectors = listOf(
            "ul.chapter-list li a[href]",
            ".chapter-list a[href]",
            ".chapterlist a[href]",
            ".chapter-list .chapter-title a[href]",
            "ul.list-chapter li a[href]",
            ".list-chapter a[href]",
            ".list-chapters a[href]",
            "#chapter-list a[href]",
            ".chapters-list a[href]",
            ".episode-list a[href]",
            "#chp-catalogue-m a[href]",
            ".chapter-item a[href]",
            ".m-newest2 ul li a[href]",
            ".catalog a[href*=chapter]",
            "[class*=chapter] a[href]",
            "a.last-read-url[href]",
            "li a[href*=chapter]",
            "a[href*=/chapter/]",
        )
        selectors.forEach { selector ->
            val chapters = doc.select(selector).mapIndexed { index, link ->
                val path = parsePath(provider, link.attr("href"))
                val order = parseChapterOrder(path) ?: index + 1
                OnlineChapterSummary(
                    order = order,
                    title = link.text().cleanText().ifBlank { "Chapter $order" },
                    path = path,
                )
            }.filter { it.path.isNotBlank() && it.isLikelyChapterLink() }
                .distinctBy { it.path }
                .sortedBy { it.order }
            if (chapters.isNotEmpty()) return chapters
        }
        return emptyList()
    }

    private fun fetchSiteChapter(
        provider: OnlineNovelProvider,
        novel: OnlineNovelDetails,
        chapter: OnlineChapterSummary,
    ): OnlineChapterContent {
        val path = chapter.path.ifBlank { "/novel/${novel.slug}_${chapter.order}.html" }
        val doc = fetchDocument(absoluteUrl(provider, path))
        val content = doc.selectFirst(".chapter-content") ?: error("Could not locate the chapter content.")
        content.select("script, style, iframe, ins, [align=center]").remove()
        val title = doc.selectFirst("#chapter-article .titles h2, h2")?.text()
            ?.cleanText()
            ?.takeIf { it.isNotBlank() }
            ?: chapter.title
        val order = parseChapterOrder(path) ?: chapter.order
        return OnlineChapterContent(
            order = order,
            title = title,
            html = ReaderHtmlSanitizer.sanitize(content.html()),
        )
    }

    private fun fetchGenericChapter(
        provider: OnlineNovelProvider,
        novel: OnlineNovelDetails,
        chapter: OnlineChapterSummary,
    ): OnlineChapterContent {
        val path = chapter.path.ifBlank { "/novel/${novel.slug}/chapter-${chapter.order}.html" }
        val doc = fetchDocument(absoluteUrl(provider, path))
        val contentSelectors = listOf(
            ".chapter-reading-section-list",
            ".txt #article",
            "#article",
            ".m-read .txt",
            ".chapter-content",
            "#chapter-content",
            ".reading-content",
            ".text-left",
            ".content-text",
            ".chapter-text",
            "#content",
            "#chapterContent",
            ".novel-content",
            ".entry-content",
        )
        val content = contentSelectors.firstNotNullOfOrNull { doc.selectFirst(it) }
            ?: doc.selectFirst("article")
            ?: doc.selectFirst("main")
            ?: doc.body()
        content.select("script, style, iframe, ins, [align=center], .ads, .ad, .read-ads, .chapter-start, .chapter-end, .paging, .catalog").remove()
        val title = doc.selectFirst("h1, h2.chapter-title, h2")?.text()
            ?.cleanText()
            ?.takeIf { it.isNotBlank() }
            ?: chapter.title
        val order = parseChapterOrder(path) ?: chapter.order
        return OnlineChapterContent(
            order = order,
            title = title,
            html = ReaderHtmlSanitizer.sanitize(content.html()),
        )
    }

    private suspend fun fetchChapterWithRetry(
        provider: OnlineNovelProvider,
        novel: OnlineNovelDetails,
        chapter: OnlineChapterSummary,
    ): OnlineChapterContent {
        var lastError: Exception? = null
        repeat(PROVIDER_RETRY_ATTEMPTS) { attempt ->
            try {
                return if (novel.providerId in siteProviders) {
                    fetchSiteChapter(provider, novel, chapter)
                } else {
                    fetchGenericChapter(provider, novel, chapter)
                }
            } catch (error: Exception) {
                lastError = error
                if (attempt < PROVIDER_RETRY_ATTEMPTS - 1) {
                    delay(PROVIDER_RETRY_BASE_DELAY_MS * (attempt + 1L))
                }
            }
        }
        throw IllegalStateException(lastError?.message ?: "Could not fetch ${chapter.title}.", lastError)
    }

    private fun buildRemoteNovelEpub(
        novel: OnlineNovelDetails,
        chapters: List<OnlineChapterContent>,
    ): File {
        val generatedDir = MiyoStorage.onlineEpubDir(appContext)
        val baseName = slugify(novel.title, "${novel.providerId.name.lowercase()}-${novel.rawId}")
        val fileName = "$baseName-${Instant.now().toEpochMilli()}.epub"
        val file = MiyoStorage.safeChild(generatedDir, fileName)
        val tempFile = MiyoStorage.safeChild(generatedDir, "$fileName.${UUID.randomUUID()}.tmp")

        try {
            ZipOutputStream(tempFile.outputStream().buffered()).use { zip ->
                zip.putStoredText("mimetype", "application/epub+zip")
                zip.putText(
                    "META-INF/container.xml",
                    """<?xml version="1.0" encoding="utf-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>"""
                )
                val cover = fetchCoverBinary(novel.coverUrl)
                if (cover != null) {
                    zip.putBytes("OEBPS/images/cover.${cover.extension}", cover.bytes)
                    zip.putText("OEBPS/cover.xhtml", buildCoverPage(novel, cover))
                }
                chapters.forEachIndexed { index, chapter ->
                    zip.putText("OEBPS/chapter-${index + 1}.xhtml", wrapChapterHtml(chapter.title, chapter.html))
                }
                zip.putText("OEBPS/nav.xhtml", buildNav(chapters))
                zip.putText("OEBPS/toc.ncx", buildNcx(novel, chapters))
                zip.putText("OEBPS/content.opf", buildOpf(novel, chapters, cover))
            }
            if (tempFile.length() <= 0L) error("Generated EPUB is empty.")
            if (!tempFile.renameTo(file)) {
                try {
                    tempFile.copyTo(file, overwrite = true)
                } catch (copyError: Throwable) {
                    file.delete()
                    throw copyError
                } finally {
                    tempFile.delete()
                }
            }
            return file
        } catch (error: Throwable) {
            tempFile.delete()
            throw error
        }
    }

    private fun fetchCoverBinary(coverUrl: String?): CoverBinary? {
        val url = coverUrl?.trim()?.takeIf { it.startsWith("https://", ignoreCase = true) } ?: return null
        return runCatching {
            validatePublicHttpsUrl(url)
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = PROVIDER_TIMEOUT_MS
                readTimeout = PROVIDER_TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("Accept", "image/avif,image/webp,image/png,image/jpeg,*/*;q=0.5")
                setRequestProperty("User-Agent", USER_AGENT)
            }
            try {
                if (connection.responseCode !in 200..299) return@runCatching null
                val contentType = connection.contentType.orEmpty().lowercase(Locale.ROOT)
                if (!contentType.startsWith("image/") || contentType.contains("svg")) return@runCatching null
                val bytes = connection.inputStream.use { it.readBoundedBytes(MAX_COVER_BYTES) }
                val mediaType = when {
                    contentType.contains("png") -> "image/png"
                    contentType.contains("webp") -> "image/webp"
                    else -> "image/jpeg"
                }
                val extension = when (mediaType) {
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    else -> "jpg"
                }
                CoverBinary(bytes = bytes, extension = extension, mediaType = mediaType)
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    private fun fetchDocument(
        url: String,
        method: Connection.Method = Connection.Method.GET,
        form: Map<String, String> = emptyMap(),
    ): Document {
        val safeUrl = validatePublicHttpsUrl(url)
        val host = URI(safeUrl).host.orEmpty()
        val cookies = routeCookies.getOrPut(host) { ConcurrentHashMap() }
        val request = Jsoup.connect(safeUrl)
            .method(method)
            .timeout(PROVIDER_TIMEOUT_MS)
            .maxBodySize(MAX_HTML_BYTES)
            .followRedirects(true)
            .ignoreHttpErrors(false)
            .userAgent(USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .cookies(cookies)
        form.forEach { (key, value) -> request.data(key, value) }
        val response = request.execute()
        cookies.putAll(response.cookies())
        return response.parse()
    }

    private fun absoluteUrl(provider: OnlineNovelProvider, value: String): String =
        URI(provider.baseUrl).resolve(value).toString()

    private fun absoluteUrlOrNull(provider: OnlineNovelProvider, value: String): String? =
        runCatching { absoluteUrl(provider, value) }.getOrNull()?.takeIf { it.isNotBlank() }

    private fun parsePath(provider: OnlineNovelProvider, value: String): String {
        if (value.isBlank()) return ""
        return runCatching {
            val resolved = URI(provider.baseUrl).resolve(value)
            val path = resolved.rawPath.orEmpty()
            val query = resolved.rawQuery?.takeIf { it.isNotBlank() }?.let { "?$it" }.orEmpty()
            "$path$query"
        }.getOrDefault(value)
    }

    private fun validatePublicHttpsUrl(url: String): String {
        val uri = URI(url.trim())
        if (!uri.scheme.equals("https", ignoreCase = true)) {
            error("Provider requests must use HTTPS.")
        }
        val host = uri.host?.lowercase(Locale.ROOT).orEmpty()
        if (host.isBlank() || host == "localhost" || host.endsWith(".local") || isPrivateIpv4(host) || isPrivateIpv6(host)) {
            error("Private-network provider URLs are blocked.")
        }
        return uri.toURL().toString()
    }

    private fun parseNextCursor(provider: OnlineNovelProvider, doc: Document, currentPage: Int): String? {
        val links = doc.select(".pagination a[href], nav a[href]")
        if (links.isEmpty()) return null
        links.firstOrNull { it.text().cleanText() in setOf(">", "Next", "next", "›") }?.let {
            return absoluteUrlOrNull(provider, it.attr("href"))
        }
        val target = currentPage + 1
        return links.firstNotNullOfOrNull { link ->
            val href = absoluteUrlOrNull(provider, link.attr("href")) ?: return@firstNotNullOfOrNull null
            val page = Regex("(?:page=|page-|newstime-)(\\d+)", RegexOption.IGNORE_CASE)
                .find(href)?.groupValues?.getOrNull(1)?.toIntOrNull()
            href.takeIf { page == target || page == target - 1 }
        }
    }

    private fun parseSiteSummary(doc: Document): String =
        doc.selectFirst(".summary .content, #info .summary .content, meta[name=description], .summary")
            ?.let { it.attr("content").ifBlank { it.text() } }
            .orEmpty()
            .cleanText()

    private fun parseSiteAuthor(doc: Document): String =
        doc.selectFirst(".author [itemprop=author], .author a, .author span:last-child, [itemprop=author]")
            ?.text()
            .orEmpty()
            .cleanText()
            .ifBlank { "Unknown Author" }

    private fun parseSiteStatus(doc: Document): String =
        doc.select(".header-stats strong").getOrNull(1)?.text()?.cleanText()
            ?: doc.textFrom(".status").ifBlank { "Unknown" }

    private fun parseSiteChapterCount(doc: Document): Int? =
        doc.selectFirst(".header-stats strong")?.text()?.parseFirstInt()

    private fun parseSiteCoverUrl(provider: OnlineNovelProvider, doc: Document): String? =
        doc.selectFirst(".cover img, meta[property=og:image], img[data-src], img[src]")
            ?.let { absoluteUrlOrNull(provider, it.attr("content").ifBlank { it.attr("data-src").ifBlank { it.attr("src") } }) }

    private fun parseSiteGenres(doc: Document): List<String> =
        doc.select(".categories .property-item, .genres a, .genre a, a[href*=genre], a[href*=category], a[href*=genres]")
            .map { it.text().cleanText() }
            .filter { it.isNotBlank() }
            .distinct()

    private fun parseSiteTags(doc: Document): List<String> =
        doc.select(".categories .tag, .tags .content .tag, .tags a, .tag a, a[href*=tag], a[href*=tags]")
            .map { it.text().cleanText() }
            .filter { it.isNotBlank() }
            .distinct()

    private fun parseGenericTitle(doc: Document): String =
        doc.selectFirst(".r-n-bk-name, h1.booktitle, h1.tit, h1.novel-title, .novel-title h1, h1.title, h1, meta[property=og:title], meta[name=twitter:title]")
            ?.let { it.attr("content").ifBlank { it.text() } }
            .orEmpty()
            .cleanText()

    private fun parseGenericAuthor(doc: Document): String =
        doc.selectFirst("[itemprop=author], .author a, .author span:last-child, .author, .book-author a, .novel-author, a[href*=/search/?author=], a[href*=/author/], meta[name=author]")
            ?.let { it.attr("content").ifBlank { it.text() } }
            .orEmpty()
            .cleanText()
            .ifBlank { "Unknown Author" }

    private fun parseGenericSummary(doc: Document): String =
        doc.selectFirst("[itemprop=description], .intro-content, .book-intro, .summary .content, .summary, .description, .synops, .synopsis, .bk-summary-txt, .book-summary-content, meta[property=og:description], meta[name=description]")
            ?.let { it.attr("content").ifBlank { it.text() } }
            .orEmpty()
            .cleanText()

    private fun parseGenericCoverUrl(provider: OnlineNovelProvider, doc: Document): String? =
        doc.selectFirst("meta[property=og:image], .cover img, .bookdetailimg img, .pic img, .book-cover img, img.cover, img[data-src], img[src]")
            ?.let { absoluteUrlOrNull(provider, it.attr("content").ifBlank { it.attr("data-src").ifBlank { it.attr("src") } }) }

    private fun parseGenericChapterCount(doc: Document): Int? =
        listOf(
            ".header-stats strong",
            ".chapter-count, [class*=chapter-count], [class*=chaptercount]",
            ".novel-stats, .chapter strong",
            ".chapter .s1, .s1",
            ".book-info .book-chapter, .book-chapter, .book-stats",
        ).firstNotNullOfOrNull { selector -> doc.selectFirst(selector)?.text()?.parseFirstInt() }

    private fun parseGenericStatus(doc: Document): String =
        doc.selectFirst(".header-stats strong:last-child, .status, [class*=novel-status], [class*=book-status], .chapter .s2")
            ?.text()
            ?.cleanText()
            .orEmpty()
            .ifBlank { "Unknown" }

    private fun OnlineChapterSummary.isLikelyChapterLink(): Boolean {
        val cleanPath = path.substringBefore('?').substringBefore('#').lowercase(Locale.ROOT)
        val cleanTitle = title.lowercase(Locale.ROOT)
        if (
            cleanPath.isBlank() ||
            cleanPath == "/" ||
            cleanPath.contains("javascript:") ||
            cleanPath.contains("/search") ||
            cleanPath.contains("/genre") ||
            cleanPath.contains("/category") ||
            cleanPath.contains("/tag/") ||
            cleanPath.contains("/tags/")
        ) {
            return false
        }
        if (cleanTitle in setOf("next", "previous", "prev", "latest", "read more", "show all")) return false
        return cleanPath.contains("chapter") ||
            cleanPath.contains("chap") ||
            parseChapterOrder(path) != null ||
            cleanTitle.startsWith("chapter") ||
            cleanTitle.startsWith("ch ") ||
            cleanTitle.startsWith("ch.") ||
            cleanTitle in setOf("prologue", "epilogue") ||
            Regex("^(chapter\\s*)?\\d+([:.\\-\\s]|$)", RegexOption.IGNORE_CASE).containsMatchIn(cleanTitle)
    }

    private fun parseChapterOrder(path: String): Int? {
        val cleanPath = path.substringBefore('?').substringBefore('#')
        return Regex("(?:chapter[-_/]|_)(\\d+)(?:\\.html?)?/?$", RegexOption.IGNORE_CASE)
            .find(cleanPath)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: Regex("/(\\d+)(?:\\.html?)?/?$", RegexOption.IGNORE_CASE)
                .find(cleanPath)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private fun wrapChapterHtml(title: String, body: String): String =
        """<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>${title.xmlEscape()}</title>
    <meta charset="utf-8" />
  </head>
  <body>
    <h1>${title.xmlEscape()}</h1>
    $body
  </body>
</html>"""

    private fun buildNav(chapters: List<OnlineChapterContent>): String =
        """<?xml version="1.0" encoding="utf-8"?>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
  <head><title>Contents</title><meta charset="utf-8" /></head>
  <body>
    <nav epub:type="toc" id="toc">
      <h1>Contents</h1>
      <ol>${chapters.mapIndexed { index, chapter -> "<li><a href=\"chapter-${index + 1}.xhtml\">${chapter.title.xmlEscape()}</a></li>" }.joinToString("")}</ol>
    </nav>
  </body>
</html>"""

    private fun buildNcx(novel: OnlineNovelDetails, chapters: List<OnlineChapterContent>): String {
        val remoteId = "${novel.providerId.name.lowercase()}-${novel.rawId}"
        val navPoints = chapters.mapIndexed { index, chapter ->
            """<navPoint id="navPoint-${index + 1}" playOrder="${index + 1}">
  <navLabel><text>${chapter.title.xmlEscape()}</text></navLabel>
  <content src="chapter-${index + 1}.xhtml"/>
</navPoint>"""
        }.joinToString("\n")
        return """<?xml version="1.0" encoding="UTF-8"?>
<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
  <head>
    <meta name="dtb:uid" content="${remoteId.xmlEscape()}"/>
    <meta name="dtb:depth" content="1"/>
    <meta name="dtb:totalPageCount" content="0"/>
    <meta name="dtb:maxPageNumber" content="0"/>
  </head>
  <docTitle><text>${novel.title.xmlEscape()}</text></docTitle>
  <navMap>
$navPoints
  </navMap>
</ncx>"""
    }

    private fun buildOpf(
        novel: OnlineNovelDetails,
        chapters: List<OnlineChapterContent>,
        cover: CoverBinary?,
    ): String {
        val remoteId = "${novel.providerId.name.lowercase()}-${novel.rawId}"
        val manifestChapters = chapters.mapIndexed { index, _ ->
            """<item id="chapter-${index + 1}" href="chapter-${index + 1}.xhtml" media-type="application/xhtml+xml"/>"""
        }.joinToString("\n    ")
        val spineChapters = chapters.mapIndexed { index, _ ->
            """<itemref idref="chapter-${index + 1}"/>"""
        }.joinToString("\n    ")
        val coverManifest = cover?.let {
            """<item id="cover-image" href="images/cover.${it.extension}" media-type="${it.mediaType}" properties="cover-image"/>
    <item id="cover-page" href="cover.xhtml" media-type="application/xhtml+xml"/>"""
        }.orEmpty()
        val coverSpine = if (cover != null) """<itemref idref="cover-page" linear="yes"/>""" else ""
        return """<?xml version="1.0" encoding="UTF-8"?>
<package xmlns="http://www.idpf.org/2007/opf" unique-identifier="BookId" version="3.0">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:identifier id="BookId">${remoteId.xmlEscape()}</dc:identifier>
    <dc:title>${novel.title.xmlEscape()}</dc:title>
    <dc:creator>${novel.author.xmlEscape()}</dc:creator>
    <dc:language>en</dc:language>
    <dc:description>${novel.summary.ifBlank { "Downloaded from ${novel.providerLabel} using Miyo." }.xmlEscape()}</dc:description>
    <meta property="dcterms:modified">${Instant.now().toString().replace(Regex("\\.\\d{3,9}Z$"), "Z")}</meta>
  </metadata>
  <manifest>
    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
    <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
    $coverManifest
    $manifestChapters
  </manifest>
  <spine toc="ncx">
    $coverSpine
    $spineChapters
  </spine>
</package>"""
    }

    private fun buildCoverPage(novel: OnlineNovelDetails, cover: CoverBinary): String =
        """<?xml version="1.0" encoding="utf-8"?>
<html xmlns="http://www.w3.org/1999/xhtml">
  <head><title>${novel.title.xmlEscape()}</title><meta charset="utf-8" /></head>
  <body><img src="images/cover.${cover.extension}" alt="${novel.title.xmlEscape()}" /></body>
</html>"""

    private fun ZipOutputStream.putStoredText(name: String, text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val crc = CRC32().apply { update(bytes) }
        val entry = ZipEntry(name).apply {
            method = ZipEntry.STORED
            size = bytes.size.toLong()
            compressedSize = bytes.size.toLong()
            this.crc = crc.value
        }
        putNextEntry(entry)
        write(bytes)
        closeEntry()
    }

    private fun ZipOutputStream.putText(name: String, text: String) {
        putBytes(name, text.toByteArray(Charsets.UTF_8))
    }

    private fun ZipOutputStream.putBytes(name: String, bytes: ByteArray) {
        putNextEntry(ZipEntry(name))
        write(bytes)
        closeEntry()
    }

    private fun java.io.InputStream.readBoundedBytes(maxBytes: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read == -1) break
            total += read
            if (total > maxBytes) error("Remote assets are larger than the safety limit.")
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun isPrivateIpv4(host: String): Boolean =
        host.startsWith("10.") ||
            host.startsWith("127.") ||
            host.startsWith("192.168.") ||
            host.startsWith("169.254.") ||
            Regex("^172\\.(1[6-9]|2\\d|3[0-1])\\.").containsMatchIn(host)

    private fun isPrivateIpv6(host: String): Boolean =
        host == "::1" ||
            host == "[::1]" ||
            host.startsWith("fc") ||
            host.startsWith("fd") ||
            host.startsWith("fe80:")

    private fun OnlineNovelSummary.matchesQuery(query: String): Boolean {
        val needle = query.trim().lowercase(Locale.ROOT)
        if (needle.isBlank()) return true
        return listOf(title, author, summary, status).any { it.lowercase(Locale.ROOT).contains(needle) }
    }

    private fun Element.textFrom(selector: String): String =
        selectFirst(selector)?.text().orEmpty().cleanText()

    private fun Document.textFrom(selector: String): String =
        selectFirst(selector)?.text().orEmpty().cleanText()

    private fun String.cleanText(): String =
        replace(Regex("\\s+"), " ").trim()

    private fun String.parseFirstInt(): Int? =
        Regex("(\\d+)").find(this)?.groupValues?.getOrNull(1)?.toIntOrNull()

    private fun String.xmlEscape(): String =
        replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

    private fun slugify(input: String, fallback: String): String =
        input.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(72)
            .ifBlank {
                fallback.lowercase(Locale.ROOT)
                    .replace(Regex("[^a-z0-9]+"), "-")
                    .trim('-')
                    .take(72)
            }
            .ifBlank { "remote-${UUID.randomUUID()}" }

    private data class SelectorSet(
        val container: String,
        val link: String,
        val title: String?,
        val cover: String?,
        val stats: String?,
        val status: String?,
        val summary: String?,
    )

    private data class CoverBinary(
        val bytes: ByteArray,
        val extension: String,
        val mediaType: String,
    )

    private companion object {
        const val USER_AGENT = "MIYO-Kotlin/1.0 (+https://miyo-reader.local)"
        const val PROVIDER_TIMEOUT_MS = 18_000
        const val MAX_HTML_BYTES = 4 * 1024 * 1024
        const val MAX_COVER_BYTES = 6 * 1024 * 1024
        const val MAX_DOWNLOAD_CHAPTERS = 250
        const val MIN_DOWNLOAD_CONCURRENCY = 2
        const val MAX_DOWNLOAD_CONCURRENCY = 10
        const val PROVIDER_RETRY_ATTEMPTS = 3
        const val PROVIDER_RETRY_BASE_DELAY_MS = 450L

        val siteProviders = setOf(
            OnlineNovelProviderId.FANMTL,
            OnlineNovelProviderId.WUXIASPOT,
            OnlineNovelProviderId.MCREADER,
        )
    }
}
