package com.miyu.reader.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miyu.reader.R
import com.miyu.reader.ui.theme.DefaultReaderThemeId
import com.miyu.reader.ui.theme.LocalMIYUColors
import com.miyu.reader.ui.theme.ReaderColors
import com.miyu.reader.ui.theme.ReaderThemeColors
import com.miyu.reader.ui.theme.SpecialThemeAsset
import com.miyu.reader.ui.theme.SpecialThemePreviewArt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePickerBottomSheet(
    selectedThemeId: String,
    onThemeSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val shellColors = LocalMIYUColors.current
    val activeTheme = ReaderColors.shellThemeFor(selectedThemeId, shellColors.isDark)
    val normalThemes = ReaderColors.allThemes.filterNot { it.isSpecial }
    val specialThemes = ReaderColors.allThemes.filter { it.isSpecial }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = shellColors.background,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = shellColors.accent.copy(alpha = 0.12f),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Palette,
                            contentDescription = null,
                            tint = shellColors.accent,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Reader Theme",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = shellColors.onBackground,
                    )
                    Text(
                        "Choose the reading palette. App navigation stays separate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = shellColors.secondaryText,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", tint = shellColors.secondaryText)
                }
            }

            ActiveThemeCard(theme = activeTheme)

            Spacer(Modifier.height(18.dp))

            ThemeSection(
                title = "Normal",
                subtitle = "Calm everyday palettes for text-heavy reading.",
                themes = normalThemes,
                selectedThemeId = selectedThemeId,
                onThemeSelected = onThemeSelected,
            )

            Spacer(Modifier.height(18.dp))

            ThemeSection(
                title = "Special",
                subtitle = "RN art packs with themed UI, loading scenes, and ambient motion.",
                themes = specialThemes,
                selectedThemeId = selectedThemeId,
                onThemeSelected = onThemeSelected,
            )

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = { onThemeSelected(DefaultReaderThemeId) }) {
                Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Reset reader theme")
            }
        }
    }
}

@Composable
private fun ActiveThemeCard(theme: ReaderThemeColors) {
    val shellColors = LocalMIYUColors.current
    Surface(
        color = shellColors.cardBackground,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThemeMiniPreview(theme, modifier = Modifier.size(width = 94.dp, height = 118.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "ACTIVE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                    ),
                    color = theme.accent,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    theme.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = shellColors.onBackground,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        theme.isSpecial -> "Special art pack"
                        theme.isDark -> "Dark reader surface"
                        else -> "Light reader surface"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = shellColors.secondaryText,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(theme.background, theme.text, theme.accent).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(1.dp, shellColors.secondaryText.copy(alpha = 0.16f), CircleShape),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSection(
    title: String,
    subtitle: String,
    themes: List<ReaderThemeColors>,
    selectedThemeId: String,
    onThemeSelected: (String) -> Unit,
) {
    val shellColors = LocalMIYUColors.current
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
        ),
        color = shellColors.secondaryText,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = shellColors.secondaryText,
    )
    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        themes.forEach { theme ->
            ThemeCard(
                theme = theme,
                isSelected = theme.id == selectedThemeId,
                onClick = { onThemeSelected(theme.id) },
                modifier = Modifier.width(156.dp),
            )
        }
    }
}

@Composable
private fun ThemeMiniPreview(
    theme: ReaderThemeColors,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = theme.cardBackground,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.background)
                .padding(12.dp),
        ) {
            SpecialThemePreviewArt(
                theme = theme,
                modifier = Modifier.matchParentSize(),
            )
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(theme.text),
                )
                Spacer(Modifier.height(10.dp))
                repeat(2) { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (index == 0) 1f else 0.84f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(theme.text.copy(alpha = if (index == 0) 0.38f else 0.24f)),
                    )
                    Spacer(Modifier.height(6.dp))
                }
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
    modifier: Modifier = Modifier,
) {
    val shellColors = LocalMIYUColors.current
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(2.dp, theme.accent, RoundedCornerShape(14.dp))
                else Modifier.border(1.dp, shellColors.secondaryText.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = theme.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
    ) {
        Column {
            ThemeMiniPreview(
                theme = theme,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp),
            )
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (theme.isSpecial) {
                    Icon(
                        painter = painterResource(specialThemeIcon(theme.assetPack)),
                        contentDescription = null,
                        tint = theme.accent,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        theme.name,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        ),
                        color = theme.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (theme.isSpecial) "Special" else if (theme.isDark) "Dark" else "Light",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.secondaryText,
                    )
                }
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(theme.accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@DrawableRes
private fun specialThemeIcon(asset: SpecialThemeAsset): Int =
    when (asset) {
        SpecialThemeAsset.BLOSSOM -> R.drawable.ic_theme_blossom
        SpecialThemeAsset.COFFEE -> R.drawable.ic_theme_coffee
        SpecialThemeAsset.PARCHMENT -> R.drawable.ic_theme_parchment
        SpecialThemeAsset.MATCHA -> R.drawable.ic_theme_matcha
        SpecialThemeAsset.NONE -> R.drawable.ic_miyu_library
    }
