package com.miyu.reader.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FormatAlignLeft
import androidx.compose.material.icons.outlined.FormatIndentIncrease
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miyu.reader.domain.model.MarginPreset
import com.miyu.reader.domain.model.TextAlign
import com.miyu.reader.domain.model.ThemeMode
import com.miyu.reader.ui.theme.LocalMIYUColors
import com.miyu.reader.ui.theme.ReaderColors
import com.miyu.reader.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

private data class SettingsDialogState(
    val title: String,
    val message: String,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onOpenThemePicker: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalMIYUColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dialogState by remember { mutableStateOf<SettingsDialogState?>(null) }

    val storagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        viewModel.setStorageDirectoryUri(uri.toString())
        dialogState = SettingsDialogState(
            title = "Storage Updated",
            message = "New watched folder: ${storageLabel(uri.toString())}",
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            color = colors.onBackground,
        )

        SettingsSection(title = "Account") {
            SettingsRow(
                icon = Icons.Outlined.Login,
                title = "Sign In / Sign Up",
                subtitle = "Auth and cloud sync are still being ported from the React Native build.",
                accentColor = colors.accent,
            )
        }

        SettingsSection(title = "Daily Goal") {
            Text(
                "Minutes to aim for each day. Progress targets persist on-device.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryText,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            )
            FlowRow(
                modifier = Modifier.padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listOf(15, 30, 45, 60, 90, 120).forEach { minutes ->
                    val selected = uiState.dailyGoalMinutes == minutes
                    OutlinedButton(
                        onClick = { viewModel.setDailyGoalMinutes(minutes) },
                        shape = RoundedCornerShape(14.dp),
                        colors = if (selected) {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = colors.accent,
                                contentColor = Color.White,
                            )
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        },
                        modifier = Modifier.width(92.dp),
                    ) {
                        Text(
                            "$minutes min",
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        SettingsSection(title = "Appearance") {
            SettingsRow(
                icon = Icons.Outlined.DarkMode,
                title = "App Theme",
                subtitle = if (uiState.themeMode == ThemeMode.DARK) "Dark mode" else "Light mode",
                onClick = {
                    viewModel.setThemeMode(
                        if (uiState.themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK,
                    )
                },
                accentColor = colors.accent,
                trailing = {
                    Text(
                        uiState.themeMode.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = colors.secondaryText,
                    )
                },
            )
            SettingsRow(
                icon = Icons.Outlined.Palette,
                title = "Reader Theme",
                subtitle = "Choose the active reading palette from the RN-style picker.",
                onClick = onOpenThemePicker,
                accentColor = colors.accent,
                trailing = {
                    Text(
                        ReaderColors.findById(uiState.readerThemeId).name,
                        color = colors.secondaryText,
                    )
                },
            )
        }

        SettingsSection(title = "Typography") {
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
            SettingsRow(
                icon = Icons.Outlined.Sort,
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
            SettingsRow(
                icon = Icons.Outlined.FormatAlignLeft,
                title = "Text Alignment",
                subtitle = uiState.typography.textAlign.name.lowercase().replaceFirstChar { it.uppercase() },
                onClick = {
                    viewModel.setTextAlign(
                        if (uiState.typography.textAlign == TextAlign.LEFT) TextAlign.JUSTIFY else TextAlign.LEFT,
                    )
                },
                accentColor = colors.accent,
            )
        }

        SettingsSection(title = "Reading") {
            SettingsRow(
                icon = Icons.Outlined.FormatIndentIncrease,
                title = "Margins",
                subtitle = uiState.readingSettings.marginPreset.name.lowercase().replaceFirstChar { it.uppercase() },
                onClick = {
                    val options = MarginPreset.entries
                    val next = (options.indexOf(uiState.readingSettings.marginPreset) + 1) % options.size
                    viewModel.setMarginPreset(options[next])
                },
                accentColor = colors.accent,
            )
            SettingsToggle(
                icon = Icons.Outlined.Fullscreen,
                title = "Immersive Mode",
                subtitle = "Hide the Android status and navigation bars while a book is open.",
                checked = uiState.readingSettings.immersiveMode,
                onCheckedChange = viewModel::setImmersiveMode,
                accentColor = colors.accent,
            )
            HorizontalDivider(color = colors.secondaryText.copy(alpha = 0.12f))
            Text(
                "Other reader behavior controls such as tap zones, volume turns, and motion profiles are still being ported. They stay hidden until the Kotlin reader actually consumes them.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.secondaryText,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            )
        }

        SettingsSection(title = "Storage") {
            SettingsRow(
                icon = Icons.Outlined.Folder,
                title = "Storage Location",
                subtitle = "Pick the Android folder used for watched-folder rescans.",
                onClick = { storagePicker.launch(null) },
                accentColor = colors.accent,
                trailing = {
                    Text(storageLabel(uiState.storageDirectoryUri), color = colors.secondaryText)
                },
            )
            SettingsRow(
                icon = Icons.Outlined.SdStorage,
                title = "Library Size",
                subtitle = "${uiState.bookCount} book(s) on disk",
                accentColor = colors.accent,
                trailing = {
                    Text(formatStorageBytes(uiState.libraryBytes), color = colors.secondaryText)
                },
            )
            SettingsRow(
                icon = Icons.Outlined.DeleteSweep,
                title = "Clear Cache",
                subtitle = "Remove temporary reader data without deleting books.",
                onClick = {
                    scope.launch {
                        viewModel.clearCache()
                        dialogState = SettingsDialogState("Cache Cleared", "Temporary reading cache was removed.")
                    }
                },
                accentColor = Color(0xFFEF4444),
            )
        }

        SettingsSection(title = "Maintenance") {
            SettingsRow(
                icon = Icons.Outlined.Sync,
                title = "Rescan Library",
                subtitle = "Re-import missing internal EPUB files and drop orphaned records.",
                onClick = {
                    scope.launch {
                        val result = viewModel.rescanLibrary()
                        dialogState = SettingsDialogState(
                            title = "Rescan Complete",
                            message = "Tracked books: ${result.tracked}\nImported: ${result.imported}\nRemoved missing entries: ${result.removed}",
                        )
                    }
                },
                accentColor = colors.accent,
            )
            SettingsRow(
                icon = Icons.Outlined.Security,
                title = "Duplicate Audit",
                subtitle = "Find repeated title/author or identifier groups in the local library.",
                onClick = {
                    scope.launch {
                        val audit = viewModel.auditDuplicates()
                        dialogState = SettingsDialogState(
                            title = "Duplicate Audit",
                            message = if (audit.exactGroups == 0) {
                                "No duplicate groups were found."
                            } else {
                                buildString {
                                    append("Duplicate groups: ${audit.exactGroups}")
                                    if (audit.samples.isNotEmpty()) {
                                        append("\n\nExamples:\n")
                                        append(audit.samples.joinToString("\n"))
                                    }
                                }
                            },
                        )
                    }
                },
                accentColor = colors.accent,
            )
            SettingsRow(
                icon = Icons.Outlined.Security,
                title = "Open App Settings",
                subtitle = "Open Android app settings for permissions and storage access.",
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                },
                accentColor = colors.accent,
            )
            SettingsRow(
                icon = Icons.Outlined.RestartAlt,
                title = "Reset All Preferences",
                subtitle = "Restore reading and display settings to their defaults.",
                onClick = {
                    viewModel.resetToDefaults()
                    dialogState = SettingsDialogState(
                        title = "Preferences Reset",
                        message = "Reading settings, typography, and daily goal were restored to default values.",
                    )
                },
                accentColor = Color(0xFFEF4444),
            )
        }

        SettingsSection(title = "About") {
            SettingsRow(
                icon = Icons.Outlined.Info,
                title = "MIYU Reader",
                subtitle = "Kotlin port in active parity work with the React Native app.",
                accentColor = colors.accent,
            )
        }
    }

    dialogState?.let { dialog ->
        AlertDialog(
            onDismissRequest = { dialogState = null },
            title = { Text(dialog.title) },
            text = { Text(dialog.message) },
            confirmButton = {
                TextButton(onClick = { dialogState = null }) {
                    Text("OK")
                }
            },
        )
    }
}

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
            if (onClick != null) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = colors.secondaryText,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    )
}

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

private fun storageLabel(uri: String?): String {
    if (uri.isNullOrBlank()) return "Internal storage/MIYO"
    val relevant = uri.substringAfter("/tree/", uri)
    return relevant.replace("primary:", "Internal storage/")
}

private fun formatStorageBytes(size: Long): String {
    if (size < 1024) return "$size B"
    if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024f)
    return String.format("%.2f MB", size / (1024f * 1024f))
}
