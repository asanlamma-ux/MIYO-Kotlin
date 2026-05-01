package com.miyu.reader.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miyu.reader.domain.model.*
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

    var showFilterMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedBook by remember { mutableStateOf<Book?>(null) }
    var showBookAction by remember { mutableStateOf(false) }

    // File picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let { viewModel.importBookFromUri(it) }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { filePickerLauncher.launch(arrayOf("application/epub+zip", "application/octet-stream")) },
                containerColor = colors.accent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
            ) {
                if (uiState.isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(Icons.Default.Add, contentDescription = "Import")
                }
                Spacer(Modifier.width(8.dp))
                Text("Import", fontWeight = FontWeight.Bold)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(padding),
        ) {
            // ── Header ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Library",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = colors.onBackground,
                    )
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colors.cardBackground,
                    ) {
                        Text(
                            "${uiState.books.size}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = colors.secondaryText,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(onClick = viewModel::toggleViewMode, shape = RoundedCornerShape(14.dp)) {
                        Icon(
                            if (uiState.viewMode == ViewMode.GRID) Icons.Outlined.ViewList else Icons.Outlined.GridView,
                            contentDescription = "Toggle View",
                        )
                    }
                }
            }
            Text(
                "OPDS catalogs and WTR Lab imports are still being ported, so only local EPUB import is exposed here right now.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.secondaryText,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            // ── Search bar ──────────────────────────────────────────
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text("Search books…") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
            )

            Spacer(Modifier.height(8.dp))

            // ── Toolbar: filter, sort, view toggle ──────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Filter chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterOption.entries.forEach { option ->
                        FilterChip(
                            selected = uiState.filterOption == option,
                            onClick = { viewModel.setFilterOption(option) },
                            label = {
                                Text(
                                    option.name.lowercase().replaceFirstChar { it.uppercase() },
                                    fontSize = 12.sp,
                                )
                            },
                            modifier = Modifier.height(32.dp),
                        )
                    }
                }

                Row {
                    Box {
                        AssistChip(
                            onClick = { showSortMenu = true },
                            leadingIcon = { Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            label = {
                                Text(
                                    when (uiState.sortOption) {
                                        SortOption.RECENT -> "Sort: Recently Read"
                                        SortOption.TITLE -> "Sort: Title"
                                        SortOption.AUTHOR -> "Sort: Author"
                                        SortOption.PROGRESS -> "Sort: Progress"
                                        SortOption.DATE_ADDED -> "Sort: Date Added"
                                    },
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                        )
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when (option) {
                                                SortOption.RECENT -> "Recently Read"
                                                SortOption.TITLE -> "Title"
                                                SortOption.AUTHOR -> "Author"
                                                SortOption.PROGRESS -> "Progress"
                                                SortOption.DATE_ADDED -> "Date Added"
                                            },
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSortOption(option)
                                        showSortMenu = false
                                    },
                                    leadingIcon = {
                                        if (uiState.sortOption == option) Icon(Icons.Default.Check, contentDescription = null)
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Book list / grid ────────────────────────────────────
            if (displayedBooks.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = colors.accent.copy(alpha = 0.1f),
                            modifier = Modifier.size(90.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(40.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            if (uiState.searchQuery.isNotBlank()) "No books found" else "Your library is empty",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.onBackground,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (uiState.searchQuery.isNotBlank()) "Try a different search term or clear your filters."
                            else "Tap the Import button to add your first EPUB.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.secondaryText,
                        )
                    }
                }
            } else if (uiState.viewMode == ViewMode.GRID) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(displayedBooks, key = { it.id }) { book ->
                        GridBookCard(
                            book = book,
                            colors = colors,
                            onPress = { onOpenBook(book.id) },
                            onLongPress = {
                                selectedBook = book
                                showBookAction = true
                            },
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(displayedBooks, key = { it.id }) { book ->
                        ListBookCard(
                            book = book,
                            colors = colors,
                            onPress = { onOpenBook(book.id) },
                            onLongPress = {
                                selectedBook = book
                                showBookAction = true
                            },
                        )
                    }
                }
            }
        }

        // ── Book action bottom sheet ────────────────────────────────
        if (showBookAction && selectedBook != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBookAction = false
                    selectedBook = null
                },
            ) {
                val book = selectedBook!!
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                    Text(
                        book.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(book.author, style = MaterialTheme.typography.bodyMedium, color = colors.secondaryText)
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    // Status toggles
                    ReadingStatus.entries.forEach { status ->
                        val label = when (status) {
                            ReadingStatus.UNREAD -> "Mark as Unread"
                            ReadingStatus.READING -> "Mark as Reading"
                            ReadingStatus.FINISHED -> "Mark as Finished"
                        }
                        val icon = when (status) {
                            ReadingStatus.UNREAD -> Icons.Outlined.BookmarkBorder
                            ReadingStatus.READING -> Icons.Outlined.MenuBook
                            ReadingStatus.FINISHED -> Icons.Outlined.CheckCircle
                        }
                        ListItem(
                            headlineContent = { Text(label) },
                            leadingContent = { Icon(icon, contentDescription = null, tint = colors.accent) },
                            trailingContent = {
                                if (book.readingStatus == status) Icon(Icons.Default.Check, contentDescription = null, tint = colors.accent)
                            },
                            modifier = Modifier.clickable {
                                viewModel.updateBookStatus(book.id, status)
                                showBookAction = false
                                selectedBook = null
                            },
                        )
                    }
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Delete Book", color = MaterialTheme.colorScheme.error) },
                        leadingContent = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable {
                            viewModel.deleteBook(book.id)
                            showBookAction = false
                            selectedBook = null
                        },
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        uiState.importFeedback?.let { feedback ->
            val icon = when (feedback.type) {
                ImportFeedbackType.SUCCESS -> Icons.Default.CheckCircle
                ImportFeedbackType.ERROR -> Icons.Default.Error
                ImportFeedbackType.WARNING -> Icons.Default.Warning
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
}

// ── Grid card ────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridBookCard(
    book: Book,
    colors: com.miyu.reader.ui.theme.MIYUColors,
    onPress: () -> Unit,
    onLongPress: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onPress, onLongClick = onLongPress),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            BookCover(
                book = book,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                book.title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = colors.onBackground,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                book.author,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = colors.secondaryText,
            )
            if (book.progress > 0f) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { book.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = colors.accent,
                    trackColor = colors.secondaryText.copy(alpha = 0.15f),
                )
            }
        }
    }
}

// ── List card ────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListBookCard(
    book: Book,
    colors: com.miyu.reader.ui.theme.MIYUColors,
    onPress: () -> Unit,
    onLongPress: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onPress, onLongClick = onLongPress),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookCover(book = book, modifier = Modifier.size(48.dp, 68.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.onBackground,
                )
                Text(book.author, style = MaterialTheme.typography.bodySmall, color = colors.secondaryText)
                if (book.progress > 0f) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { book.progress / 100f },
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = colors.accent,
                            trackColor = colors.secondaryText.copy(alpha = 0.15f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("${book.progress.toInt()}%", style = MaterialTheme.typography.labelSmall, color = colors.secondaryText)
                    }
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(18.dp))
        }
    }
}
