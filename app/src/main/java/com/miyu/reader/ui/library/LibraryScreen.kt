package com.miyu.reader.ui.library

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miyu.reader.domain.model.Book
import com.miyu.reader.domain.model.FilterOption
import com.miyu.reader.domain.model.ReadingStatus
import com.miyu.reader.domain.model.SortOption
import com.miyu.reader.domain.model.ViewMode
import com.miyu.reader.ui.components.BookCover
import com.miyu.reader.ui.theme.LocalMIYUColors
import com.miyu.reader.viewmodel.ImportFeedbackType
import com.miyu.reader.viewmodel.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onOpenBook: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val displayedBooks by viewModel.displayedBooks.collectAsStateWithLifecycle()
    val colors = LocalMIYUColors.current
    val context = LocalContext.current

    var showSortMenu by remember { mutableStateOf(false) }
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    var showBookAction by remember { mutableStateOf(false) }
    var coverTargetBookId by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let { viewModel.importBookFromUri(it) }
    }
    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        val bookId = coverTargetBookId
        if (uri != null && bookId != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            viewModel.updateBookCover(bookId, uri)
        }
        coverTargetBookId = null
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { filePickerLauncher.launch(arrayOf("application/epub+zip", "application/epub", "application/octet-stream")) },
                modifier = Modifier.navigationBarsPadding(),
                containerColor = colors.accent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(22.dp),
            ) {
                if (uiState.isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(Icons.Filled.Add, contentDescription = "Import")
                }
                Spacer(Modifier.width(8.dp))
                Text("Import", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = colors.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .blur(if (showBookAction) 2.dp else 0.dp)
                .background(colors.background)
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            LibraryTopBar(
                count = uiState.books.size,
                viewMode = uiState.viewMode,
                showSortMenu = showSortMenu,
                currentSort = uiState.sortOption,
                onToggleSort = { showSortMenu = true },
                onDismissSort = { showSortMenu = false },
                onSortSelected = {
                    viewModel.setSortOption(it)
                    showSortMenu = false
                },
                onToggleView = viewModel::toggleViewMode,
            )

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text("Search books...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        Icon(
                            Icons.Filled.Clear,
                            contentDescription = "Clear search",
                            modifier = Modifier.clickable { viewModel.setSearchQuery("") },
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                singleLine = true,
            )

            Spacer(Modifier.height(12.dp))

            FilterRow(
                selected = uiState.filterOption,
                onSelect = viewModel::setFilterOption,
            )

            Spacer(Modifier.height(12.dp))

            when {
                displayedBooks.isEmpty() -> EmptyLibraryState(
                    hasSearch = uiState.searchQuery.isNotBlank(),
                    modifier = Modifier.weight(1f),
                )

                uiState.viewMode == ViewMode.GRID -> LazyVerticalGrid(
                    modifier = Modifier.weight(1f),
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 4.dp, end = 4.dp, bottom = 104.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(displayedBooks, key = { it.id }) { book ->
                        GridBookCard(
                            book = book,
                            onPress = { onOpenBook(book.id) },
                            onLongPress = {
                                selectedBook = book
                                showBookAction = true
                            },
                            onMore = {
                                selectedBook = book
                                showBookAction = true
                            },
                        )
                    }
                }

                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(displayedBooks, key = { it.id }) { book ->
                        ListBookCard(
                            book = book,
                            onPress = { onOpenBook(book.id) },
                            onLongPress = {
                                selectedBook = book
                                showBookAction = true
                            },
                            onMore = {
                                selectedBook = book
                                showBookAction = true
                            },
                        )
                    }
                }
            }
        }

        if (showBookAction && selectedBook != null) {
            BookActionSheet(
                book = selectedBook!!,
                onDismiss = {
                    showBookAction = false
                    selectedBook = null
                },
                onStatusSelected = { status ->
                    selectedBook?.let { viewModel.updateBookStatus(it.id, status) }
                    showBookAction = false
                    selectedBook = null
                },
                onDelete = {
                    selectedBook?.let { viewModel.deleteBook(it.id) }
                    showBookAction = false
                    selectedBook = null
                },
                onChangeCover = {
                    coverTargetBookId = selectedBook?.id
                    showBookAction = false
                    selectedBook = null
                    coverPickerLauncher.launch(arrayOf("image/*"))
                },
            )
        }

        ImportFeedbackDialog(
            viewModel = viewModel,
            onOpenBook = onOpenBook,
        )
    }
}

@Composable
private fun LibraryTopBar(
    count: Int,
    viewMode: ViewMode,
    showSortMenu: Boolean,
    currentSort: SortOption,
    onToggleSort: () -> Unit,
    onDismissSort: () -> Unit,
    onSortSelected: (SortOption) -> Unit,
    onToggleView: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Library",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = colors.onBackground,
            )
            Spacer(Modifier.width(10.dp))
            Surface(
                shape = CircleShape,
                color = colors.cardBackground,
                shadowElevation = 1.dp,
            ) {
                Text(
                    count.toString(),
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                    color = colors.secondaryText,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box {
                FilledTonalIconButton(
                    onClick = onToggleSort,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Outlined.Tune, contentDescription = "Sort")
                }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = onDismissSort) {
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label()) },
                            onClick = { onSortSelected(option) },
                            leadingIcon = {
                                if (currentSort == option) Icon(Icons.Filled.Check, contentDescription = null)
                            },
                        )
                    }
                }
            }
            FilledTonalIconButton(
                onClick = onToggleView,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    if (viewMode == ViewMode.GRID) Icons.Outlined.ViewList else Icons.Outlined.GridView,
                    contentDescription = "Toggle view",
                )
            }
        }
    }
}

@Composable
private fun FilterRow(
    selected: FilterOption,
    onSelect: (FilterOption) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 4.dp),
    ) {
        items(FilterOption.entries) { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(option.label(), fontWeight = FontWeight.SemiBold) },
                shape = RoundedCornerShape(14.dp),
            )
        }
    }
}

@Composable
private fun EmptyLibraryState(
    hasSearch: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMIYUColors.current
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
            Surface(
                shape = CircleShape,
                color = colors.accent.copy(alpha = 0.12f),
                modifier = Modifier.size(88.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.MenuBook,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                if (hasSearch) "No books found" else "No books yet",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (hasSearch) "Clear search or change filters." else "Import an EPUB to start reading.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryText,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridBookCard(
    book: Book,
    onPress: () -> Unit,
    onLongPress: () -> Unit,
    onMore: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onPress, onLongClick = onLongPress),
    ) {
        BookCover(
            book = book,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .padding(4.dp),
            cornerRadius = 10,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .padding(4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.72f),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
        ) {
            Text(
                book.title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
            )
            Text(
                book.author.takeIf { it.isNotBlank() } ?: "Unknown",
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.White.copy(alpha = 0.78f),
            )
        }
        if (book.progress > 0f) {
            Surface(
                color = colors.accent,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
            ) {
                Text(
                    "${book.progress.toInt()}%",
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        BookStatusDot(book = book, modifier = Modifier.align(Alignment.TopStart).padding(10.dp))
        IconButton(
            onClick = onMore,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(32.dp),
        ) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Book options", tint = Color.White)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListBookCard(
    book: Book,
    onPress: () -> Unit,
    onLongPress: () -> Unit,
    onMore: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onPress, onLongClick = onLongPress),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookCover(book = book, modifier = Modifier.size(56.dp, 76.dp), cornerRadius = 10)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.onBackground,
                )
                Text(
                    book.author.takeIf { it.isNotBlank() } ?: "Unknown",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (book.totalChapters > 0) {
                    Spacer(Modifier.height(6.dp))
                    AssistChip(
                        onClick = {},
                        label = { Text("${book.totalChapters} chapters") },
                        enabled = false,
                    )
                }
            }
            BookStatusDot(book = book)
            IconButton(onClick = onMore) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Book options", tint = colors.secondaryText)
            }
        }
    }
}

@Composable
private fun BookStatusDot(
    book: Book,
    modifier: Modifier = Modifier,
) {
    val color = when {
        book.readingStatus == ReadingStatus.READING || book.progress > 0f && book.progress < 100f -> Color(0xFF22C55E)
        book.lastReadAt == null -> Color(0xFFEF4444)
        else -> Color(0xFF9CA3AF)
    }
    Surface(
        color = color,
        shape = CircleShape,
        modifier = modifier.size(9.dp),
        shadowElevation = 1.dp,
    ) {}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookActionSheet(
    book: Book,
    onDismiss: () -> Unit,
    onStatusSelected: (ReadingStatus) -> Unit,
    onDelete: () -> Unit,
    onChangeCover: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.cardBackground) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BookCover(book = book, modifier = Modifier.size(82.dp, 116.dp), cornerRadius = 14)
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        book.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.onBackground,
                    )
                    Text(book.author, style = MaterialTheme.typography.bodyMedium, color = colors.secondaryText)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${book.totalChapters} chapters · ${book.progress.toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.secondaryText,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Change Cover") },
                supportingContent = { Text("Import an image and fit it to the book card.") },
                leadingContent = { Icon(Icons.Outlined.Image, contentDescription = null, tint = colors.accent) },
                modifier = Modifier.clickable(onClick = onChangeCover),
            )
            ReadingStatus.entries.forEach { status ->
                val icon = when (status) {
                    ReadingStatus.UNREAD -> Icons.Outlined.BookmarkBorder
                    ReadingStatus.READING -> Icons.Outlined.MenuBook
                    ReadingStatus.FINISHED -> Icons.Outlined.CheckCircle
                }
                ListItem(
                    headlineContent = { Text(status.actionLabel()) },
                    leadingContent = { Icon(icon, contentDescription = null, tint = colors.accent) },
                    trailingContent = {
                        if (book.readingStatus == status) Icon(Icons.Filled.Check, contentDescription = null, tint = colors.accent)
                    },
                    modifier = Modifier.clickable { onStatusSelected(status) },
                )
            }
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Delete Book", color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable(onClick = onDelete),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ImportFeedbackDialog(
    viewModel: LibraryViewModel,
    onOpenBook: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalMIYUColors.current
    uiState.importFeedback?.let { feedback ->
        val icon = when (feedback.type) {
            ImportFeedbackType.SUCCESS -> Icons.Filled.CheckCircle
            ImportFeedbackType.ERROR -> Icons.Filled.Error
            ImportFeedbackType.WARNING -> Icons.Filled.Warning
        }
        val tint = when (feedback.type) {
            ImportFeedbackType.SUCCESS -> colors.accent
            ImportFeedbackType.ERROR -> MaterialTheme.colorScheme.error
            ImportFeedbackType.WARNING -> Color(0xFFB7791F)
        }
        AlertDialog(
            onDismissRequest = viewModel::clearImportFeedback,
            icon = { Icon(icon, contentDescription = null, tint = tint) },
            title = { Text(feedback.title) },
            text = { Text(feedback.message) },
            confirmButton = {
                if (feedback.bookId != null) {
                    TextButton(
                        onClick = {
                            val bookId = feedback.bookId
                            viewModel.clearImportFeedback()
                            onOpenBook(bookId)
                        },
                    ) {
                        Text("Read now", color = colors.accent)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::clearImportFeedback) {
                    Text(if (feedback.bookId == null) "Close" else "Later")
                }
            },
        )
    }
}

private fun FilterOption.label(): String = when (this) {
    FilterOption.ALL -> "All"
    FilterOption.UNREAD -> "Unread"
    FilterOption.READING -> "Reading"
    FilterOption.FINISHED -> "Finished"
}

private fun SortOption.label(): String = when (this) {
    SortOption.RECENT -> "Recently read"
    SortOption.TITLE -> "Title"
    SortOption.AUTHOR -> "Author"
    SortOption.PROGRESS -> "Progress"
    SortOption.DATE_ADDED -> "Date added"
}

private fun ReadingStatus.actionLabel(): String = when (this) {
    ReadingStatus.UNREAD -> "Mark as unread"
    ReadingStatus.READING -> "Mark as reading"
    ReadingStatus.FINISHED -> "Mark as finished"
}
