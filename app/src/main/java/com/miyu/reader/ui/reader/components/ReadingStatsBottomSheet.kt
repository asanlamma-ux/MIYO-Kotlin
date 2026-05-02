package com.miyu.reader.ui.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import com.miyu.reader.domain.model.Book
import com.miyu.reader.ui.core.theme.MiyoSpacing
import com.miyu.reader.ui.theme.ReaderThemeColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingStatsBottomSheet(
    book: Book,
    chapterIndex: Int,
    totalChapters: Int,
    totalHighlights: Int,
    totalBookmarks: Int,
    readerTheme: ReaderThemeColors,
    onDismiss: () -> Unit,
) {
    val progress = if (totalChapters > 0) ((chapterIndex + 1).toFloat() / totalChapters) * 100f else 0f
    val estimatedWordsRead = (chapterIndex + 1) * 2000 // Mock: ~2k words per chapter
    val estimatedMinutes = estimatedWordsRead / 250 // Mock: 250 wpm
    val hoursRead = estimatedMinutes / 60
    val minutesRead = estimatedMinutes % 60

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = readerTheme.cardBackground,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(readerTheme.secondaryText.copy(alpha = 0.4f))
                    .align(Alignment.CenterHorizontally)
            )

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MiyoSpacing.large, vertical = MiyoSpacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Reading Stats",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = readerTheme.text,
                    )
                    Text(
                        book.title,
                        color = readerTheme.secondaryText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = readerTheme.secondaryText)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MiyoSpacing.large)
            ) {
                // Progress Bar
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = MiyoSpacing.large)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(readerTheme.secondaryText.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress / 100f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(readerTheme.accent)
                        )
                    }
                    Spacer(Modifier.height(MiyoSpacing.small))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Start", color = readerTheme.secondaryText, fontSize = 12.sp)
                        Text("${progress.toInt()}%", color = readerTheme.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("End", color = readerTheme.secondaryText, fontSize = 12.sp)
                    }
                }

                // Stats Grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.small)) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.TrendingUp,
                        iconColor = readerTheme.accent,
                        value = "${progress.toInt()}%",
                        label = "Reading Progress",
                        subValue = "${chapterIndex + 1} of $totalChapters chapters",
                        readerTheme = readerTheme
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Schedule,
                        iconColor = Color(0xFF3B82F6), // Blue
                        value = if (hoursRead > 0) "${hoursRead}h ${minutesRead}m" else "${minutesRead}m",
                        label = "Est. Reading Time",
                        subValue = "~$estimatedWordsRead words read",
                        readerTheme = readerTheme
                    )
                }
                Spacer(Modifier.height(MiyoSpacing.small))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.small)) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.FormatPaint,
                        iconColor = Color(0xFFF59E0B), // Amber
                        value = totalHighlights.toString(),
                        label = "Highlights",
                        subValue = if (totalHighlights == 1) "1 annotation" else "$totalHighlights annotations",
                        readerTheme = readerTheme
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.BookmarkBorder,
                        iconColor = Color(0xFF10B981), // Emerald
                        value = totalBookmarks.toString(),
                        label = "Bookmarks",
                        subValue = if (totalBookmarks == 1) "1 bookmark" else "$totalBookmarks bookmarks",
                        readerTheme = readerTheme
                    )
                }
                Spacer(Modifier.height(MiyoSpacing.large))

                // Info Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = readerTheme.secondaryText.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(MiyoSpacing.medium),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoItem("Status", book.readingStatus?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Reading", readerTheme)
                    InfoDivider(readerTheme)
                    InfoItem("Last Read", formatDate(book.lastReadAt), readerTheme)
                    InfoDivider(readerTheme)
                    InfoItem("Added", formatDate(book.dateAdded), readerTheme)
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    value: String,
    label: String,
    subValue: String,
    readerTheme: ReaderThemeColors
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(readerTheme.secondaryText.copy(alpha = 0.05f))
            .border(1.dp, readerTheme.secondaryText.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
            .padding(MiyoSpacing.medium)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(MiyoSpacing.small))
        Text(value, color = readerTheme.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
        Text(label, color = readerTheme.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp)
        Spacer(Modifier.height(MiyoSpacing.extraSmall))
        Text(subValue, color = readerTheme.secondaryText, fontSize = 11.sp, lineHeight = 15.sp, maxLines = 2)
    }
}

@Composable
private fun InfoItem(label: String, value: String, readerTheme: ReaderThemeColors) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = readerTheme.secondaryText, fontSize = 12.sp)
        Spacer(Modifier.height(MiyoSpacing.extraSmall))
        Text(value, color = readerTheme.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun InfoDivider(readerTheme: ReaderThemeColors) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(readerTheme.secondaryText.copy(alpha = 0.2f))
    )
}

private fun formatDate(dateStr: String?): String {
    if (dateStr == null) return "Never"
    return try {
        val instant = Instant.parse(dateStr)
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (_: Exception) {
        "Unknown"
    }
}
