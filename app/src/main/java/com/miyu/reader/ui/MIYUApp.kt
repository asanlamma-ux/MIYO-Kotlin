package com.miyu.reader.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.miyu.reader.R
import com.miyu.reader.domain.model.OnlineNovelSummary
import com.miyu.reader.domain.model.ThemeMode
import com.miyu.reader.permissions.MiyoPermissions
import com.miyu.reader.storage.MiyoBackupManager
import com.miyu.reader.ui.browse.BrowseWorkflowScreen
import com.miyu.reader.ui.browse.DownloadsWorkflowScreen
import com.miyu.reader.ui.browse.GlobalSourceSearchScreen
import com.miyu.reader.ui.browse.MigrationWorkflowScreen
import com.miyu.reader.ui.browse.OnlineNovelDetailsScreen
import com.miyu.reader.ui.browse.ProviderRepositoriesScreen
import com.miyu.reader.ui.browse.SourceVerifierScreen
import com.miyu.reader.ui.browse.SourceUpdatesWorkflowScreen
import com.miyu.reader.ui.browse.SourceWorkflowDetailScreen
import com.miyu.reader.ui.home.HomeScreen
import com.miyu.reader.ui.library.BookDetailsScreen
import com.miyu.reader.ui.library.LibraryScreen
import com.miyu.reader.ui.terms.TermsScreen
import com.miyu.reader.ui.history.HistoryScreen
import com.miyu.reader.ui.onboarding.InitialSetupBottomSheet
import com.miyu.reader.ui.settings.AdvancedSettingsScreen
import com.miyu.reader.ui.settings.ReaderSettingsScreen
import com.miyu.reader.ui.settings.SettingsScreen
import com.miyu.reader.ui.reader.ReaderScreen
import com.miyu.reader.ui.components.ThemePickerBottomSheet
import com.miyu.reader.ui.core.components.material.MiyoNavigationBar
import com.miyu.reader.ui.core.components.material.MiyoNavigationBarItem
import com.miyu.reader.ui.core.components.material.MiyoScaffold
import com.miyu.reader.ui.core.components.MiyoAboutDialog
import com.miyu.reader.ui.core.components.MiyoBackupChoiceDialog
import com.miyu.reader.ui.permissions.MiyoStoragePermissionDialog
import com.miyu.reader.ui.theme.DefaultReaderThemeId
import com.miyu.reader.ui.theme.LocalMIYUColors
import com.miyu.reader.ui.theme.MIYUTheme
import com.miyu.reader.ui.theme.SpecialThemeBackdrop
import com.miyu.reader.viewmodel.AppPermissionViewModel
import com.miyu.reader.viewmodel.ThemeViewModel
import kotlinx.coroutines.launch

sealed class Screen(
    val route: String,
    val title: String,
    @DrawableRes val iconRes: Int? = null,
) {
    data object Home : Screen("home", "Home", iconRes = R.drawable.ic_nav_home)
    data object Library : Screen("library", "Library", iconRes = R.drawable.ic_nav_library)
    data object Browse : Screen("browse", "Browse", iconRes = R.drawable.ic_nav_browse)
    data object History : Screen("history", "History", iconRes = R.drawable.ic_nav_history)
    data object Settings : Screen("settings", "Settings", iconRes = R.drawable.ic_nav_settings)
    data object BrowseSearch : Screen("browse/search", "Global Search")
    data object SourceRepositories : Screen("browse/repositories", "Source Repositories")
    data object SourceMigration : Screen("browse/migration", "Source Migration")
    data object SourceUpdates : Screen("browse/updates", "Source Updates")
    data object DownloadQueue : Screen("browse/downloads", "Downloads")
    data object SourceDetails : Screen("browse/source/{sourceId}", "Source Details")
    data object SourceVerifier : Screen("browse/source/{sourceId}/verify", "Source Verifier")
    data object OnlineNovelDetails : Screen("browse/novel?providerId={providerId}&path={path}&title={title}", "Novel Details")
    data object BookDetails : Screen("library/book/{bookId}", "Book Details")
    data object AdvancedSettings : Screen("settings/advanced", "Advanced Settings")
    data object ReaderSettings : Screen("settings/reader", "Reader Settings")
    data object Terms : Screen("settings/terms", "Terms")
}

val bottomNavItems = listOf(
    Screen.Home, Screen.Library, Screen.Browse, Screen.History
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MIYUApp() {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val permissionViewModel: AppPermissionViewModel = hiltViewModel()
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val readerThemeId by themeViewModel.readerThemeId.collectAsStateWithLifecycle(initialValue = DefaultReaderThemeId)
    val shouldShowInitialSetup by themeViewModel.shouldShowInitialSetup.collectAsStateWithLifecycle(initialValue = false)
    val permissionState by permissionViewModel.uiState.collectAsStateWithLifecycle()
    var setupPreviewThemeMode by remember { mutableStateOf<ThemeMode?>(null) }
    var setupPreviewReaderThemeId by remember { mutableStateOf<String?>(null) }
    // First-run previews must stay in memory. Writing them to DataStore marks setup as no longer fresh.
    val effectiveThemeMode = if (shouldShowInitialSetup) setupPreviewThemeMode ?: themeMode else themeMode
    val effectiveReaderThemeId = if (shouldShowInitialSetup) setupPreviewReaderThemeId ?: readerThemeId else readerThemeId

    LaunchedEffect(shouldShowInitialSetup) {
        if (!shouldShowInitialSetup) {
            setupPreviewThemeMode = null
            setupPreviewReaderThemeId = null
        }
    }

    MIYUTheme(themeMode = effectiveThemeMode, readerThemeId = effectiveReaderThemeId) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val scope = rememberCoroutineScope()
        val navController = rememberNavController()
        val colors = LocalMIYUColors.current
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        var showThemePicker by remember { mutableStateOf(false) }
        var showBackupDialog by remember { mutableStateOf(false) }
        var showAboutDialog by remember { mutableStateOf(false) }
        var backupMessage by remember { mutableStateOf<String?>(null) }
        var dismissStorageDialogThisSession by remember { mutableStateOf(false) }
        val exportBackupLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        ) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            scope.launch {
                backupMessage = runCatching { MiyoBackupManager.exportToUri(context, uri) }
                    .getOrElse { it.message ?: "Backup export failed." }
            }
        }
        val importBackupLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            scope.launch {
                backupMessage = runCatching { MiyoBackupManager.importFromUri(context, uri) }
                    .getOrElse { it.message ?: "Backup import failed." }
            }
        }
        val requestRuntimePermissions = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) {
            permissionViewModel.refresh()
            MiyoPermissions.openStoragePermissionSettings(context)
        }
        val configureStoragePermissions = {
            val runtimePermissions = MiyoPermissions.runtimePermissionsToRequest(context)
            if (runtimePermissions.isNotEmpty()) {
                requestRuntimePermissions.launch(runtimePermissions)
            } else {
                MiyoPermissions.openStoragePermissionSettings(context)
            }
        }

        DisposableEffect(lifecycleOwner.lifecycle) {
            val observer = object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    permissionViewModel.refresh()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        LaunchedEffect(
            shouldShowInitialSetup,
            permissionState.storageAutoRedirectComplete,
            permissionState.snapshot.missingCriticalStorage,
        ) {
            if (
                !shouldShowInitialSetup &&
                permissionState.snapshot.missingCriticalStorage &&
                !permissionState.storageAutoRedirectComplete
            ) {
                permissionViewModel.markStorageAutoRedirectComplete()
                configureStoragePermissions()
            }
        }

        val fullScreenRoutes = remember {
            setOf(
                Screen.BrowseSearch.route,
                Screen.SourceRepositories.route,
                Screen.SourceMigration.route,
                Screen.SourceUpdates.route,
                Screen.DownloadQueue.route,
                Screen.SourceDetails.route,
                Screen.SourceVerifier.route,
                Screen.OnlineNovelDetails.route,
                Screen.BookDetails.route,
                Screen.Settings.route,
                Screen.AdvancedSettings.route,
                Screen.ReaderSettings.route,
                Screen.Terms.route,
            )
        }
        val currentRoute = currentDestination?.route
        val showBottomBar = currentRoute?.startsWith("reader/") != true && currentRoute !in fullScreenRoutes
        val openReader: (String, Int?) -> Unit = { bookId, chapterIndex ->
            val route = if (chapterIndex != null && chapterIndex >= 0) {
                "reader/$bookId?chapterIndex=$chapterIndex"
            } else {
                "reader/$bookId"
            }
            navController.navigate(route)
        }
        val openBookDetails: (String) -> Unit = { bookId ->
            navController.navigate("library/book/$bookId")
        }
        val openOnlineNovelDetails: (OnlineNovelSummary) -> Unit = { novel ->
            navController.navigate(
                "browse/novel?providerId=${novel.providerId.name}&path=${Uri.encode(novel.path)}&title=${Uri.encode(novel.title)}",
            )
        }
        val openSettings = {
            navController.navigate(Screen.Settings.route) {
                launchSingleTop = true
            }
        }
        val openBackupDialog = { showBackupDialog = true }
        val startExportBackup = { exportBackupLauncher.launch("miyo_backup.miyo") }
        val startImportBackup = { importBackupLauncher.launch(arrayOf("application/octet-stream", "application/zip", "*/*")) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background),
        ) {
            SpecialThemeBackdrop(
                readerThemeId = effectiveReaderThemeId,
                darkTheme = colors.isDark,
                modifier = Modifier.matchParentSize(),
            )
            MiyoScaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    if (showBottomBar) {
                        MiyoNavigationBar {
                            bottomNavItems.forEach { screen ->
                                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                MiyoNavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    label = screen.title,
                                ) { tint ->
                                    if (screen.iconRes != null) {
                                        Icon(
                                            painter = painterResource(screen.iconRes),
                                            contentDescription = screen.title,
                                            tint = tint,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.padding(innerPadding),
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            onOpenBook = openBookDetails,
                            onOpenThemePicker = { showThemePicker = true },
                            onOpenSettings = openSettings,
                            onSaveAndExport = openBackupDialog,
                            onAbout = { showAboutDialog = true },
                        )
                    }
                    composable(Screen.Library.route) {
                        LibraryScreen(
                            onOpenBook = openBookDetails,
                            onReadBook = { bookId -> openReader(bookId, null) },
                            onOpenBookDetails = openBookDetails,
                            onOpenBrowse = {
                                navController.navigate(Screen.Browse.route)
                            },
                            onOpenSettings = openSettings,
                            onOpenThemePicker = { showThemePicker = true },
                            onSaveAndExport = openBackupDialog,
                            onAbout = { showAboutDialog = true },
                        )
                    }
                    composable(Screen.Browse.route) {
                        BrowseWorkflowScreen(
                            onOpenSource = { sourceId -> navController.navigate("browse/source/$sourceId") },
                            onOpenGlobalSearch = { navController.navigate(Screen.BrowseSearch.route) },
                            onOpenRepositories = { navController.navigate(Screen.SourceRepositories.route) },
                            onOpenMigration = { navController.navigate(Screen.SourceMigration.route) },
                            onOpenDownloads = { navController.navigate(Screen.DownloadQueue.route) },
                            onOpenUpdates = { navController.navigate(Screen.SourceUpdates.route) },
                            onOpenSettings = openSettings,
                            onOpenThemePicker = { showThemePicker = true },
                            onSaveAndExport = openBackupDialog,
                            onAbout = { showAboutDialog = true },
                        )
                    }
                    composable(Screen.History.route) {
                        HistoryScreen(
                            onOpenBook = openBookDetails,
                            onOpenOnlineNovel = { providerId, path, title ->
                                navController.navigate(
                                    "browse/novel?providerId=$providerId&path=${Uri.encode(path)}&title=${Uri.encode(title ?: "")}",
                                )
                            },
                            onOpenSettings = openSettings,
                            onOpenThemePicker = { showThemePicker = true },
                            onSaveAndExport = openBackupDialog,
                            onAbout = { showAboutDialog = true },
                        )
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onOpenThemePicker = { showThemePicker = true },
                            onOpenAdvancedSettings = { navController.navigate(Screen.AdvancedSettings.route) },
                            onOpenReaderSettings = { navController.navigate(Screen.ReaderSettings.route) },
                        )
                    }
                    composable(Screen.AdvancedSettings.route) {
                        AdvancedSettingsScreen(
                            onBack = { navController.popBackStack() },
                            onOpenReaderSettings = { navController.navigate(Screen.ReaderSettings.route) },
                            onOpenTerms = { navController.navigate(Screen.Terms.route) },
                        )
                    }
                    composable(Screen.ReaderSettings.route) {
                        ReaderSettingsScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Screen.BrowseSearch.route) {
                        GlobalSourceSearchScreen(
                            onBack = { navController.popBackStack() },
                            onOpenSource = { sourceId -> navController.navigate("browse/source/$sourceId") },
                            onOpenVerifier = { sourceId -> navController.navigate("browse/source/$sourceId/verify") },
                            onOpenNovel = openOnlineNovelDetails,
                        )
                    }
                    composable(Screen.SourceRepositories.route) {
                        ProviderRepositoriesScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Screen.SourceMigration.route) {
                        MigrationWorkflowScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Screen.SourceUpdates.route) {
                        SourceUpdatesWorkflowScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Screen.DownloadQueue.route) {
                        DownloadsWorkflowScreen(onBack = { navController.popBackStack() })
                    }
                    composable(
                        route = Screen.SourceDetails.route,
                        arguments = listOf(navArgument("sourceId") { type = NavType.StringType }),
                    ) { backStackEntry ->
                        val sourceId = backStackEntry.arguments?.getString("sourceId").orEmpty()
                        SourceWorkflowDetailScreen(
                            sourceId = sourceId,
                            onBack = { navController.popBackStack() },
                            onOpenDownloads = { navController.navigate(Screen.DownloadQueue.route) },
                            onOpenVerifier = { navController.navigate("browse/source/$sourceId/verify") },
                            onOpenNovel = openOnlineNovelDetails,
                        )
                    }
                    composable(
                        route = Screen.OnlineNovelDetails.route,
                        arguments = listOf(
                            navArgument("providerId") {
                                type = NavType.StringType
                                defaultValue = ""
                            },
                            navArgument("path") {
                                type = NavType.StringType
                                defaultValue = ""
                            },
                            navArgument("title") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                        ),
                    ) { backStackEntry ->
                        OnlineNovelDetailsScreen(
                            providerId = backStackEntry.arguments?.getString("providerId").orEmpty(),
                            path = Uri.decode(backStackEntry.arguments?.getString("path").orEmpty()),
                            fallbackTitle = backStackEntry.arguments?.getString("title")?.let(Uri::decode),
                            onBack = { navController.popBackStack() },
                            onOpenReader = openReader,
                        )
                    }
                    composable(
                        route = Screen.SourceVerifier.route,
                        arguments = listOf(navArgument("sourceId") { type = NavType.StringType }),
                    ) { backStackEntry ->
                        SourceVerifierScreen(
                            sourceId = backStackEntry.arguments?.getString("sourceId").orEmpty(),
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(
                        route = Screen.BookDetails.route,
                        arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
                    ) {
                        BookDetailsScreen(
                            onBack = { navController.popBackStack() },
                            onOpenReader = openReader,
                        )
                    }
                    composable(
                        route = "reader/{bookId}?chapterIndex={chapterIndex}",
                        arguments = listOf(
                            navArgument("bookId") { type = NavType.StringType },
                            navArgument("chapterIndex") {
                                type = NavType.IntType
                                defaultValue = -1
                            },
                        ),
                    ) { backStackEntry ->
                        val bookId = backStackEntry.arguments?.getString("bookId") ?: return@composable
                        ReaderScreen(
                            bookId = bookId,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(Screen.Terms.route) {
                        TermsScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }

        if (showThemePicker) {
            ThemePickerBottomSheet(
                selectedThemeId = readerThemeId,
                onThemeSelected = themeViewModel::setReaderThemeId,
                onDismiss = { showThemePicker = false },
            )
        }

        if (showBackupDialog) {
            MiyoBackupChoiceDialog(
                onDismiss = { showBackupDialog = false },
                onExportData = startExportBackup,
                onImportData = startImportBackup,
            )
        }

        if (showAboutDialog) {
            MiyoAboutDialog(onDismiss = { showAboutDialog = false })
        }

        backupMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { backupMessage = null },
                title = { Text("MIYO Backup") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { backupMessage = null }) {
                        Text("Close")
                    }
                },
            )
        }

        if (shouldShowInitialSetup) {
            InitialSetupBottomSheet(
                initialReaderThemeId = effectiveReaderThemeId,
                onGrantInstallApps = { MiyoPermissions.openAppSettings(context) },
                onGrantNotifications = {
                    val runtimePermissions = MiyoPermissions.runtimePermissionsToRequest(context)
                    if (runtimePermissions.isNotEmpty()) {
                        requestRuntimePermissions.launch(runtimePermissions)
                    } else {
                        MiyoPermissions.openAppSettings(context)
                    }
                },
                onGrantBattery = { MiyoPermissions.openBatteryOptimizationSettings(context) },
                onPreviewThemeMode = { setupPreviewThemeMode = it },
                onPreviewReaderTheme = { setupPreviewReaderThemeId = it },
                onSave = themeViewModel::saveInitialSetup,
            )
        }

        if (
            !shouldShowInitialSetup &&
            permissionState.storageAutoRedirectComplete &&
            permissionState.snapshot.missingCriticalStorage &&
            !dismissStorageDialogThisSession
        ) {
            MiyoStoragePermissionDialog(
                snapshot = permissionState.snapshot,
                onConfigure = {
                    dismissStorageDialogThisSession = true
                    configureStoragePermissions()
                },
                onDismiss = { dismissStorageDialogThisSession = true },
            )
        }
    }
}
