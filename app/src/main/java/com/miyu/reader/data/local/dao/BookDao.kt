package com.miyu.reader.data.local.dao

import androidx.room.*
import com.miyu.reader.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastReadAt DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookById(bookId: String): BookEntity?

    @Query("SELECT * FROM books WHERE identityKey = :identityKey LIMIT 1")
    suspend fun getBookByIdentityKey(identityKey: String): BookEntity?

    @Query("SELECT * FROM books WHERE id = :bookId")
    fun observeBook(bookId: String): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE readingStatus = :status ORDER BY lastReadAt DESC")
    fun getBooksByStatus(status: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%'")
    fun searchBooks(query: String): Flow<List<BookEntity>>

    @Upsert
    suspend fun upsertBook(book: BookEntity)

    @Upsert
    suspend fun upsertBooks(books: List<BookEntity>)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBookById(bookId: String)

    // Bookmarks
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getBookmarks(bookId: String): Flow<List<BookmarkEntity>>

    @Upsert
    suspend fun upsertBookmark(bookmark: BookmarkEntity)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE bookId = :bookId")
    suspend fun deleteBookmarksByBookId(bookId: String)

    // Highlights
    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getHighlights(bookId: String): Flow<List<HighlightEntity>>

    @Upsert
    suspend fun upsertHighlight(highlight: HighlightEntity)

    @Delete
    suspend fun deleteHighlight(highlight: HighlightEntity)

    @Query("DELETE FROM highlights WHERE id = :highlightId")
    suspend fun deleteHighlightById(highlightId: String)

    @Query("DELETE FROM highlights WHERE bookId = :bookId")
    suspend fun deleteHighlightsByBookId(bookId: String)

    @Query("UPDATE highlights SET note = :note WHERE id = :highlightId")
    suspend fun updateHighlightNote(highlightId: String, note: String?)

    // Reading position
    @Query("SELECT * FROM reading_positions WHERE bookId = :bookId")
    suspend fun getReadingPosition(bookId: String): ReadingPositionEntity?

    @Upsert
    suspend fun saveReadingPosition(position: ReadingPositionEntity)

    @Query("DELETE FROM reading_positions WHERE bookId = :bookId")
    suspend fun deleteReadingPosition(bookId: String)
}
