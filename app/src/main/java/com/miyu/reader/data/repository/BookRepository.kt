package com.miyu.reader.data.repository

import com.miyu.reader.data.local.dao.BookDao
import com.miyu.reader.data.local.entity.*
import com.miyu.reader.data.toDomain
import com.miyu.reader.data.toEntity
import com.miyu.reader.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepository @Inject constructor(
    private val bookDao: BookDao,
) {
    fun getAllBooks(): Flow<List<Book>> = bookDao.getAllBooks().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getAllBooksOnce(): List<Book> = getAllBooks().first()

    fun getBooksByStatus(status: ReadingStatus): Flow<List<Book>> =
        bookDao.getBooksByStatus(status.name).map { entities ->
            entities.map { it.toDomain() }
        }

    fun searchBooks(query: String): Flow<List<Book>> =
        bookDao.searchBooks(query).map { entities ->
            entities.map { it.toDomain() }
        }

    fun observeBook(bookId: String): Flow<Book?> =
        bookDao.observeBook(bookId).map { it?.toDomain() }

    suspend fun getBook(bookId: String): Book? = bookDao.getBookById(bookId)?.toDomain()

    suspend fun saveBook(book: Book) {
        bookDao.upsertBook(book.toEntity())
    }

    suspend fun deleteBook(bookId: String) {
        bookDao.deleteBookById(bookId)
    }

    suspend fun updateProgress(bookId: String, chapterIndex: Int, progress: Float) {
        val book = bookDao.getBookById(bookId) ?: return
        val readingStatus = when {
            progress >= 100f -> ReadingStatus.FINISHED
            progress > 0f -> ReadingStatus.READING
            else -> book.readingStatus
        }
        bookDao.upsertBook(book.copy(
            currentChapter = chapterIndex,
            progress = progress,
            lastReadAt = java.time.Instant.now().toString(),
            readingStatus = readingStatus,
        ))
    }

    suspend fun updateBookTags(bookId: String, tags: List<String>) {
        val book = bookDao.getBookById(bookId) ?: return
        bookDao.upsertBook(book.copy(tags = tags))
    }

    suspend fun importBook(book: Book): Boolean {
        val existing = bookDao.getBookById(book.id)
        if (existing != null) return false
        bookDao.upsertBook(book.toEntity())
        return true
    }

    // Bookmark operations
    fun getBookmarks(bookId: String): Flow<List<Bookmark>> =
        bookDao.getBookmarks(bookId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun addBookmark(bookmark: Bookmark) {
        val entity = BookmarkEntity(
            id = bookmark.id,
            bookId = bookmark.bookId,
            chapterIndex = bookmark.chapterIndex,
            position = bookmark.position,
            text = bookmark.text,
            createdAt = bookmark.createdAt,
        )
        bookDao.upsertBookmark(entity)
    }

    suspend fun removeBookmark(bookmark: Bookmark) {
        bookDao.deleteBookmark(BookmarkEntity(
            id = bookmark.id,
            bookId = bookmark.bookId,
            chapterIndex = bookmark.chapterIndex,
            position = bookmark.position,
            text = bookmark.text,
            createdAt = bookmark.createdAt,
        ))
    }

    // Highlight operations
    fun getHighlights(bookId: String): Flow<List<Highlight>> =
        bookDao.getHighlights(bookId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun addHighlight(highlight: Highlight) {
        val entity = HighlightEntity(
            id = highlight.id,
            bookId = highlight.bookId,
            chapterIndex = highlight.chapterIndex,
            startOffset = highlight.startOffset,
            endOffset = highlight.endOffset,
            text = highlight.text,
            color = highlight.color,
            textColor = highlight.textColor,
            note = highlight.note,
            createdAt = highlight.createdAt,
        )
        bookDao.upsertHighlight(entity)
    }

    suspend fun removeHighlight(highlightId: String) {
        bookDao.deleteHighlightById(highlightId)
    }

    suspend fun updateHighlightNote(highlightId: String, note: String?) {
        // Load all highlights to find the one, update, and re-save
        // Room @Upsert handles the rest
        val highlights = bookDao.getAllBooks() // Need a better approach
        // For now, read+update is handled via the ViewModel
    }

    // Reading position
    suspend fun saveReadingPosition(position: ReadingPosition) {
        bookDao.saveReadingPosition(ReadingPositionEntity(
            bookId = position.bookId,
            chapterIndex = position.chapterIndex,
            scrollPosition = position.scrollPosition,
            chapterScrollPercent = position.chapterScrollPercent,
            timestamp = position.timestamp,
        ))
    }

    suspend fun getReadingPosition(bookId: String): ReadingPosition? =
        bookDao.getReadingPosition(bookId)?.toDomain()
}
