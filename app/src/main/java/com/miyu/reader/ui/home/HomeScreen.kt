package com.miyu.reader.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miyu.reader.domain.model.Book
import com.miyu.reader.ui.components.BookCover
import com.miyu.reader.ui.core.components.MiyoEmptyScreen
import com.miyu.reader.ui.core.components.MiyoIconActionButton
import com.miyu.reader.ui.core.components.MiyoScreenHeader
import com.miyu.reader.ui.core.theme.MiyoSpacing
import com.miyu.reader.ui.theme.LocalMIYUColors
import com.miyu.reader.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenBook: (String) -> Unit = {},
    onOpenThemePicker: () -> Unit = {},
) {
    val books: List<Book> by viewModel.recentBooks.collectAsStateWithLifecycle(initialValue = emptyList())
    val colors = LocalMIYUColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item {
            MiyoScreenHeader(
                title = "Miyo",
                eyebrow = "Good Morning",
                subtitle = if (books.isEmpty()) {
                    "Import a book to start reading"
                } else {
                    "${books.size} book${if (books.size != 1) "s" else ""} in your library"
                },
            ) {
                MiyoIconActionButton(
                    icon = Icons.Outlined.Palette,
                    contentDescription = "Themes",
                    onClick = onOpenThemePicker,
                )
            }
        }

        // ── Continue reading carousel ───────────────────────────────
        val readingBooks = books.filter { it.progress > 0f && it.progress < 100f }
        if (readingBooks.isNotEmpty()) {
            item {
                Text(
                    "Continue Reading",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(horizontal = MiyoSpacing.large, vertical = MiyoSpacing.small),
                    color = colors.onBackground,
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = MiyoSpacing.large),
                    horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.medium),
                ) {
                    items(readingBooks) { book ->
                        ContinueReadingCard(book = book, onClick = { onOpenBook(book.id) }, colors = colors)
                    }
                }
                Spacer(Modifier.height(MiyoSpacing.medium))
            }
        }

        // ── Empty state ─────────────────────────────────────────────
        if (books.isEmpty()) {
            item {
                MiyoEmptyScreen(
                    icon = Icons.Default.MenuBook,
                    title = "No Books Yet",
                    message = "Go to the Library tab to import your first EPUB.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 60.dp),
                )
            }
        }

        // ── Recent books list ───────────────────────────────────────
        if (books.isNotEmpty()) {
            item {
                Text(
                    "All Books",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(horizontal = MiyoSpacing.large, vertical = MiyoSpacing.small),
                    color = colors.onBackground,
                )
            }
            items(books) { book ->
                BookCard(book = book, onClick = { onOpenBook(book.id) }, colors = colors)
            }
        }
    }
}

@Composable
private fun ContinueReadingCard(
    book: Book,
    onClick: () -> Unit,
    colors: com.miyu.reader.ui.theme.MIYUColors,
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(MiyoSpacing.medium)) {
            BookCover(book = book, modifier = Modifier.fillMaxWidth().height(96.dp))
            Spacer(Modifier.height(MiyoSpacing.small))
            Text(
                book.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = colors.onBackground,
            )
            Spacer(Modifier.height(MiyoSpacing.extraSmall))
            Text(
                book.author,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = colors.secondaryText,
            )
            Spacer(Modifier.height(MiyoSpacing.small))
            LinearProgressIndicator(
                progress = { book.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = colors.accent,
                trackColor = colors.secondaryText.copy(alpha = 0.15f),
            )
            Spacer(Modifier.height(MiyoSpacing.extraSmall))
            Text(
                "${book.progress.toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = colors.secondaryText,
            )
        }
    }
}

@Composable
private fun BookCard(
    book: Book,
    onClick: () -> Unit,
    colors: com.miyu.reader.ui.theme.MIYUColors,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MiyoSpacing.large, vertical = MiyoSpacing.extraSmall)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(MiyoSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookCover(book = book, modifier = Modifier.size(48.dp, 64.dp))
            Spacer(Modifier.width(MiyoSpacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.onBackground,
                )
                Spacer(Modifier.height(MiyoSpacing.extraSmall))
                Text(
                    book.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryText,
                )
                if (book.progress > 0f) {
                    Spacer(Modifier.height(MiyoSpacing.small))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { book.progress / 100f },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = colors.accent,
                            trackColor = colors.secondaryText.copy(alpha = 0.15f),
                        )
                        Spacer(Modifier.width(MiyoSpacing.small))
                        Text(
                            "${book.progress.toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.secondaryText,
                        )
                    }
                }
            }
        }
    }
}
