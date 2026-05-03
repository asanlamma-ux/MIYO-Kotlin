package com.miyu.reader.ui.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miyu.reader.domain.model.*
import com.miyu.reader.ui.core.theme.MiyoSpacing
import com.miyu.reader.ui.theme.ReaderThemeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderLayoutPanelBottomSheet(
    settings: ReadingSettings,
    readerTheme: ReaderThemeColors,
    onSettingsChanged: (ReadingSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = readerTheme.cardBackground,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ViewColumn, contentDescription = null, tint = readerTheme.accent, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Layout & display",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = readerTheme.text,
                        )
                        Text(
                    "Reader controls",
                            color = readerTheme.secondaryText,
                            fontSize = 12.sp,
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = readerTheme.secondaryText)
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MiyoSpacing.large)
            ) {
                SectionHeader("READER MODE", Icons.Outlined.ViewColumn, readerTheme)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    ReaderMode.entries.forEach { mode ->
                        val active = settings.readerMode == mode
                        Chip(
                            label = when (mode) {
                                ReaderMode.SCROLL -> "Scroll"
                                ReaderMode.SINGLE -> "Single page"
                                ReaderMode.DOUBLE -> "Double page"
                            },
                            active = active,
                            readerTheme = readerTheme,
                            onClick = {
                                onSettingsChanged(
                                    settings.copy(
                                        readerMode = mode,
                                        readerColumnLayout = if (mode == ReaderMode.DOUBLE) {
                                            ReaderColumnLayout.TWO
                                        } else {
                                            ReaderColumnLayout.SINGLE
                                        },
                                    ),
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(MiyoSpacing.large))

                SectionHeader("FLOW", Icons.Outlined.Tune, readerTheme)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    PageAnimation.entries.forEach { animation ->
                        val active = settings.pageAnimation == animation
                        Chip(
                            label = animation.name.lowercase().replaceFirstChar { it.uppercase() },
                            active = active,
                            readerTheme = readerTheme,
                            onClick = { onSettingsChanged(settings.copy(pageAnimation = animation)) },
                        )
                    }
                }
                Spacer(Modifier.height(MiyoSpacing.medium))
                ToggleRow(
                    title = "Tap zones",
                    subtitle = "Use reader edges for page movement",
                    icon = Icons.Outlined.TouchApp,
                    active = settings.tapZonesEnabled,
                    readerTheme = readerTheme,
                    onToggle = { onSettingsChanged(settings.copy(tapZonesEnabled = !settings.tapZonesEnabled)) },
                )
                Spacer(Modifier.height(MiyoSpacing.small))
                ToggleRow(
                    title = "Chapter edge taps",
                    subtitle = "When off, side taps scroll within the chapter",
                    icon = Icons.Outlined.FormatIndentIncrease,
                    active = settings.tapZoneNavMode == TapZoneNavMode.CHAPTER,
                    readerTheme = readerTheme,
                    onToggle = {
                        onSettingsChanged(
                            settings.copy(
                                tapZoneNavMode = if (settings.tapZoneNavMode == TapZoneNavMode.CHAPTER) {
                                    TapZoneNavMode.SCROLL
                                } else {
                                    TapZoneNavMode.CHAPTER
                                },
                            ),
                        )
                    },
                )
                Spacer(Modifier.height(MiyoSpacing.small))
                ToggleRow(
                    title = "Continuous chapters",
                    subtitle = "Advance when the scroll reaches the end",
                    icon = Icons.Outlined.Sync,
                    active = settings.autoAdvanceChapter,
                    readerTheme = readerTheme,
                    onToggle = { onSettingsChanged(settings.copy(autoAdvanceChapter = !settings.autoAdvanceChapter)) },
                )
                Spacer(Modifier.height(MiyoSpacing.small))
                ToggleRow(
                    title = "Bionic reading",
                    subtitle = "Bold word openings for faster scanning",
                    icon = Icons.Outlined.TextFields,
                    active = settings.bionicReading,
                    readerTheme = readerTheme,
                    onToggle = { onSettingsChanged(settings.copy(bionicReading = !settings.bionicReading)) },
                )
                Spacer(Modifier.height(MiyoSpacing.large))

                SectionHeader("MARGINS", Icons.Outlined.AlignHorizontalCenter, readerTheme)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    MarginPreset.entries.forEach { preset ->
                        val active = settings.marginPreset == preset
                        Chip(
                            label = preset.name.lowercase().replaceFirstChar { it.uppercase() },
                            active = active,
                            readerTheme = readerTheme,
                            onClick = { onSettingsChanged(settings.copy(marginPreset = preset)) }
                        )
                    }
                }
                Spacer(Modifier.height(MiyoSpacing.large))

                SectionHeader("COLUMN WIDTH", Icons.Outlined.ViewColumn, readerTheme)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    listOf(null, 560, 640, 720, 840).forEach { width ->
                        val active = settings.contentColumnWidth == width
                        Chip(
                            label = width?.let { "${it}px" } ?: "Full",
                            active = active,
                            readerTheme = readerTheme,
                            onClick = { onSettingsChanged(settings.copy(contentColumnWidth = width)) },
                        )
                    }
                }
                Spacer(Modifier.height(MiyoSpacing.medium))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ReaderColumnLayout.entries.forEach { layout ->
                        val active = settings.readerColumnLayout == layout
                        Chip(
                            label = if (layout == ReaderColumnLayout.SINGLE) "Single" else "Two columns",
                            active = active,
                            readerTheme = readerTheme,
                            onClick = { onSettingsChanged(settings.copy(readerColumnLayout = layout)) },
                        )
                    }
                }
                Spacer(Modifier.height(MiyoSpacing.large))

                SectionHeader(
                    "READER CONTROLS",
                    Icons.Outlined.Tune,
                    readerTheme,
                    hint = "Reader chrome and selection behavior tuned for the current reading flow.",
                )
                ToggleRow(
                    title = "Hide top controls",
                    subtitle = "Keep the chapter header out of the reading space",
                    icon = Icons.Outlined.Fullscreen,
                    active = settings.hideReaderHeader,
                    readerTheme = readerTheme,
                    onToggle = { onSettingsChanged(settings.copy(hideReaderHeader = !settings.hideReaderHeader)) },
                )
                Spacer(Modifier.height(MiyoSpacing.small))
                ToggleRow(
                    title = "Hide bottom controls",
                    subtitle = "Hide progress and quick reader tools when controls open",
                    icon = Icons.Outlined.Fullscreen,
                    active = settings.hideReaderFooter,
                    readerTheme = readerTheme,
                    onToggle = { onSettingsChanged(settings.copy(hideReaderFooter = !settings.hideReaderFooter)) },
                )
                Spacer(Modifier.height(MiyoSpacing.small))
                ToggleRow(
                    title = "Lock reader navigation",
                    subtitle = "Disable edge taps and pull chapter changes",
                    icon = Icons.Outlined.TouchApp,
                    active = settings.readerNavLocked,
                    readerTheme = readerTheme,
                    onToggle = { onSettingsChanged(settings.copy(readerNavLocked = !settings.readerNavLocked)) },
                )
                Spacer(Modifier.height(MiyoSpacing.small))
                ToggleRow(
                    title = "Selection tools popup",
                    subtitle = "Show MIYO tools after text selection",
                    icon = Icons.Outlined.TextFields,
                    active = settings.selectionPopupEnabled,
                    readerTheme = readerTheme,
                    onToggle = { onSettingsChanged(settings.copy(selectionPopupEnabled = !settings.selectionPopupEnabled)) },
                )
                Spacer(Modifier.height(MiyoSpacing.small))
                ToggleRow(
                    title = "Show page border",
                    subtitle = "Add a subtle reading page frame",
                    icon = Icons.Outlined.FormatIndentIncrease,
                    active = settings.showPageBorder,
                    readerTheme = readerTheme,
                    onToggle = { onSettingsChanged(settings.copy(showPageBorder = !settings.showPageBorder)) },
                )
                Spacer(Modifier.height(MiyoSpacing.small))
                ToggleRow(
                    title = "Overwrite link style",
                    subtitle = "Normalize EPUB links to the reader palette",
                    icon = Icons.Outlined.MenuBook,
                    active = settings.overwriteLinkStyle,
                    readerTheme = readerTheme,
                    onToggle = { onSettingsChanged(settings.copy(overwriteLinkStyle = !settings.overwriteLinkStyle)) },
                )
                Spacer(Modifier.height(MiyoSpacing.small))
                ToggleRow(
                    title = "Overwrite text style",
                    subtitle = "Force imported chapter text to match typography settings",
                    icon = Icons.Outlined.TextFields,
                    active = settings.overwriteTextStyle,
                    readerTheme = readerTheme,
                    onToggle = { onSettingsChanged(settings.copy(overwriteTextStyle = !settings.overwriteTextStyle)) },
                )
                Spacer(Modifier.height(MiyoSpacing.large))
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    readerTheme: ReaderThemeColors,
    hint: String? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = MiyoSpacing.small)) {
        Icon(icon, contentDescription = null, tint = readerTheme.secondaryText, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(MiyoSpacing.small))
        Text(
            title,
            color = readerTheme.secondaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
        )
    }
    if (hint != null) {
        Text(
            hint,
            color = readerTheme.secondaryText.copy(alpha = 0.9f),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = MiyoSpacing.small)
        )
    }
}

@Composable
private fun Chip(
    label: String,
    active: Boolean,
    readerTheme: ReaderThemeColors,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) readerTheme.accent.copy(alpha = 0.15f) else readerTheme.background)
            .border(
                width = 1.5.dp,
                color = if (active) readerTheme.accent else readerTheme.secondaryText.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = MiyoSpacing.medium, vertical = 10.dp)
    ) {
        Text(
            label,
            color = if (active) readerTheme.accent else readerTheme.text,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    readerTheme: ReaderThemeColors,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(readerTheme.background)
            .border(
                width = 1.5.dp,
                color = if (active) readerTheme.accent else readerTheme.secondaryText.copy(alpha = 0.2f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onToggle)
            .padding(MiyoSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (active) readerTheme.accent else readerTheme.secondaryText,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(MiyoSpacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = readerTheme.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    subtitle,
                    color = readerTheme.secondaryText,
                    fontSize = 12.sp,
                )
            }
        }
        Spacer(Modifier.width(MiyoSpacing.medium))
        Switch(
            checked = active,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedTrackColor = readerTheme.accent,
                checkedThumbColor = readerTheme.background,
                uncheckedTrackColor = readerTheme.secondaryText.copy(alpha = 0.22f),
                uncheckedThumbColor = readerTheme.cardBackground,
            )
        )
    }
}
