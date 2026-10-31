package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.VideoLibraryApp
import com.example.data.database.VideoEntity
import com.example.ui.components.VideoGridCard
import com.example.ui.components.formatDuration
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToDetails: (Long) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToAddVideo: () -> Unit,
    onNavigateToWebImporter: () -> Unit,
    onNavigateToFtpImporter: () -> Unit,
    onNavigateToCategory: (String) -> Unit,
    onNavigateToCategories: () -> Unit
) {
    val repository = remember { VideoLibraryApp.instance.videoRepository }
    val scope = rememberCoroutineScope()

    val continueWatching by repository.getContinueWatchingFlow().collectAsState(initial = emptyList())
    val recentlyPlayed by repository.getRecentlyPlayedFlow().collectAsState(initial = emptyList())
    val recentlyAdded by repository.getRecentlyAddedFlow().collectAsState(initial = emptyList())
    val favorites by repository.getFavoriteVideosFlow().collectAsState(initial = emptyList())
    val categories by repository.getAllCategoriesFlow().collectAsState(initial = emptyList())
    val liveStreams by repository.getLiveStreamsFlow().collectAsState(initial = emptyList())

    val featuredVideo = remember(recentlyAdded) { recentlyAdded.firstOrNull() }
    var showAddMenuSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = ElectricViolet,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Video Library",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSearch,
                        modifier = Modifier.testTag("home_search_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search videos",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddMenuSheet = true },
                containerColor = ElectricViolet,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("home_fab_add")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add or Import")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Featured Hero Banner
            if (featuredVideo != null) {
                item {
                    HeroBanner(
                        video = featuredVideo,
                        onPlayClick = { onNavigateToPlayer(featuredVideo.id) },
                        onDetailsClick = { onNavigateToDetails(featuredVideo.id) }
                    )
                }
            }

            // Square Categories & Quick Access Section
            item {
                SectionHeader(title = "Browse & Explore", onSeeAll = onNavigateToCategories)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SquareCategoryCard(
                        categoryCount = categories.size,
                        onClick = onNavigateToCategories
                    )

                    if (liveStreams.isNotEmpty()) {
                        SquareLiveStreamsCard(
                            streamCount = liveStreams.size,
                            onClick = { onNavigateToCategory("Live Streams") }
                        )
                    }
                }
            }

            // Continue Watching Section
            if (continueWatching.isNotEmpty()) {
                item {
                    SectionHeader(title = "Continue Watching", onSeeAll = null)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(continueWatching) { video ->
                            VideoGridCard(
                                video = video,
                                onClick = { onNavigateToPlayer(video.id) },
                                onFavoriteClick = {
                                    scope.launch { repository.setFavorite(video.id, !video.isFavorite) }
                                },
                                modifier = Modifier.width(220.dp)
                            )
                        }
                    }
                }
            }

            // Recently Played Section
            if (recentlyPlayed.isNotEmpty()) {
                item {
                    SectionHeader(title = "Recently Played", onSeeAll = null)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recentlyPlayed) { video ->
                            VideoGridCard(
                                video = video,
                                onClick = { onNavigateToPlayer(video.id) },
                                onFavoriteClick = {
                                    scope.launch { repository.setFavorite(video.id, !video.isFavorite) }
                                },
                                modifier = Modifier.width(200.dp)
                            )
                        }
                    }
                }
            }

            // Recently Added Section
            if (recentlyAdded.isNotEmpty()) {
                item {
                    SectionHeader(title = "Recently Added", onSeeAll = null)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recentlyAdded) { video ->
                            VideoGridCard(
                                video = video,
                                onClick = { onNavigateToDetails(video.id) },
                                onFavoriteClick = {
                                    scope.launch { repository.setFavorite(video.id, !video.isFavorite) }
                                },
                                modifier = Modifier.width(200.dp)
                            )
                        }
                    }
                }
            }

            // Favorites Section
            if (favorites.isNotEmpty()) {
                item {
                    SectionHeader(title = "Favorites", onSeeAll = null)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(favorites) { video ->
                            VideoGridCard(
                                video = video,
                                onClick = { onNavigateToDetails(video.id) },
                                onFavoriteClick = {
                                    scope.launch { repository.setFavorite(video.id, !video.isFavorite) }
                                },
                                modifier = Modifier.width(200.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Modal Bottom Sheet for Add/Import options
    if (showAddMenuSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showAddMenuSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Add Media to Library",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(16.dp))

                ImportOptionItem(
                    icon = Icons.Default.Link,
                    title = "Add Video URL",
                    description = "Add a single public direct media stream or video file URL",
                    onClick = {
                        showAddMenuSheet = false
                        onNavigateToAddVideo()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ImportOptionItem(
                    icon = Icons.Default.Language,
                    title = "Import Website / Directory",
                    description = "Recursively crawl a website, Apache/Nginx directory index or media server",
                    onClick = {
                        showAddMenuSheet = false
                        onNavigateToWebImporter()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                ImportOptionItem(
                    icon = Icons.Default.Storage,
                    title = "Import from FTP / FTPS",
                    description = "Scan public or authenticated FTP media archives",
                    onClick = {
                        showAddMenuSheet = false
                        onNavigateToFtpImporter()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun HeroBanner(
    video: VideoEntity,
    onPlayClick: () -> Unit,
    onDetailsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(210.dp)
            .testTag("hero_banner"),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!video.artworkUri.isNullOrBlank()) {
                AsyncImage(
                    model = video.artworkUri,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Cinematic gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0x33000000), Color(0xAA000000), Color(0xF00D0B14))
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Surface(
                    color = ElectricViolet,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = video.categoryName.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (video.description.isNotBlank()) {
                    Text(
                        text = video.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onPlayClick,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("hero_play_btn")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play Now", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onDetailsClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x55FFFFFF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Details", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    onSeeAll: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        if (onSeeAll != null) {
            Text(
                text = "See All",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = ElectricViolet,
                modifier = Modifier.clickable(onClick = onSeeAll)
            )
        }
    }
}

@Composable
fun ImportOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = ElectricViolet.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = ElectricViolet,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SquareCategoryCard(
    categoryCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .size(116.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("home_categories_square_btn"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = ElectricViolet.copy(alpha = 0.15f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Categories",
                        tint = ElectricViolet,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Categories",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$categoryCount folders",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun SquareLiveStreamsCard(
    streamCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .size(116.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("home_live_streams_square_btn"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFDC2626).copy(alpha = 0.15f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Live Streams",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Live Streams",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$streamCount live",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFEF4444),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
