package com.miyu.reader.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.room.withTransaction
import com.miyu.reader.data.local.dao.BookDao
import com.miyu.reader.data.local.MIYUDatabase
import com.miyu.reader.data.local.entity.*
import com.miyu.reader.data.toDomain
import com.miyu.reader.data.toEntity
import com.miyu.reader.domain.model.*
import com.miyu.reader.engine.bridge.EpubEngineBridge
import com.miyu.reader.storage.MiyoStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepository @Inject constructor(
    private val bookDao: BookDao,
    private val database: MIYUDatabase,
    private val epubEngine: EpubEngineBridge,
    @ApplicationContext private val appContext: Context,
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
        val book = bookDao.getBookById(bookId)?.toDomain() ?: return
        database.withTransaction {
            bookDao.deleteBookmarksByBookId(bookId)
            bookDao.deleteHighlightsByBookId(bookId)
            bookDao.deleteReadingPosition(bookId)
            bookDao.deleteBookById(bookId)
        }
        runCatching { epubEngine.evictCache(book.filePath) }
        cleanupManagedFiles(book)
    }

    suspend fun updateProgress(bookId: String, chapterIndex: Int, progress: Float) {
        val book = bookDao.getBookById(bookId) ?: return
        val readingStatus = when {
            progress >= 100f -> ReadingStatus.FINISHED
            progress > 0f -> ReadingStatus.READING
            else -> ReadingStatus.UNREAD
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

    suspend fun importGeneratedOnlineNovelEpub(
        filePath: String,
        fileName: String,
        suggestedTitle: String,
        identityKey: String? = null,
        tags: List<String>? = null,
    ): Book = withContext(Dispatchers.IO) {
        val normalizedIdentityKey = identityKey?.trim()?.takeIf { it.isNotBlank() }
        val normalizedTags = tags
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.distinct()
        val existingBook = normalizedIdentityKey
            ?.let { key -> bookDao.getBookByIdentityKey(key)?.toDomain() }
        val destFile = copyGeneratedEpubToInternalBookFile(
            sourceFile = File(filePath),
            fileName = uniqueImportFileName(fileName),
        )
        try {
            val parsed = JSONObject(epubEngine.parseEpub(destFile.absolutePath))
            val metadata = parsed.optJSONObject("metadata")
            val totalChapters = parsed.optInt("totalChapters", 0)
            if (totalChapters <= 0) error("The generated EPUB did not contain readable chapters.")

            val bookId = existingBook?.id ?: UUID.randomUUID().toString()
            val title = metadata?.optString("title")
                ?.takeIf { it.isNotBlank() }
                ?: suggestedTitle.ifBlank { destFile.nameWithoutExtension.replace("_", " ") }
            val author = metadata?.optString("author")
                ?.takeIf { it.isNotBlank() }
                ?: existingBook?.author
                ?: "Unknown"
            val coverUri = epubEngine.extractCoverImage(destFile.absolutePath)
                ?.takeIf { it.isNotBlank() }
                ?.let { if (it.startsWith("data:", ignoreCase = true)) it else "data:image/jpeg;base64,$it" }
                ?.let { persistCoverImage(bookId, it) ?: it }
                ?: existingBook?.coverUri

            val importedBook = Book(
                id = bookId,
                title = title,
                author = author,
                coverUri = coverUri,
                filePath = destFile.absolutePath,
                fileName = destFile.name,
                identityKey = normalizedIdentityKey ?: existingBook?.identityKey,
                epubIdentifier = metadata?.optString("identifier")?.takeIf { it.isNotBlank() } ?: existingBook?.epubIdentifier,
                language = metadata?.optString("language")?.takeIf { it.isNotBlank() } ?: existingBook?.language,
                progress = existingBook?.progress ?: 0f,
                currentChapter = existingBook?.currentChapter ?: 0,
                totalChapters = totalChapters,
                lastReadAt = existingBook?.lastReadAt,
                dateAdded = existingBook?.dateAdded ?: java.time.Instant.now().toString(),
                readingStatus = existingBook?.readingStatus ?: ReadingStatus.UNREAD,
                tags = normalizedTags ?: existingBook?.tags ?: emptyList(),
            )

            bookDao.upsertBook(importedBook.toEntity())

            if (existingBook != null && existingBook.filePath != importedBook.filePath) {
                cleanupManagedFiles(existingBook)
            }
            val original = runCatching { File(filePath).canonicalFile }.getOrNull()
            if (original != null && original.path != destFile.canonicalPath) {
                deleteManagedPath(original.path, managedBookRoots())
            }
            importedBook
        } catch (error: Throwable) {
            runCatching { destFile.delete() }
            throw error
        }
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
        bookDao.updateHighlightNote(highlightId, note?.trim()?.takeIf { it.isNotBlank() })
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

    private fun cleanupManagedFiles(book: Book) {
        deleteManagedPath(book.filePath, managedBookRoots())
        deleteManagedPath(book.coverUri, managedCoverRoots())
    }

    private fun uniqueImportFileName(fileName: String): String =
        "${System.currentTimeMillis()}_${UUID.randomUUID()}_${sanitizeFileName(fileName)}"

    private fun sanitizeFileName(fileName: String): String {
        val cleaned = fileName.replace(Regex("""[<>:"/\\|?*]"""), "_").trim()
        val withFallback = cleaned.ifBlank { "book.epub" }.take(MAX_SAFE_FILE_NAME_LENGTH)
        return if (withFallback.endsWith(".epub", ignoreCase = true)) withFallback else "$withFallback.epub"
    }

    private fun copyGeneratedEpubToInternalBookFile(sourceFile: File, fileName: String): File {
        val source = sourceFile.canonicalFile
        if (!source.exists() || !source.isFile) error("Generated EPUB file was not found.")
        if (source.length() <= 0L) error("Generated EPUB is empty.")
        if (source.length() > MAX_IMPORT_BYTES) {
            error("This EPUB is larger than the ${MAX_IMPORT_BYTES / 1024 / 1024} MB safety limit.")
        }
        val booksDir = MiyoStorage.booksDir(appContext)
        val destFile = MiyoStorage.safeChild(booksDir, fileName)
        val tempFile = MiyoStorage.safeChild(booksDir, "$fileName.${UUID.randomUUID()}.tmp")
        try {
            source.inputStream().use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output, COPY_BUFFER_SIZE)
                }
            }
        } catch (error: Throwable) {
            tempFile.delete()
            throw error
        }
        if (tempFile.length() <= 0L) {
            tempFile.delete()
            error("Generated EPUB is empty.")
        }
        commitTempFile(tempFile, destFile)
        return destFile
    }

    private fun persistCoverImage(bookId: String, dataUri: String): String? {
        val match = Regex(
            pattern = "^data:([^;,]+);base64,(.*)$",
            options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).find(dataUri.trim())
        val mime = match?.groupValues?.getOrNull(1).orEmpty()
        val base64Payload = match?.groupValues?.getOrNull(2)?.replace(Regex("\\s"), "").orEmpty()
        if (base64Payload.isBlank()) return null
        if (!mime.startsWith("image/", ignoreCase = true)) return null
        if (mime.contains("svg", ignoreCase = true)) return null
        if (base64Payload.length > MAX_COVER_BASE64_CHARS) return null

        val extension = when {
            mime.contains("png", ignoreCase = true) -> "png"
            mime.contains("webp", ignoreCase = true) -> "webp"
            mime.contains("gif", ignoreCase = true) -> "gif"
            else -> "jpg"
        }

        return runCatching {
            val coverFile = MiyoStorage.safeChild(MiyoStorage.coversDir(appContext), "$bookId.$extension")
            coverFile.writeBytes(Base64.decode(base64Payload, Base64.DEFAULT))
            Uri.fromFile(coverFile).toString()
        }.getOrNull()
    }

    private fun commitTempFile(tempFile: File, destFile: File) {
        if (destFile.exists()) destFile.delete()
        if (!tempFile.renameTo(destFile)) {
            try {
                tempFile.copyTo(destFile, overwrite = true)
            } catch (copyError: Throwable) {
                destFile.delete()
                throw copyError
            } finally {
                tempFile.delete()
            }
        }
    }

    private fun deleteManagedPath(rawPath: String?, allowedRoots: List<File>) {
        val file = rawPath?.toLocalFile() ?: return
        val canonicalFile = runCatching { file.canonicalFile }.getOrNull() ?: return
        val canonicalRoots = allowedRoots.mapNotNull { runCatching { it.canonicalFile }.getOrNull() }
        val managed = canonicalRoots.any { root ->
            canonicalFile == root || canonicalFile.path.startsWith(root.path + File.separator)
        }
        if (!managed) return
        if (!canonicalFile.exists()) return
        if (canonicalFile.isDirectory) {
            canonicalFile.deleteRecursively()
        } else {
            canonicalFile.delete()
        }
    }

    private fun managedBookRoots(): List<File> = listOf(
        MiyoStorage.booksDir(appContext),
        MiyoStorage.onlineEpubDir(appContext),
        MiyoStorage.legacyBooksDir(appContext),
    )

    private fun managedCoverRoots(): List<File> = listOf(
        MiyoStorage.coversDir(appContext),
        MiyoStorage.legacyCoversDir(appContext),
    )

    private fun String.toLocalFile(): File? {
        val uri = runCatching { Uri.parse(this) }.getOrNull()
        return when {
            uri == null -> File(this)
            uri.scheme.isNullOrBlank() -> File(this)
            uri.scheme.equals("file", ignoreCase = true) -> uri.path?.let(::File)
            else -> null
        }
    }

    private companion object {
        const val COPY_BUFFER_SIZE = 64 * 1024
        const val MAX_IMPORT_BYTES = 64L * 1024L * 1024L
        const val MAX_SAFE_FILE_NAME_LENGTH = 120
        const val MAX_COVER_BASE64_CHARS = 10_000_000
    }
}
