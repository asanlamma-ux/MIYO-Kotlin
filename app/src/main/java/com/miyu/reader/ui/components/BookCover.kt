package com.miyu.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.miyu.reader.domain.model.Book

private val fallbackCoverColors = listOf(
    Color(0xFFE9E6DA),
    Color(0xFFDDE7E0),
    Color(0xFFE7E0D9),
    Color(0xFFDCE4EA),
    Color(0xFFE5E0E8),
    Color(0xFFE8E4D4),
)

@Composable
fun BookCover(
    book: Book,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 8,
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    val fallbackColor = fallbackCoverColors[(book.id.hashCode() and Int.MAX_VALUE) % fallbackCoverColors.size]

    Box(
        modifier = modifier
            .clip(shape)
            .background(fallbackColor),
        contentAlignment = Alignment.Center,
    ) {
        if (!book.coverUri.isNullOrBlank()) {
            AsyncImage(
                model = book.coverUri,
                contentDescription = book.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Outlined.MenuBook,
                contentDescription = null,
                tint = Color(0xFF59636A).copy(alpha = 0.72f),
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
