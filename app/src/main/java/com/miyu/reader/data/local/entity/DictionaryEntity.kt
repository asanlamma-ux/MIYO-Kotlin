package com.miyu.reader.data.local.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "dictionaries")
data class DictionaryEntity(
    @PrimaryKey val id: String,
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
)

@Keep
@Entity(tableName = "dictionary_entries")
data class DictionaryEntryEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val dictionaryId: String,
    val term: String,
    val definition: String,
    val partOfSpeech: String? = null,
    val example: String? = null,
    val aliases: List<String> = emptyList(),
    val source: String? = null,
)