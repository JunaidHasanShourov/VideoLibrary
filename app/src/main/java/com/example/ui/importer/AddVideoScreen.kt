package com.example.ui.importer

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.crawler.FilenameCleaner
import com.example.data.database.VideoEntity
import com.example.data.network.MediaProbeResult
import com.example.data.network.NetworkClient
import com.example.ui.downloads.formatBytes
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVideoScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (Long) -> Unit
) {
    val repository = remember { VideoLibraryApp.instance.videoRepository }
    val downloadManager = remember { VideoLibraryApp.instance.downloadManager }
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var description by remember { mutableStateOf("") }
    var artworkUrl by remember { mutableStateOf("") }
    var subtitleUrl by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var rating by remember { mutableFloatStateOf(0f) }
    var isLiveStream by remember { mutableStateOf(false) }

    var isProbing by remember { mutableStateOf(false) }
    var probeResult by remember { mutableStateOf<MediaProbeResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun validateAndProbe() {
        if (url.isBlank()) {
            errorMessage = "Please enter a valid video URL"
            return
        }
        isProbing = true
        errorMessage = null

        scope.launch {
            val result = NetworkClient.probeUrl(url.trim())
            probeResult = result
            isProbing = false

            if (result.isAccessible) {
                val filename = url.substringAfterLast('/').substringBefore('?')
                if (title.isBlank()) {
                    title = FilenameCleaner.clean(filename)
                }
                if (url.contains(".m3u8", ignoreCase = true) || result.contentType.contains("mpegurl", ignoreCase = true)) {
                    isLiveStream = true
                    if (category.equals("General", ignoreCase = true)) {
                        category = "Live Streams"
                    }
                } else if (category.equals("General", ignoreCase = true)) {
                    category = FilenameCleaner.extractCategoryFromPath(url)
                }
            } else {
                errorMessage = result.errorMessage ?: "Could not access URL (Status: ${result.statusCode})"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Video URL") },
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
            // URL Input Row
            Text(
                text = "Video Stream or File URL",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            OutlinedTextField(
                value = url,
                onValueChange = {
                    url = it
                    if (title.isBlank() && it.contains("/")) {
                        val filename = it.substringAfterLast('/').substringBefore('?')
                        if (filename.length > 3) title = FilenameCleaner.clean(filename)
                    }
                },
                placeholder = { Text("https://example.com/media/sample.mp4") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_video_url_input"),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { validateAndProbe() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    modifier = Modifier.testTag("validate_url_btn")
                ) {
                    if (isProbing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text("Validate & Probe")
                }

                // Quick pre-fill button for testing
                OutlinedButton(
                    onClick = {
                        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4"
                        title = "Subaru Outback On Street and Dirt"
                        category = "Automotive"
                        description = "Automotive commercial showcasing vehicle performance across mixed terrain."
                        artworkUrl = "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=800"
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Insert Test Stream")
                }
            }

            // Probe Result Feedback
            if (probeResult != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (probeResult!!.isAccessible) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (probeResult!!.isAccessible) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (probeResult!!.isAccessible) NeonCyan else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (probeResult!!.isAccessible) "Online & Accessible (HTTP ${probeResult!!.statusCode})" else "Resource Inaccessible",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (probeResult!!.isAccessible) {
                                Text(
                                    text = "Type: ${probeResult!!.contentType.ifBlank { "video/mp4" }} • Size: ${if (probeResult!!.contentLength > 0) formatBytes(probeResult!!.contentLength) else "Stream"} • Resumable: ${probeResult!!.acceptsRanges}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata Form
            Text(
                text = "Video Metadata",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Display Title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_video_title_input")
            )

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category / Folder") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            OutlinedTextField(
                value = artworkUrl,
                onValueChange = { artworkUrl = it },
                label = { Text("Artwork / Thumbnail URL") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = subtitleUrl,
                onValueChange = { subtitleUrl = it },
                label = { Text("Subtitle URL (.srt, .vtt)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                label = { Text("Tags (comma-separated)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Live Stream Broadcast", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                    Text("HLS / DASH public live media stream", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                androidx.compose.material3.Switch(
                    checked = isLiveStream,
                    onCheckedChange = { isLiveStream = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (url.isBlank()) {
                            errorMessage = "URL is required"
                            return@Button
                        }
                        val filename = url.substringAfterLast('/').substringBefore('?')
                        val finalTitle = title.ifBlank { FilenameCleaner.clean(filename) }

                        val video = VideoEntity(
                            title = finalTitle,
                            originalFilename = filename,
                            description = description,
                            sourceUrl = url.trim(),
                            artworkUri = artworkUrl.ifBlank { null },
                            categoryName = if (isLiveStream && category == "General") "Live Streams" else category.ifBlank { "General" },
                            subtitleUri = subtitleUrl.ifBlank { null },
                            tags = tags,
                            format = filename.substringAfterLast('.', "mp4"),
                            isLiveStream = isLiveStream
                        )

                        scope.launch {
                            repository.insertVideo(video)
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_video_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save to Library", fontWeight = FontWeight.Bold)
                }

                if (!isLiveStream) {
                    Button(
                        onClick = {
                            if (url.isBlank()) return@Button
                            val filename = url.substringAfterLast('/').substringBefore('?')
                            val finalTitle = title.ifBlank { FilenameCleaner.clean(filename) }

                            val video = VideoEntity(
                                title = finalTitle,
                                originalFilename = filename,
                                description = description,
                                sourceUrl = url.trim(),
                                artworkUri = artworkUrl.ifBlank { null },
                                categoryName = category.ifBlank { "General" },
                                subtitleUri = subtitleUrl.ifBlank { null },
                                tags = tags,
                                format = filename.substringAfterLast('.', "mp4"),
                                isLiveStream = false
                            )

                            scope.launch {
                                val id = repository.insertVideo(video)
                                downloadManager.enqueueDownload(id, url.trim(), finalTitle)
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_and_download_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = androidx.compose.ui.graphics.Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save & Download Now", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = {
                        if (url.isNotBlank()) {
                            val filename = url.substringAfterLast('/').substringBefore('?')
                            val finalTitle = title.ifBlank { FilenameCleaner.clean(filename) }

                            val video = VideoEntity(
                                title = finalTitle,
                                originalFilename = filename,
                                description = description,
                                sourceUrl = url.trim(),
                                artworkUri = artworkUrl.ifBlank { null },
                                categoryName = if (isLiveStream && category == "General") "Live Streams" else category.ifBlank { "General" },
                                subtitleUri = subtitleUrl.ifBlank { null },
                                tags = tags,
                                format = filename.substringAfterLast('.', "mp4"),
                                isLiveStream = isLiveStream
                            )

                            scope.launch {
                                val id = repository.insertVideo(video)
                                onNavigateToPlayer(id)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play Directly")
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
