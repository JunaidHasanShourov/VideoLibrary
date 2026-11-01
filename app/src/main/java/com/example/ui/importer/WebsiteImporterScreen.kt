package com.example.ui.importer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.VideoLibraryApp
import com.example.crawler.WebCrawler
import com.example.model.CrawlerConfig
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebsiteImporterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToResults: () -> Unit
) {
    val repository = remember { VideoLibraryApp.instance.videoRepository }
    val userPrefs = remember { VideoLibraryApp.instance.userPreferences }
    val scope = rememberCoroutineScope()

    var rootUrl by remember { mutableStateOf("") }
    var maxDepth by remember { mutableFloatStateOf(3f) }
    var maxPages by remember { mutableFloatStateOf(100f) }
    var requestDelayMs by remember { mutableFloatStateOf(250f) }
    var sameDomainOnly by remember { mutableStateOf(true) }
    var sameRootOnly by remember { mutableStateOf(true) }

    var isCrawling by remember { mutableStateOf(false) }
    var activeCrawler by remember { mutableStateOf<WebCrawler?>(null) }
    val crawlerProgress by activeCrawler?.progress?.collectAsState() ?: remember { mutableStateOf(null) }

    fun startCrawl() {
        if (rootUrl.isBlank()) return
        val config = CrawlerConfig(
            maxDepth = maxDepth.toInt(),
            maxPages = maxPages.toInt(),
            maxVideos = 100,
            requestDelayMs = requestDelayMs.toLong(),
            sameDomainOnly = sameDomainOnly,
            sameRootOnly = sameRootOnly
        )

        scope.launch {
            val allVideos = repository.getVideoCount()
            val crawler = WebCrawler(config = config)
            activeCrawler = crawler
            isCrawling = true

            val results = crawler.crawl(rootUrl.trim())
            ImportHolder.setDiscoveredItems(results)
            isCrawling = false
            onNavigateToResults()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Website & Directory Importer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Target Website or Directory URL",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            OutlinedTextField(
                value = rootUrl,
                onValueChange = { rootUrl = it },
                placeholder = { Text("https://example.com/videos/") },
                leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("crawler_root_url_input"),
                singleLine = true,
                enabled = !isCrawling
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Crawler Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Crawler Safety & Scope",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Max Crawl Depth: ${maxDepth.toInt()} levels", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = maxDepth,
                        onValueChange = { maxDepth = it },
                        valueRange = 1f..10f,
                        steps = 8,
                        enabled = !isCrawling
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Max Pages to Scan: ${maxPages.toInt()}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = maxPages,
                        onValueChange = { maxPages = it },
                        valueRange = 20f..500f,
                        steps = 23,
                        enabled = !isCrawling
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Polite Request Delay: ${requestDelayMs.toInt()} ms", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = requestDelayMs,
                        onValueChange = { requestDelayMs = it },
                        valueRange = 0f..1000f,
                        steps = 3,
                        enabled = !isCrawling
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Restrict to Same Domain", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = sameDomainOnly,
                            onCheckedChange = { sameDomainOnly = it },
                            enabled = !isCrawling
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Restrict to Same Root Path", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = sameRootOnly,
                            onCheckedChange = { sameRootOnly = it },
                            enabled = !isCrawling
                        )
                    }
                }
            }

            // Crawler Progress Display
            if (isCrawling && crawlerProgress != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("crawler_progress_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Crawling in Progress...",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ElectricViolet
                            )
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = ElectricViolet)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = crawlerProgress!!.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats Grid
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            ProgressStat(label = "Pages", value = crawlerProgress!!.pagesScanned.toString())
                            ProgressStat(label = "Videos", value = crawlerProgress!!.videosDiscovered.toString(), highlightColor = NeonCyan)
                            ProgressStat(label = "Images", value = crawlerProgress!!.imagesDiscovered.toString())
                            ProgressStat(label = "Subtitles", value = crawlerProgress!!.subtitlesDiscovered.toString())
                            ProgressStat(label = "Depth", value = "${crawlerProgress!!.currentDepth}/${maxDepth.toInt()}")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (crawlerProgress!!.isPaused) {
                                Button(
                                    onClick = { activeCrawler?.resume() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Resume", color = Color.Black)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { activeCrawler?.pause() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pause")
                                }
                            }

                            Button(
                                onClick = { activeCrawler?.cancel() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop")
                            }
                        }
                    }
                }
            } else {
                Button(
                    onClick = { startCrawl() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("start_crawler_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Media Discovery Crawl", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun ProgressStat(label: String, value: String, highlightColor: Color = Color.Unspecified) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (highlightColor != Color.Unspecified) highlightColor else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
