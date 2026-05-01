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
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
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
import com.miyu.reader.domain.model.PageAnimation
import com.miyu.reader.domain.model.TapZoneNavMode
import com.miyu.reader.domain.model.TextAlign
import com.miyu.reader.domain.model.ThemeMode
import com.miyu.reader.domain.model.DownloadedDictionary
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
    var showDictionaryLibrary by remember { mutableStateOf(false) }
    var dictionaryImportUrl by remember { mutableStateOf("") }

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
    val dictionaryFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val message = runCatching { viewModel.importDictionaryFromUri(uri) }
                .getOrElse { it.message ?: "Dictionary import failed." }
            dialogState = SettingsDialogState("Dictionary Library", message)
        }
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
                subtitle = "Uses Supabase when MIYU_SUPABASE_URL and MIYU_SUPABASE_ANON_KEY are configured.",
                onClick = {
                    dialogState = SettingsDialogState(
                        title = "Supabase Configuration",
                        message = "Set MIYU_SUPABASE_URL and MIYU_SUPABASE_ANON_KEY as Gradle properties or environment variables before building.",
                    )
                },
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
                subtitle = when (uiState.themeMode) {
                    ThemeMode.SYSTEM -> "Follow OS"
                    ThemeMode.LIGHT -> "Light mode"
                    ThemeMode.DARK -> "Dark mode"
                },
                onClick = {
                    val options = ThemeMode.entries
                    val next = (options.indexOf(uiState.themeMode) + 1) % options.size
                    viewModel.setThemeMode(options[next])
                },
                accentColor = colors.accent,
                trailing = {
                    Text(
                        when (uiState.themeMode) {
                            ThemeMode.SYSTEM -> "Follow OS"
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.DARK -> "Dark"
                        },
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
                icon = Icons.Outlined.Tune,
                title = "Page Animation",
                subtitle = uiState.readingSettings.pageAnimation.name.lowercase().replaceFirstChar { it.uppercase() },
                onClick = {
                    val options = PageAnimation.entries
                    val next = (options.indexOf(uiState.readingSettings.pageAnimation) + 1) % options.size
                    viewModel.setPageAnimation(options[next])
                },
                accentColor = colors.accent,
            )
            SettingsToggle(
                icon = Icons.Outlined.TouchApp,
                title = "Tap Zones",
                subtitle = "Left/right reader edge taps scroll or change chapters.",
                checked = uiState.readingSettings.tapZonesEnabled,
                onCheckedChange = viewModel::setTapZonesEnabled,
                accentColor = colors.accent,
            )
            SettingsRow(
                icon = Icons.Outlined.TouchApp,
                title = "Side Tap Action",
                subtitle = when (uiState.readingSettings.tapZoneNavMode) {
                    TapZoneNavMode.SCROLL -> "Scroll within chapter"
                    TapZoneNavMode.CHAPTER -> "Previous/next chapter"
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
            SettingsRow(
                icon = Icons.Outlined.Info,
                title = "Sleep Timer",
                subtitle = if (uiState.readingSettings.sleepTimerMinutes == 0) {
                    "Off"
                } else {
                    "${uiState.readingSettings.sleepTimerMinutes} minutes"
                },
                onClick = {
                    val options = listOf(0, 15, 30, 45, 60, 90, 120)
                    val next = (options.indexOf(uiState.readingSettings.sleepTimerMinutes).coerceAtLeast(0) + 1) % options.size
                    viewModel.setSleepTimer(options[next])
                },
                accentColor = colors.accent,
            )
            SettingsToggle(
                icon = Icons.Outlined.Sync,
                title = "Continuous Chapter Loading",
                subtitle = "Prepare the next chapter when you reach the end.",
                checked = uiState.readingSettings.autoAdvanceChapter,
                onCheckedChange = viewModel::setAutoAdvanceChapter,
                accentColor = colors.accent,
            )
            SettingsToggle(
                icon = Icons.Outlined.TouchApp,
                title = "Volume Button Navigation",
                subtitle = "Use hardware volume buttons to turn chapters.",
                checked = uiState.readingSettings.volumeButtonPageTurn,
                onCheckedChange = viewModel::setVolumeButtonPageTurn,
                accentColor = colors.accent,
            )
            SettingsToggle(
                icon = Icons.Outlined.TextFields,
                title = "Bionic Reading",
                subtitle = "Emphasize word beginnings in the reader.",
                checked = uiState.readingSettings.bionicReading,
                onCheckedChange = viewModel::setBionicReading,
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
            SettingsToggle(
                icon = Icons.Outlined.Fullscreen,
                title = "Keep Screen On",
                subtitle = "Prevent sleep while a book is open.",
                checked = uiState.readingSettings.keepScreenOn,
                onCheckedChange = viewModel::setKeepScreenOn,
                accentColor = colors.accent,
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

        SettingsSection(title = "Language Tools") {
            SettingsRow(
                icon = Icons.Outlined.MenuBook,
                title = "Dictionary Library",
                subtitle = "${uiState.downloadedDictionaries.size} installed · offline lookup packs and imports",
                onClick = { showDictionaryLibrary = true },
                accentColor = colors.accent,
            )
        }

        SettingsSection(title = "About") {
            SettingsRow(
                icon = Icons.Outlined.Info,
                title = "MIYU Reader",
                subtitle = "Version 1.0.0",
                accentColor = colors.accent,
            )
        }
    }

    if (showDictionaryLibrary) {
        DictionaryLibrarySheet(
            installed = uiState.downloadedDictionaries,
            starterPacks = uiState.starterDictionaries,
            busy = uiState.dictionaryBusy,
            importUrl = dictionaryImportUrl,
            onImportUrlChange = { dictionaryImportUrl = it },
            onDismiss = { showDictionaryLibrary = false },
            onInstallStarter = { id ->
                scope.launch {
                    val message = viewModel.installStarterDictionary(id)
                    dialogState = SettingsDialogState("Dictionary Library", message)
                }
            },
            onRemove = { id ->
                scope.launch {
                    val message = viewModel.removeDictionary(id)
                    dialogState = SettingsDialogState("Dictionary Library", message)
                }
            },
            onImportFile = { dictionaryFilePicker.launch(arrayOf("application/json", "application/zip", "application/octet-stream")) },
            onImportUrl = {
                val url = dictionaryImportUrl.trim()
                if (url.isNotBlank()) {
                    scope.launch {
                        val message = runCatching { viewModel.importDictionaryFromUrl(url) }
                            .onSuccess { dictionaryImportUrl = "" }
                            .getOrElse { it.message ?: "Dictionary import failed." }
                        dialogState = SettingsDialogState("Dictionary Library", message)
                    }
                }
            },
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DictionaryLibrarySheet(
    installed: List<DownloadedDictionary>,
    starterPacks: List<DownloadedDictionary>,
    busy: Boolean,
    importUrl: String,
    onImportUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onInstallStarter: (String) -> Unit,
    onRemove: (String) -> Unit,
    onImportFile: () -> Unit,
    onImportUrl: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    val installedIds = installed.map { it.id }.toSet()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                "Dictionary Library",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = colors.onBackground,
            )
            Text(
                "Install RN starter dictionaries, import Miyo JSON/ZIP packs, and keep offline lookup available in the reader.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryText,
                modifier = Modifier.padding(top = 6.dp, bottom = 18.dp),
            )

            SectionLabel("Starter Packs")
            starterPacks.forEach { dictionary ->
                DictionaryRow(
                    dictionary = dictionary,
                    installed = dictionary.id in installedIds,
                    busy = busy,
                    actionLabel = if (dictionary.id in installedIds) "Installed" else "Install",
                    onAction = { if (dictionary.id !in installedIds) onInstallStarter(dictionary.id) },
                )
            }

            Spacer(Modifier.height(18.dp))
            SectionLabel("Import Package")
            OutlinedTextField(
                value = importUrl,
                onValueChange = onImportUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("HTTPS package URL") },
                placeholder = { Text("https://example.com/dictionary.json") },
                singleLine = true,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 10.dp),
            ) {
                OutlinedButton(
                    onClick = onImportFile,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Import file")
                }
                Button(
                    onClick = onImportUrl,
                    enabled = !busy && importUrl.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Import URL")
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionLabel("Installed")
            if (installed.isEmpty()) {
                Text(
                    "No dictionaries installed yet. Lookup will auto-install the starter packs the first time it runs.",
                    color = colors.secondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            } else {
                installed.forEach { dictionary ->
                    DictionaryRow(
                        dictionary = dictionary,
                        installed = true,
                        busy = busy,
                        actionLabel = "Remove",
                        destructive = true,
                        onAction = { onRemove(dictionary.id) },
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun DictionaryRow(
    dictionary: DownloadedDictionary,
    installed: Boolean,
    busy: Boolean,
    actionLabel: String,
    destructive: Boolean = false,
    onAction: () -> Unit,
) {
    val colors = LocalMIYUColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Outlined.MenuBook,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.padding(top = 3.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(dictionary.name, color = colors.onBackground, fontWeight = FontWeight.Bold)
                Text(
                    "${dictionary.entriesCount} entries · ${dictionary.language.uppercase()} · ${dictionary.version}",
                    color = colors.secondaryText,
                    style = MaterialTheme.typography.labelMedium,
                )
                dictionary.description?.let {
                    Text(
                        it,
                        color = colors.secondaryText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            TextButton(
                onClick = onAction,
                enabled = !busy && (!installed || destructive),
            ) {
                Text(
                    actionLabel,
                    color = if (destructive) MaterialTheme.colorScheme.error else colors.accent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = LocalMIYUColors.current
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        color = colors.secondaryText,
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
    )
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
