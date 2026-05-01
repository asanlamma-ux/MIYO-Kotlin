package com.miyu.reader.ui

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.miyu.reader.R
import com.miyu.reader.domain.model.ThemeMode
import com.miyu.reader.ui.home.HomeScreen
import com.miyu.reader.ui.library.BookDetailsScreen
import com.miyu.reader.ui.library.LibraryScreen
import com.miyu.reader.ui.library.OnlineNovelBrowserWorkspace
import com.miyu.reader.ui.library.OpdsCatalogWorkspace
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
import com.miyu.reader.ui.theme.DefaultReaderThemeId
import com.miyu.reader.ui.theme.LocalMIYUColors
import com.miyu.reader.ui.theme.MIYUTheme
import com.miyu.reader.ui.theme.SpecialThemeBackdrop
import com.miyu.reader.viewmodel.LibraryViewModel
import com.miyu.reader.viewmodel.ThemeViewModel

sealed class Screen(
    val route: String,
    val title: String,
    @DrawableRes val iconRes: Int? = null,
    val icon: ImageVector? = null,
) {
    data object Home : Screen("home", "Home", iconRes = R.drawable.ic_nav_home)
    data object Library : Screen("library", "Library", iconRes = R.drawable.ic_nav_library)
    data object Terms : Screen("terms", "Terms", icon = Icons.Outlined.Translate)
    data object History : Screen("history", "History", iconRes = R.drawable.ic_nav_history)
    data object Settings : Screen("settings", "Settings", iconRes = R.drawable.ic_nav_settings)
    data object OnlineBrowser : Screen("library/online", "Online Browser")
    data object OpdsCatalogs : Screen("library/opds", "OPDS Catalogs")
    data object BookDetails : Screen("library/book/{bookId}", "Book Details")
    data object AdvancedSettings : Screen("settings/advanced", "Advanced Settings")
    data object ReaderSettings : Screen("settings/reader", "Reader Settings")
}

val bottomNavItems = listOf(
    Screen.Home, Screen.Library, Screen.Terms, Screen.History, Screen.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MIYUApp() {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val readerThemeId by themeViewModel.readerThemeId.collectAsStateWithLifecycle(initialValue = DefaultReaderThemeId)
    val shouldShowInitialSetup by themeViewModel.shouldShowInitialSetup.collectAsStateWithLifecycle(initialValue = false)

    MIYUTheme(themeMode = themeMode, readerThemeId = readerThemeId) {
        val navController = rememberNavController()
        val colors = LocalMIYUColors.current
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        var showThemePicker by remember { mutableStateOf(false) }

        val fullScreenRoutes = remember {
            setOf(
                Screen.OnlineBrowser.route,
                Screen.OpdsCatalogs.route,
                Screen.BookDetails.route,
                Screen.AdvancedSettings.route,
                Screen.ReaderSettings.route,
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background),
        ) {
            SpecialThemeBackdrop(
                readerThemeId = readerThemeId,
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
                                            modifier = Modifier.size(24.dp),
                                        )
                                    } else {
                                        Icon(
                                            imageVector = screen.icon ?: Icons.Outlined.Translate,
                                            contentDescription = screen.title,
                                            tint = tint,
                                            modifier = Modifier.size(24.dp),
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
                            onOpenBook = { bookId ->
                                openReader(bookId, null)
                            },
                            onOpenThemePicker = { showThemePicker = true },
                        )
                    }
                    composable(Screen.Library.route) {
                        LibraryScreen(
                            onOpenBook = { bookId ->
                                openReader(bookId, null)
                            },
                            onOpenBookDetails = { bookId ->
                                navController.navigate("library/book/$bookId")
                            },
                            onOpenOnline = {
                                navController.navigate(Screen.OnlineBrowser.route)
                            },
                            onOpenOpds = {
                                navController.navigate(Screen.OpdsCatalogs.route)
                            },
                        )
                    }
                    composable(Screen.Terms.route) { TermsScreen() }
                    composable(Screen.History.route) {
                        HistoryScreen(onOpenBook = { bookId ->
                            openReader(bookId, null)
                        })
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            onOpenThemePicker = { showThemePicker = true },
                            onOpenAdvancedSettings = { navController.navigate(Screen.AdvancedSettings.route) },
                        )
                    }
                    composable(Screen.AdvancedSettings.route) {
                        AdvancedSettingsScreen(
                            onBack = { navController.popBackStack() },
                            onOpenReaderSettings = { navController.navigate(Screen.ReaderSettings.route) },
                        )
                    }
                    composable(Screen.ReaderSettings.route) {
                        ReaderSettingsScreen(onBack = { navController.popBackStack() })
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
                    composable(Screen.OnlineBrowser.route) { backStackEntry ->
                        val libraryEntry = remember(backStackEntry) {
                            navController.getBackStackEntry(Screen.Library.route)
                        }
                        val libraryViewModel: LibraryViewModel = hiltViewModel(libraryEntry)
                        OnlineNovelBrowserWorkspace(
                            onDismiss = { navController.popBackStack() },
                            onImportGeneratedEpub = { generated ->
                                libraryViewModel.importGeneratedOnlineNovelEpub(
                                    filePath = generated.filePath,
                                    fileName = generated.fileName,
                                    suggestedTitle = generated.title,
                                )
                            },
                        )
                    }
                    composable(Screen.OpdsCatalogs.route) { backStackEntry ->
                        val libraryEntry = remember(backStackEntry) {
                            navController.getBackStackEntry(Screen.Library.route)
                        }
                        val libraryViewModel: LibraryViewModel = hiltViewModel(libraryEntry)
                        OpdsCatalogWorkspace(
                            onDismiss = { navController.popBackStack() },
                            onImportEntry = { entry, href ->
                                libraryViewModel.importBookFromRemoteEpub(href, entry.title)
                            },
                        )
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

        if (shouldShowInitialSetup) {
            InitialSetupBottomSheet(
                initialReaderThemeId = readerThemeId,
                onSkip = themeViewModel::skipInitialSetup,
                onPreviewThemeMode = themeViewModel::setThemeMode,
                onPreviewReaderTheme = themeViewModel::setReaderThemeId,
                onSave = themeViewModel::saveInitialSetup,
            )
        }
    }
}
