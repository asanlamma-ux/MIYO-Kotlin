package com.miyu.reader.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
<<<<<<< HEAD
=======
import androidx.navigation.NavType
>>>>>>> debug
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
<<<<<<< HEAD
import com.miyu.reader.domain.model.ThemeMode
import com.miyu.reader.ui.home.HomeScreen
import com.miyu.reader.ui.library.LibraryScreen
import com.miyu.reader.ui.library.TermsScreen
import com.miyu.reader.ui.library.HistoryScreen
import com.miyu.reader.ui.library.SettingsScreen
=======
import androidx.navigation.navArgument
import com.miyu.reader.domain.model.ThemeMode
import com.miyu.reader.ui.home.HomeScreen
import com.miyu.reader.ui.library.LibraryScreen
import com.miyu.reader.ui.terms.TermsScreen
import com.miyu.reader.ui.history.HistoryScreen
import com.miyu.reader.ui.settings.SettingsScreen
import com.miyu.reader.ui.reader.ReaderScreen
>>>>>>> debug
import com.miyu.reader.ui.theme.MIYUTheme
import com.miyu.reader.viewmodel.ThemeViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Outlined.Home, Icons.Filled.Home)
    data object Library : Screen("library", "Library", Icons.AutoMirrored.Outlined.LibraryBooks, Icons.AutoMirrored.Filled.LibraryBooks)
    data object Terms : Screen("terms", "Terms", Icons.Outlined.Translate, Icons.Filled.Translate)
    data object History : Screen("history", "History", Icons.Outlined.AccessTime, Icons.Filled.AccessTime)
    data object Settings : Screen("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
}

val bottomNavItems = listOf(
    Screen.Home, Screen.Library, Screen.Terms, Screen.History, Screen.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MIYUApp() {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.LIGHT)
<<<<<<< HEAD

    MIYUTheme(themeMode = themeMode) {
=======
    val readerThemeId by themeViewModel.readerThemeId.collectAsStateWithLifecycle(initialValue = "sepia-classic")

    MIYUTheme(themeMode = themeMode, readerThemeId = readerThemeId) {
>>>>>>> debug
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

<<<<<<< HEAD
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.icon,
                                    contentDescription = screen.title,
                                )
                            },
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
                        )
=======
        // Hide bottom bar when the reader is open
        val showBottomBar = currentDestination?.route?.startsWith("reader/") != true

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        bottomNavItems.forEach { screen ->
                            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (selected) screen.selectedIcon else screen.icon,
                                        contentDescription = screen.title,
                                    )
                                },
                                label = { Text(screen.title) },
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
                            )
                        }
>>>>>>> debug
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding),
            ) {
<<<<<<< HEAD
                composable(Screen.Home.route) { HomeScreen() }
                composable(Screen.Library.route) { LibraryScreen() }
                composable(Screen.Terms.route) { TermsScreen() }
                composable(Screen.History.route) { HistoryScreen() }
                composable(Screen.Settings.route) { SettingsScreen() }
=======
                composable(Screen.Home.route) {
                    HomeScreen(onOpenBook = { bookId ->
                        navController.navigate("reader/$bookId")
                    })
                }
                composable(Screen.Library.route) {
                    LibraryScreen(onOpenBook = { bookId ->
                        navController.navigate("reader/$bookId")
                    })
                }
                composable(Screen.Terms.route) { TermsScreen() }
                composable(Screen.History.route) {
                    HistoryScreen(onOpenBook = { bookId ->
                        navController.navigate("reader/$bookId")
                    })
                }
                composable(Screen.Settings.route) { SettingsScreen() }
                composable(
                    route = "reader/{bookId}",
                    arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val bookId = backStackEntry.arguments?.getString("bookId") ?: return@composable
                    ReaderScreen(
                        bookId = bookId,
                        onBack = { navController.popBackStack() },
                    )
                }
>>>>>>> debug
            }
        }
    }
}