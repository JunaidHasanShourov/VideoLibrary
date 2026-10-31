package com.example.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.VideoLibraryApp
import com.example.data.database.VideoEntity
import com.example.model.FilterOption
import com.example.model.SortOption
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FilterChipRow
import com.example.ui.components.VideoGridCard
import com.example.ui.components.VideoListItem
import com.example.ui.theme.ElectricViolet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToDetails: (Long) -> Unit,
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToAddVideo: () -> Unit,
    initialCategoryFilter: String? = null
) {
    val repository = remember { VideoLibraryApp.instance.videoRepository }
    val userPrefs = remember { VideoLibraryApp.instance.userPreferences }
    val scope = rememberCoroutineScope()

    val allVideos by repository.getAllVideosFlow().collectAsState(initial = emptyList())
    val viewMode by userPrefs.viewMode.collectAsState()

    var selectedFilter by remember { mutableStateOf(FilterOption.ALL) }
    var selectedSort by remember { mutableStateOf(SortOption.RECENTLY_ADDED) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Filter and Sort logic
    val filteredVideos = remember(allVideos, selectedFilter, selectedSort, initialCategoryFilter) {
        var list = allVideos

        if (initialCategoryFilter != null) {
            list = list.filter { it.categoryName.equals(initialCategoryFilter, ignoreCase = true) }
        }

        list = when (selectedFilter) {
            FilterOption.ALL -> list
            FilterOption.FAVORITES -> list.filter { it.isFavorite }
            FilterOption.DOWNLOADED -> list.filter { it.isDownloaded }
            FilterOption.STREAMING -> list.filter { !it.isDownloaded && !it.isLiveStream }
            FilterOption.LIVE_STREAMS -> list.filter { it.isLiveStream }
            FilterOption.WATCHED -> list.filter { !it.isLiveStream && it.watchedPercentage >= 0.90f }
            FilterOption.UNWATCHED -> list.filter { !it.isLiveStream && it.watchedPercentage < 0.90f }
            FilterOption.HAS_SUBTITLES -> list.filter { !it.subtitleUri.isNullOrBlank() }
        }

        when (selectedSort) {
            SortOption.TITLE_ASC -> list.sortedBy { it.title.lowercase() }
            SortOption.TITLE_DESC -> list.sortedByDescending { it.title.lowercase() }
            SortOption.RECENTLY_ADDED -> list.sortedByDescending { it.dateAdded }
            SortOption.RECENTLY_PLAYED -> list.sortedByDescending { it.lastPlayed }
            SortOption.DURATION -> list.sortedByDescending { it.durationMs }
            SortOption.RATING -> list.sortedByDescending { it.rating }
            SortOption.FILE_SIZE -> list.sortedByDescending { it.fileSize }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (initialCategoryFilter != null) initialCategoryFilter else "Library",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${filteredVideos.size} videos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Sort Menu
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("library_sort_btn")
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOption.values().forEach { sort ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = sort.displayName,
                                            fontWeight = if (sort == selectedSort) FontWeight.Bold else FontWeight.Normal,
                                            color = if (sort == selectedSort) ElectricViolet else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        selectedSort = sort
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Grid / List toggle
                    IconButton(
                        onClick = {
                            val newMode = if (viewMode == "grid") "list" else "grid"
                            userPrefs.setViewMode(newMode)
                        },
                        modifier = Modifier.testTag("library_view_toggle")
                    ) {
                        Icon(
                            imageVector = if (viewMode == "grid") Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle view mode"
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
            // Filter chips
            FilterChipRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )

            if (filteredVideos.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.VideoLibrary,
                    title = "No Videos Found",
                    message = if (selectedFilter != FilterOption.ALL) "No videos match the selected filter." else "Your Video Library is empty. Add a video or crawl a website to begin.",
                    actionButtonText = if (selectedFilter == FilterOption.ALL) "Add Video" else "Clear Filter",
                    onActionClick = {
                        if (selectedFilter != FilterOption.ALL) {
                            selectedFilter = FilterOption.ALL
                        } else {
                            onNavigateToAddVideo()
                        }
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
                            .testTag("library_grid"),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredVideos, key = { it.id }) { video ->
                            VideoGridCard(
                                video = video,
                                onClick = { onNavigateToDetails(video.id) },
                                onFavoriteClick = {
                                    scope.launch { repository.setFavorite(video.id, !video.isFavorite) }
                                }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .testTag("library_list"),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredVideos, key = { it.id }) { video ->
                            VideoListItem(
                                video = video,
                                onClick = { onNavigateToDetails(video.id) },
                                onFavoriteClick = {
                                    scope.launch { repository.setFavorite(video.id, !video.isFavorite) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
