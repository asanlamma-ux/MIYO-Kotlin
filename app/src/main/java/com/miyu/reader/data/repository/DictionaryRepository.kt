package com.miyu.reader.data.repository

import com.miyu.reader.data.local.dao.DictionaryDao
import com.miyu.reader.data.local.entity.DictionaryEntity
import com.miyu.reader.data.local.entity.DictionaryEntryEntity
import com.miyu.reader.domain.model.DictionaryEntry
import com.miyu.reader.domain.model.DictionaryLookupResult
import com.miyu.reader.domain.model.DownloadedDictionary
import com.miyu.reader.domain.model.LookupSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DictionaryRepository @Inject constructor(
    private val dictionaryDao: DictionaryDao,
) {
    fun getDownloadedDictionaries(): Flow<List<DownloadedDictionary>> =
        dictionaryDao.getAllDictionaries().map { dictionaries ->
            dictionaries.map { it.toDownloadedDictionary() }
        }

    fun getStarterDictionaries(): List<DownloadedDictionary> = DefaultDictionaries.packs

    suspend fun getDownloadedDictionaryCount(): Int = dictionaryDao.getAllDictionaries().first().size

    suspend fun lookupWord(word: String): DictionaryLookupResult? {
        val normalized = normalizeWord(word)
        if (normalized.isBlank()) return null
        seedStarterDictionariesIfEmpty()
        return findOfflineEntries(word, normalized) ?: fetchOnlineDefinition(word, normalized)
    }

    suspend fun installStarterDictionary(dictionaryId: String): Boolean = withContext(Dispatchers.IO) {
        val dictionary = DefaultDictionaries.packs.find { it.id == dictionaryId } ?: return@withContext false
        upsertDictionary(dictionary.copy(downloadedAt = Instant.now().toString()))
        true
    }

    suspend fun removeDictionary(dictionaryId: String) {
        val dictionary = dictionaryDao.getDictionary(dictionaryId) ?: return
        dictionaryDao.deleteEntries(dictionaryId)
        dictionaryDao.deleteDictionary(dictionary)
    }

    suspend fun importDictionaryFromText(
        text: String,
        packageUrl: String? = null,
        packageSizeBytes: Long? = null,
    ): DownloadedDictionary = withContext(Dispatchers.IO) {
        parseDictionaryJson(text, packageUrl, packageSizeBytes).also { upsertDictionary(it) }
    }

    suspend fun importDictionaryFromBytes(
        bytes: ByteArray,
        fileName: String,
        packageUrl: String? = null,
    ): DownloadedDictionary = withContext(Dispatchers.IO) {
        if (bytes.size > MAX_DICTIONARY_PACKAGE_BYTES) {
            error("Dictionary packages are limited to ${MAX_DICTIONARY_PACKAGE_BYTES / 1024 / 1024} MB.")
        }
        val dictionary = if (fileName.endsWith(".zip", ignoreCase = true)) {
            parseDictionaryZip(bytes, packageUrl)
        } else {
            parseDictionaryJson(bytes.toString(StandardCharsets.UTF_8), packageUrl, bytes.size.toLong())
        }
        upsertDictionary(dictionary)
        dictionary
    }

    suspend fun importDictionaryFromUrl(url: String): DownloadedDictionary = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizePackageUrl(url)
        val connection = (URL(normalizedUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json,application/zip,*/*;q=0.8")
            setRequestProperty("User-Agent", "MIYO-Kotlin/1.0")
        }
        try {
            if (connection.responseCode !in 200..299) {
                error("Dictionary download failed (${connection.responseCode}).")
            }
            val contentLength = connection.contentLengthLong.takeIf { it > 0 }
            if (contentLength != null && contentLength > MAX_DICTIONARY_PACKAGE_BYTES) {
                error("Dictionary packages are limited to ${MAX_DICTIONARY_PACKAGE_BYTES / 1024 / 1024} MB.")
            }
            val contentType = connection.contentType.orEmpty().lowercase()
            val bytes = connection.inputStream.readBoundedBytes(MAX_DICTIONARY_PACKAGE_BYTES)
            val dictionary = if (contentType.contains("zip") || normalizedUrl.endsWith(".zip", ignoreCase = true)) {
                parseDictionaryZip(bytes, normalizedUrl)
            } else {
                parseDictionaryJson(bytes.toString(StandardCharsets.UTF_8), normalizedUrl, bytes.size.toLong())
            }
            upsertDictionary(dictionary)
            dictionary
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun findOfflineEntries(
        originalWord: String,
        normalizedWord: String,
    ): DictionaryLookupResult? {
        val dictionaries = dictionaryDao.getAllDictionaries().first()
        if (dictionaries.isEmpty()) return null

        val entries = dictionaryDao.searchEntries(normalizedWord, dictionaries.map { it.id })
            .filter { entry ->
                listOf(entry.term, *entry.aliases.toTypedArray())
                    .map(::normalizeWord)
                    .contains(normalizedWord)
            }

        if (entries.isEmpty()) return null

        val namesById = dictionaries.associate { it.id to it.name }
        return DictionaryLookupResult(
            source = LookupSource.OFFLINE,
            word = originalWord.trim(),
            dictionaryName = entries.mapNotNull { namesById[it.dictionaryId] }.distinct().joinToString(", "),
            entries = entries.map(::mapEntry),
        )
    }

    private suspend fun seedStarterDictionariesIfEmpty() {
        if (dictionaryDao.getAllDictionaries().first().isNotEmpty()) return
        DefaultDictionaries.packs.forEach { dictionary ->
            upsertDictionary(dictionary.copy(downloadedAt = Instant.now().toString()))
        }
    }

    private suspend fun upsertDictionary(dictionary: DownloadedDictionary) {
        val sanitized = dictionary.sanitized()
        dictionaryDao.upsertDictionary(sanitized.toEntity())
        dictionaryDao.deleteEntries(sanitized.id)
        dictionaryDao.upsertEntries(sanitized.entries.map { it.toEntity(sanitized.id) })
    }

    private fun parseDictionaryJson(
        text: String,
        packageUrl: String?,
        packageSizeBytes: Long?,
    ): DownloadedDictionary {
        val trimmed = text.trim()
        if (trimmed.isBlank()) error("The dictionary package is empty.")
        val root = JSONObject(trimmed)
        if (root.has("magic") && root.optString("magic") != DICTIONARY_PACKAGE_MAGIC) {
            error("This file is not a supported Miyo dictionary package.")
        }
        val manifest = root.optJSONObject("manifest") ?: root
        val entriesJson = root.optJSONArray("entries") ?: JSONArray()
        val entries = buildList {
            for (index in 0 until entriesJson.length()) {
                sanitizeEntry(entriesJson.optJSONObject(index))?.let(::add)
                if (size >= MAX_DICTIONARY_ENTRIES) break
            }
        }
        if (entries.isEmpty()) error("This dictionary package does not contain valid entries.")

        val name = manifest.optString("name", "Imported Dictionary").sanitizePlainText(MAX_NAME_CHARS)
            .ifBlank { "Imported Dictionary" }
        return DownloadedDictionary(
            id = manifest.optString("id").sanitizeDictionaryId().ifBlank { slugify(name) },
            name = name,
            description = manifest.optString("description").sanitizePlainText(MAX_DESCRIPTION_CHARS).takeIf { it.isNotBlank() },
            language = manifest.optString("language", "en").sanitizePlainText(MAX_LANGUAGE_CHARS).ifBlank { "en" },
            version = manifest.optString("version", "1.0.0").sanitizePlainText(MAX_VERSION_CHARS).ifBlank { "1.0.0" },
            tags = manifest.optJSONArray("tags").toStringList(MAX_TAGS, MAX_TAG_CHARS),
            entriesCount = entries.size,
            downloadCount = manifest.optInt("downloadCount", manifest.optInt("download_count", 0)).coerceAtLeast(0),
            attribution = manifest.optString("attribution").sanitizePlainText(MAX_ATTRIBUTION_CHARS).takeIf { it.isNotBlank() },
            sourceUrl = manifest.optString("sourceUrl").sanitizePlainText(MAX_URL_CHARS).takeIf { it.isNotBlank() } ?: packageUrl,
            packageUrl = packageUrl,
            packageSizeBytes = packageSizeBytes,
            downloadedAt = Instant.now().toString(),
            entries = entries,
        ).sanitized()
    }

    private fun parseDictionaryZip(bytes: ByteArray, packageUrl: String?): DownloadedDictionary {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name.substringAfterLast('/').lowercase()
                if (!entry.isDirectory && (name == "dictionary.json" || name == "package.json" || name.endsWith(".json"))) {
                    val payload = zip.readBoundedBytes(MAX_DICTIONARY_JSON_BYTES).toString(StandardCharsets.UTF_8)
                    return parseDictionaryJson(payload, packageUrl, bytes.size.toLong())
                }
            }
        }
        error("The ZIP file does not include a dictionary JSON payload.")
    }

    private fun sanitizeEntry(input: JSONObject?): DictionaryEntry? {
        if (input == null) return null
        val term = input.optString("term").sanitizePlainText(MAX_TERM_CHARS)
        val definition = input.optString("definition").sanitizePlainText(MAX_DEFINITION_CHARS)
        if (term.isBlank() || definition.isBlank()) return null
        return DictionaryEntry(
            term = term,
            definition = definition,
            partOfSpeech = input.optString("partOfSpeech").sanitizePlainText(MAX_PART_OF_SPEECH_CHARS).takeIf { it.isNotBlank() },
            example = input.optString("example").sanitizePlainText(MAX_EXAMPLE_CHARS).takeIf { it.isNotBlank() },
            aliases = input.optJSONArray("aliases").toStringList(MAX_ALIASES, MAX_TERM_CHARS),
            source = input.optString("source").sanitizePlainText(MAX_SOURCE_CHARS).takeIf { it.isNotBlank() },
        )
    }

    private suspend fun fetchOnlineDefinition(
        originalWord: String,
        normalizedWord: String,
    ): DictionaryLookupResult? = withContext(Dispatchers.IO) {
        runCatching {
            val encodedWord = URLEncoder.encode(normalizedWord, StandardCharsets.UTF_8.name())
            val sourceUrl = "https://api.dictionaryapi.dev/api/v2/entries/en/$encodedWord"
            val connection = (URL(sourceUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 10000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "MIYO-Kotlin/1.0")
            }

            if (connection.responseCode !in 200..299) {
                connection.disconnect()
                return@runCatching null
            }

            connection.inputStream.bufferedReader().use { reader ->
                val payload = JSONArray(reader.readText())
                val entries = parseDictionaryApiEntries(payload, normalizedWord)

                if (entries.isEmpty()) return@runCatching null

                DictionaryLookupResult(
                    source = LookupSource.ONLINE,
                    word = originalWord.trim(),
                    dictionaryName = "Online Dictionary",
                    entries = entries,
                    sourceUrl = sourceUrl,
                )
            }
        }.getOrNull()
    }

    private fun parseDictionaryApiEntries(
        payload: JSONArray,
        normalizedWord: String,
    ): List<DictionaryEntry> {
        val entries = mutableListOf<DictionaryEntry>()
        for (itemIndex in 0 until payload.length()) {
            val item = payload.getJSONObject(itemIndex)
            val term = item.optString("word", normalizedWord)
            val meanings = item.optJSONArray("meanings") ?: continue
            for (meaningIndex in 0 until meanings.length()) {
                val meaning = meanings.getJSONObject(meaningIndex)
                val partOfSpeech = meaning.optString("partOfSpeech").takeIf { it.isNotBlank() }
                val definitions = meaning.optJSONArray("definitions") ?: continue
                for (definitionIndex in 0 until definitions.length()) {
                    val definition = definitions.getJSONObject(definitionIndex)
                    val text = definition.optString("definition").takeIf { it.isNotBlank() } ?: continue
                    entries += DictionaryEntry(
                        term = term,
                        definition = text,
                        partOfSpeech = partOfSpeech,
                        example = definition.optString("example").takeIf { it.isNotBlank() },
                        source = "dictionaryapi.dev",
                    )
                }
            }
        }
        return entries
    }

    private fun mapEntry(entry: DictionaryEntryEntity): DictionaryEntry = DictionaryEntry(
        term = entry.term,
        definition = entry.definition,
        partOfSpeech = entry.partOfSpeech,
        example = entry.example,
        aliases = entry.aliases,
        source = entry.source,
    )

    private fun DownloadedDictionary.sanitized(): DownloadedDictionary =
        copy(
            id = id.sanitizeDictionaryId().ifBlank { slugify(name) },
            name = name.sanitizePlainText(MAX_NAME_CHARS).ifBlank { "Imported Dictionary" },
            description = description?.sanitizePlainText(MAX_DESCRIPTION_CHARS)?.takeIf { it.isNotBlank() },
            language = language.sanitizePlainText(MAX_LANGUAGE_CHARS).ifBlank { "en" },
            version = version.sanitizePlainText(MAX_VERSION_CHARS).ifBlank { "1.0.0" },
            tags = tags.map { it.sanitizePlainText(MAX_TAG_CHARS) }.filter { it.isNotBlank() }.distinct().take(MAX_TAGS),
            entries = entries.mapNotNull {
                val term = it.term.sanitizePlainText(MAX_TERM_CHARS)
                val definition = it.definition.sanitizePlainText(MAX_DEFINITION_CHARS)
                if (term.isBlank() || definition.isBlank()) {
                    null
                } else {
                    it.copy(
                        term = term,
                        definition = definition,
                        partOfSpeech = it.partOfSpeech?.sanitizePlainText(MAX_PART_OF_SPEECH_CHARS)?.takeIf { value -> value.isNotBlank() },
                        example = it.example?.sanitizePlainText(MAX_EXAMPLE_CHARS)?.takeIf { value -> value.isNotBlank() },
                        aliases = it.aliases.map { alias -> alias.sanitizePlainText(MAX_TERM_CHARS) }.filter { alias -> alias.isNotBlank() }.take(MAX_ALIASES),
                        source = it.source?.sanitizePlainText(MAX_SOURCE_CHARS)?.takeIf { value -> value.isNotBlank() },
                    )
                }
            }.take(MAX_DICTIONARY_ENTRIES),
        ).let { it.copy(entriesCount = it.entries.size) }

    private fun DownloadedDictionary.toEntity(): DictionaryEntity = DictionaryEntity(
        id = id,
        name = name,
        description = description,
        language = language,
        version = version,
        tags = tags,
        entriesCount = entriesCount,
        downloadCount = downloadCount,
        attribution = attribution,
        sourceUrl = sourceUrl,
        packageUrl = packageUrl,
        packageSizeBytes = packageSizeBytes,
        downloadedAt = downloadedAt,
    )

    private fun DictionaryEntity.toDownloadedDictionary(): DownloadedDictionary = DownloadedDictionary(
        id = id,
        name = name,
        description = description,
        language = language,
        version = version,
        tags = tags,
        entriesCount = entriesCount,
        downloadCount = downloadCount,
        attribution = attribution,
        sourceUrl = sourceUrl,
        packageUrl = packageUrl,
        packageSizeBytes = packageSizeBytes,
        downloadedAt = downloadedAt,
        entries = emptyList(),
    )

    private fun DictionaryEntry.toEntity(dictionaryId: String): DictionaryEntryEntity = DictionaryEntryEntity(
        dictionaryId = dictionaryId,
        term = normalizeWord(term),
        definition = definition,
        partOfSpeech = partOfSpeech,
        example = example,
        aliases = aliases.map(::normalizeWord).filter { it.isNotBlank() },
        source = source,
    )

    private fun normalizePackageUrl(url: String): String {
        val candidate = url.trim()
        if (!candidate.startsWith("https://", ignoreCase = true)) {
            error("Dictionary package URLs must use HTTPS.")
        }
        val uri = URI(candidate)
        val host = uri.host?.lowercase().orEmpty()
        if (host.isBlank() || host == "localhost" || host.endsWith(".local") || isPrivateIpv4(host) || isPrivateIpv6(host)) {
            error("Private-network dictionary package URLs are blocked.")
        }
        return uri.toURL().toString()
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

    private fun InputStream.readBoundedBytes(maxBytes: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read == -1) break
            total += read
            if (total > maxBytes) error("Dictionary packages are limited to ${maxBytes / 1024 / 1024} MB.")
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun JSONArray?.toStringList(maxItems: Int, maxChars: Int): List<String> {
        if (this == null) return emptyList()
        val output = mutableListOf<String>()
        for (index in 0 until length()) {
            val value = optString(index).sanitizePlainText(maxChars)
            if (value.isNotBlank()) output += value
            if (output.size >= maxItems) break
        }
        return output.distinct()
    }

    private fun String.sanitizePlainText(maxChars: Int): String =
        replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]"), "")
            .trim()
            .take(maxChars)

    private fun String.sanitizeDictionaryId(): String =
        lowercase()
            .replace(Regex("[^a-z0-9._-]+"), "-")
            .trim('-')
            .take(80)

    private fun slugify(value: String): String =
        value.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(64)
            .ifBlank { "dictionary-${System.currentTimeMillis()}" }

    private fun normalizeWord(value: String): String = value
        .trim()
        .lowercase()
        .replace(Regex("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$"), "")

    private companion object {
        const val DICTIONARY_PACKAGE_MAGIC = "MIYO_DICTIONARY_V1"
        const val MAX_DICTIONARY_PACKAGE_BYTES = 12 * 1024 * 1024
        const val MAX_DICTIONARY_JSON_BYTES = 10 * 1024 * 1024
        const val MAX_DICTIONARY_ENTRIES = 50_000
        const val MAX_NAME_CHARS = 120
        const val MAX_DESCRIPTION_CHARS = 400
        const val MAX_LANGUAGE_CHARS = 16
        const val MAX_VERSION_CHARS = 24
        const val MAX_TAGS = 16
        const val MAX_TAG_CHARS = 32
        const val MAX_ATTRIBUTION_CHARS = 160
        const val MAX_URL_CHARS = 400
        const val MAX_TERM_CHARS = 120
        const val MAX_DEFINITION_CHARS = 1_200
        const val MAX_PART_OF_SPEECH_CHARS = 40
        const val MAX_EXAMPLE_CHARS = 280
        const val MAX_ALIASES = 12
        const val MAX_SOURCE_CHARS = 120
    }
}
