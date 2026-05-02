package com.miyu.reader.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miyu.reader.data.preferences.UserPreferences
import com.miyu.reader.data.repository.BookRepository
import com.miyu.reader.domain.model.*
import com.miyu.reader.engine.bridge.EpubEngineBridge
import com.miyu.reader.storage.MiyoStorage
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

data class LibraryCategory(
    val id: String,
    val name: String,
    val count: Int,
    val isSystem: Boolean = false,
    val isMutable: Boolean = false,
    val isHidden: Boolean = false,
)

data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val categories: List<LibraryCategory> = emptyList(),
    val selectedCategoryId: String = LibraryCategoryIds.ALL,
    val selectedBookIds: Set<String> = emptySet(),
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
    private val userPreferences: UserPreferences,
    private val epubEngine: EpubEngineBridge,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val allBooks: StateFlow<List<Book>> = bookRepository.getAllBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            combine(allBooks, userPreferences.libraryCategories, userPreferences.hiddenLibraryCategories) { books, storedCategories, hiddenCategories ->
                books to buildLibraryCategories(books, storedCategories, hiddenCategories)
            }.collect { (books, categories) ->
                _uiState.update { current ->
                    val existingIds = books.mapTo(mutableSetOf()) { it.id }
                    val selectedCategory = current.selectedCategoryId
                        .takeIf { id -> categories.any { it.id == id && !it.isHidden } }
                        ?: LibraryCategoryIds.ALL
                    current.copy(
                        books = books,
                        categories = categories,
                        selectedCategoryId = selectedCategory,
                        selectedBookIds = current.selectedBookIds.intersect(existingIds),
                    )
                }
            }
        }
    }

    val displayedBooks: StateFlow<List<Book>> = _uiState.map { state ->
        var list = state.books

        // Category filtering and reading-status filtering stay independent so a
        // user can inspect a status slice inside any custom category.
        list = when (state.selectedCategoryId) {
            LibraryCategoryIds.ALL -> list
            else -> state.selectedCategoryId.customCategoryName()?.let { name ->
                list.filter { book -> book.libraryCategoryNames().any { it.equals(name, ignoreCase = true) } }
            } ?: list
        }

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

    fun setSelectedCategory(categoryId: String) {
        _uiState.update { current ->
            if (current.categories.any { it.id == categoryId }) {
                current.copy(selectedCategoryId = categoryId, selectedBookIds = emptySet())
            } else {
                current
            }
        }
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

    fun toggleBookSelection(bookId: String) {
        _uiState.update { current ->
            val next = if (bookId in current.selectedBookIds) {
                current.selectedBookIds - bookId
            } else {
                current.selectedBookIds + bookId
            }
            current.copy(selectedBookIds = next)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedBookIds = emptySet()) }
    }

    fun createCategory(name: String) {
        val cleanName = sanitizeCategoryName(name) ?: return
        viewModelScope.launch {
            val existing = userPreferences.libraryCategories.first()
            if (existing.none { it.equals(cleanName, ignoreCase = true) }) {
                userPreferences.setLibraryCategories(existing + cleanName)
            }
            _uiState.update { it.copy(selectedCategoryId = LibraryCategoryIds.custom(cleanName)) }
        }
    }

    fun renameCategory(categoryId: String, newName: String) {
        val oldName = categoryId.customCategoryName() ?: return
        val cleanName = sanitizeCategoryName(newName) ?: return
        if (oldName.equals(cleanName, ignoreCase = true)) return
        viewModelScope.launch {
            val existing = userPreferences.libraryCategories.first()
            val hidden = userPreferences.hiddenLibraryCategories.first()
            if (existing.any { it.equals(cleanName, ignoreCase = true) }) return@launch
            userPreferences.setLibraryCategories(existing
                .filterNot { it.equals(oldName, ignoreCase = true) }
                .toSet() + cleanName)
            userPreferences.setHiddenLibraryCategories(
                hidden
                    .filterNot { it.equals(oldName, ignoreCase = true) }
                    .toSet()
                    .let { names -> if (hidden.any { it.equals(oldName, ignoreCase = true) }) names + cleanName else names },
            )
            bookRepository.getAllBooksOnce().forEach { book ->
                if (book.libraryCategoryNames().any { it.equals(oldName, ignoreCase = true) }) {
                    bookRepository.updateBookTags(
                        book.id,
                        book.tags
                            .removeLibraryCategory(oldName)
                            .addLibraryCategory(cleanName),
                    )
                }
            }
            _uiState.update {
                it.copy(
                    selectedCategoryId = LibraryCategoryIds.custom(cleanName),
                    selectedBookIds = emptySet(),
                )
            }
        }
    }

    fun deleteCategory(categoryId: String) {
        val name = categoryId.customCategoryName() ?: return
        viewModelScope.launch {
            val existing = userPreferences.libraryCategories.first()
            val hidden = userPreferences.hiddenLibraryCategories.first()
            userPreferences.setLibraryCategories(existing.filterNot { it.equals(name, ignoreCase = true) }.toSet())
            userPreferences.setHiddenLibraryCategories(hidden.filterNot { it.equals(name, ignoreCase = true) }.toSet())
            bookRepository.getAllBooksOnce().forEach { book ->
                if (book.libraryCategoryNames().any { it.equals(name, ignoreCase = true) }) {
                    bookRepository.updateBookTags(book.id, book.tags.removeLibraryCategory(name))
                }
            }
            _uiState.update {
                it.copy(
                    selectedCategoryId = LibraryCategoryIds.ALL,
                    selectedBookIds = emptySet(),
                )
            }
        }
    }

    fun assignSelectionToCategory(categoryId: String) {
        val selectedIds = _uiState.value.selectedBookIds
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            val categoryName = categoryId.customCategoryName()
            if (categoryId != LibraryCategoryIds.ALL && categoryName == null) return@launch
            if (categoryName != null) {
                val existing = userPreferences.libraryCategories.first()
                if (existing.none { it.equals(categoryName, ignoreCase = true) }) {
                    userPreferences.setLibraryCategories(existing + categoryName)
                }
            }
            bookRepository.getAllBooksOnce()
                .filter { it.id in selectedIds }
                .forEach { book ->
                    val nextTags = if (categoryId == LibraryCategoryIds.ALL) {
                        book.tags.removeAllLibraryCategories()
                    } else {
                        book.tags
                            .removeAllLibraryCategories()
                            .addLibraryCategory(categoryName.orEmpty())
                    }
                    bookRepository.updateBookTags(book.id, nextTags)
                }
            _uiState.update {
                it.copy(
                    selectedBookIds = emptySet(),
                    selectedCategoryId = if (categoryName != null) LibraryCategoryIds.custom(categoryName) else LibraryCategoryIds.ALL,
                )
            }
        }
    }

    fun assignBookToCategory(bookId: String, categoryId: String) {
        viewModelScope.launch {
            val categoryName = categoryId.customCategoryName()
            if (categoryId != LibraryCategoryIds.ALL && categoryName == null) return@launch
            if (categoryName != null) {
                val existing = userPreferences.libraryCategories.first()
                if (existing.none { it.equals(categoryName, ignoreCase = true) }) {
                    userPreferences.setLibraryCategories(existing + categoryName)
                }
            }
            val book = bookRepository.getBook(bookId) ?: return@launch
            val nextTags = if (categoryId == LibraryCategoryIds.ALL) {
                book.tags.removeAllLibraryCategories()
            } else {
                book.tags
                    .removeAllLibraryCategories()
                    .addLibraryCategory(categoryName.orEmpty())
            }
            bookRepository.updateBookTags(book.id, nextTags)
        }
    }

    fun deleteSelectedBooks() {
        val selectedIds = _uiState.value.selectedBookIds
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            selectedIds.forEach { bookRepository.deleteBook(it) }
            _uiState.update { it.copy(selectedBookIds = emptySet()) }
        }
    }

    fun setCategoryHidden(categoryId: String, hidden: Boolean) {
        val name = categoryId.customCategoryName() ?: return
        viewModelScope.launch {
            val existing = userPreferences.hiddenLibraryCategories.first().toMutableSet()
            if (hidden) existing.add(name) else existing.removeAll { it.equals(name, ignoreCase = true) }
            userPreferences.setHiddenLibraryCategories(existing)
            if (hidden && _uiState.value.selectedCategoryId == categoryId) {
                _uiState.update { it.copy(selectedCategoryId = LibraryCategoryIds.ALL, selectedBookIds = emptySet()) }
            }
        }
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
                val fileName = uniqueImportFileName("${suggestedTitle.ifBlank { "remote-book" }}.epub")
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
                            title = "Remote EPUB import complete",
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
                            title = "Remote EPUB import failed",
                            message = e.message ?: "The selected remote EPUB could not be downloaded.",
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
        val booksDir = MiyoStorage.booksDir(appContext)
        val destFile = MiyoStorage.safeChild(booksDir, fileName)
        val tempFile = MiyoStorage.safeChild(booksDir, "$fileName.${UUID.randomUUID()}.tmp")

        var copied = 0L
        val buffer = ByteArray(COPY_BUFFER_SIZE)
        val inputStream = appContext.contentResolver.openInputStream(uri)
            ?: error("Could not open the selected EPUB.")
        try {
            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        copied += read
                        if (copied > MAX_IMPORT_BYTES) {
                            output.flush()
                            error("This EPUB is larger than the ${MAX_IMPORT_BYTES / 1024 / 1024} MB safety limit.")
                        }
                        output.write(buffer, 0, read)
                    }
                }
            }
        } catch (error: Throwable) {
            tempFile.delete()
            throw error
        }
        if (copied == 0L) {
            tempFile.delete()
            error("The selected EPUB is empty.")
        }
        commitTempFile(tempFile, destFile)
        return destFile
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
        val booksDir = MiyoStorage.booksDir(appContext)
        val destFile = MiyoStorage.safeChild(booksDir, fileName)
        val tempFile = MiyoStorage.safeChild(booksDir, "$fileName.${UUID.randomUUID()}.tmp")
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
                    tempFile.outputStream().use { output ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            copied += read
                            if (copied > MAX_IMPORT_BYTES) {
                                output.flush()
                                error("This EPUB is larger than the ${MAX_IMPORT_BYTES / 1024 / 1024} MB safety limit.")
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                }
            } catch (error: Throwable) {
                tempFile.delete()
                throw error
            }
            if (copied == 0L) {
                tempFile.delete()
                error("The downloaded EPUB is empty.")
            }
            commitTempFile(tempFile, destFile)
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

    private companion object {
        const val MAX_IMPORT_BYTES = 250L * 1024L * 1024L
        const val MAX_COVER_BASE64_CHARS = 14 * 1024 * 1024
        const val MAX_SAFE_FILE_NAME_LENGTH = 96
        const val COPY_BUFFER_SIZE = 8 * 1024
    }
}

object LibraryCategoryIds {
    const val ALL = "system:all"
    private const val CUSTOM_PREFIX = "custom:"

    fun custom(name: String): String = "$CUSTOM_PREFIX${name.trim()}"

    fun customName(id: String): String? =
        id.takeIf { it.startsWith(CUSTOM_PREFIX) }
            ?.removePrefix(CUSTOM_PREFIX)
            ?.trim()
            ?.takeIf(String::isNotBlank)
}

private const val LIBRARY_CATEGORY_TAG_PREFIX = "category:"

private fun buildLibraryCategories(
    books: List<Book>,
    storedCategoryNames: Set<String>,
    hiddenCategoryNames: Set<String>,
): List<LibraryCategory> {
    val discoveredNames = books.flatMap { it.libraryCategoryNames() }
    val customNames = (storedCategoryNames + discoveredNames)
        .mapNotNull(::sanitizeCategoryName)
        .distinctBy { it.lowercase() }
        .sortedBy { it.lowercase() }
    val hiddenNames = hiddenCategoryNames
        .mapNotNull(::sanitizeCategoryName)
        .toSet()

    return buildList {
        add(
            LibraryCategory(
                id = LibraryCategoryIds.ALL,
                name = "All",
                count = books.size,
                isSystem = true,
            ),
        )
        customNames.forEach { name ->
            add(
                LibraryCategory(
                    id = LibraryCategoryIds.custom(name),
                    name = name,
                    count = books.count { book ->
                        book.libraryCategoryNames().any { it.equals(name, ignoreCase = true) }
                    },
                    isMutable = true,
                    isHidden = hiddenNames.any { it.equals(name, ignoreCase = true) },
                ),
            )
        }
    }
}

private fun String.customCategoryName(): String? = LibraryCategoryIds.customName(this)

private fun sanitizeCategoryName(name: String): String? =
    name.trim()
        .replace(Regex("\\s+"), " ")
        .takeIf { it.length in 1..48 }

private fun Book.libraryCategoryNames(): List<String> =
    tags.mapNotNull { tag ->
        tag.takeIf { it.startsWith(LIBRARY_CATEGORY_TAG_PREFIX, ignoreCase = true) }
            ?.drop(LIBRARY_CATEGORY_TAG_PREFIX.length)
            ?.let(::sanitizeCategoryName)
    }.distinctBy { it.lowercase() }

private fun List<String>.addLibraryCategory(name: String): List<String> {
    val cleanName = sanitizeCategoryName(name) ?: return this
    return (removeLibraryCategory(cleanName) + "$LIBRARY_CATEGORY_TAG_PREFIX$cleanName")
        .distinctBy { it.lowercase() }
}

private fun List<String>.removeLibraryCategory(name: String): List<String> =
    filterNot { tag ->
        tag.startsWith(LIBRARY_CATEGORY_TAG_PREFIX, ignoreCase = true) &&
            tag.drop(LIBRARY_CATEGORY_TAG_PREFIX.length).trim().equals(name.trim(), ignoreCase = true)
    }

private fun List<String>.removeAllLibraryCategories(): List<String> =
    filterNot { it.startsWith(LIBRARY_CATEGORY_TAG_PREFIX, ignoreCase = true) }
