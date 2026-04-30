package com.miyu.reader.domain.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class DictionaryEntry(
    val term: String,
    val definition: String,
    val partOfSpeech: String? = null,
    val example: String? = null,
    val aliases: List<String> = emptyList(),
    val source: String? = null,
)

@Keep
@Serializable
data class DictionaryManifest(
    val id: String,
    val name: String,
    val description: String? = null,
    val language: String,
    val version: String,
    val tags: List<String> = emptyList(),
    val entriesCount: Int,
    val downloadCount: Int = 0,
    val attribution: String? = null,
    val sourceUrl: String? = null,
    val packageUrl: String? = null,
    val packageSizeBytes: Long? = null,
)

@Keep
@Serializable
data class DownloadedDictionary(
    val id: String,
    val name: String,
    val description: String? = null,
    val language: String,
    val version: String,
    val tags: List<String> = emptyList(),
    val entriesCount: Int,
    val downloadCount: Int = 0,
    val attribution: String? = null,
    val sourceUrl: String? = null,
    val packageUrl: String? = null,
    val packageSizeBytes: Long? = null,
    val downloadedAt: String,
    val entries: List<DictionaryEntry> = emptyList(),
)

@Keep
@Serializable
data class DictionaryLookupResult(
    val source: LookupSource,
    val word: String,
    val dictionaryName: String,
    val entries: List<DictionaryEntry>,
    val sourceUrl: String? = null,
)

@Keep
enum class LookupSource { OFFLINE, ONLINE }