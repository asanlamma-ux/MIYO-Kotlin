package com.miyu.reader.domain.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Keep
@Serializable
data class Chapter(
    val id: String,
    val title: String,
    val href: String,
    val order: Int,
)

@Keep
@Serializable
data class ReadingPosition(
    val bookId: String,
    val chapterIndex: Int,
    val scrollPosition: Float,
    val chapterScrollPercent: Float? = null,
    val timestamp: String,
)

@Keep
@Serializable
data class Bookmark(
    val id: String,
    val bookId: String,
    val chapterIndex: Int,
    val position: Float,
    val text: String,
    val createdAt: String,
)

@Keep
@Serializable
data class Highlight(
    val id: String,
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