package com.miyu.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miyu.reader.ui.theme.ReaderColors
import com.miyu.reader.ui.theme.ReaderThemeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePickerBottomSheet(
    selectedThemeId: String,
    onThemeSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                "Reader Themes",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Choose a theme for the reading experience",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 130.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 400.dp),
            ) {
                items(ReaderColors.allThemes) { theme ->
                    ThemeCard(
                        theme = theme,
                        isSelected = theme.id == selectedThemeId,
                        onClick = { onThemeSelected(theme.id) },
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ThemeCard(
    theme: ReaderThemeColors,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(2.dp, theme.accent, RoundedCornerShape(10.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = theme.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            // Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .background(theme.background)
                    .padding(10.dp),
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(theme.text),
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(theme.text.copy(alpha = 0.5f)),
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(theme.text.copy(alpha = 0.35f)),
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(theme.accent),
                    )
                }
            }

            // Footer
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        theme.name,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        ),
                        maxLines = 1,
                    )
                    if (theme.isSpecial) {
                        Text(
                            "Special",
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.accent,
                        )
                    }
                }
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(theme.accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }

            // Swatches
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                listOf(theme.background, theme.text, theme.accent).forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(1.dp, Color.Black.copy(alpha = 0.1f), CircleShape),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
