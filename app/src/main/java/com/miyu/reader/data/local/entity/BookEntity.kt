package com.miyu.reader.data.local.entity

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.miyu.reader.domain.model.ReadingStatus
import com.miyu.reader.domain.model.StorageLocation

@Keep
@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
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
    val progress: Float = 0f,
    val currentChapter: Int = 0,
    val totalChapters: Int = 0,
    val lastReadAt: String? = null,
    val dateAdded: String,
    val readingStatus: ReadingStatus = ReadingStatus.UNREAD,
    val tags: List<String> = emptyList(),
)

@Keep
@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val chapterIndex: Int,
    val position: Float,
    val text: String,
    val createdAt: String,
)

@Keep
@Entity(tableName = "highlights")
data class HighlightEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val chapterIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val text: String,
    val color: String,
    val textColor: String? = null,
    val note: String? = null,
    val createdAt: String,
)

@Keep
@Entity(tableName = "reading_positions")
data class ReadingPositionEntity(
    @PrimaryKey val bookId: String,
    val chapterIndex: Int,
    val scrollPosition: Float,
    val chapterScrollPercent: Float? = null,
    val timestamp: String,
)