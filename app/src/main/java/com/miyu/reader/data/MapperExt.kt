package com.miyu.reader.data

import com.miyu.reader.data.local.entity.*
import com.miyu.reader.domain.model.*

// Book mappings
fun Book.toEntity() = BookEntity(
    id = id, title = title, author = author, coverUri = coverUri,
    filePath = filePath, fileName = fileName, contentHash = contentHash,
    identityKey = identityKey, epubIdentifier = epubIdentifier,
    language = language, storageLocation = storageLocation,
    storageFolderUri = storageFolderUri, progress = progress,
    currentChapter = currentChapter, totalChapters = totalChapters,
    lastReadAt = lastReadAt, dateAdded = dateAdded,
    readingStatus = readingStatus, tags = tags,
)

fun BookEntity.toDomain() = Book(
    id = id, title = title, author = author, coverUri = coverUri,
    filePath = filePath, fileName = fileName, contentHash = contentHash,
    identityKey = identityKey, epubIdentifier = epubIdentifier,
    language = language, storageLocation = storageLocation,
    storageFolderUri = storageFolderUri, progress = progress,
    currentChapter = currentChapter, totalChapters = totalChapters,
    lastReadAt = lastReadAt, dateAdded = dateAdded,
    readingStatus = readingStatus, tags = tags,
)

// Bookmark mappings
fun BookmarkEntity.toDomain() = Bookmark(
    id = id, bookId = bookId, chapterIndex = chapterIndex,
    position = position, text = text, createdAt = createdAt,
)

// Highlight mappings
fun HighlightEntity.toDomain() = Highlight(
    id = id, bookId = bookId, chapterIndex = chapterIndex,
    startOffset = startOffset, endOffset = endOffset,
    text = text, color = color, textColor = textColor,
    note = note, createdAt = createdAt,
)

// ReadingPosition mappings
fun ReadingPositionEntity.toDomain() = ReadingPosition(
    bookId = bookId, chapterIndex = chapterIndex,
    scrollPosition = scrollPosition,
    chapterScrollPercent = chapterScrollPercent,
    timestamp = timestamp,
)

// Term mappings
fun TermEntity.toDomain() = Term(
    id = id, originalText = originalText, translationText = translationText,
    correctedText = correctedText, context = context, imageUri = imageUri,
    createdAt = createdAt, updatedAt = updatedAt,
)

fun Term.toEntity(groupId: String) = TermEntity(
    id = id, originalText = originalText, translationText = translationText,
    correctedText = correctedText, context = context, imageUri = imageUri,
    createdAt = createdAt, updatedAt = updatedAt, groupId = groupId,
)

// TermGroup mappings
fun TermGroupEntity.toDomain(terms: List<Term> = emptyList()) = TermGroup(
    id = id, name = name, description = description,
    terms = terms, appliedToBooks = appliedToBooks,
    createdAt = createdAt, updatedAt = updatedAt,
)