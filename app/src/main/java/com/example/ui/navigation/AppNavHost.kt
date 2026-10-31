package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.categories.CategoriesScreen
import com.example.ui.components.AppBottomNavBar
import com.example.ui.details.VideoDetailsScreen
import com.example.ui.downloads.DownloadsScreen
import com.example.ui.favorites.FavoritesScreen
import com.example.ui.home.HomeScreen
import com.example.ui.importer.AddVideoScreen
import com.example.ui.importer.FtpImporterScreen
import com.example.ui.importer.ImportResultsScreen
import com.example.ui.importer.WebsiteImporterScreen
import com.example.ui.library.LibraryScreen
import com.example.ui.player.VideoPlayerScreen
import com.example.ui.search.SearchScreen
import com.example.ui.settings.SettingsScreen

@Composable
fun MainApp(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavRoute.Home.route

    val isTopLevelRoute = currentRoute in listOf(
        NavRoute.Home.route,
        NavRoute.Library.route,
        NavRoute.Favorites.route,
        NavRoute.Downloads.route,
        NavRoute.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (isTopLevelRoute) {
                AppBottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoute.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isTopLevelRoute) innerPadding else androidx.compose.foundation.layout.PaddingValues())
        ) {
            // Home
            composable(NavRoute.Home.route) {
                HomeScreen(
                    onNavigateToPlayer = { id -> navController.navigate(NavRoute.videoPlayer(id)) },
                    onNavigateToDetails = { id -> navController.navigate(NavRoute.videoDetails(id)) },
                    onNavigateToSearch = { navController.navigate(NavRoute.SEARCH) },
                    onNavigateToBrowser = { navController.navigate(NavRoute.BROWSER) },
                    onNavigateToAddVideo = { navController.navigate(NavRoute.ADD_VIDEO) },
                    onNavigateToWebImporter = { navController.navigate(NavRoute.WEBSITE_IMPORTER) },
                    onNavigateToFtpImporter = { navController.navigate(NavRoute.FTP_IMPORTER) },
                    onNavigateToCategory = { cat -> navController.navigate(NavRoute.categoryDetail(cat)) },
                    onNavigateToCategories = { navController.navigate(NavRoute.CATEGORIES) }
                )
            }

            // Categories
            composable(NavRoute.CATEGORIES) {
                com.example.ui.categories.CategoriesScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onCategoryClick = { cat -> navController.navigate(NavRoute.categoryDetail(cat)) }
                )
            }

            // Library
            composable(NavRoute.Library.route) {
                LibraryScreen(
                    onNavigateToDetails = { id -> navController.navigate(NavRoute.videoDetails(id)) },
                    onNavigateToPlayer = { id -> navController.navigate(NavRoute.videoPlayer(id)) },
                    onNavigateToAddVideo = { navController.navigate(NavRoute.ADD_VIDEO) }
                )
            }

            // Category filtered Library
            composable(
                route = NavRoute.CATEGORY_DETAIL,
                arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
            ) { backStackEntry ->
                val categoryName = backStackEntry.arguments?.getString("categoryName")
                LibraryScreen(
                    onNavigateToDetails = { id -> navController.navigate(NavRoute.videoDetails(id)) },
                    onNavigateToPlayer = { id -> navController.navigate(NavRoute.videoPlayer(id)) },
                    onNavigateToAddVideo = { navController.navigate(NavRoute.ADD_VIDEO) },
                    initialCategoryFilter = categoryName
                )
            }

            // Favorites
            composable(NavRoute.Favorites.route) {
                FavoritesScreen(
                    onNavigateToDetails = { id -> navController.navigate(NavRoute.videoDetails(id)) },
                    onNavigateToLibrary = { navController.navigate(NavRoute.Library.route) }
                )
            }

            // Downloads
            composable(NavRoute.Downloads.route) {
                DownloadsScreen(
                    onNavigateToPlayer = { id -> navController.navigate(NavRoute.videoPlayer(id)) },
                    onNavigateToLibrary = { navController.navigate(NavRoute.Library.route) }
                )
            }

            // Settings
            composable(NavRoute.Settings.route) {
                SettingsScreen()
            }

            // Search
            composable(NavRoute.SEARCH) {
                SearchScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetails = { id -> navController.navigate(NavRoute.videoDetails(id)) }
                )
            }

            // Video Details
            composable(
                route = NavRoute.VIDEO_DETAILS,
                arguments = listOf(navArgument("videoId") { type = NavType.LongType })
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getLong("videoId") ?: 0L
                VideoDetailsScreen(
                    videoId = videoId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlayer = { id -> navController.navigate(NavRoute.videoPlayer(id)) }
                )
            }

            // Video Player
            composable(
                route = NavRoute.VIDEO_PLAYER,
                arguments = listOf(navArgument("videoId") { type = NavType.LongType })
            ) { backStackEntry ->
                val videoId = backStackEntry.arguments?.getLong("videoId") ?: 0L
                VideoPlayerScreen(
                    videoId = videoId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Add Video
            composable(NavRoute.ADD_VIDEO) {
                AddVideoScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlayer = { id -> navController.navigate(NavRoute.videoPlayer(id)) }
                )
            }

            // Website Importer
            composable(NavRoute.WEBSITE_IMPORTER) {
                WebsiteImporterScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToResults = { navController.navigate(NavRoute.IMPORT_RESULTS) }
                )
            }

            // FTP Importer
            composable(NavRoute.FTP_IMPORTER) {
                FtpImporterScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToResults = { navController.navigate(NavRoute.IMPORT_RESULTS) }
                )
            }

            // Import Results
            composable(NavRoute.IMPORT_RESULTS) {
                ImportResultsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLibrary = {
                        navController.navigate(NavRoute.Library.route) {
                            popUpTo(NavRoute.Home.route)
                        }
                    }
                )
            }

            // In-Built Video Browser
            composable(NavRoute.BROWSER) {
                com.example.ui.browser.BrowserScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlayer = { id -> navController.navigate(NavRoute.videoPlayer(id)) }
                )
            }
        }
    }
}
