package com.miyu.reader.domain.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val coverUri: String? = null,
    val filePath: String,
    val fileName: String? = null,
    val contentHash: String? = null,
    val identityKey: String? = null,
    val epubIdentifier: String? = null,
    val language: String? = null,
    val storageLocation: StorageLocation = StorageLocation.APP,
    val storageFolderUri: String? = null,
    val progress: Float = 0f, // 0-100
    val currentChapter: Int = 0,
    val totalChapters: Int = 0,
    val lastReadAt: String? = null,
    val dateAdded: String,
    val readingStatus: ReadingStatus = ReadingStatus.UNREAD,
    val tags: List<String> = emptyList(),
)

@Keep
enum class StorageLocation { APP, SAF }

@Keep
enum class ReadingStatus { UNREAD, READING, FINISHED }

@Keep
enum class SortOption { RECENT, TITLE, AUTHOR, PROGRESS, DATE_ADDED }

@Keep
enum class FilterOption { ALL, UNREAD, READING, FINISHED }

@Keep
enum class ViewMode { GRID, LIST }