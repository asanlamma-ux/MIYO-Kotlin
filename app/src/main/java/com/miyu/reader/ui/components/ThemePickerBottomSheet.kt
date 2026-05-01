package com.miyu.reader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.*
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ThemePickerBottomSheet(
    selectedThemeId: String,
    onThemeSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectedTheme = ReaderColors.findById(selectedThemeId)
    val normalThemes = ReaderColors.allThemes.filterNot { it.isSpecial }
    val specialThemes = ReaderColors.allThemes.filter { it.isSpecial }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = selectedTheme.cardBackground,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                "Theme Selection",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = selectedTheme.text,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Split between Normal UI palettes and Special UI palettes from the RN app.",
                style = MaterialTheme.typography.bodySmall,
                color = selectedTheme.secondaryText,
            )
            Spacer(Modifier.height(16.dp))

            Surface(
                color = selectedTheme.background.copy(alpha = 0.82f),
                shape = RoundedCornerShape(18.dp),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "ACTIVE PROFILE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = selectedTheme.accent,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            selectedTheme.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = selectedTheme.text,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (selectedTheme.isSpecial) "Special palette with decorative styling hooks." else "Standard reading palette for everyday use.",
                            style = MaterialTheme.typography.bodySmall,
                            color = selectedTheme.secondaryText,
                        )
                    }
                    ThemeMiniPreview(selectedTheme)
                }
            }

            Spacer(Modifier.height(18.dp))

            ThemeSection(
                title = "Normal UI",
                subtitle = "Balanced reading palettes for the default Kotlin UI shell.",
                themes = normalThemes,
                selectedThemeId = selectedThemeId,
                onThemeSelected = onThemeSelected,
                sectionColor = selectedTheme,
            )
            Spacer(Modifier.height(18.dp))
            ThemeSection(
                title = "Special UI",
                subtitle = "Decorative reading palettes carried over from the RN theme set.",
                themes = specialThemes,
                selectedThemeId = selectedThemeId,
                onThemeSelected = onThemeSelected,
                sectionColor = selectedTheme,
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeSection(
    title: String,
    subtitle: String,
    themes: List<ReaderThemeColors>,
    selectedThemeId: String,
    onThemeSelected: (String) -> Unit,
    sectionColor: ReaderThemeColors,
) {
    Text(
        title,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = sectionColor.secondaryText,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = sectionColor.secondaryText,
    )
    Spacer(Modifier.height(12.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        themes.forEach { theme ->
            Box(modifier = Modifier.width(156.dp)) {
                ThemeCard(
                    theme = theme,
                    isSelected = theme.id == selectedThemeId,
                    onClick = { onThemeSelected(theme.id) },
                )
            }
        }
    }
}

@Composable
private fun ThemeMiniPreview(theme: ReaderThemeColors) {
    Surface(
        color = theme.cardBackground,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.size(width = 92.dp, height = 112.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.background)
                .padding(12.dp),
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(theme.text),
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(theme.text.copy(alpha = 0.38f)),
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.84f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(theme.text.copy(alpha = 0.24f)),
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .width(30.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(theme.accent),
                )
            }
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
