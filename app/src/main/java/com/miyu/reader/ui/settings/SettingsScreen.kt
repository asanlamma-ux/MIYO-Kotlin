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
                icon = Icons.Outlined.Person,
                title = "Sign In / Sign Up",
                subtitle = "Unlock cloud sync and auto-translation",
                onClick = { /* TODO: Auth flow */ },
                accentColor = colors.accent,
            )
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
                subtitle = "Enable left/right edge taps",
                checked = uiState.readingSettings.tapZonesEnabled,
                onCheckedChange = viewModel::setTapZonesEnabled,
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
        SettingsSection(title = "Cloud Sync") {
            SettingsRow(
                icon = Icons.Outlined.Cloud,
                title = "Google Drive",
                subtitle = "Back up reading data (coming soon)",
                accentColor = colors.accent,
            )
            SettingsRow(
                icon = Icons.Outlined.Storage,
                title = "WebDAV Server",
                subtitle = "Use your own server (coming soon)",
                accentColor = colors.accent,
            )
        }

        SettingsSection(title = "AI & Secrets") {
            SettingsRow(
                icon = Icons.Outlined.AutoAwesome,
                title = "AI Provider Configuration",
                subtitle = "Configure OpenAI, Claude, or Gemini (coming soon)",
                accentColor = colors.accent,
            )
        }

        // ── Storage ─────────────────────────────────────────────────
        SettingsSection(title = "Storage") {
            SettingsRow(
                icon = Icons.Outlined.SdStorage,
                title = "Library Size",
                subtitle = "${uiState.bookCount} book(s) on disk",
                accentColor = colors.accent,
            )
            SettingsRow(
                icon = Icons.Outlined.DeleteSweep,
                title = "Clear Cache",
                subtitle = "Remove temporary reading data",
                onClick = { /* TODO: clear cache */ },
                accentColor = Color(0xFFEF4444),
            )
        }

        // ── Advanced ────────────────────────────────────────────────
        SettingsSection(title = "Advanced") {
            SettingsRow(
                icon = Icons.Outlined.RestartAlt,
                title = "Reset Preferences",
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
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column { content() }
        }
    }
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
