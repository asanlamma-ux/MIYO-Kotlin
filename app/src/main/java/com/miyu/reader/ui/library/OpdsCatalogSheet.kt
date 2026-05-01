package com.miyu.reader.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miyu.reader.domain.model.OpdsEntry
import com.miyu.reader.ui.theme.LocalMIYUColors
import com.miyu.reader.viewmodel.OpdsCatalogViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpdsCatalogSheet(
    onDismiss: () -> Unit,
    onImportEntry: (OpdsEntry, String) -> Unit,
    viewModel: OpdsCatalogViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val entries = viewModel.filteredEntries
    val colors = LocalMIYUColors.current

    LaunchedEffect(Unit) {
        viewModel.open()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 6.dp),
        ) {
            WorkspaceExitButton(label = "Exit OPDS", onClick = onDismiss)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "OPDS Catalogs",
                        color = colors.onBackground,
                        style = androidx.compose.material3.MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        "Browse online catalogs and import EPUBs directly.",
                        color = colors.secondaryText,
                    )
                }
                if (state.loading) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.catalogUrl,
                    onValueChange = viewModel::setCatalogUrl,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Language, contentDescription = null) },
                    placeholder = { Text("Add HTTPS OPDS URL") },
                )
                Button(
                    onClick = viewModel::addCatalog,
                    enabled = state.catalogUrl.isNotBlank() && !state.savingCatalog,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    if (state.savingCatalog) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    } else {
                        Text("Add")
                    }
                }
            }

            if (state.catalogs.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    items(state.catalogs, key = { it.id }) { catalog ->
                        FilterChip(
                            selected = state.activeCatalogId == catalog.id,
                            onClick = { viewModel.loadFeed(catalog.url, pushHistory = false, catalogId = catalog.id) },
                            label = { Text(catalog.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = {
                                Icon(
                                    if (state.activeCatalogId == catalog.id) Icons.Filled.Check else Icons.Outlined.Language,
                                    contentDescription = null,
                                )
                            },
                            trailingIcon = {
                                if (!catalog.isDefault) {
                                    IconButton(onClick = { viewModel.removeCatalog(catalog.id) }) {
                                        Icon(Icons.Outlined.Delete, contentDescription = "Remove catalog")
                                    }
                                }
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text("Filter loaded entries...") },
            )

            state.error?.let {
                Text(
                    it,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            Row(
                modifier = Modifier.padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.feedStack.isNotEmpty()) {
                    TextButton(onClick = viewModel::goBackFeed) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = null)
                        Text("Back")
                    }
                }
                state.feed?.previousUrl?.let { url ->
                    TextButton(onClick = { viewModel.loadFeed(url, pushHistory = false) }) {
                        Text("Previous")
                    }
                }
                state.feed?.nextUrl?.let { url ->
                    TextButton(onClick = { viewModel.loadFeed(url, pushHistory = false) }) {
                        Text("Next")
                    }
                }
            }

            Text(
                state.feed?.title ?: "No catalog loaded",
                color = colors.onBackground,
                fontWeight = FontWeight.Bold,
            )
            Text(
                state.feed?.url ?: "Save or select a catalog to start.",
                color = colors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(10.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 28.dp),
            ) {
                if (!state.loading && entries.isEmpty()) {
                    item {
                        Text(
                            "No entries found in this feed.",
                            color = colors.secondaryText,
                            modifier = Modifier.padding(vertical = 18.dp),
                        )
                    }
                }
                items(entries, key = { it.id }) { entry ->
                    OpdsEntryCard(
                        entry = entry,
                        importing = state.importingEntryId == entry.id,
                        onOpenNavigation = { link -> viewModel.loadFeed(link, pushHistory = true) },
                        onImport = { link ->
                            viewModel.markImporting(entry.id)
                            onImportEntry(entry, link)
                            viewModel.markImporting(null)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkspaceExitButton(
    label: String,
    onClick: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    TextButton(
        onClick = onClick,
        modifier = Modifier.padding(bottom = 8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
    ) {
        Text(
            "> $label",
            color = colors.secondaryText,
            textDecoration = TextDecoration.Underline,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun OpdsEntryCard(
    entry: OpdsEntry,
    importing: Boolean,
    onOpenNavigation: (String) -> Unit,
    onImport: (String) -> Unit,
) {
    val colors = LocalMIYUColors.current
    val acquisition = entry.acquisitionLinks.firstOrNull {
        it.type.equals("application/epub+zip", ignoreCase = true) || it.rel.contains("acquisition", ignoreCase = true)
    }
    val navigation = entry.navigationLinks.firstOrNull()
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Book, contentDescription = null, tint = colors.accent)
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.title, color = colors.onBackground, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(entry.author, color = colors.secondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            entry.summary?.let {
                Text(
                    it,
                    color = colors.secondaryText,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                if (navigation != null) {
                    AssistChip(
                        onClick = { onOpenNavigation(navigation.href) },
                        label = { Text(navigation.title ?: "Open feed") },
                    )
                }
                if (acquisition != null) {
                    Button(
                        onClick = { onImport(acquisition.href) },
                        enabled = !importing,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        if (importing) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                        }
                        Text("Import")
                    }
                }
            }
        }
    }
}
