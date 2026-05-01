package com.miyu.reader.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyu.reader.data.repository.BookRepository
import com.miyu.reader.domain.model.*
import com.miyu.reader.engine.bridge.EpubEngineBridge
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.UUID
import javax.inject.Inject

enum class ImportFeedbackType { SUCCESS, ERROR, WARNING }

data class ImportFeedback(
    val type: ImportFeedbackType,
    val title: String,
    val message: String,
    val bookId: String? = null,
)

data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val searchQuery: String = "",
    val viewMode: ViewMode = ViewMode.GRID,
    val sortOption: SortOption = SortOption.RECENT,
    val filterOption: FilterOption = FilterOption.ALL,
    val isImporting: Boolean = false,
    val importFeedback: ImportFeedback? = null,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val epubEngine: EpubEngineBridge,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val allBooks: StateFlow<List<Book>> = bookRepository.getAllBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            allBooks.collect { books ->
                _uiState.update { it.copy(books = books) }
            }
        }
    }

    val displayedBooks: StateFlow<List<Book>> = _uiState.map { state ->
        var list = state.books

        // Filter
        list = when (state.filterOption) {
            FilterOption.ALL -> list
            FilterOption.UNREAD -> list.filter { it.readingStatus == ReadingStatus.UNREAD }
            FilterOption.READING -> list.filter { it.readingStatus == ReadingStatus.READING }
            FilterOption.FINISHED -> list.filter { it.readingStatus == ReadingStatus.FINISHED }
        }

        // Search
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) || it.author.lowercase().contains(q)
            }
        }

        // Sort
        list = when (state.sortOption) {
            SortOption.RECENT -> list.sortedWith(
                compareBy<Book> { book ->
                    when {
                        book.readingStatus == ReadingStatus.READING || book.progress > 0f && book.progress < 100f -> 0
                        book.lastReadAt == null -> 1
                        else -> 2
                    }
                }
                    .thenByDescending { it.dateAdded }
                    .thenByDescending { it.lastReadAt ?: "" },
            )
            SortOption.TITLE -> list.sortedBy { it.title.lowercase() }
            SortOption.AUTHOR -> list.sortedBy { it.author.lowercase() }
            SortOption.PROGRESS -> list.sortedByDescending { it.progress }
            SortOption.DATE_ADDED -> list.sortedByDescending { it.dateAdded }
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleViewMode() {
        _uiState.update {
            it.copy(viewMode = if (it.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID)
        }
    }

    fun setSortOption(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }

    fun setFilterOption(option: FilterOption) {
        _uiState.update { it.copy(filterOption = option) }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            bookRepository.deleteBook(bookId)
        }
    }

    fun updateBookStatus(bookId: String, status: ReadingStatus) {
        viewModelScope.launch {
            val book = bookRepository.getBook(bookId) ?: return@launch
            bookRepository.saveBook(book.copy(readingStatus = status))
        }
    }

    fun updateBookCover(bookId: String, coverUri: Uri) {
        viewModelScope.launch {
            val book = bookRepository.getBook(bookId) ?: return@launch
            bookRepository.saveBook(book.copy(coverUri = coverUri.toString()))
        }
    }

    fun clearImportFeedback() {
        _uiState.update { it.copy(importFeedback = null) }
    }

    fun importBookFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importFeedback = null) }
            try {
                val originalName = resolveDisplayName(uri) ?: "book_${System.currentTimeMillis()}.epub"
                validateImportUri(uri, originalName)

                val fileName = uniqueImportFileName(originalName)
                val destFile = withContext(Dispatchers.IO) {
                    copyUriToInternalBookFile(uri, fileName)
                }

                val parsed = withContext(Dispatchers.IO) {
                    JSONObject(epubEngine.parseEpub(destFile.absolutePath))
                }
                val metadata = parsed.optJSONObject("metadata")
                val totalChapters = parsed.optInt("totalChapters", 0)
                if (totalChapters <= 0) error("The EPUB imported, but no readable chapters were found.")

                val title = metadata?.optString("title")
                    ?.takeIf { it.isNotBlank() }
                    ?: destFile.nameWithoutExtension.replace("_", " ")
                val author = metadata?.optString("author")
                    ?.takeIf { it.isNotBlank() }
                    ?: "Unknown"
                val bookId = UUID.randomUUID().toString()
                val coverUri = withContext(Dispatchers.IO) {
                    epubEngine.extractCoverImage(destFile.absolutePath)
                        ?.takeIf { it.isNotBlank() }
                        ?.let { if (it.startsWith("data:", ignoreCase = true)) it else "data:image/jpeg;base64,$it" }
                        ?.let { persistCoverImage(bookId, it) ?: it }
                }

                val book = Book(
                    id = bookId,
                    title = title,
                    author = author,
                    coverUri = coverUri,
                    filePath = destFile.absolutePath,
                    fileName = fileName,
                    epubIdentifier = metadata?.optString("identifier")?.takeIf { it.isNotBlank() },
                    language = metadata?.optString("language")?.takeIf { it.isNotBlank() },
                    totalChapters = totalChapters,
                    dateAdded = java.time.Instant.now().toString(),
                )

                bookRepository.importBook(book)
                _uiState.update {
                    it.copy(
                        importFeedback = ImportFeedback(
                            type = ImportFeedbackType.SUCCESS,
                            title = "Import complete",
                            message = "\"${book.title}\" is ready with $totalChapters chapter${if (totalChapters == 1) "" else "s"}.",
                            bookId = book.id,
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        importFeedback = ImportFeedback(
                            type = ImportFeedbackType.ERROR,
                            title = "Import failed",
                            message = e.message ?: "The selected EPUB could not be imported.",
                        )
                    )
                }
            } finally {
                _uiState.update { it.copy(isImporting = false) }
            }
        }
    }

    fun importBookFromRemoteEpub(url: String, suggestedTitle: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importFeedback = null) }
            try {
                val normalizedUrl = validateRemoteEpubUrl(url)
                val fileName = uniqueImportFileName("${suggestedTitle.ifBlank { "opds-book" }}.epub")
                val destFile = withContext(Dispatchers.IO) {
                    downloadRemoteEpub(normalizedUrl, fileName)
                }

                val parsed = withContext(Dispatchers.IO) {
                    JSONObject(epubEngine.parseEpub(destFile.absolutePath))
                }
                val metadata = parsed.optJSONObject("metadata")
                val totalChapters = parsed.optInt("totalChapters", 0)
                if (totalChapters <= 0) error("The downloaded EPUB did not contain readable chapters.")

                val title = metadata?.optString("title")
                    ?.takeIf { it.isNotBlank() }
                    ?: suggestedTitle.ifBlank { destFile.nameWithoutExtension.replace("_", " ") }
                val author = metadata?.optString("author")
                    ?.takeIf { it.isNotBlank() }
                    ?: "Unknown"
                val bookId = UUID.randomUUID().toString()
                val coverUri = withContext(Dispatchers.IO) {
                    epubEngine.extractCoverImage(destFile.absolutePath)
                        ?.takeIf { it.isNotBlank() }
                        ?.let { if (it.startsWith("data:", ignoreCase = true)) it else "data:image/jpeg;base64,$it" }
                        ?.let { persistCoverImage(bookId, it) ?: it }
                }
                val book = Book(
                    id = bookId,
                    title = title,
                    author = author,
                    coverUri = coverUri,
                    filePath = destFile.absolutePath,
                    fileName = fileName,
                    epubIdentifier = metadata?.optString("identifier")?.takeIf { it.isNotBlank() },
                    language = metadata?.optString("language")?.takeIf { it.isNotBlank() },
                    totalChapters = totalChapters,
                    dateAdded = java.time.Instant.now().toString(),
                )
                bookRepository.importBook(book)
                _uiState.update {
                    it.copy(
                        importFeedback = ImportFeedback(
                            type = ImportFeedbackType.SUCCESS,
                            title = "OPDS import complete",
                            message = "\"${book.title}\" downloaded with $totalChapters chapter${if (totalChapters == 1) "" else "s"}.",
                            bookId = book.id,
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        importFeedback = ImportFeedback(
                            type = ImportFeedbackType.ERROR,
                            title = "OPDS import failed",
                            message = e.message ?: "The selected OPDS book could not be downloaded.",
                        )
                    )
                }
            } finally {
                _uiState.update { it.copy(isImporting = false) }
            }
        }
    }

    fun importGeneratedOnlineNovelEpub(filePath: String, fileName: String, suggestedTitle: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importFeedback = null) }
            try {
                val destFile = withContext(Dispatchers.IO) {
                    copyGeneratedEpubToInternalBookFile(File(filePath), uniqueImportFileName(fileName))
                }

                val parsed = withContext(Dispatchers.IO) {
                    JSONObject(epubEngine.parseEpub(destFile.absolutePath))
                }
                val metadata = parsed.optJSONObject("metadata")
                val totalChapters = parsed.optInt("totalChapters", 0)
                if (totalChapters <= 0) error("The generated EPUB did not contain readable chapters.")

                val title = metadata?.optString("title")
                    ?.takeIf { it.isNotBlank() }
                    ?: suggestedTitle.ifBlank { destFile.nameWithoutExtension.replace("_", " ") }
                val author = metadata?.optString("author")
                    ?.takeIf { it.isNotBlank() }
                    ?: "Unknown"
                val bookId = UUID.randomUUID().toString()
                val coverUri = withContext(Dispatchers.IO) {
                    epubEngine.extractCoverImage(destFile.absolutePath)
                        ?.takeIf { it.isNotBlank() }
                        ?.let { if (it.startsWith("data:", ignoreCase = true)) it else "data:image/jpeg;base64,$it" }
                        ?.let { persistCoverImage(bookId, it) ?: it }
                }
                val book = Book(
                    id = bookId,
                    title = title,
                    author = author,
                    coverUri = coverUri,
                    filePath = destFile.absolutePath,
                    fileName = destFile.name,
                    epubIdentifier = metadata?.optString("identifier")?.takeIf { it.isNotBlank() },
                    language = metadata?.optString("language")?.takeIf { it.isNotBlank() },
                    totalChapters = totalChapters,
                    dateAdded = java.time.Instant.now().toString(),
                )
                bookRepository.importBook(book)
                _uiState.update {
                    it.copy(
                        importFeedback = ImportFeedback(
                            type = ImportFeedbackType.SUCCESS,
                            title = "Online novel imported",
                            message = "\"${book.title}\" is ready with $totalChapters chapter${if (totalChapters == 1) "" else "s"}.",
                            bookId = book.id,
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        importFeedback = ImportFeedback(
                            type = ImportFeedbackType.ERROR,
                            title = "Online import failed",
                            message = e.message ?: "The generated EPUB could not be imported.",
                        )
                    )
                }
            } finally {
                _uiState.update { it.copy(isImporting = false) }
            }
        }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
    }

    private fun sanitizeFileName(fileName: String): String {
        val cleaned = fileName.replace(Regex("""[<>:"/\\|?*]"""), "_").trim()
        val withFallback = cleaned.ifBlank { "book.epub" }.take(MAX_SAFE_FILE_NAME_LENGTH)
        return if (withFallback.endsWith(".epub", ignoreCase = true)) withFallback else "$withFallback.epub"
    }

    private fun uniqueImportFileName(fileName: String): String =
        "${System.currentTimeMillis()}_${UUID.randomUUID()}_${sanitizeFileName(fileName)}"

    private fun validateImportUri(uri: Uri, originalName: String) {
        val scheme = uri.scheme?.lowercase().orEmpty()
        if (scheme !in setOf("content", "file")) {
            error("Unsupported import source.")
        }

        val mimeType = appContext.contentResolver.getType(uri)?.lowercase()
        val allowedMime = mimeType == null ||
            mimeType in setOf(
                "application/epub+zip",
                "application/zip",
                "application/octet-stream",
                "application/x-zip-compressed",
            )
        if (!allowedMime || !originalName.endsWith(".epub", ignoreCase = true)) {
            error("Only EPUB files can be imported.")
        }
    }

    private fun copyUriToInternalBookFile(uri: Uri, fileName: String): File {
        val booksDir = File(appContext.filesDir, "books").canonicalFile.apply { mkdirs() }
        val destFile = File(booksDir, fileName).canonicalFile
        if (!destFile.path.startsWith(booksDir.path + File.separator)) {
            error("Invalid import destination.")
        }

        var copied = 0L
        val buffer = ByteArray(COPY_BUFFER_SIZE)
        val inputStream = appContext.contentResolver.openInputStream(uri)
            ?: error("Could not open the selected EPUB.")
        try {
            inputStream.use { input ->
                destFile.outputStream().use { output ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        copied += read
                        if (copied > MAX_IMPORT_BYTES) {
                            output.flush()
                            destFile.delete()
                            error("This EPUB is larger than the ${MAX_IMPORT_BYTES / 1024 / 1024} MB safety limit.")
                        }
                        output.write(buffer, 0, read)
                    }
                }
            }
        } catch (error: Throwable) {
            destFile.delete()
            throw error
        }
        if (copied == 0L) {
            destFile.delete()
            error("The selected EPUB is empty.")
        }
        return destFile
    }

    private fun copyGeneratedEpubToInternalBookFile(sourceFile: File, fileName: String): File {
        val source = sourceFile.canonicalFile
        if (!source.exists() || !source.isFile) error("Generated EPUB file was not found.")
        if (source.length() <= 0L) error("Generated EPUB is empty.")
        if (source.length() > MAX_IMPORT_BYTES) {
            error("This EPUB is larger than the ${MAX_IMPORT_BYTES / 1024 / 1024} MB safety limit.")
        }
        val booksDir = File(appContext.filesDir, "books").canonicalFile.apply { mkdirs() }
        val destFile = File(booksDir, fileName).canonicalFile
        if (!destFile.path.startsWith(booksDir.path + File.separator)) {
            error("Invalid import destination.")
        }
        source.inputStream().use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output, COPY_BUFFER_SIZE)
            }
        }
        if (destFile.length() <= 0L) {
            destFile.delete()
            error("Generated EPUB is empty.")
        }
        return destFile
    }

    private fun validateRemoteEpubUrl(url: String): String {
        val uri = URI(url.trim())
        if (!uri.scheme.equals("https", ignoreCase = true)) {
            error("Remote EPUB downloads must use HTTPS.")
        }
        val host = uri.host?.lowercase().orEmpty()
        if (host.isBlank() || host == "localhost" || host.endsWith(".local") || isPrivateIpv4(host) || isPrivateIpv6(host)) {
            error("Private-network EPUB downloads are blocked.")
        }
        return uri.toURL().toString()
    }

    private fun downloadRemoteEpub(url: String, fileName: String): File {
        val booksDir = File(appContext.filesDir, "books").canonicalFile.apply { mkdirs() }
        val destFile = File(booksDir, fileName).canonicalFile
        if (!destFile.path.startsWith(booksDir.path + File.separator)) {
            error("Invalid import destination.")
        }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/epub+zip,application/octet-stream,*/*;q=0.8")
            setRequestProperty("User-Agent", "MIYO-Kotlin/1.0")
        }
        try {
            if (connection.responseCode !in 200..299) {
                error("Download failed (${connection.responseCode}).")
            }
            val contentLength = connection.contentLengthLong.takeIf { it > 0 }
            if (contentLength != null && contentLength > MAX_IMPORT_BYTES) {
                error("This EPUB is larger than the ${MAX_IMPORT_BYTES / 1024 / 1024} MB safety limit.")
            }

            var copied = 0L
            val buffer = ByteArray(COPY_BUFFER_SIZE)
            try {
                connection.inputStream.use { input ->
                    destFile.outputStream().use { output ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            copied += read
                            if (copied > MAX_IMPORT_BYTES) {
                                output.flush()
                                destFile.delete()
                                error("This EPUB is larger than the ${MAX_IMPORT_BYTES / 1024 / 1024} MB safety limit.")
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                }
            } catch (error: Throwable) {
                destFile.delete()
                throw error
            }
            if (copied == 0L) {
                destFile.delete()
                error("The downloaded EPUB is empty.")
            }
            return destFile
        } finally {
            connection.disconnect()
        }
    }

    private fun isPrivateIpv4(host: String): Boolean =
        host.startsWith("10.") ||
            host.startsWith("127.") ||
            host.startsWith("192.168.") ||
            host.startsWith("169.254.") ||
            Regex("^172\\.(1[6-9]|2\\d|3[0-1])\\.").containsMatchIn(host)

    private fun isPrivateIpv6(host: String): Boolean =
        host == "::1" ||
            host == "[::1]" ||
            host.startsWith("fc") ||
            host.startsWith("fd") ||
            host.startsWith("fe80:")

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
            val coverDir = File(appContext.filesDir, "covers").apply { mkdirs() }
            val coverFile = File(coverDir, "$bookId.$extension")
            coverFile.writeBytes(Base64.decode(base64Payload, Base64.DEFAULT))
            Uri.fromFile(coverFile).toString()
        }.getOrNull()
    }

    private companion object {
        const val MAX_IMPORT_BYTES = 250L * 1024L * 1024L
        const val MAX_COVER_BASE64_CHARS = 14 * 1024 * 1024
        const val MAX_SAFE_FILE_NAME_LENGTH = 96
        const val COPY_BUFFER_SIZE = 8 * 1024
    }
}
