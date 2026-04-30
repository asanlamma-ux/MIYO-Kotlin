package com.miyu.reader.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miyu.reader.domain.model.*
import com.miyu.reader.ui.theme.LocalMIYUColors
import com.miyu.reader.ui.theme.ReaderColors
import com.miyu.reader.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalMIYUColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        // ── Header ──────────────────────────────────────────────────
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            color = colors.onBackground,
        )

        // ── Account ─────────────────────────────────────────────────
        SettingsSection(title = "Account") {
            SettingsRow(
                icon = Icons.Outlined.Login,
                title = "Sign In / Sign Up",
                subtitle = "Unlock auto-translation features",
                onClick = { /* TODO: Auth flow */ },
                accentColor = colors.accent,
            )
        }

        // ── Translations ────────────────────────────────────────────
        SettingsSection(title = "Translations") {
            SettingsRow(
                icon = Icons.Outlined.Translate,
                title = "Auto Translation Mode",
                subtitle = "Off — translate only when you tap Translate",
                onClick = { /* TODO: provider configuration */ },
                accentColor = colors.accent,
                trailing = { Text("Off", color = colors.secondaryText) },
            )
            SettingsRow(
                icon = Icons.Outlined.Translate,
                title = "Translation Language",
                subtitle = "Target language for chapter auto-translation",
                onClick = { /* TODO: language picker */ },
                accentColor = colors.accent,
                trailing = { Text("English", color = colors.secondaryText) },
            )
        }

        // ── Daily Goal ──────────────────────────────────────────────
        SettingsSection(title = "Daily Goal") {
            Text(
                "Minutes to aim for each day. Progress appears on the Home tab using logged reading time.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryText,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            )
            DailyGoalButtons(selectedMinutes = 30, accentColor = colors.accent)
        }

        // ── Appearance ──────────────────────────────────────────────
        SettingsSection(title = "Appearance") {
            // App theme mode
            SettingsRow(
                icon = Icons.Outlined.DarkMode,
                title = "App Theme",
                subtitle = if (uiState.themeMode == ThemeMode.DARK) "Dark mode" else "Light mode",
                onClick = {
                    viewModel.setThemeMode(
                        if (uiState.themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
                    )
                },
                accentColor = colors.accent,
                trailing = {
                    Text(
                        uiState.themeMode.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.secondaryText,
                    )
                },
            )
        }

        // ── Reader Themes ───────────────────────────────────────────
        SettingsSection(title = "Reader Theme") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(ReaderColors.allThemes) { theme ->
                    val isSelected = theme.id == uiState.readerThemeId
                    ThemePreviewCard(
                        theme = theme,
                        isSelected = isSelected,
                        onClick = { viewModel.setReaderThemeId(theme.id) },
                    )
                }
            }
        }

        // ── Typography ──────────────────────────────────────────────
        SettingsSection(title = "Typography") {
            // Font size
            SettingsRow(
                icon = Icons.Outlined.TextFields,
                title = "Font Size",
                subtitle = "${uiState.typography.fontSize.toInt()}px",
                accentColor = colors.accent,
            )
            Slider(
                value = uiState.typography.fontSize,
                onValueChange = viewModel::setFontSize,
                valueRange = 12f..28f,
                steps = 15,
                modifier = Modifier.padding(horizontal = 20.dp),
                colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent),
            )

            // Line height
            SettingsRow(
                icon = Icons.Outlined.FormatLineSpacing,
                title = "Line Height",
                subtitle = "%.1f".format(uiState.typography.lineHeight),
                accentColor = colors.accent,
            )
            Slider(
                value = uiState.typography.lineHeight,
                onValueChange = viewModel::setLineHeight,
                valueRange = 1.2f..2.0f,
                steps = 7,
                modifier = Modifier.padding(horizontal = 20.dp),
                colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent),
            )

            // Text alignment
            SettingsRow(
                icon = Icons.Outlined.FormatAlignLeft,
                title = "Text Alignment",
                subtitle = uiState.typography.textAlign.name.lowercase().replaceFirstChar { it.uppercase() },
                onClick = {
                    viewModel.setTextAlign(
                        if (uiState.typography.textAlign == TextAlign.LEFT) TextAlign.JUSTIFY else TextAlign.LEFT
                    )
                },
                accentColor = colors.accent,
            )
        }

        // ── Reading ─────────────────────────────────────────────────
        SettingsSection(title = "Reading") {
            SettingsRow(
                icon = Icons.Outlined.TextFields,
                title = "Typography",
                subtitle = "System Default, ${uiState.typography.fontSize.toInt()}px",
                onClick = { /* Controls below are already live */ },
                accentColor = colors.accent,
            )
            SettingsRow(
                icon = Icons.Outlined.Animation,
                title = "Page Animation",
                subtitle = uiState.readingSettings.pageAnimation.name.lowercase().replaceFirstChar { it.uppercase() },
                onClick = {
                    val options = PageAnimation.entries
                    val idx = options.indexOf(uiState.readingSettings.pageAnimation)
                    viewModel.setPageAnimation(options[(idx + 1) % options.size])
                },
                accentColor = colors.accent,
            )
            SettingsToggle(
                icon = Icons.Outlined.TouchApp,
                title = "Tap Zones",
                subtitle = "Enable left/right edge taps in the reader",
                checked = uiState.readingSettings.tapZonesEnabled,
                onCheckedChange = viewModel::setTapZonesEnabled,
                accentColor = colors.accent,
            )
            SettingsRow(
                icon = Icons.Outlined.Tune,
                title = "Side tap & swipe",
                subtitle = if (uiState.readingSettings.tapZoneNavMode == TapZoneNavMode.CHAPTER) {
                    "Turn chapters at the edges"
                } else {
                    "Scroll within chapter (default)"
                },
                onClick = {
                    viewModel.setTapZoneNavMode(
                        if (uiState.readingSettings.tapZoneNavMode == TapZoneNavMode.SCROLL) {
                            TapZoneNavMode.CHAPTER
                        } else {
                            TapZoneNavMode.SCROLL
                        },
                    )
                },
                accentColor = colors.accent,
            )
            SettingsRow(
                icon = Icons.Outlined.Timer,
                title = "Sleep timer",
                subtitle = "Off",
                onClick = { /* TODO: sleep timer */ },
                accentColor = colors.accent,
            )
            SettingsToggle(
                icon = Icons.Outlined.Download,
                title = "Continuous chapter loading",
                subtitle = "Append the next chapter in the same reading space when you reach the end",
                checked = true,
                onCheckedChange = { /* TODO: continuous reader pipeline */ },
                accentColor = colors.accent,
            )
            SettingsToggle(
                icon = Icons.Outlined.VolumeUp,
                title = "Volume Button Navigation",
                subtitle = "Use volume buttons to turn pages",
                checked = uiState.readingSettings.volumeButtonPageTurn,
                onCheckedChange = viewModel::setVolumeButtonPageTurn,
                accentColor = colors.accent,
            )
            SettingsToggle(
                icon = Icons.Outlined.Tune,
                title = "Bionic reading",
                subtitle = "Emphasize word beginnings in the reader",
                checked = false,
                onCheckedChange = { /* TODO: bionic renderer */ },
                accentColor = colors.accent,
            )
            SettingsToggle(
                icon = Icons.Outlined.PhoneAndroid,
                title = "Keep screen on",
                subtitle = "While a book is open in the reader",
                checked = true,
                onCheckedChange = { /* TODO: window flag integration */ },
                accentColor = colors.accent,
            )
        }

        // ── Display ─────────────────────────────────────────────────
        SettingsSection(title = "Display") {
            SettingsToggle(
                icon = Icons.Outlined.Fullscreen,
                title = "Immersive Mode",
                subtitle = "Hide status and navigation bars",
                checked = uiState.readingSettings.immersiveMode,
                onCheckedChange = viewModel::setImmersiveMode,
                accentColor = colors.accent,
            )
            SettingsRow(
                icon = Icons.Outlined.FormatIndentIncrease,
                title = "Margins",
                subtitle = uiState.readingSettings.marginPreset.name.lowercase().replaceFirstChar { it.uppercase() },
                onClick = {
                    val options = MarginPreset.entries
                    val idx = options.indexOf(uiState.readingSettings.marginPreset)
                    viewModel.setMarginPreset(options[(idx + 1) % options.size])
                },
                accentColor = colors.accent,
            )
        }

        // ── Cloud Sync & AI ─────────────────────────────────────────
        SettingsSection(title = "AI & Secrets") {
            SettingsRow(
                icon = Icons.Outlined.AutoAwesome,
                title = "AI Provider Configuration",
                subtitle = "Configure OpenAI, Claude, Gemini, or custom endpoints",
                onClick = { /* TODO: AI provider screen */ },
                accentColor = colors.accent,
            )
        }

        SettingsSection(title = "Storage") {
            SettingsRow(
                icon = Icons.Outlined.Folder,
                title = "Storage Location",
                subtitle = "Select the folder used for new imports and watched-folder rescans",
                onClick = { /* TODO: SAF folder picker */ },
                accentColor = colors.accent,
                trailing = { Text("Internal storage/MIYO", color = colors.secondaryText) },
            )
            SettingsRow(
                icon = Icons.Outlined.SdStorage,
                title = "Library Size",
                subtitle = "${uiState.bookCount} book(s) on disk",
                accentColor = colors.accent,
            )
            SettingsRow(
                icon = Icons.Outlined.FolderOpen,
                title = "Watched Folder",
                subtitle = "Rescan imports new EPUBs from this folder without copying them again",
                onClick = { /* TODO: watched folder rescan */ },
                accentColor = colors.accent,
                trailing = { Text("Active", color = colors.secondaryText) },
            )
            SettingsRow(
                icon = Icons.Outlined.DeleteSweep,
                title = "Clear Cache",
                subtitle = "Remove temporary reading data",
                onClick = { /* TODO: clear cache */ },
                accentColor = Color(0xFFEF4444),
            )
        }

        SettingsSection(title = "Cloud Sync & Backup") {
            SettingsRow(
                icon = Icons.Outlined.Cloud,
                title = "Connect Google Drive",
                subtitle = "Back up reading data to your Google account",
                onClick = { /* TODO: Google Drive sync */ },
                accentColor = colors.accent,
            )
            SettingsRow(
                icon = Icons.Outlined.Storage,
                title = "Connect WebDAV Server",
                subtitle = "Use your own server (Nextcloud, ownCloud, etc.)",
                onClick = { /* TODO: WebDAV sync */ },
                accentColor = colors.accent,
            )
        }

        SettingsSection(title = "Permissions") {
            SettingsRow(
                icon = Icons.Outlined.Security,
                title = "Storage Permission",
                subtitle = "Required to import and read EPUB files",
                onClick = { /* TODO: permission manager */ },
                accentColor = colors.accent,
            )
            SettingsRow(
                icon = Icons.Outlined.Folder,
                title = "Open App Settings",
                subtitle = "Manage app permissions",
                onClick = { /* TODO: launch app settings */ },
                accentColor = colors.accent,
            )
        }

        SettingsSection(title = "Language Tools") {
            SettingsRow(
                icon = Icons.Outlined.MenuBook,
                title = "Dictionary Library",
                subtitle = "Download offline dictionaries and manage lookup packages",
                onClick = { /* TODO: dictionary packages */ },
                accentColor = colors.accent,
            )
        }

        // ── Advanced ────────────────────────────────────────────────
        SettingsSection(title = "Advanced") {
            SettingsToggle(
                icon = Icons.Outlined.ZoomInMap,
                title = "Reduced Motion",
                subtitle = "Minimize animations",
                checked = uiState.readingSettings.reducedMotion,
                onCheckedChange = viewModel::setReducedMotion,
                accentColor = colors.accent,
            )
            SettingsRow(
                icon = Icons.Outlined.Sync,
                title = "Rescan Library",
                subtitle = "Find missing or new books",
                onClick = { /* TODO: library rescan */ },
                accentColor = colors.accent,
            )
            SettingsRow(
                icon = Icons.Outlined.Storage,
                title = "Duplicate Audit",
                subtitle = "Detect exact and probable duplicate book entries",
                onClick = { /* TODO: duplicate audit */ },
                accentColor = colors.accent,
            )
            SettingsRow(
                icon = Icons.Outlined.RestartAlt,
                title = "Reset All Preferences",
                subtitle = "Restore defaults for reading and display",
                onClick = viewModel::resetToDefaults,
                accentColor = Color(0xFFEF4444),
            )
        }

        // ── About ───────────────────────────────────────────────────
        SettingsSection(title = "About") {
            SettingsRow(
                icon = Icons.Outlined.Info,
                title = "MIYU Reader",
                subtitle = "Version 1.0.0 · Built with ❤️",
                accentColor = colors.accent,
            )
        }
    }
}

// ── Section header ──────────────────────────────────────────────────
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalMIYUColors.current
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            ),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            color = colors.secondaryText,
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column { content() }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyGoalButtons(
    selectedMinutes: Int,
    accentColor: Color,
) {
    val options = listOf(15, 30, 45, 60, 90, 120)
    FlowRow(
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        options.forEach { minutes ->
            val selected = minutes == selectedMinutes
            OutlinedButton(
                onClick = { /* TODO: persist daily goal */ },
                shape = RoundedCornerShape(14.dp),
                colors = if (selected) {
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = accentColor,
                        contentColor = Color.White,
                    )
                } else {
                    ButtonDefaults.outlinedButtonColors()
                },
                modifier = Modifier.width(92.dp),
            ) {
                Text("$minutes min", fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

// ── Row ─────────────────────────────────────────────────────────────
@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    accentColor: Color = LocalMIYUColors.current.accent,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = LocalMIYUColors.current
    ListItem(
        headlineContent = {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.onBackground)
        },
        supportingContent = {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.secondaryText)
        },
        leadingContent = {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
        },
        trailingContent = trailing ?: {
            if (onClick != null) Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(18.dp))
        },
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
}

// ── Toggle row ──────────────────────────────────────────────────────
@Composable
private fun SettingsToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color = LocalMIYUColors.current.accent,
) {
    val colors = LocalMIYUColors.current
    ListItem(
        headlineContent = {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = colors.onBackground)
        },
        supportingContent = {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.secondaryText)
        },
        leadingContent = {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedTrackColor = accentColor),
            )
        },
    )
}

// ── Theme preview card ──────────────────────────────────────────────
@Composable
private fun ThemePreviewCard(
    theme: com.miyu.reader.ui.theme.ReaderThemeColors,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(2.dp, theme.accent, RoundedCornerShape(12.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = theme.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            // Preview area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .background(theme.background)
                    .padding(10.dp),
            ) {
                Column {
                    // Simulated text lines
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
                            .fillMaxWidth(0.8f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(theme.text.copy(alpha = 0.35f)),
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(20.dp)
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
                Text(
                    theme.name,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    ),
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(theme.accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp),
                        )
                    }
                }
            }
            // Color swatches
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                listOf(theme.background, theme.text, theme.accent).forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
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
