package com.miyu.reader.data.repository

import android.content.Context
import com.miyu.reader.domain.model.AddOpdsCatalogResult
import com.miyu.reader.domain.model.OpdsCatalog
import com.miyu.reader.domain.model.OpdsEntry
import com.miyu.reader.domain.model.OpdsFeed
import com.miyu.reader.domain.model.OpdsLink
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import javax.xml.parsers.DocumentBuilderFactory

@Singleton
class OpdsRepository @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    private val prefs by lazy {
        appContext.getSharedPreferences("miyo_opds_catalogs", Context.MODE_PRIVATE)
    }

    suspend fun getSavedCatalogs(): List<OpdsCatalog> = withContext(Dispatchers.IO) {
        val custom = runCatching {
            val raw = prefs.getString(KEY_CATALOGS, null) ?: return@runCatching emptyList()
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        OpdsCatalog(
                            id = item.optString("id"),
                            title = item.optString("title"),
                            url = item.optString("url"),
                            addedAt = item.optString("addedAt"),
                            isDefault = false,
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
        mergeCatalogs(custom.filter { it.id.isNotBlank() && it.url.isNotBlank() })
    }

    suspend fun addCatalog(url: String, title: String? = null): AddOpdsCatalogResult = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeCatalogUrl(url)
        val validatedFeed = fetchFeed(normalizedUrl)
        val canonicalUrl = validatedFeed.selfUrl ?: validatedFeed.url
        val catalogs = getSavedCatalogs()
        catalogs.find { it.url == canonicalUrl }?.let { existing ->
            return@withContext AddOpdsCatalogResult(catalogs, existing, alreadySaved = true)
        }

        val catalog = OpdsCatalog(
            id = "opds_${UUID.randomUUID()}",
            title = title?.trim()?.takeIf { it.isNotBlank() } ?: validatedFeed.title,
            url = canonicalUrl,
            addedAt = Instant.now().toString(),
            isDefault = false,
        )
        val next = catalogs + catalog
        saveCustomCatalogs(next)
        AddOpdsCatalogResult(next, catalog, alreadySaved = false)
    }

    suspend fun removeCatalog(catalogId: String): List<OpdsCatalog> = withContext(Dispatchers.IO) {
        val next = getSavedCatalogs().filter { it.id != catalogId || it.isDefault }
        saveCustomCatalogs(next)
        next
    }

    suspend fun fetchFeed(url: String): OpdsFeed = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeCatalogUrl(url)
        val connection = (URL(normalizedUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = OPDS_TIMEOUT_MS
            readTimeout = OPDS_TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/atom+xml,application/xml,text/xml;q=0.9,*/*;q=0.8")
            setRequestProperty("User-Agent", "MIYO-Kotlin/1.0")
        }
        try {
            if (connection.responseCode == 401 || connection.responseCode == 403) {
                error("This catalog requires authentication or blocks anonymous app requests.")
            }
            if (connection.responseCode !in 200..299) {
                error("Catalog request failed (${connection.responseCode}).")
            }
            val xml = connection.inputStream.use { it.readBoundedBytes(MAX_OPDS_FEED_BYTES) }.toString(Charsets.UTF_8)
            if (!xml.contains("<feed", ignoreCase = true)) {
                error("The response is not a valid OPDS feed.")
            }
            parseFeed(xml, connection.url?.toString() ?: normalizedUrl)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseFeed(xml: String, baseUrl: String): OpdsFeed {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            trySetFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            trySetFeature("http://xml.org/sax/features/external-general-entities", false)
            trySetFeature("http://xml.org/sax/features/external-parameter-entities", false)
            trySetFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
        val document = factory.newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
        val feed = document.documentElement ?: error("Could not parse this OPDS feed.")
        val links = parseLinks(feed.childElements("link"), baseUrl)
        val entries = feed.childElements("entry").mapIndexed { index, entry ->
            parseEntry(entry, baseUrl, index)
        }
        return OpdsFeed(
            title = feed.firstChildText("title").ifBlank { "Catalog" },
            url = baseUrl,
            entries = entries,
            selfUrl = links.findRel("self"),
            nextUrl = links.findRel("next"),
            previousUrl = links.findRel("previous") ?: links.findRel("prev"),
        )
    }

    private fun parseEntry(entry: Element, baseUrl: String, index: Int): OpdsEntry {
        val links = parseLinks(entry.childElements("link"), baseUrl)
        val acquisition = links.filter { link ->
            link.rel.contains("acquisition", ignoreCase = true) ||
                link.type.equals("application/epub+zip", ignoreCase = true)
        }
        val authors = entry.childElements("author")
            .map { author -> author.firstChildText("name").ifBlank { author.textContent.orEmpty().trim() } }
            .filter { it.isNotBlank() }
        val cover = links.firstOrNull { it.rel.contains("image", ignoreCase = true) && !it.rel.contains("thumbnail", ignoreCase = true) }
        val thumbnail = links.firstOrNull { it.rel.contains("thumbnail", ignoreCase = true) }
        val title = entry.firstChildText("title").ifBlank { "Untitled" }
        return OpdsEntry(
            id = entry.firstChildText("id").ifBlank { "$index-$title" },
            title = title,
            author = authors.joinToString(", ").ifBlank { "Unknown Author" },
            summary = entry.firstChildText("summary").ifBlank { entry.firstChildText("content") }.takeIf { it.isNotBlank() },
            coverUrl = cover?.href,
            thumbnailUrl = thumbnail?.href,
            acquisitionLinks = acquisition,
            navigationLinks = links.filterNot { it in acquisition },
        )
    }

    private fun parseLinks(elements: List<Element>, baseUrl: String): List<OpdsLink> =
        elements.mapNotNull { element ->
            val href = element.getAttribute("href").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            OpdsLink(
                href = runCatching { URI(baseUrl).resolve(href).toString() }.getOrDefault(href),
                rel = element.getAttribute("rel").takeIf { it.isNotBlank() } ?: "alternate",
                type = element.getAttribute("type").takeIf { it.isNotBlank() },
                title = element.getAttribute("title").takeIf { it.isNotBlank() },
            )
        }

    private fun normalizeCatalogUrl(url: String): String {
        val trimmed = url.trim()
        val candidate = if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            trimmed
        } else {
            "https://$trimmed"
        }
        val uri = URI(candidate)
        if (!uri.scheme.equals("https", ignoreCase = true)) {
            error("Use an HTTPS OPDS catalog URL. Insecure feeds are blocked.")
        }
        val host = uri.host?.lowercase().orEmpty()
        if (host.isBlank() || host == "localhost" || host.endsWith(".local") || isPrivateIpv4(host) || isPrivateIpv6(host)) {
            error("Private-network OPDS feeds are blocked. Use a public HTTPS catalog URL.")
        }
        return uri.toURL().toString()
    }

    private fun mergeCatalogs(custom: List<OpdsCatalog>): List<OpdsCatalog> =
        defaultCatalogs + custom.filter { catalog -> defaultCatalogs.none { it.url == catalog.url } }

    private fun saveCustomCatalogs(catalogs: List<OpdsCatalog>) {
        val array = JSONArray()
        catalogs.filterNot { it.isDefault }.forEach { catalog ->
            array.put(
                JSONObject()
                    .put("id", catalog.id)
                    .put("title", catalog.title)
                    .put("url", catalog.url)
                    .put("addedAt", catalog.addedAt)
            )
        }
        prefs.edit().putString(KEY_CATALOGS, array.toString()).apply()
    }

    private fun List<OpdsLink>.findRel(rel: String): String? =
        firstOrNull { it.rel.equals(rel, ignoreCase = true) || it.rel.contains(rel, ignoreCase = true) }?.href

    private fun Element.childElements(name: String): List<Element> = buildList {
        val children = childNodes
        for (index in 0 until children.length) {
            val node = children.item(index)
            if (node.nodeType == Node.ELEMENT_NODE && node.nodeNameMatches(name)) {
                add(node as Element)
            }
        }
    }

    private fun Element.firstChildText(name: String): String =
        childElements(name).firstOrNull()?.textContent?.trim().orEmpty()

    private fun Node.nodeNameMatches(expected: String): Boolean {
        val local = localName ?: nodeName.substringAfter(':', nodeName)
        return local.equals(expected, ignoreCase = true)
    }

    private fun DocumentBuilderFactory.trySetFeature(feature: String, enabled: Boolean) {
        // Android XML parsers vary by API/OEM. Keep XXE hardening where supported
        // without breaking valid OPDS feeds when a feature URI is unavailable.
        runCatching { setFeature(feature, enabled) }
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

    private fun java.io.InputStream.readBoundedBytes(maxBytes: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read == -1) break
            total += read
            if (total > maxBytes) error("OPDS feeds are limited to ${maxBytes / 1024 / 1024} MB.")
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private companion object {
        const val KEY_CATALOGS = "catalogs"
        const val OPDS_TIMEOUT_MS = 12_000
        const val MAX_OPDS_FEED_BYTES = 4 * 1024 * 1024

        val defaultCatalogs = listOf(
            OpdsCatalog(
                id = "default-gutenberg",
                title = "Project Gutenberg",
                url = "https://www.gutenberg.org/ebooks.opds/",
                addedAt = "default",
                isDefault = true,
            )
        )
    }
}
