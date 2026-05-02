package com.miyu.reader.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miyu.reader.domain.model.Book
import com.miyu.reader.domain.model.FilterOption
import com.miyu.reader.domain.model.ReadingStatus
import com.miyu.reader.domain.model.SortOption
import com.miyu.reader.domain.model.ViewMode
import com.miyu.reader.ui.components.BookCover
import com.miyu.reader.ui.core.components.MiyoMainOverflowMenu
import com.miyu.reader.ui.core.components.MiyoEmptyScreen
import com.miyu.reader.ui.core.components.MiyoTopSearchBar
import com.miyu.reader.ui.theme.LocalMIYUColors
import com.miyu.reader.viewmodel.ImportFeedbackType
import com.miyu.reader.viewmodel.LibraryCategory
import com.miyu.reader.viewmodel.LibraryCategoryIds
import com.miyu.reader.viewmodel.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onOpenBook: (String) -> Unit = {},
    onOpenBookDetails: (String) -> Unit = {},
    onOpenBrowse: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenThemePicker: () -> Unit = {},
    onSaveAndExport: () -> Unit = {},
    onAbout: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val displayedBooks by viewModel.displayedBooks.collectAsStateWithLifecycle()
    val colors = LocalMIYUColors.current
    val categories = uiState.categories.ifEmpty {
        listOf(
            LibraryCategory(
                id = LibraryCategoryIds.ALL,
                name = "All",
                count = uiState.books.size,
                isSystem = true,
            ),
        )
    }
    val visibleCategories = categories.filter { !it.isHidden || it.id == LibraryCategoryIds.ALL }
    val selectedCategory = visibleCategories.firstOrNull { it.id == uiState.selectedCategoryId } ?: visibleCategories.first()

    var showSortMenu by remember { mutableStateOf(false) }
    var showCreateCategory by remember { mutableStateOf(false) }
    var showCategoryManager by remember { mutableStateOf(false) }
    var showAssignCategory by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var incognitoMode by remember { mutableStateOf(false) }
    var selectedBookForOptions by remember { mutableStateOf<Book?>(null) }
    var pendingBookCategoryId by remember { mutableStateOf(LibraryCategoryIds.ALL) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let { viewModel.importBookFromUri(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showCategoryManager) {
            CategoryManagerScreen(
                categories = categories.filter { it.id != LibraryCategoryIds.ALL },
                onBack = { showCategoryManager = false },
                onCreateCategory = { showCreateCategory = true },
                onSelectCategory = {
                    viewModel.setSelectedCategory(it)
                    showCategoryManager = false
                },
                onDeleteCategories = { ids ->
                    ids.forEach(viewModel::deleteCategory)
                },
                onSetHidden = { ids, hidden ->
                    ids.forEach { id -> viewModel.setCategoryHidden(id, hidden) }
                },
            )
            if (showCreateCategory) {
                CategoryNameDialog(
                    title = "Create category",
                    initialName = "",
                    confirmLabel = "Create",
                    onDismiss = { showCreateCategory = false },
                    onConfirm = {
                        viewModel.createCategory(it)
                        showCreateCategory = false
                    },
                )
            }
            return@Box
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background),
        ) {
            AnimatedVisibility(visible = uiState.selectedBookIds.isNotEmpty()) {
                LibrarySelectionToolbar(
                    selectedCount = uiState.selectedBookIds.size,
                    onClear = viewModel::clearSelection,
                    onAssignCategory = { showAssignCategory = true },
                    onDeleteSelection = viewModel::deleteSelectedBooks,
                )
            }

            AnimatedVisibility(visible = uiState.selectedBookIds.isEmpty()) {
                LibraryToolbar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::setSearchQuery,
                    viewMode = uiState.viewMode,
                    filter = uiState.filterOption,
                    sort = uiState.sortOption,
                    showSortMenu = showSortMenu,
                    incognitoMode = incognitoMode,
                    onShowSortMenu = { showSortMenu = true },
                    onDismissSortMenu = { showSortMenu = false },
                    onSortSelected = {
                        viewModel.setSortOption(it)
                        showSortMenu = false
                    },
                    onFilterSelected = {
                        viewModel.setFilterOption(it)
                        showSortMenu = false
                    },
                    onOpenBrowse = onOpenBrowse,
                    onToggleView = viewModel::toggleViewMode,
                    onManageCategory = { showCategoryManager = true },
                    onToggleIncognito = { incognitoMode = !incognitoMode },
                    onOpenSettings = onOpenSettings,
                    onOpenThemePicker = onOpenThemePicker,
                    onSaveAndExport = onSaveAndExport,
                    onAbout = onAbout,
                )
            }

            LibraryCategorySelectorRow(
                selectedCategory = selectedCategory,
                onOpenPicker = { showCategoryPicker = true },
            )

            Spacer(Modifier.height(6.dp))

            AnimatedVisibility(visible = uiState.filterOption != FilterOption.ALL) {
                ActiveFilterRow(
                    filter = uiState.filterOption,
                    onClear = { viewModel.setFilterOption(FilterOption.ALL) },
                )
            }

            when {
                displayedBooks.isEmpty() -> EmptyLibraryState(
                    hasSearch = uiState.searchQuery.isNotBlank(),
                    hasBooks = uiState.books.isNotEmpty(),
                    selectedCategory = selectedCategory.name,
                    modifier = Modifier.weight(1f),
                )

                uiState.viewMode == ViewMode.GRID -> LazyVerticalGrid(
                    modifier = Modifier.weight(1f),
                    columns = GridCells.Adaptive(104.dp),
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 104.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(displayedBooks, key = { it.id }) { book ->
                        LibraryGridItem(
                            book = book,
                            selected = book.id in uiState.selectedBookIds,
                            selectionMode = uiState.selectedBookIds.isNotEmpty(),
                            onPress = {
                                if (uiState.selectedBookIds.isNotEmpty()) viewModel.toggleBookSelection(book.id)
                                else onOpenBook(book.id)
                            },
                            onLongPress = { viewModel.toggleBookSelection(book.id) },
                            onMore = {
                                pendingBookCategoryId = book.primaryLibraryCategoryId()
                                selectedBookForOptions = book
                            },
                        )
                    }
                }

                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(displayedBooks, key = { it.id }) { book ->
                        LibraryListItem(
                            book = book,
                            selected = book.id in uiState.selectedBookIds,
                            selectionMode = uiState.selectedBookIds.isNotEmpty(),
                            onPress = {
                                if (uiState.selectedBookIds.isNotEmpty()) viewModel.toggleBookSelection(book.id)
                                else onOpenBook(book.id)
                            },
                            onLongPress = { viewModel.toggleBookSelection(book.id) },
                            onMore = {
                                pendingBookCategoryId = book.primaryLibraryCategoryId()
                                selectedBookForOptions = book
                            },
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.selectedBookIds.isEmpty(),
            modifier = Modifier.align(Alignment.BottomEnd),
        ) {
            ExtendedFloatingActionButton(
                onClick = { filePickerLauncher.launch(arrayOf("application/epub+zip", "application/epub", "application/octet-stream")) },
                modifier = Modifier.padding(end = 24.dp, bottom = 20.dp),
                containerColor = colors.accent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
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
                Spacer(Modifier.width(10.dp))
                Text("Import", fontWeight = FontWeight.Bold)
            }
        }

        ImportFeedbackDialog(
            viewModel = viewModel,
            onOpenBook = onOpenBook,
        )

        if (showCreateCategory) {
            CategoryNameDialog(
                title = "Create category",
                initialName = "",
                confirmLabel = "Create",
                onDismiss = { showCreateCategory = false },
                onConfirm = {
                    viewModel.createCategory(it)
                    showCreateCategory = false
                },
            )
        }

        if (showAssignCategory) {
            CategoryPickerBottomSheet(
                title = "Move selected books",
                categories = visibleCategories,
                onDismiss = { showAssignCategory = false },
                onChoose = {
                    viewModel.assignSelectionToCategory(it)
                    showAssignCategory = false
                },
                onCreateCategory = {
                    showAssignCategory = false
                    showCreateCategory = true
                },
            )
        }

        if (showCategoryPicker) {
            CategoryPickerBottomSheet(
                title = "Categories",
                categories = visibleCategories,
                onDismiss = { showCategoryPicker = false },
                onChoose = {
                    viewModel.setSelectedCategory(it)
                    showCategoryPicker = false
                },
                onCreateCategory = {
                    showCategoryPicker = false
                    showCreateCategory = true
                },
            )
        }

        selectedBookForOptions?.let { book ->
            BookOptionsBottomSheet(
                book = book,
                categories = visibleCategories,
                selectedCategoryId = pendingBookCategoryId,
                onDismiss = { selectedBookForOptions = null },
                onChooseCategory = { pendingBookCategoryId = it },
                onApplyCategory = {
                    viewModel.assignBookToCategory(book.id, pendingBookCategoryId)
                    selectedBookForOptions = null
                },
                onCreateCategory = {
                    selectedBookForOptions = null
                    showCreateCategory = true
                },
                onDelete = {
                    viewModel.deleteBook(book.id)
                    selectedBookForOptions = null
                },
                onOpenDetails = {
                    selectedBookForOptions = null
                    onOpenBookDetails(book.id)
                },
            )
        }
    }
}

@Composable
private fun LibraryToolbar(
    query: String,
    onQueryChange: (String) -> Unit,
    viewMode: ViewMode,
    filter: FilterOption,
    sort: SortOption,
    showSortMenu: Boolean,
    incognitoMode: Boolean,
    onShowSortMenu: () -> Unit,
    onDismissSortMenu: () -> Unit,
    onSortSelected: (SortOption) -> Unit,
    onFilterSelected: (FilterOption) -> Unit,
    onOpenBrowse: () -> Unit,
    onToggleView: () -> Unit,
    onManageCategory: () -> Unit,
    onToggleIncognito: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenThemePicker: () -> Unit,
    onSaveAndExport: () -> Unit,
    onAbout: () -> Unit,
) {
    MiyoTopSearchBar(
        query = query,
        onQueryChange = onQueryChange,
        placeholder = "Search library...",
        actions = {
            Box {
                MiyoMainOverflowMenu(
                    onOpenSettings = onOpenSettings,
                    onOpenThemePicker = onOpenThemePicker,
                    onExportData = onSaveAndExport,
                    onAbout = onAbout,
                    extraItems = { dismiss ->
                        DropdownMenuItem(
                            text = { Text("Sort and filter") },
                            onClick = {
                                dismiss()
                                onShowSortMenu()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Tune, contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = { Text("Browse sources") },
                            onClick = {
                                dismiss()
                                onOpenBrowse()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Explore, contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = { Text(if (viewMode == ViewMode.GRID) "List view" else "Grid view") },
                            onClick = {
                                dismiss()
                                onToggleView()
                            },
                            leadingIcon = {
                                Icon(
                                    if (viewMode == ViewMode.GRID) Icons.Outlined.ViewList else Icons.Outlined.GridView,
                                    contentDescription = null,
                                )
                            },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Categories") },
                            onClick = {
                                dismiss()
                                onManageCategory()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Category, contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Incognito mode", modifier = Modifier.weight(1f))
                                    Checkbox(checked = incognitoMode, onCheckedChange = null)
                                }
                            },
                            onClick = {
                                onToggleIncognito()
                                dismiss()
                            },
                            leadingIcon = { Icon(Icons.Outlined.FilterList, contentDescription = null) },
                        )
                        HorizontalDivider()
                    },
                )
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = onDismissSortMenu,
                ) {
                    DropdownMenuHeader("Sort")
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label()) },
                            onClick = { onSortSelected(option) },
                            leadingIcon = {
                                if (sort == option) Icon(Icons.Filled.Check, contentDescription = null)
                                else Icon(Icons.Outlined.Sort, contentDescription = null)
                            },
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuHeader("Filter")
                    FilterOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label()) },
                            onClick = { onFilterSelected(option) },
                            leadingIcon = {
                                if (filter == option) Icon(Icons.Filled.Check, contentDescription = null)
                                else Icon(Icons.Outlined.FilterList, contentDescription = null)
                            },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun LibrarySelectionToolbar(
    selectedCount: Int,
    onClear: () -> Unit,
    onAssignCategory: () -> Unit,
    onDeleteSelection: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
        color = colors.cardBackground,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClear) {
                Icon(Icons.Filled.Clear, contentDescription = "Clear selection", tint = colors.onBackground)
            }
            Text(
                text = "$selectedCount selected",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.onBackground,
            )
            IconButton(onClick = onAssignCategory) {
                Icon(Icons.Filled.Label, contentDescription = "Assign category", tint = colors.accent)
            }
            IconButton(onClick = onDeleteSelection) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete selected books", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun LibrarySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search library...") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotBlank()) {
                Icon(
                    Icons.Filled.Clear,
                    contentDescription = "Clear search",
                    modifier = Modifier.clickable { onQueryChange("") },
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(18.dp),
        singleLine = true,
    )
}

@Composable
private fun LibraryCategorySelectorRow(
    selectedCategory: LibraryCategory,
    onOpenPicker: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp),
            color = colors.cardBackground,
            border = BorderStroke(1.dp, colors.secondaryText.copy(alpha = 0.22f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenPicker)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Outlined.Category, contentDescription = null, tint = colors.accent)
                Text(
                    selectedCategory.name,
                    modifier = Modifier.weight(1f),
                    color = colors.onBackground,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                CountPill(count = selectedCategory.count, compact = true, selected = true)
            }
        }
    }
}

@Composable
private fun ActiveFilterRow(
    filter: FilterOption,
    onClear: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    Surface(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 4.dp),
        color = colors.accent.copy(alpha = 0.14f),
        contentColor = colors.accent,
        shape = RoundedCornerShape(100),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Outlined.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(filter.label(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Icon(
                Icons.Filled.Clear,
                contentDescription = "Clear filter",
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClear),
            )
        }
    }
}

@Composable
private fun EmptyLibraryState(
    hasSearch: Boolean,
    hasBooks: Boolean,
    selectedCategory: String,
    modifier: Modifier = Modifier,
) {
    MiyoEmptyScreen(
        icon = Icons.Filled.MenuBook,
        title = when {
            hasSearch -> "No matches"
            hasBooks -> "No books in $selectedCategory"
            else -> "No books yet"
        },
        message = when {
            hasSearch -> "Try another search, category, or filter."
            hasBooks -> "Long-press books in another category to assign them here."
            else -> "Import an EPUB or browse online sources to build your library."
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 96.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryGridItem(
    book: Book,
    selected: Boolean,
    selectionMode: Boolean,
    onPress: () -> Unit,
    onLongPress: () -> Unit,
    onMore: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .combinedClickable(onClick = onPress, onLongClick = onLongPress),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = if (selected) 2.dp else 0.dp,
                    color = if (selected) colors.accent else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                ),
        ) {
            BookCover(
                book = book,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 12,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.52f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.78f),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
            ) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White,
                )
                Text(
                    book.author.takeIf { it.isNotBlank() } ?: "Unknown",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White.copy(alpha = 0.76f),
                )
            }
            if (book.progress > 0f) {
                ProgressBadge(
                    progress = book.progress,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                )
            }
            if (selected || selectionMode) {
                SelectionBadge(
                    selected = selected,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                )
            } else {
                BookStatusDot(book = book, modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
                IconButton(
                    onClick = onMore,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(34.dp),
                ) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Book options", tint = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryListItem(
    book: Book,
    selected: Boolean,
    selectionMode: Boolean,
    onPress: () -> Unit,
    onLongPress: () -> Unit,
    onMore: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) colors.accent else Color.Transparent,
                shape = RoundedCornerShape(18.dp),
            )
            .combinedClickable(onClick = onPress, onLongClick = onLongPress),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                BookCover(book = book, modifier = Modifier.size(58.dp, 82.dp), cornerRadius = 10)
                if (selected || selectionMode) {
                    SelectionBadge(
                        selected = selected,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
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
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (book.totalChapters > 0) {
                        AssistChip(
                            onClick = {},
                            label = { Text("${book.totalChapters} ch.") },
                            leadingIcon = {
                                Icon(Icons.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            enabled = false,
                        )
                    }
                    if (book.progress > 0f) {
                        ProgressBadge(progress = book.progress)
                    }
                }
            }
            if (!selectionMode) {
                BookStatusDot(book = book)
                IconButton(onClick = onMore) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Book options", tint = colors.secondaryText)
                }
            }
        }
    }
}

@Composable
private fun CountPill(
    count: Int,
    compact: Boolean = false,
    selected: Boolean = false,
) {
    val colors = LocalMIYUColors.current
    Surface(
        color = if (selected) colors.accent.copy(alpha = 0.16f) else colors.cardBackground,
        shape = CircleShape,
        tonalElevation = if (compact) 0.dp else 1.dp,
        border = if (compact) BorderStroke(1.dp, colors.secondaryText.copy(alpha = 0.18f)) else null,
    ) {
        Text(
            text = count.toString(),
            modifier = Modifier.padding(
                horizontal = if (compact) 8.dp else 13.dp,
                vertical = if (compact) 3.dp else 8.dp,
            ),
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.titleMedium,
            color = if (selected) colors.accent else colors.secondaryText,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun HeaderActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.cardBackground,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        modifier = Modifier.size(44.dp),
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = contentDescription, tint = colors.onBackground, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun ProgressBadge(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMIYUColors.current
    Surface(
        modifier = modifier,
        color = colors.accent.copy(alpha = 0.92f),
        contentColor = Color.White,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = "${progress.toInt()}%",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun SelectionBadge(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalMIYUColors.current
    Surface(
        modifier = modifier.size(28.dp),
        shape = CircleShape,
        color = if (selected) colors.accent else Color.Black.copy(alpha = 0.52f),
        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.92f)),
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.padding(4.dp))
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

@Composable
private fun CategoryNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Category, contentDescription = null) },
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Category name") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.trim().isNotBlank(),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerBottomSheet(
    title: String,
    categories: List<LibraryCategory>,
    onDismiss: () -> Unit,
    onChoose: (String) -> Unit,
    onCreateCategory: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val colors = LocalMIYUColors.current
    val filteredCategories = remember(categories, query) {
        categories.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = colors.onBackground,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text("Search categories") },
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredCategories, key = { it.id }) { category ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onChoose(category.id) },
                        shape = RoundedCornerShape(16.dp),
                        color = colors.cardBackground,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                if (category.id == LibraryCategoryIds.ALL) Icons.Filled.Category else Icons.Filled.BookmarkAdded,
                                contentDescription = null,
                                tint = colors.accent,
                            )
                            Text(
                                category.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.onBackground,
                            )
                            CountPill(count = category.count, compact = true)
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = onCreateCategory,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("New category")
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookOptionsBottomSheet(
    book: Book,
    categories: List<LibraryCategory>,
    selectedCategoryId: String,
    onDismiss: () -> Unit,
    onChooseCategory: (String) -> Unit,
    onApplyCategory: () -> Unit,
    onCreateCategory: () -> Unit,
    onDelete: () -> Unit,
    onOpenDetails: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId } ?: categories.first()
    var showInlinePicker by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BookCover(book = book, modifier = Modifier.size(56.dp, 78.dp), cornerRadius = 12)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        book.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.onBackground,
                    )
                    Text(
                        book.author.takeIf { it.isNotBlank() } ?: "Unknown",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.cardBackground,
                border = BorderStroke(1.dp, colors.secondaryText.copy(alpha = 0.18f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showInlinePicker = true },
                    ) {
                        Text(
                            "Category",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.secondaryText,
                        )
                        Text(
                            selectedCategory.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.onBackground,
                        )
                    }
                    HeaderActionButton(
                        icon = Icons.Filled.Check,
                        contentDescription = "Apply category",
                        onClick = onApplyCategory,
                    )
                }
            }
            OutlinedButton(
                onClick = onOpenDetails,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Filled.MenuBook, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("View details")
            }
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text("Delete book", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
    if (showInlinePicker) {
        CategoryPickerBottomSheet(
            title = "Choose category",
            categories = categories,
            onDismiss = { showInlinePicker = false },
            onChoose = {
                onChooseCategory(it)
                showInlinePicker = false
            },
            onCreateCategory = {
                showInlinePicker = false
                onCreateCategory()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun CategoryManagerScreen(
    categories: List<LibraryCategory>,
    onBack: () -> Unit,
    onCreateCategory: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onDeleteCategories: (Set<String>) -> Unit,
    onSetHidden: (Set<String>, Boolean) -> Unit,
) {
    val colors = LocalMIYUColors.current
    var query by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val filteredCategories = remember(categories, query) {
        categories.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HeaderActionButton(
                    icon = if (selectedIds.isEmpty()) Icons.Filled.ArrowBack else Icons.Filled.Clear,
                    contentDescription = if (selectedIds.isEmpty()) "Back" else "Cancel selection",
                    onClick = {
                        if (selectedIds.isEmpty()) onBack() else selectedIds = emptySet()
                    },
                )
                Text(
                    if (selectedIds.isEmpty()) "Categories" else "${selectedIds.size} selected",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = colors.onBackground,
                )
                if (selectedIds.isNotEmpty()) {
                    val selectedCategories = categories.filter { it.id in selectedIds }
                    val shouldHideSelected = selectedCategories.any { !it.isHidden }
                    HeaderActionButton(
                        icon = if (shouldHideSelected) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (shouldHideSelected) "Hide categories" else "Show categories",
                        onClick = {
                            onSetHidden(selectedIds, shouldHideSelected)
                            selectedIds = emptySet()
                        },
                    )
                    HeaderActionButton(
                        icon = Icons.Filled.Delete,
                        contentDescription = "Delete categories",
                        onClick = {
                            onDeleteCategories(selectedIds)
                            selectedIds = emptySet()
                        },
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text("Search categories") },
            )
            Spacer(Modifier.height(14.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredCategories, key = { it.id }) { category ->
                    val isSelected = category.id in selectedIds
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (selectedIds.isNotEmpty()) {
                                        selectedIds = if (isSelected) selectedIds - category.id else selectedIds + category.id
                                    } else {
                                        onSelectCategory(category.id)
                                    }
                                },
                                onLongClick = {
                                    selectedIds = if (isSelected) selectedIds - category.id else selectedIds + category.id
                                },
                            ),
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected) colors.accent.copy(alpha = 0.14f) else colors.cardBackground,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) colors.accent.copy(alpha = 0.38f) else colors.secondaryText.copy(alpha = 0.16f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                if (category.isHidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null,
                                tint = if (category.isHidden) colors.secondaryText else colors.accent,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    category.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = colors.onBackground,
                                )
                                Text(
                                    "${category.count} book${if (category.count == 1) "" else "s"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.secondaryText,
                                )
                            }
                            if (selectedIds.isEmpty()) {
                                HeaderActionButton(
                                    icon = if (category.isHidden) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (category.isHidden) "Show category" else "Hide category",
                                    onClick = { onSetHidden(setOf(category.id), !category.isHidden) },
                                )
                            } else if (isSelected) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = colors.accent)
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onCreateCategory,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 24.dp),
            containerColor = colors.accent,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(Icons.Filled.Add, contentDescription = "New category")
        }
    }
}

private const val LIBRARY_CATEGORY_TAG_PREFIX = "category:"

private fun Book.primaryLibraryCategoryId(): String {
    val category = tags
        .asSequence()
        .mapNotNull { tag ->
            tag.takeIf { it.startsWith(LIBRARY_CATEGORY_TAG_PREFIX, ignoreCase = true) }
                ?.substringAfter(':')
                ?.trim()
                ?.takeIf(String::isNotBlank)
        }
        .firstOrNull()
    return category?.let(LibraryCategoryIds::custom) ?: LibraryCategoryIds.ALL
}

@Composable
private fun DropdownMenuHeader(label: String) {
    Text(
        text = label,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
    )
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
