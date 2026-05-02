package com.miyu.reader.ui.history

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miyu.reader.domain.model.Book
import com.miyu.reader.ui.core.components.MiyoEmptyScreen
import com.miyu.reader.ui.core.components.MiyoMainOverflowMenu
import com.miyu.reader.ui.core.components.MiyoTopSearchBar
import com.miyu.reader.ui.core.theme.MiyoSpacing
import com.miyu.reader.ui.theme.LocalMIYUColors
import com.miyu.reader.viewmodel.HistoryViewModel
import java.time.Instant
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onOpenBook: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenThemePicker: () -> Unit = {},
    onSaveAndExport: () -> Unit = {},
    onAbout: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.readingHistory.collectAsStateWithLifecycle()
    val colors = LocalMIYUColors.current
    var query by remember { mutableStateOf("") }
    val visibleHistory = remember(history, query) {
        if (query.isBlank()) {
            history
        } else {
            val needle = query.trim().lowercase()
            history.filter { book ->
                book.title.lowercase().contains(needle) ||
                    book.author.lowercase().contains(needle)
            }
        }
    }

    // Group by time period
    val grouped = remember(visibleHistory) {
        visibleHistory.groupBy { book ->
            val readAt = try { Instant.parse(book.lastReadAt ?: "") } catch (_: Exception) { Instant.now() }
            val days = ChronoUnit.DAYS.between(readAt, Instant.now())
            when {
                days < 1 -> "Today"
                days < 2 -> "Yesterday"
                days < 7 -> "This Week"
                days < 30 -> "This Month"
                else -> "Earlier"
            }
        }.toList()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MiyoTopSearchBar(
            query = query,
            onQueryChange = { query = it },
            placeholder = "Search history...",
            actions = {
                if (uiState.isSelecting) {
                    IconButton(onClick = viewModel::cancelSelection) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                } else {
                    MiyoMainOverflowMenu(
                        onOpenSettings = onOpenSettings,
                        onOpenThemePicker = onOpenThemePicker,
                        onExportData = onSaveAndExport,
                        onImportData = onSaveAndExport,
                        onAbout = onAbout,
                    )
                }
            },
        )

        // ── Selection bar ───────────────────────────────────────────
        AnimatedVisibility(visible = uiState.isSelecting) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MiyoSpacing.large, vertical = MiyoSpacing.extraSmall),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = MiyoSpacing.medium, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "${uiState.selectedIds.size} selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.secondaryText,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = viewModel::clearSelectedHistory,
                        enabled = uiState.selectedIds.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = MiyoSpacing.medium, vertical = MiyoSpacing.small),
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Remove", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // ── Content ─────────────────────────────────────────────────
        if (visibleHistory.isEmpty()) {
            MiyoEmptyScreen(
                icon = Icons.Outlined.AccessTime,
                title = if (query.isBlank()) "No Reading History" else "No matches",
                message = if (query.isBlank()) {
                    "Start reading a book and it will appear here."
                } else {
                    "Try another title or author."
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(MiyoSpacing.extraLarge),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = MiyoSpacing.large,
                    top = MiyoSpacing.extraSmall,
                    end = MiyoSpacing.large,
                    bottom = 96.dp,
                ),
            ) {
                grouped.forEach { (label, books) ->
                    // Section header
                    item(key = "header_$label") {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.secondaryText,
                            modifier = Modifier.padding(vertical = MiyoSpacing.small),
                        )
                    }
                    items(books, key = { it.id }) { book ->
                        val isSelected = uiState.selectedIds.contains(book.id)
                        HistoryItem(
                            book = book,
                            isSelected = isSelected,
                            isSelecting = uiState.isSelecting,
                            colors = colors,
                            onPress = {
                                if (uiState.isSelecting) viewModel.toggleSelection(book.id)
                                else onOpenBook(book.id)
                            },
                            onLongPress = {
                                if (!uiState.isSelecting) viewModel.startSelecting(book.id)
                                else viewModel.toggleSelection(book.id)
                            },
                        )
                        Spacer(Modifier.height(MiyoSpacing.small))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryItem(
    book: Book,
    isSelected: Boolean,
    isSelecting: Boolean,
    colors: com.miyu.reader.ui.theme.MIYUColors,
    onPress: () -> Unit,
    onLongPress: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onPress, onLongClick = onLongPress),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) colors.accent.copy(alpha = 0.1f) else colors.cardBackground,
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(colors.accent)
        ) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(MiyoSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Selection indicator
            if (isSelecting) {
                Icon(
                    if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) colors.accent else colors.secondaryText.copy(alpha = 0.4f),
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = MiyoSpacing.extraSmall),
                )
            }

            // Cover placeholder
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = colors.accent.copy(alpha = 0.12f),
                modifier = Modifier.size(48.dp, 68.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.width(MiyoSpacing.medium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.onBackground,
                )
                Text(book.author, style = MaterialTheme.typography.bodySmall, color = colors.secondaryText)

                Spacer(Modifier.height(MiyoSpacing.small))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { book.progress / 100f },
                        modifier = Modifier
                            .width(50.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = colors.accent,
                        trackColor = colors.secondaryText.copy(alpha = 0.15f),
                    )
                    Spacer(Modifier.width(MiyoSpacing.small))
                    Text("${book.progress.toInt()}%", style = MaterialTheme.typography.labelSmall, color = colors.secondaryText)

                    Spacer(Modifier.weight(1f))

                    Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        formatRelativeTime(book.lastReadAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.secondaryText,
                    )
                }
            }

            if (!isSelecting) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun formatRelativeTime(isoString: String?): String {
    if (isoString == null) return ""
    return try {
        val readAt = Instant.parse(isoString)
        val now = Instant.now()
        val minutes = ChronoUnit.MINUTES.between(readAt, now)
        val hours = ChronoUnit.HOURS.between(readAt, now)
        val days = ChronoUnit.DAYS.between(readAt, now)
        when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            days < 7 -> "${days}d ago"
            else -> "${days / 7}w ago"
        }
    } catch (_: Exception) {
        ""
    }
}
