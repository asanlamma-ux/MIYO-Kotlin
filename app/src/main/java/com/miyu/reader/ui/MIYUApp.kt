package com.miyu.reader.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.annotation.DrawableRes
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
import com.miyu.reader.ui.library.LibraryScreen
import com.miyu.reader.ui.terms.TermsScreen
import com.miyu.reader.ui.history.HistoryScreen
import com.miyu.reader.ui.settings.SettingsScreen
import com.miyu.reader.ui.reader.ReaderScreen
import com.miyu.reader.ui.theme.LocalMIYUColors
import com.miyu.reader.ui.theme.MIYUTheme
import com.miyu.reader.viewmodel.ThemeViewModel

sealed class Screen(val route: String, val title: String, @DrawableRes val icon: Int) {
    data object Home : Screen("home", "Home", R.drawable.ic_miyu_home)
    data object Library : Screen("library", "Library", R.drawable.ic_miyu_library)
    data object Terms : Screen("terms", "Terms", R.drawable.ic_miyu_terms)
    data object History : Screen("history", "History", R.drawable.ic_miyu_history)
    data object Settings : Screen("settings", "Settings", R.drawable.ic_miyu_settings)
}

val bottomNavItems = listOf(
    Screen.Home, Screen.Library, Screen.Terms, Screen.History, Screen.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MIYUApp() {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.LIGHT)
    val readerThemeId by themeViewModel.readerThemeId.collectAsStateWithLifecycle(initialValue = "sepia-classic")

    MIYUTheme(themeMode = themeMode, readerThemeId = readerThemeId) {
        val navController = rememberNavController()
        val colors = LocalMIYUColors.current
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        var bottomNavVisible by rememberSaveable { mutableStateOf(false) }
        var verticalDragAmount by remember { mutableStateOf(0f) }

        // Hide bottom bar when the reader is open
        val showBottomBar = currentDestination?.route?.startsWith("reader/") != true

        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    if (showBottomBar) {
                        AnimatedVisibility(
                            visible = bottomNavVisible,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                        ) {
                            NavigationBar {
                                bottomNavItems.forEach { screen ->
                                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                    NavigationBarItem(
                                        icon = {
                                            Icon(
                                                painter = painterResource(screen.icon),
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
                                        }
                                    )
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
                }
            }

            AnimatedVisibility(
                visible = showBottomBar && !bottomNavVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .height(48.dp)
                    .pointerInput(showBottomBar) {
                        detectVerticalDragGestures(
                            onDragStart = { verticalDragAmount = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                verticalDragAmount += dragAmount
                            },
                            onDragEnd = {
                                if (showBottomBar && verticalDragAmount < -32f) {
                                    bottomNavVisible = true
                                }
                            },
                        )
                    },
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .size(width = 42.dp, height = 5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(colors.accent.copy(alpha = 0.65f)),
                    )
                }
            }
        }
    }
}
