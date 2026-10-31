package com.example.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.VideoLibraryApp
import com.example.ui.components.EmptyStateView
import com.example.ui.components.VideoGridCard
import com.example.ui.components.VideoListItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onNavigateToDetails: (Long) -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    val repository = remember { VideoLibraryApp.instance.videoRepository }
    val userPrefs = remember { VideoLibraryApp.instance.userPreferences }
    val scope = rememberCoroutineScope()

    val favorites by repository.getFavoriteVideosFlow().collectAsState(initial = emptyList())
    val viewMode by userPrefs.viewMode.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val displayedFavorites = remember(favorites, searchQuery) {
        if (searchQuery.isBlank()) favorites
        else favorites.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.categoryName.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Favorites",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${favorites.size} saved videos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val newMode = if (viewMode == "grid") "list" else "grid"
                            userPrefs.setViewMode(newMode)
                        },
                        modifier = Modifier.testTag("fav_view_toggle")
                    ) {
                        Icon(
                            imageVector = if (viewMode == "grid") Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle view"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (favorites.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter favorites...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("favorites_search_field"),
                    singleLine = true
                )
            }

            if (displayedFavorites.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Favorite,
                    title = if (searchQuery.isNotBlank()) "No Matching Favorites" else "No favorite videos yet",
                    message = if (searchQuery.isNotBlank()) "No favorites match '$searchQuery'." else "Tap the heart icon on any video in your library to add it here.",
                    actionButtonText = if (searchQuery.isNotBlank()) "Clear Filter" else "Browse Library",
                    onActionClick = {
                        if (searchQuery.isNotBlank()) searchQuery = ""
                        else onNavigateToLibrary()
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                if (viewMode == "grid") {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                            .testTag("favorites_grid"),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(displayedFavorites, key = { it.id }) { video ->
                            VideoGridCard(
                                video = video,
                                onClick = { onNavigateToDetails(video.id) },
                                onFavoriteClick = {
                                    scope.launch { repository.setFavorite(video.id, false) }
                                }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .testTag("favorites_list"),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayedFavorites, key = { it.id }) { video ->
                            VideoListItem(
                                video = video,
                                onClick = { onNavigateToDetails(video.id) },
                                onFavoriteClick = {
                                    scope.launch { repository.setFavorite(video.id, false) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
