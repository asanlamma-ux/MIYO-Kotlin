package com.miyu.reader.ui.library

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.miyu.reader.domain.model.Book
import com.miyu.reader.domain.model.ReadingStatus
import com.miyu.reader.ui.components.BookCover
import com.miyu.reader.ui.core.components.MiyoSectionCard
import com.miyu.reader.ui.core.theme.MiyoSpacing
import com.miyu.reader.ui.theme.LocalMIYUColors
import com.miyu.reader.viewmodel.BookDetailsChapter
import com.miyu.reader.viewmodel.BookDetailsViewModel
import java.io.File

@Composable
fun BookDetailsScreen(
    onBack: () -> Unit,
    onOpenReader: (String, Int?) -> Unit,
    viewModel: BookDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalMIYUColors.current
    val context = LocalContext.current

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        viewModel.updateBookCover(uri)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        state.book?.coverUri?.let { coverUri ->
            AsyncImage(
                model = coverUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(34.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to colors.background.copy(alpha = 0.54f),
                        0.2f to colors.background.copy(alpha = 0.84f),
                        1f to colors.background.copy(alpha = 0.97f),
                    ),
                ),
        )

        LibraryWorkspaceSurface {
            if (state.isLoading && state.book == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.accent)
                }
                return@LibraryWorkspaceSurface
            }

            WorkspaceExitButton(label = "Exit book details", onClick = onBack)

            state.book?.let { book ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(MiyoSpacing.medium),
                ) {
                    item {
                        BookHeroCard(
                            book = book,
                            fileSizeBytes = state.fileSizeBytes,
                            language = state.language,
                            onRead = { onOpenReader(book.id, null) },
                            onChangeCover = { coverPickerLauncher.launch(arrayOf("image/*")) },
                            onDelete = {
                                viewModel.deleteBook()
                                onBack()
                            },
                        )
                    }
                    item {
                        ReadingStatusRow(
                            activeStatus = book.readingStatus,
                            onSelect = viewModel::setReadingStatus,
                        )
                    }
                    item {
                        DetailSectionCard(title = "Overview") {
                            val description = state.description.ifBlank {
                                "No publisher description was embedded in this EPUB."
                            }
                            Text(
                                description,
                                color = colors.onBackground,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            MetadataLine("Publisher", state.publisher ?: "Unknown")
                            MetadataLine("Published", state.publishDate ?: "Unknown")
                            MetadataLine("Storage", File(book.filePath).parentFile?.name ?: "Miyo")
                            MetadataLine("Progress", "${book.progress.toInt()}% · Chapter ${book.currentChapter + 1}")
                        }
                    }
                    if (state.subjects.isNotEmpty()) {
                        item {
                            DetailSectionCard(title = "Subjects") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    state.subjects.take(6).forEach { subject ->
                                        AssistChip(onClick = {}, label = { Text(subject) }, enabled = false)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        DetailSectionCard(title = "Table of Contents") {
                            if (state.chapters.isEmpty()) {
                                Text(
                                    "No table of contents could be extracted from this EPUB.",
                                    color = colors.secondaryText,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    state.chapters.forEach { chapter ->
                                        TocRow(
                                            chapter = chapter,
                                            onClick = { onOpenReader(book.id, chapter.index) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    state.errorMessage?.let { message ->
                        item {
                            DetailSectionCard(title = "Issue") {
                                Text(
                                    message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookHeroCard(
    book: Book,
    fileSizeBytes: Long,
    language: String?,
    onRead: () -> Unit,
    onChangeCover: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    Card(
        shape = RoundedCornerShape(MiyoSpacing.extraLarge),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground.copy(alpha = 0.92f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(MiyoSpacing.medium)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.medium),
                verticalAlignment = Alignment.Top,
            ) {
                BookCover(
                    book = book,
                    modifier = Modifier.size(width = 112.dp, height = 158.dp),
                    cornerRadius = 18,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        book.title,
                        color = colors.onBackground,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(MiyoSpacing.small))
                    Text(
                        book.author.ifBlank { "Unknown Author" },
                        color = colors.secondaryText,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(MiyoSpacing.medium))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.small),
            ) {
                BookMetaChip("${book.totalChapters} chapters")
                BookMetaChip(formatStorageBytes(fileSizeBytes))
                if (!language.isNullOrBlank()) {
                    BookMetaChip(language.uppercase())
                }
            }
            Spacer(Modifier.height(MiyoSpacing.medium))
            BookProgressSnapshot(book = book)
            Spacer(Modifier.height(MiyoSpacing.medium))
            Button(
                onClick = onRead,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                Spacer(Modifier.size(MiyoSpacing.small))
                Text("Continue", maxLines = 1, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(MiyoSpacing.small))
            OutlinedButton(
                onClick = onChangeCover,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Outlined.Image, contentDescription = null)
                Spacer(Modifier.size(MiyoSpacing.small))
                Text("Change cover", maxLines = 1)
            }
            Spacer(Modifier.height(MiyoSpacing.small))
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                Spacer(Modifier.size(MiyoSpacing.small))
                Text("Delete book", maxLines = 1)
            }
        }
    }
}

@Composable
private fun BookProgressSnapshot(book: Book) {
    val colors = LocalMIYUColors.current
    val safeProgress = book.progress.coerceIn(0f, 100f)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.background.copy(alpha = 0.54f),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MiyoSpacing.medium, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Reading progress",
                    color = colors.secondaryText,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    "${safeProgress.toInt()}%",
                    color = colors.onBackground,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                )
            }
            Spacer(Modifier.height(MiyoSpacing.small))
            LinearProgressIndicator(
                progress = safeProgress / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = colors.accent,
                trackColor = colors.secondaryText.copy(alpha = 0.18f),
            )
            Spacer(Modifier.height(MiyoSpacing.small))
            Text(
                if (book.totalChapters > 0) {
                    "Chapter ${(book.currentChapter + 1).coerceAtMost(book.totalChapters)} of ${book.totalChapters}"
                } else {
                    "No chapter progress recorded yet"
                },
                color = colors.secondaryText,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun BookMetaChip(label: String) {
    val colors = LocalMIYUColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.background.copy(alpha = 0.62f),
        tonalElevation = 0.dp,
        modifier = Modifier.widthIn(max = 132.dp),
    ) {
        Text(
            label,
            color = colors.secondaryText,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun ReadingStatusRow(
    activeStatus: ReadingStatus,
    onSelect: (ReadingStatus) -> Unit,
) {
    val colors = LocalMIYUColors.current
    DetailSectionCard(title = "Reading Status") {
        Row(horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.small), modifier = Modifier.fillMaxWidth()) {
            ReadingStatus.entries.forEach { status ->
                FilterChip(
                    selected = activeStatus == status,
                    onClick = { onSelect(status) },
                    label = { Text(status.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }
        Spacer(Modifier.height(MiyoSpacing.small))
        Text(
            "Status updates here also change the library filters and progress grouping.",
            color = colors.secondaryText,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    MiyoSectionCard(title = title, content = content)
}

@Composable
private fun MetadataLine(label: String, value: String) {
    val colors = LocalMIYUColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = colors.secondaryText, style = MaterialTheme.typography.labelLarge)
        Text(
            value,
            color = colors.onBackground,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TocRow(
    chapter: BookDetailsChapter,
    onClick: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = colors.background.copy(alpha = 0.56f),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MiyoSpacing.medium, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MiyoSpacing.medium),
        ) {
            Surface(
                shape = CircleShape,
                color = colors.accent.copy(alpha = 0.14f),
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    chapter.title,
                    color = colors.onBackground,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (chapter.wordCount > 0) "${chapter.wordCount} words" else "Open chapter",
                    color = colors.secondaryText,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                (chapter.index + 1).toString(),
                color = colors.secondaryText,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private fun formatStorageBytes(size: Long): String = when {
    size >= 1024L * 1024L * 1024L -> String.format("%.1f GB", size / (1024f * 1024f * 1024f))
    size >= 1024L * 1024L -> String.format("%.1f MB", size / (1024f * 1024f))
    size >= 1024L -> String.format("%.1f KB", size / 1024f)
    else -> "$size B"
}
