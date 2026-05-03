package com.miyu.reader.ui.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.miyu.reader.ui.core.theme.MiyoSpacing
import com.miyu.reader.ui.theme.LocalMIYUColors

@Composable
fun MiyoNovelDetailsScaffold(
    coverModel: Any?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalMIYUColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        if (coverModel != null) {
            AsyncImage(
                model = coverModel,
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
                        0.18f to colors.background.copy(alpha = 0.84f),
                        1f to colors.background.copy(alpha = 0.97f),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MiyoSpacing.medium, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.onBackground,
                    )
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }

            // Keep library and online details on the same shell so layout and
            // behavior changes land in one place instead of drifting apart.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                content = content,
            )
        }

        bottomBar()
    }
}

@Composable
fun MiyoNovelDetailsBottomDock(
    onOpenChapters: () -> Unit,
    primaryLabel: String,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
) {
    val colors = LocalMIYUColors.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        color = colors.cardBackground.copy(alpha = 0.96f),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FilledTonalIconButton(
                onClick = onOpenChapters,
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Outlined.FormatListBulleted, contentDescription = "Chapters")
            }
            Button(
                onClick = onPrimaryAction,
                enabled = primaryEnabled,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
            ) {
                Text(
                    primaryLabel,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}
