package com.miyu.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.miyu.reader.domain.model.Book
import com.miyu.reader.ui.core.theme.MiyoSpacing

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
    val seed = (book.title.ifBlank { book.id }.hashCode() and Int.MAX_VALUE)
    val fallbackColor = fallbackCoverColors[seed % fallbackCoverColors.size]
    val inkColor = Color(0xFF4E5148).copy(alpha = 0.86f)

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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(fallbackColor),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.08f)
                        .height(1000.dp)
                        .background(inkColor.copy(alpha = 0.34f))
                        .align(Alignment.CenterStart),
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(MiyoSpacing.medium),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.78f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(inkColor.copy(alpha = 0.72f)),
                    )
                    Spacer(Modifier.height(7.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.58f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(50))
                            .background(inkColor.copy(alpha = 0.34f)),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        book.title,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = inkColor,
                        textAlign = TextAlign.Center,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
