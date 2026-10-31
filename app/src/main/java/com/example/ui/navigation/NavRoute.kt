package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavRoute(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : NavRoute("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Library : NavRoute("library", "Library", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary)
    object Favorites : NavRoute("favorites", "Favorites", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder)
    object Downloads : NavRoute("downloads", "Downloads", Icons.Filled.Download, Icons.Outlined.Download)
    object Settings : NavRoute("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)

    companion object {
        const val SEARCH = "search"
        const val VIDEO_DETAILS = "video_details/{videoId}"
        const val VIDEO_PLAYER = "player/{videoId}"
        const val ADD_VIDEO = "add_video"
        const val WEBSITE_IMPORTER = "import_website"
        const val FTP_IMPORTER = "import_ftp"
        const val IMPORT_RESULTS = "import_results"
        const val CATEGORIES = "categories"
        const val CATEGORY_DETAIL = "category/{categoryName}"
        const val BROWSER = "browser"

        fun videoDetails(videoId: Long) = "video_details/$videoId"
        fun videoPlayer(videoId: Long) = "player/$videoId"
        fun categoryDetail(categoryName: String) = "category/$categoryName"
    }
}
