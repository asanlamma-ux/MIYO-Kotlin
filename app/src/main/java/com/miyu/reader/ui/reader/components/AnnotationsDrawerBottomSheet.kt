package com.miyu.reader.ui.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Note
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miyu.reader.domain.model.Bookmark
import com.miyu.reader.domain.model.Highlight
import com.miyu.reader.ui.core.theme.MiyoSpacing
import com.miyu.reader.ui.theme.ReaderThemeColors
import java.time.Instant
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnotationsDrawerBottomSheet(
    highlights: List<Highlight>,
    bookmarks: List<Bookmark>,
    readerTheme: ReaderThemeColors,
    onDismiss: () -> Unit,
    onHighlightClick: (Highlight) -> Unit,
    onBookmarkClick: (Bookmark) -> Unit,
    onDeleteHighlight: (String) -> Unit,
    onDeleteBookmark: (Bookmark) -> Unit,
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Highlights", "Bookmarks")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = readerTheme.cardBackground,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            // Header
            Text(
                "Annotations",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                color = readerTheme.text,
            )

            // Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = readerTheme.accent,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = readerTheme.accent
                    )
                },
                divider = {
                    Divider(color = readerTheme.secondaryText.copy(alpha = 0.2f))
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                title,
                                color = if (selectedTabIndex == index) readerTheme.accent else readerTheme.secondaryText,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            // Content
            if (selectedTabIndex == 0) {
                HighlightsList(
                    highlights = highlights,
                    readerTheme = readerTheme,
                    onClick = onHighlightClick,
                    onDelete = onDeleteHighlight,
                )
            } else {
                BookmarksList(
                    bookmarks = bookmarks,
                    readerTheme = readerTheme,
                    onClick = onBookmarkClick,
                    onDelete = onDeleteBookmark,
                )
            }
        }
    }
}

@Composable
private fun HighlightsList(
    highlights: List<Highlight>,
    readerTheme: ReaderThemeColors,
    onClick: (Highlight) -> Unit,
    onDelete: (String) -> Unit,
) {
    if (highlights.isEmpty()) {
        EmptyState("No highlights yet", Icons.Outlined.Circle, readerTheme)
        return
    }

    LazyColumn(contentPadding = PaddingValues(bottom = MiyoSpacing.extraLarge)) {
        items(highlights, key = { it.id }) { highlight ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(highlight) }
                    .padding(horizontal = MiyoSpacing.large, vertical = MiyoSpacing.medium)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(highlight.color)))
                    )
                    Spacer(Modifier.width(MiyoSpacing.medium))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            highlight.text,
                            color = readerTheme.text,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        )
                        if (!highlight.note.isNullOrBlank()) {
                            Spacer(Modifier.height(MiyoSpacing.small))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(readerTheme.secondaryText.copy(alpha = 0.08f))
                                    .padding(12.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Note,
                                    contentDescription = null,
                                    tint = readerTheme.secondaryText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(MiyoSpacing.small))
                                Text(
                                    highlight.note,
                                    color = readerTheme.secondaryText,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                        Spacer(Modifier.height(MiyoSpacing.small))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Chapter ${highlight.chapterIndex + 1} • ${formatRelativeTime(highlight.createdAt)}",
                                color = readerTheme.secondaryText.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                            )
                            IconButton(
                                onClick = { onDelete(highlight.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = readerTheme.secondaryText.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
            Divider(color = readerTheme.secondaryText.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = MiyoSpacing.large))
        }
    }
}

@Composable
private fun BookmarksList(
    bookmarks: List<Bookmark>,
    readerTheme: ReaderThemeColors,
    onClick: (Bookmark) -> Unit,
    onDelete: (Bookmark) -> Unit,
) {
    if (bookmarks.isEmpty()) {
        EmptyState("No bookmarks yet", Icons.Outlined.Bookmark, readerTheme)
        return
    }

    LazyColumn(contentPadding = PaddingValues(bottom = MiyoSpacing.extraLarge)) {
        items(bookmarks, key = { it.id }) { bookmark ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(bookmark) }
                    .padding(horizontal = MiyoSpacing.large, vertical = MiyoSpacing.medium)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Outlined.Bookmark,
                        contentDescription = null,
                        tint = readerTheme.accent,
                        modifier = Modifier.padding(top = 2.dp).size(18.dp)
                    )
                    Spacer(Modifier.width(MiyoSpacing.medium))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (bookmark.text.isNotBlank()) bookmark.text else "Bookmark at Chapter ${bookmark.chapterIndex + 1}",
                            color = readerTheme.text,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(MiyoSpacing.small))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Chapter ${bookmark.chapterIndex + 1} • ${formatRelativeTime(bookmark.createdAt)}",
                                color = readerTheme.secondaryText.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                            )
                            IconButton(
                                onClick = { onDelete(bookmark) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = readerTheme.secondaryText.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
            Divider(color = readerTheme.secondaryText.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = MiyoSpacing.large))
        }
    }
}

@Composable
private fun EmptyState(
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    readerTheme: ReaderThemeColors
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                tint = readerTheme.secondaryText.copy(alpha = 0.3f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(MiyoSpacing.medium))
            Text(
                message,
                color = readerTheme.secondaryText,
                fontSize = 15.sp,
            )
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
            else -> "${days}d ago"
        }
    } catch (_: Exception) {
        ""
    }
}
