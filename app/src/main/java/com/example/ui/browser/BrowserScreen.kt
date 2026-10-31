package com.example.ui.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.VideoLibraryApp
import com.example.crawler.FilenameCleaner
import com.example.crawler.WebCrawler
import com.example.data.database.VideoEntity
import com.example.data.network.NetworkClient
import com.example.model.CrawlerConfig
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder

data class DetectedVideoItem(
    val url: String,
    val title: String,
    val format: String,
    val artworkUrl: String? = null,
    val isAdded: Boolean = false,
    val isSelected: Boolean = true
)

data class BrowserShortcut(
    val title: String,
    val subtitle: String,
    val url: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { VideoLibraryApp.instance.videoRepository }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    var webView by remember { mutableStateOf<WebView?>(null) }
    var inputUrl by remember { mutableStateOf("") }
    var currentWebUrl by remember { mutableStateOf("") }
    var pageTitle by remember { mutableStateOf("") }
    var isStartPage by remember { mutableStateOf(true) }
    var pageProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }

    val detectedVideos = remember { mutableStateListOf<DetectedVideoItem>() }
    var showReviewSheet by remember { mutableStateOf(false) }
    var showFloatingBanner by remember { mutableStateOf(true) }
    var isDeepScanning by remember { mutableStateOf(false) }

    val popularShortcuts = remember {
        listOf(
            BrowserShortcut(
                title = "Internet Archive",
                subtitle = "Classic films, documentaries & open media",
                url = "https://archive.org/details/moviesandfilms",
                icon = Icons.Default.Explore
            ),
            BrowserShortcut(
                title = "Wikimedia Commons",
                subtitle = "Public domain videos & educational films",
                url = "https://commons.wikimedia.org/wiki/Category:Videos",
                icon = Icons.Default.TravelExplore
            ),
            BrowserShortcut(
                title = "Open Media Samples",
                subtitle = "Big Buck Bunny, Sintel, Tears of Steel",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/",
                icon = Icons.Default.PlayArrow
            ),
            BrowserShortcut(
                title = "Public Domain Movies",
                subtitle = "Feature movies in the public domain",
                url = "https://publicdomainmovie.net/",
                icon = Icons.Default.VideoLibrary
            )
        )
    }

    val videoExtensions = remember { setOf("mp4", "m3u8", "webm", "mkv", "mov", "ts", "m4v") }

    fun addDetectedVideo(url: String, titleHint: String? = null, artwork: String? = null) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank() || cleanUrl.startsWith("blob:") || cleanUrl.startsWith("data:")) return
        if (detectedVideos.any { it.url.equals(cleanUrl, ignoreCase = true) }) return

        val filename = cleanUrl.substringAfterLast('/').substringBefore('?').substringBefore('#')
        val ext = filename.substringAfterLast('.', "mp4").lowercase()
        val title = titleHint?.takeIf { it.isNotBlank() }
            ?: FilenameCleaner.resolveTitle(filename, cleanUrl, pageTitle.takeIf { it.isNotBlank() })

        scope.launch {
            val existsInDb = repository.getVideoBySourceUrl(cleanUrl) != null
            detectedVideos.add(
                DetectedVideoItem(
                    url = cleanUrl,
                    title = title,
                    format = ext,
                    artworkUrl = artwork,
                    isAdded = existsInDb,
                    isSelected = !existsInDb
                )
            )
        }
    }

    fun navigateToUrl(rawInput: String) {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return
        focusManager.clearFocus()

        val targetUrl = when {
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> "https://duckduckgo.com/?q=${URLEncoder.encode(trimmed, "UTF-8")}"
        }

        inputUrl = targetUrl
        isStartPage = false
        showFloatingBanner = true
        webView?.loadUrl(targetUrl)
    }

    fun runDeepScan() {
        val url = currentWebUrl.takeIf { it.isNotBlank() && !isStartPage } ?: return
        isDeepScanning = true
        scope.launch {
            try {
                val crawler = WebCrawler(
                    config = CrawlerConfig(
                        maxDepth = 1,
                        maxPages = 8,
                        maxVideos = 50,
                        requestDelayMs = 150
                    )
                )
                val discovered = crawler.crawl(url)
                discovered.forEach { item ->
                    addDetectedVideo(item.url, item.title, item.artworkUrl)
                }
                snackbarHostState.showSnackbar("Deep scan found ${discovered.size} media stream(s)")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Scan error: ${e.message?.take(50)}")
            } finally {
                isDeepScanning = false
            }
        }
    }

    fun importSingleVideo(item: DetectedVideoItem) {
        scope.launch {
            val entity = VideoEntity(
                title = item.title,
                originalFilename = item.url.substringAfterLast('/').substringBefore('?'),
                description = "Imported from web: $currentWebUrl",
                sourceUrl = item.url,
                artworkUri = item.artworkUrl,
                categoryName = "Web Downloads",
                format = item.format,
                dateAdded = System.currentTimeMillis()
            )
            repository.insertVideo(entity)

            val idx = detectedVideos.indexOfFirst { it.url == item.url }
            if (idx != -1) {
                detectedVideos[idx] = detectedVideos[idx].copy(isAdded = true)
            }
            snackbarHostState.showSnackbar("Added '${item.title}' to Library")
        }
    }

    fun importAllSelected() {
        val toImport = detectedVideos.filter { it.isSelected && !it.isAdded }
        if (toImport.isEmpty()) return

        scope.launch {
            val entities = toImport.map { item ->
                VideoEntity(
                    title = item.title,
                    originalFilename = item.url.substringAfterLast('/').substringBefore('?'),
                    description = "Imported from web: $currentWebUrl",
                    sourceUrl = item.url,
                    artworkUri = item.artworkUrl,
                    categoryName = "Web Downloads",
                    format = item.format,
                    dateAdded = System.currentTimeMillis()
                )
            }
            repository.insertVideos(entities)

            // Update state
            toImport.forEach { imported ->
                val idx = detectedVideos.indexOfFirst { it.url == imported.url }
                if (idx != -1) {
                    detectedVideos[idx] = detectedVideos[idx].copy(isAdded = true)
                }
            }
            snackbarHostState.showSnackbar("Imported ${entities.size} video(s) to Library")
            showReviewSheet = false
        }
    }

    // Back handling
    BackHandler {
        when {
            showReviewSheet -> showReviewSheet = false
            webView?.canGoBack() == true -> webView?.goBack()
            !isStartPage -> {
                isStartPage = true
                inputUrl = ""
                webView?.loadUrl("about:blank")
            }
            else -> onNavigateBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            placeholder = { Text("Search or type URL...", fontSize = 14.sp) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isStartPage) Icons.Default.Search else Icons.Default.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = ElectricViolet
                                )
                            },
                            trailingIcon = {
                                if (inputUrl.isNotBlank()) {
                                    IconButton(onClick = { inputUrl = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = { navigateToUrl(inputUrl) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricViolet,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("browser_url_input")
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (webView?.canGoBack() == true) {
                                    webView?.goBack()
                                } else if (!isStartPage) {
                                    isStartPage = true
                                    inputUrl = ""
                                    webView?.loadUrl("about:blank")
                                } else {
                                    onNavigateBack()
                                }
                            },
                            modifier = Modifier.testTag("browser_back_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Manual video extraction icon with badge
                        IconButton(
                            onClick = { showReviewSheet = true },
                            modifier = Modifier.testTag("browser_extract_btn")
                        ) {
                            if (detectedVideos.isNotEmpty()) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = ElectricViolet,
                                            contentColor = Color.White
                                        ) {
                                            Text("${detectedVideos.size}")
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.VideoLibrary, contentDescription = "Detected Videos", tint = ElectricViolet)
                                }
                            } else {
                                Icon(Icons.Default.VideoLibrary, contentDescription = "Fetch Videos")
                            }
                        }

                        // Navigation controls menu
                        IconButton(
                            onClick = {
                                if (isStartPage) {
                                    navigateToUrl("https://duckduckgo.com")
                                } else {
                                    webView?.reload()
                                }
                            },
                            modifier = Modifier.testTag("browser_refresh_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload")
                        }

                        IconButton(
                            onClick = {
                                isStartPage = true
                                inputUrl = ""
                                webView?.loadUrl("about:blank")
                            },
                            modifier = Modifier.testTag("browser_home_btn")
                        ) {
                            Icon(Icons.Default.Home, contentDescription = "Start Page")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Web loading progress bar
                if (isLoading && pageProgress in 0.01f..0.99f) {
                    LinearProgressIndicator(
                        progress = { pageProgress },
                        color = ElectricViolet,
                        trackColor = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Embedded WebView
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            userAgentString = NetworkClient.DEFAULT_USER_AGENT
                            mediaPlaybackRequiresUserGesture = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                url?.let {
                                    if (it != "about:blank") {
                                        currentWebUrl = it
                                        inputUrl = it
                                    }
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                pageTitle = view?.title ?: ""

                                // DOM Video Sniffing Script
                                view?.evaluateJavascript(
                                    """
                                    (function() {
                                        var results = [];
                                        var vids = document.querySelectorAll('video, video source');
                                        for (var i = 0; i < vids.length; i++) {
                                            var src = vids[i].src || vids[i].getAttribute('src');
                                            if (src && !src.startsWith('blob:')) {
                                                var poster = vids[i].poster || '';
                                                results.push(src + '|||' + poster);
                                            }
                                        }
                                        var links = document.querySelectorAll('a[href]');
                                        for (var j = 0; j < links.length; j++) {
                                            var href = links[j].href;
                                            if (href && (href.endsWith('.mp4') || href.endsWith('.m3u8') || href.endsWith('.webm') || href.endsWith('.mkv'))) {
                                                var img = links[j].querySelector('img');
                                                var thumb = img ? img.src : '';
                                                results.push(href + '|||' + thumb);
                                            }
                                        }
                                        return results.join(';;;');
                                    })();
                                    """.trimIndent()
                                ) { rawResult ->
                                    val cleaned = rawResult?.removeSurrounding("\"")?.replace("\\\"", "\"") ?: ""
                                    if (cleaned.isNotBlank() && cleaned != "null") {
                                        val items = cleaned.split(";;;")
                                        items.forEach { entry ->
                                            val parts = entry.split("|||")
                                            val videoUrl = parts.getOrNull(0)?.trim()
                                            val poster = parts.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
                                            if (!videoUrl.isNullOrBlank()) {
                                                addDetectedVideo(videoUrl, artwork = poster)
                                            }
                                        }
                                    }
                                }
                            }

                            override fun onLoadResource(view: WebView?, url: String?) {
                                super.onLoadResource(view, url)
                                url?.let { resUrl ->
                                    val ext = resUrl.substringAfterLast('/').substringBefore('?').substringAfterLast('.', "").lowercase()
                                    if (videoExtensions.contains(ext) || resUrl.contains(".m3u8", ignoreCase = true) || resUrl.contains(".mp4?", ignoreCase = true)) {
                                        addDetectedVideo(resUrl)
                                    }
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                pageProgress = newProgress / 100f
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                pageTitle = title ?: ""
                            }
                        }

                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Start Page Overlay (shown when isStartPage == true)
            if (isStartPage) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Surface(
                            shape = CircleShape,
                            color = ElectricViolet.copy(alpha = 0.15f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.TravelExplore,
                                    contentDescription = null,
                                    tint = ElectricViolet,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "In-Built Video Browser",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Search the web, access public directories, and import videos directly into your library with automated stream detection.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Quick Media Hubs",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Tap to open",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    items(popularShortcuts) { shortcut ->
                        Card(
                            onClick = { navigateToUrl(shortcut.url) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shortcut_${shortcut.title.lowercase().replace(" ", "_")}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = ElectricViolet.copy(alpha = 0.15f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(shortcut.icon, contentDescription = null, tint = ElectricViolet)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = shortcut.title,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = shortcut.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = NeonAmber.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = null,
                                    tint = NeonAmber,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Any webpage or public directory containing MP4, HLS (.m3u8), WebM or embedded videos will automatically be detected and ready for one-tap library import.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }

            // Dual Import UX: 1. Automatic Floating Detection Banner
            AnimatedVisibility(
                visible = !isStartPage && showFloatingBanner && detectedVideos.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("floating_detected_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = ElectricViolet,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.VideoLibrary,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${detectedVideos.size} Video(s) Detected",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Available for library import",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { showReviewSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("floating_review_videos_btn")
                            ) {
                                Text("Review & Add", fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { showFloatingBanner = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Dual Import UX: 2. Manual Video Review & Deep Scan Bottom Sheet
    if (showReviewSheet) {
        ModalBottomSheet(
            onDismissRequest = { showReviewSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Detected Media (${detectedVideos.size})",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Import detected streams or scan deeper",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalButton(
                        onClick = { runDeepScan() },
                        enabled = !isDeepScanning,
                        modifier = Modifier.testTag("deep_scan_button")
                    ) {
                        if (isDeepScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scanning...")
                        } else {
                            Icon(Icons.Default.TravelExplore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Deep Scan")
                        }
                    }
                }

                if (detectedVideos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No media detected on current view",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap 'Deep Scan' above to thoroughly inspect HTML & links.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(detectedVideos) { item ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Thumbnail / Icon
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1E1B2E),
                                        modifier = Modifier.size(width = 64.dp, height = 48.dp)
                                    ) {
                                        if (!item.artworkUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = item.artworkUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = ElectricViolet,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${item.format.uppercase()} • ${item.url}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    if (item.isAdded) {
                                        Surface(
                                            color = NeonCyan.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Added", style = MaterialTheme.typography.labelSmall, color = NeonCyan)
                                            }
                                        }
                                    } else {
                                        IconButton(
                                            onClick = { importSingleVideo(item) },
                                            modifier = Modifier.testTag("import_item_btn")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Add to Library", tint = ElectricViolet)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Batch import button
                    val unaddedCount = detectedVideos.count { !it.isAdded }
                    if (unaddedCount > 0) {
                        Button(
                            onClick = { importAllSelected() },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .height(48.dp)
                                .testTag("import_all_videos_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import All Videos ($unaddedCount)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
