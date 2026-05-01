package com.miyu.reader.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.miyu.reader.ui.components.ThemePickerBottomSheet
import com.miyu.reader.ui.theme.DefaultReaderThemeId
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
    val readerThemeId by themeViewModel.readerThemeId.collectAsStateWithLifecycle(initialValue = DefaultReaderThemeId)

    MIYUTheme(themeMode = themeMode, readerThemeId = readerThemeId) {
        val navController = rememberNavController()
        val colors = LocalMIYUColors.current
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        var showThemePicker by remember { mutableStateOf(false) }

        val showBottomBar = currentDestination?.route?.startsWith("reader/") != true

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomBar) {
                    Surface(
                        color = colors.cardBackground,
                        tonalElevation = 0.dp,
                        shadowElevation = 8.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            bottomNavItems.forEach { screen ->
                                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                        .padding(vertical = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(bottom = 6.dp)
                                            .size(width = 20.dp, height = 3.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                if (selected) colors.accent else Color.Transparent,
                                            ),
                                    )
                                    Icon(
                                        painter = painterResource(screen.icon),
                                        contentDescription = screen.title,
                                        tint = if (selected) colors.accent else colors.secondaryText,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        screen.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) colors.onBackground else colors.secondaryText,
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
                            navController.navigate("reader/$bookId")
                        },
                        onOpenThemePicker = { showThemePicker = true },
                    )
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
                composable(Screen.Settings.route) {
                    SettingsScreen(onOpenThemePicker = { showThemePicker = true })
                }
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

        if (showThemePicker) {
            ThemePickerBottomSheet(
                selectedThemeId = readerThemeId,
                onThemeSelected = themeViewModel::setReaderThemeId,
                onDismiss = { showThemePicker = false },
            )
        }
    }
}
