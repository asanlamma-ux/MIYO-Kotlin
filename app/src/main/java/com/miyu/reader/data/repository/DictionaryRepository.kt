package com.miyu.reader.data.repository

import com.miyu.reader.data.local.dao.DictionaryDao
import com.miyu.reader.data.local.entity.DictionaryEntryEntity
import com.miyu.reader.domain.model.DictionaryEntry
import com.miyu.reader.domain.model.DictionaryLookupResult
import com.miyu.reader.domain.model.LookupSource
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DictionaryRepository @Inject constructor(
    private val dictionaryDao: DictionaryDao,
) {
    suspend fun getDownloadedDictionaryCount(): Int = dictionaryDao.getAllDictionaries().first().size

    suspend fun lookupWord(word: String): DictionaryLookupResult? {
        val normalized = normalizeWord(word)
        if (normalized.isBlank()) return null
        return findOfflineEntries(word, normalized) ?: fetchOnlineDefinition(word, normalized)
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

    private suspend fun fetchOnlineDefinition(
        originalWord: String,
        normalizedWord: String,
    ): DictionaryLookupResult? {
        return runCatching {
            val connection = (URL("https://api.dictionaryapi.dev/api/v2/entries/en/$normalizedWord").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("Accept", "application/json")
            }

            connection.inputStream.bufferedReader().use { reader ->
                if (connection.responseCode !in 200..299) return null
                val payload = JSONArray(reader.readText())
                val entries = buildList {
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
                                add(
                                    DictionaryEntry(
                                        term = term,
                                        definition = text,
                                        partOfSpeech = partOfSpeech,
                                        example = definition.optString("example").takeIf { it.isNotBlank() },
                                        source = "dictionaryapi.dev",
                                    ),
                                )
                            }
                        }
                    }
                }

                if (entries.isEmpty()) return null

                DictionaryLookupResult(
                    source = LookupSource.ONLINE,
                    word = originalWord.trim(),
                    dictionaryName = "Online Dictionary",
                    entries = entries,
                    sourceUrl = "https://api.dictionaryapi.dev/api/v2/entries/en/$normalizedWord",
                )
            }
        }.getOrNull()
    }

    private fun mapEntry(entry: DictionaryEntryEntity): DictionaryEntry = DictionaryEntry(
        term = entry.term,
        definition = entry.definition,
        partOfSpeech = entry.partOfSpeech,
        example = entry.example,
        aliases = entry.aliases,
        source = entry.source,
    )

    private fun normalizeWord(value: String): String = value
        .trim()
        .lowercase()
        .replace(Regex("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$"), "")
}
