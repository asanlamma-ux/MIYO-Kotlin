package com.miyu.reader.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.miyu.reader.domain.model.MarginPreset
import com.miyu.reader.domain.model.PageAnimation
import com.miyu.reader.domain.model.ReaderMode
import com.miyu.reader.domain.model.TapZoneNavMode
import com.miyu.reader.domain.model.TextAlign
import com.miyu.reader.domain.model.ThemeMode
import com.miyu.reader.domain.model.DownloadedDictionary
import com.miyu.reader.permissions.MiyoPermissions
import com.miyu.reader.storage.MiyoStorage
import com.miyu.reader.ui.core.components.MiyoScreenHeader
import com.miyu.reader.ui.core.components.settings.MiyoExpandableChoiceSetting as ExpandableChoiceSetting
import com.miyu.reader.ui.core.components.settings.MiyoSectionLabel as SectionLabel
import com.miyu.reader.ui.core.components.settings.MiyoSettingsItem as SettingsRow
import com.miyu.reader.ui.core.components.settings.MiyoSettingsSection as SettingsSection
import com.miyu.reader.ui.core.components.settings.MiyoSettingsSwitch as SettingsToggle
import com.miyu.reader.ui.core.theme.MiyoSpacing
import com.miyu.reader.ui.library.LibraryWorkspaceSurface
import com.miyu.reader.ui.library.WorkspaceExitButton
import com.miyu.reader.ui.theme.LocalMIYUColors
import com.miyu.reader.ui.theme.ReaderColors
import com.miyu.reader.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class SettingsDialogState(
    val title: String,
    val message: String,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onOpenThemePicker: () -> Unit = {},
    onOpenAdvancedSettings: () -> Unit = {},
    onOpenReaderSettings: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalMIYUColors.current
    val scope = rememberCoroutineScope()
    var dialogState by remember { mutableStateOf<SettingsDialogState?>(null) }
    var showDictionaryLibrary by remember { mutableStateOf(false) }
    var dictionaryImportUrl by remember { mutableStateOf("") }
    var expandedSettingKey by remember { mutableStateOf<String?>(null) }

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
            .background(colors.background.copy(alpha = 0.94f))
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MiyoSpacing.medium, vertical = MiyoSpacing.small),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = colors.onBackground,
                )
                Text(
                    text = "Appearance, reader, library, downloads, backup, and about.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryText,
                )
            }
            IconButton(onClick = { dialogState = SettingsDialogState("Search", "Settings search is planned for the next routing pass.") }) {
                Icon(Icons.Outlined.Search, contentDescription = "Search settings")
            }
        }

        SettingsSection(title = "Appearance") {
            ExpandableChoiceSetting(
                expanded = expandedSettingKey == "theme_mode",
                onExpandedChange = { expandedSettingKey = if (it) "theme_mode" else null },
                icon = Icons.Outlined.DarkMode,
                title = "App Theme",
                subtitle = "Controls the shell palette. Reader themes stay selectable from the palette picker.",
                accentColor = colors.accent,
                currentValue = when (uiState.themeMode) {
                    ThemeMode.SYSTEM -> "Follow OS"
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                },
                choices = ThemeMode.entries,
                choiceLabel = { mode ->
                    when (mode) {
                        ThemeMode.SYSTEM -> "Follow OS"
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                    }
                },
                selectedChoice = uiState.themeMode,
                onChoiceSelected = viewModel::setThemeMode,
            )
            SettingsRow(
                icon = Icons.Outlined.Palette,
                title = "Reader Theme",
                subtitle = "Choose special RN-style palettes and apply them immediately.",
                onClick = onOpenThemePicker,
                accentColor = colors.accent,
                trailing = {
                    Text(
                        ReaderColors.findById(uiState.readerThemeId).name,
                        color = colors.secondaryText,
                    )
                },
            )
            ExpandableChoiceSetting(
                expanded = expandedSettingKey == "daily_goal",
                onExpandedChange = { expandedSettingKey = if (it) "daily_goal" else null },
                icon = Icons.Outlined.TrackChanges,
                title = "Daily Reading Goal",
                subtitle = "Home tab progress target for reading time.",
                accentColor = colors.accent,
                currentValue = "${uiState.dailyGoalMinutes} min",
                choices = listOf(15, 30, 45, 60, 90, 120),
                choiceLabel = { "$it min" },
                selectedChoice = uiState.dailyGoalMinutes,
                onChoiceSelected = viewModel::setDailyGoalMinutes,
            )
        }

        SettingsSection(title = "Reader") {
            SettingsRow(
                icon = Icons.Outlined.MenuBook,
                title = "Reader Settings",
                subtitle = "Typography, page animation, tap zones, margins, and chapter flow.",
                onClick = onOpenReaderSettings,
                accentColor = colors.accent,
            )
        }

        SettingsSection(title = "Library & Storage") {
            SettingsRow(
                icon = Icons.Outlined.Folder,
                title = "Library & Storage",
                subtitle = "Watched folder, app-data directory, cache, rescan, and duplicate audit.",
                onClick = onOpenAdvancedSettings,
                accentColor = colors.accent,
                trailing = {
                    Text(
                        formatStorageBytes(uiState.libraryBytes),
                        color = colors.secondaryText,
                    )
                },
            )
        }

        SettingsSection(title = "Downloads & Sources") {
            SettingsRow(
                icon = Icons.Outlined.CloudDownload,
                title = "Downloads & Sources",
                subtitle = "Provider routing, background notifications, and chapter concurrency.",
                onClick = onOpenAdvancedSettings,
                accentColor = colors.accent,
                trailing = {
                    Text(
                        "${uiState.downloadConcurrency} workers",
                        color = colors.secondaryText,
                    )
                },
            )
        }

        SettingsSection(title = "Data & Backup") {
            SettingsRow(
                icon = Icons.Outlined.MenuBook,
                title = "Dictionary Library",
                subtitle = "${uiState.downloadedDictionaries.size} installed · offline lookup packs and imports",
                onClick = { showDictionaryLibrary = true },
                accentColor = colors.accent,
            )
            SettingsRow(
                icon = Icons.Outlined.Tune,
                title = "Advanced Settings",
                subtitle = "Permissions, backups, maintenance, source internals, and developer-level tools.",
                onClick = onOpenAdvancedSettings,
                accentColor = colors.accent,
            )
            SettingsRow(
                icon = Icons.Outlined.Login,
                title = "Sign In / Sign Up",
                subtitle = "Supabase-backed sync is available when project keys are supplied at build time.",
                onClick = {
                    dialogState = SettingsDialogState(
                        title = "Supabase Configuration",
                        message = "Set MIYU_SUPABASE_URL and MIYU_SUPABASE_ANON_KEY as Gradle properties or environment variables before building.",
                    )
                },
                accentColor = colors.accent,
            )
            SettingsRow(
                icon = Icons.Outlined.Sync,
                title = "Sync & Backup",
                subtitle = "Export/import settings snapshots locally. Cloud sync remains opt-in.",
                onClick = onOpenAdvancedSettings,
                accentColor = colors.accent,
            )
        }

        SettingsSection(title = "About") {
            SettingsRow(
                icon = Icons.Outlined.Info,
                title = "MIYU Reader",
                subtitle = "Version 1.0.0 · ${uiState.bookCount} book(s) tracked",
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvancedSettingsScreen(
    onBack: () -> Unit,
    onOpenReaderSettings: () -> Unit,
    onOpenTerms: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalMIYUColors.current
    val context = LocalContext.current
    val appDataPath = remember(context) {
        MiyoStorage.rootDir(context).absolutePath.substringAfter("/Android/data/")
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var dialogState by remember { mutableStateOf<SettingsDialogState?>(null) }
    var expandedSettingKey by remember { mutableStateOf<String?>(null) }
    var permissionSnapshot by remember { mutableStateOf(MiyoPermissions.snapshot(context)) }
    val runtimePermissionRequester = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionSnapshot = MiyoPermissions.snapshot(context)
        MiyoPermissions.openStoragePermissionSettings(context)
    }
    val requestRuntimeThenOpenStorage = {
        val permissions = MiyoPermissions.runtimePermissionsToRequest(context)
        if (permissions.isNotEmpty()) {
            runtimePermissionRequester.launch(permissions)
        } else {
            MiyoPermissions.openStoragePermissionSettings(context)
        }
    }

    DisposableEffect(lifecycleOwner.lifecycle) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                permissionSnapshot = MiyoPermissions.snapshot(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
    var pendingSettingsSnapshot by remember { mutableStateOf<String?>(null) }
    val settingsExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val payload = pendingSettingsSnapshot
        pendingSettingsSnapshot = null
        if (uri == null || payload == null) return@rememberLauncherForActivityResult
        scope.launch {
            val message = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                        writer.write(payload)
                    } ?: error("Could not open the selected export file.")
                }
                "Settings snapshot was exported."
            }.getOrElse { it.message ?: "Settings export failed." }
            dialogState = SettingsDialogState("Settings Snapshot", message)
        }
    }
    val settingsImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val message = runCatching {
                val rawJson = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("Could not open the selected settings file.")
                }
                viewModel.importSettingsSnapshot(rawJson)
            }.getOrElse { it.message ?: "Settings import failed." }
            dialogState = SettingsDialogState("Settings Snapshot", message)
        }
    }

    LibraryWorkspaceSurface {
        WorkspaceExitButton(label = "Exit advanced settings", onClick = onBack)
        Text(
            "Advanced Settings",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
            color = colors.onBackground,
        )
        Text(
            "This screen holds reader behavior, storage, and maintenance tools so the normal settings page stays compact.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.secondaryText,
            modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsSection(title = "Reader") {
                SettingsRow(
                    icon = Icons.Outlined.MenuBook,
                    title = "Reader Settings",
                    subtitle = "Page animation, tap behavior, chapter loading, immersion, and related controls.",
                    onClick = onOpenReaderSettings,
                    accentColor = colors.accent,
                )
            }

            SettingsSection(title = "Language Tools") {
                SettingsRow(
                    icon = Icons.Outlined.Translate,
                    title = "Term Groups",
                    subtitle = "Manage MTL correction groups and reader replacement dictionaries.",
                    onClick = onOpenTerms,
                    accentColor = colors.accent,
                )
            }

            SettingsSection(title = "Permissions") {
                SettingsRow(
                    icon = Icons.Outlined.Folder,
                    title = "Storage Permission",
                    subtitle = "Open Android's all-files toggle for watched folders and EPUB exports.",
                    onClick = requestRuntimeThenOpenStorage,
                    accentColor = colors.accent,
                    trailing = {
                        Text(
                            if (permissionSnapshot.storageReady) "Granted" else "Missing",
                            color = if (permissionSnapshot.storageReady) colors.accent else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                )
                SettingsRow(
                    icon = Icons.Outlined.Image,
                    title = "Media Cover Access",
                    subtitle = "Request Android media access for external book covers on Android 13+.",
                    onClick = {
                        val permissions = MiyoPermissions.runtimePermissionsToRequest(context)
                        if (permissions.isNotEmpty()) runtimePermissionRequester.launch(permissions)
                        else permissionSnapshot = MiyoPermissions.snapshot(context)
                    },
                    accentColor = colors.accent,
                    trailing = {
                        Text(
                            if (permissionSnapshot.mediaImagesGranted) "Granted" else "Missing",
                            color = if (permissionSnapshot.mediaImagesGranted) colors.accent else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                )
                SettingsRow(
                    icon = Icons.Outlined.Notifications,
                    title = "Download Notifications",
                    subtitle = "Allow progress and failure notifications for background downloads.",
                    onClick = {
                        val permissions = MiyoPermissions.runtimePermissionsToRequest(context)
                        if (permissions.isNotEmpty()) runtimePermissionRequester.launch(permissions)
                        else permissionSnapshot = MiyoPermissions.snapshot(context)
                    },
                    accentColor = colors.accent,
                    trailing = {
                        Text(
                            if (permissionSnapshot.notificationGranted) "Granted" else "Missing",
                            color = if (permissionSnapshot.notificationGranted) colors.accent else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                )
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    title = "Background Downloads",
                    subtitle = "Open Android's battery optimization control so downloads can continue outside Miyo.",
                    onClick = {
                        MiyoPermissions.openBatteryOptimizationSettings(context)
                        permissionSnapshot = MiyoPermissions.snapshot(context)
                    },
                    accentColor = colors.accent,
                    trailing = {
                        Text(
                            if (permissionSnapshot.batteryOptimizationIgnored) "Allowed" else "Limited",
                            color = if (permissionSnapshot.batteryOptimizationIgnored) colors.accent else colors.secondaryText,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                )
            }

            SettingsSection(title = "Downloads") {
                ExpandableChoiceSetting(
                    expanded = expandedSettingKey == "download_concurrency",
                    onExpandedChange = { expandedSettingKey = if (it) "download_concurrency" else null },
                    icon = Icons.Outlined.CloudDownload,
                    title = "Download Concurrency",
                    subtitle = "Parallel chapter requests. Higher values are faster but harder on unstable sites.",
                    accentColor = colors.accent,
                    currentValue = "${uiState.downloadConcurrency} workers",
                    choices = (2..10).toList(),
                    choiceLabel = { "$it" },
                    selectedChoice = uiState.downloadConcurrency,
                    onChoiceSelected = viewModel::setDownloadConcurrency,
                )
                SettingsRow(
                    icon = Icons.Outlined.Public,
                    title = "Recommendation Routing",
                    subtitle = "Browse ranks pinned, last-used, direct HTTP, and language-matching sources first.",
                    accentColor = colors.accent,
                )
            }

            SettingsSection(title = "Data & Backup") {
                SettingsRow(
                    icon = Icons.Outlined.FileUpload,
                    title = "Export Settings Snapshot",
                    subtitle = "Save a Koodo-style JSON snapshot of reader, typography, theme, and download settings.",
                    onClick = {
                        scope.launch {
                            val snapshot = runCatching { viewModel.exportSettingsSnapshot() }
                                .getOrElse {
                                    dialogState = SettingsDialogState(
                                        "Settings Snapshot",
                                        it.message ?: "Settings export failed.",
                                    )
                                    return@launch
                                }
                            pendingSettingsSnapshot = snapshot
                            settingsExportLauncher.launch("miyo-settings.json")
                        }
                    },
                    accentColor = colors.accent,
                )
                SettingsRow(
                    icon = Icons.Outlined.FileDownload,
                    title = "Import Settings Snapshot",
                    subtitle = "Restore a previously exported MIYO settings JSON snapshot.",
                    onClick = { settingsImportLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
                    accentColor = colors.accent,
                )
            }

            SettingsSection(title = "Storage") {
                SettingsRow(
                    icon = Icons.Outlined.Folder,
                    title = "App Data Directory",
                    subtitle = "Imports and downloaded novels are stored in Miyo's Android app-data folder.",
                    accentColor = colors.accent,
                    trailing = {
                        Text(
                            appDataPath,
                            color = colors.secondaryText,
                            modifier = Modifier.widthIn(max = 180.dp),
                            maxLines = 2,
                        )
                    },
                )
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
                    icon = Icons.Outlined.Inventory2,
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
                    icon = Icons.Outlined.Autorenew,
                    title = "Rescan Library",
                    subtitle = "Re-import missing internal EPUB files and restore missing covers where possible.",
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
                    icon = Icons.Outlined.FindInPage,
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
                    icon = Icons.Outlined.OpenInNew,
                    title = "Open App Settings",
                    subtitle = "Open Android permission and storage settings for Miyo.",
                    onClick = { MiyoPermissions.openAppSettings(context) },
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
            Spacer(Modifier.height(44.dp))
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReaderSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalMIYUColors.current
    var expandedSettingKey by remember { mutableStateOf<String?>(null) }

    LibraryWorkspaceSurface {
        WorkspaceExitButton(label = "Exit reader settings", onClick = onBack)
        Text(
            "Reader Settings",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
            color = colors.onBackground,
        )
        Text(
            "These controls define how the reading space behaves. Choice rows expand inline, while toggles apply immediately.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.secondaryText,
            modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
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
                    modifier = Modifier.padding(horizontal = 24.dp),
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
                    modifier = Modifier.padding(horizontal = 24.dp),
                    colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent),
                )
                ExpandableChoiceSetting(
                    expanded = expandedSettingKey == "text_align",
                    onExpandedChange = { expandedSettingKey = if (it) "text_align" else null },
                    icon = Icons.Outlined.FormatAlignLeft,
                    title = "Text Alignment",
                    subtitle = uiState.typography.textAlign.name.lowercase().replaceFirstChar { it.uppercase() },
                    accentColor = colors.accent,
                    currentValue = uiState.typography.textAlign.name.lowercase().replaceFirstChar { it.uppercase() },
                    choices = TextAlign.entries,
                    choiceLabel = { align -> align.name.lowercase().replaceFirstChar { it.uppercase() } },
                    selectedChoice = uiState.typography.textAlign,
                    onChoiceSelected = viewModel::setTextAlign,
                )
            }

            SettingsSection(title = "Flow") {
                ExpandableChoiceSetting(
                    expanded = expandedSettingKey == "reader_mode",
                    onExpandedChange = { expandedSettingKey = if (it) "reader_mode" else null },
                    icon = Icons.Outlined.ViewColumn,
                    title = "Reader Mode",
                    subtitle = "Koodo-style scroll, single page, or double page layout.",
                    accentColor = colors.accent,
                    currentValue = when (uiState.readingSettings.readerMode) {
                        ReaderMode.SCROLL -> "Scroll"
                        ReaderMode.SINGLE -> "Single page"
                        ReaderMode.DOUBLE -> "Double page"
                    },
                    choices = ReaderMode.entries,
                    choiceLabel = {
                        when (it) {
                            ReaderMode.SCROLL -> "Scroll"
                            ReaderMode.SINGLE -> "Single page"
                            ReaderMode.DOUBLE -> "Double page"
                        }
                    },
                    selectedChoice = uiState.readingSettings.readerMode,
                    onChoiceSelected = viewModel::setReaderMode,
                )
                ExpandableChoiceSetting(
                    expanded = expandedSettingKey == "page_animation",
                    onExpandedChange = { expandedSettingKey = if (it) "page_animation" else null },
                    icon = Icons.Outlined.Tune,
                    title = "Page Animation",
                    subtitle = "Transition used when page navigation is triggered.",
                    accentColor = colors.accent,
                    currentValue = uiState.readingSettings.pageAnimation.name.lowercase().replaceFirstChar { it.uppercase() },
                    choices = PageAnimation.entries,
                    choiceLabel = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } },
                    selectedChoice = uiState.readingSettings.pageAnimation,
                    onChoiceSelected = viewModel::setPageAnimation,
                )
                SettingsToggle(
                    icon = Icons.Outlined.TouchApp,
                    title = "Tap Zones",
                    subtitle = "Left and right edges can trigger scroll or chapter moves.",
                    checked = uiState.readingSettings.tapZonesEnabled,
                    onCheckedChange = viewModel::setTapZonesEnabled,
                    accentColor = colors.accent,
                )
                ExpandableChoiceSetting(
                    expanded = expandedSettingKey == "side_tap",
                    onExpandedChange = { expandedSettingKey = if (it) "side_tap" else null },
                    icon = Icons.Outlined.TouchApp,
                    title = "Side Tap Action",
                    subtitle = "Choose whether edge taps scroll inside the chapter or switch chapters.",
                    accentColor = colors.accent,
                    currentValue = when (uiState.readingSettings.tapZoneNavMode) {
                        TapZoneNavMode.SCROLL -> "Scroll within chapter"
                        TapZoneNavMode.CHAPTER -> "Previous and next chapter"
                    },
                    choices = TapZoneNavMode.entries,
                    choiceLabel = {
                        when (it) {
                            TapZoneNavMode.SCROLL -> "Scroll within chapter"
                            TapZoneNavMode.CHAPTER -> "Previous and next chapter"
                        }
                    },
                    selectedChoice = uiState.readingSettings.tapZoneNavMode,
                    onChoiceSelected = viewModel::setTapZoneNavMode,
                )
                ExpandableChoiceSetting(
                    expanded = expandedSettingKey == "sleep_timer",
                    onExpandedChange = { expandedSettingKey = if (it) "sleep_timer" else null },
                    icon = Icons.Outlined.Info,
                    title = "Sleep Timer",
                    subtitle = "Show a reader timeout dialog after the selected duration.",
                    accentColor = colors.accent,
                    currentValue = if (uiState.readingSettings.sleepTimerMinutes == 0) "Off" else "${uiState.readingSettings.sleepTimerMinutes} minutes",
                    choices = listOf(0, 15, 30, 45, 60, 90, 120),
                    choiceLabel = { if (it == 0) "Off" else "$it min" },
                    selectedChoice = uiState.readingSettings.sleepTimerMinutes,
                    onChoiceSelected = viewModel::setSleepTimer,
                )
                SettingsToggle(
                    icon = Icons.Outlined.Sync,
                    title = "Continuous Chapter Loading",
                    subtitle = "Append the next chapter under the current one when enabled.",
                    checked = uiState.readingSettings.autoAdvanceChapter,
                    onCheckedChange = viewModel::setAutoAdvanceChapter,
                    accentColor = colors.accent,
                )
                SettingsToggle(
                    icon = Icons.Outlined.TouchApp,
                    title = "Volume Button Navigation",
                    subtitle = "Use the hardware volume keys to move between chapters.",
                    checked = uiState.readingSettings.volumeButtonPageTurn,
                    onCheckedChange = viewModel::setVolumeButtonPageTurn,
                    accentColor = colors.accent,
                )
            }

            SettingsSection(title = "Display") {
                ExpandableChoiceSetting(
                    expanded = expandedSettingKey == "margins",
                    onExpandedChange = { expandedSettingKey = if (it) "margins" else null },
                    icon = Icons.Outlined.FormatIndentIncrease,
                    title = "Margins",
                    subtitle = "Tune the horizontal padding inside the reading space.",
                    accentColor = colors.accent,
                    currentValue = uiState.readingSettings.marginPreset.name.lowercase().replaceFirstChar { it.uppercase() },
                    choices = MarginPreset.entries,
                    choiceLabel = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } },
                    selectedChoice = uiState.readingSettings.marginPreset,
                    onChoiceSelected = viewModel::setMarginPreset,
                )
                SettingsToggle(
                    icon = Icons.Outlined.TextFields,
                    title = "Bionic Reading",
                    subtitle = "Emphasize word beginnings for faster scanning.",
                    checked = uiState.readingSettings.bionicReading,
                    onCheckedChange = viewModel::setBionicReading,
                    accentColor = colors.accent,
                )
                SettingsToggle(
                    icon = Icons.Outlined.Fullscreen,
                    title = "Immersive Mode",
                    subtitle = "Hide Android system bars while a book is open.",
                    checked = uiState.readingSettings.immersiveMode,
                    onCheckedChange = viewModel::setImmersiveMode,
                    accentColor = colors.accent,
                )
                SettingsToggle(
                    icon = Icons.Outlined.Fullscreen,
                    title = "Keep Screen On",
                    subtitle = "Prevent the device from sleeping during reading sessions.",
                    checked = uiState.readingSettings.keepScreenOn,
                    onCheckedChange = viewModel::setKeepScreenOn,
                    accentColor = colors.accent,
                )
            }

            SettingsSection(title = "Reader Chrome") {
                SettingsToggle(
                    icon = Icons.Outlined.Fullscreen,
                    title = "Hide Top Controls",
                    subtitle = "Keep the header controls out of the reader overlay.",
                    checked = uiState.readingSettings.hideReaderHeader,
                    onCheckedChange = viewModel::setHideReaderHeader,
                    accentColor = colors.accent,
                )
                SettingsToggle(
                    icon = Icons.Outlined.Fullscreen,
                    title = "Hide Bottom Controls",
                    subtitle = "Hide progress and quick tools from the reader overlay.",
                    checked = uiState.readingSettings.hideReaderFooter,
                    onCheckedChange = viewModel::setHideReaderFooter,
                    accentColor = colors.accent,
                )
                SettingsToggle(
                    icon = Icons.Outlined.TouchApp,
                    title = "Lock Reader Navigation",
                    subtitle = "Disable edge taps and pull chapter navigation.",
                    checked = uiState.readingSettings.readerNavLocked,
                    onCheckedChange = viewModel::setReaderNavLocked,
                    accentColor = colors.accent,
                )
                SettingsToggle(
                    icon = Icons.Outlined.TextFields,
                    title = "Selection Tools Popup",
                    subtitle = "Show MIYO tools after selecting reader text.",
                    checked = uiState.readingSettings.selectionPopupEnabled,
                    onCheckedChange = viewModel::setSelectionPopupEnabled,
                    accentColor = colors.accent,
                )
                SettingsToggle(
                    icon = Icons.Outlined.FormatIndentIncrease,
                    title = "Show Page Border",
                    subtitle = "Draw a subtle Koodo-style frame inside the reader.",
                    checked = uiState.readingSettings.showPageBorder,
                    onCheckedChange = viewModel::setShowPageBorder,
                    accentColor = colors.accent,
                )
                SettingsToggle(
                    icon = Icons.Outlined.MenuBook,
                    title = "Overwrite Link Style",
                    subtitle = "Normalize EPUB links to the active reader palette.",
                    checked = uiState.readingSettings.overwriteLinkStyle,
                    onCheckedChange = viewModel::setOverwriteLinkStyle,
                    accentColor = colors.accent,
                )
                SettingsToggle(
                    icon = Icons.Outlined.TextFields,
                    title = "Overwrite Text Style",
                    subtitle = "Force imported chapter text to use MIYO typography.",
                    checked = uiState.readingSettings.overwriteTextStyle,
                    onCheckedChange = viewModel::setOverwriteTextStyle,
                    accentColor = colors.accent,
                )
            }
            Spacer(Modifier.height(22.dp))
        }
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
                .padding(horizontal = 24.dp, vertical = 8.dp),
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
