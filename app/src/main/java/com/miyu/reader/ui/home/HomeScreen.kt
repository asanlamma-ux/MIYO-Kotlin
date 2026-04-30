package com.miyu.reader.ui.home

<<<<<<< HEAD
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.miyu.reader.domain.model.Book
import com.miyu.reader.ui.theme.LocalMIYUColors
=======
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miyu.reader.domain.model.Book
import com.miyu.reader.ui.theme.LocalMIYUColors
import com.miyu.reader.viewmodel.HomeViewModel
>>>>>>> debug

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
<<<<<<< HEAD
=======
    onOpenBook: (String) -> Unit = {},
>>>>>>> debug
) {
    val books: List<Book> by viewModel.recentBooks.collectAsStateWithLifecycle(initialValue = emptyList())
    val colors = LocalMIYUColors.current

<<<<<<< HEAD
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (books.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No books yet. Import an EPUB to get started.", color = colors.secondaryText)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(books) { book ->
                    BookCard(book = book)
                }
=======
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        // ── Header ──────────────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = "Welcome back 👋",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (books.isEmpty()) "Import a book to start reading"
                    else "${books.size} book${if (books.size != 1) "s" else ""} in your library",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.secondaryText,
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
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    color = colors.onBackground,
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(readingBooks) { book ->
                        ContinueReadingCard(book = book, onClick = { onOpenBook(book.id) }, colors = colors)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // ── Empty state ─────────────────────────────────────────────
        if (books.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 60.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = colors.accent.copy(alpha = 0.12f),
                            modifier = Modifier.size(100.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(48.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "No Books Yet",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.onBackground,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Go to the Library tab to import your first EPUB.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.secondaryText,
                        )
                    }
                }
            }
        }

        // ── Recent books list ───────────────────────────────────────
        if (books.isNotEmpty()) {
            item {
                Text(
                    "All Books",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    color = colors.onBackground,
                )
            }
            items(books) { book ->
                BookCard(book = book, onClick = { onOpenBook(book.id) }, colors = colors)
>>>>>>> debug
            }
        }
    }
}

@Composable
<<<<<<< HEAD
private fun BookCard(book: Book) {
    val colors = LocalMIYUColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text(book.author, style = MaterialTheme.typography.bodyMedium, color = colors.secondaryText)
                if (book.progress > 0f) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { book.progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
=======
private fun ContinueReadingCard(
    book: Book,
    onClick: () -> Unit,
    colors: com.miyu.reader.ui.theme.MIYUColors,
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                Icons.Default.Book,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                book.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = colors.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                book.author,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = colors.secondaryText,
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { book.progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = colors.accent,
                trackColor = colors.secondaryText.copy(alpha = 0.15f),
            )
            Spacer(Modifier.height(4.dp))
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
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Placeholder cover icon
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.accent.copy(alpha = 0.12f),
                modifier = Modifier.size(48.dp, 64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Book,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.onBackground,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    book.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryText,
                )
                if (book.progress > 0f) {
                    Spacer(Modifier.height(8.dp))
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
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${book.progress.toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.secondaryText,
                        )
                    }
                }
            }
>>>>>>> debug
        }
    }
}