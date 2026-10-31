package com.example.crawler

import com.example.data.network.NetworkClient
import com.example.model.CrawlerConfig
import com.example.model.DiscoveredMedia
import com.example.model.MediaType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI

data class CrawlerProgress(
    val currentUrl: String = "",
    val currentFolder: String = "",
    val pagesScanned: Int = 0,
    val foldersDiscovered: Int = 0,
    val videosDiscovered: Int = 0,
    val imagesDiscovered: Int = 0,
    val subtitlesDiscovered: Int = 0,
    val currentDepth: Int = 0,
    val errorsCount: Int = 0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val statusMessage: String = ""
)

class WebCrawler(
    private val config: CrawlerConfig,
    private val existingUrls: Set<String> = emptySet()
) {
    private val _progress = MutableStateFlow(CrawlerProgress())
    val progress: StateFlow<CrawlerProgress> = _progress.asStateFlow()

    @Volatile
    private var isCancelled = false

    @Volatile
    private var isPaused = false

    private val visitedUrls = mutableSetOf<String>()
    private val discoveredItems = mutableListOf<DiscoveredMedia>()

    private val videoExtensions = setOf("mp4", "webm", "mkv", "mov", "m4v", "avi", "ts", "m3u8")
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp")
    private val subtitleExtensions = setOf("srt", "vtt", "ass", "ssa")

    fun pause() {
        isPaused = true
        _progress.value = _progress.value.copy(isPaused = true, statusMessage = "Crawling paused")
    }

    fun resume() {
        isPaused = false
        _progress.value = _progress.value.copy(isPaused = false, statusMessage = "Crawling resumed")
    }

    fun cancel() {
        isCancelled = true
        _progress.value = _progress.value.copy(isRunning = false, statusMessage = "Crawling cancelled")
    }

    suspend fun crawl(rootUrl: String): List<DiscoveredMedia> = withContext(Dispatchers.IO) {
        isCancelled = false
        isPaused = false
        visitedUrls.clear()
        discoveredItems.clear()

        _progress.value = CrawlerProgress(
            currentUrl = rootUrl,
            isRunning = true,
            statusMessage = "Starting crawl..."
        )

        val rootUri = try {
            URI(rootUrl)
        } catch (e: Exception) {
            _progress.value = _progress.value.copy(
                isRunning = false,
                statusMessage = "Invalid root URL: ${e.message}"
            )
            return@withContext emptyList()
        }

        val queue = ArrayDeque<Pair<String, Int>>()
        queue.add(Pair(rootUrl, 0))

        while (queue.isNotEmpty() && !isCancelled) {
            while (isPaused && !isCancelled) {
                delay(300)
            }

            if (isCancelled) break
            if (_progress.value.pagesScanned >= config.maxPages) break
            if (_progress.value.videosDiscovered >= config.maxVideos) break

            val (currentUrl, depth) = queue.removeFirst()
            if (visitedUrls.contains(currentUrl)) continue
            visitedUrls.add(currentUrl)

            if (depth > config.maxDepth) continue

            // Respect polite crawler request delay
            if (config.requestDelayMs > 0) {
                delay(config.requestDelayMs)
            }

            val currentFolder = FilenameCleaner.extractCategoryFromPath(currentUrl)
            _progress.value = _progress.value.copy(
                currentUrl = currentUrl,
                currentFolder = currentFolder,
                currentDepth = depth,
                pagesScanned = _progress.value.pagesScanned + 1,
                statusMessage = "Scanning $currentUrl"
            )

            try {
                crawlUrl(currentUrl, depth, rootUri, queue)
            } catch (e: CancellationException) {
                break
            } catch (e: Exception) {
                _progress.value = _progress.value.copy(
                    errorsCount = _progress.value.errorsCount + 1,
                    statusMessage = "Error: ${e.message?.take(50)}"
                )
            }
        }

        // Post-processing: Associate nearby posters, thumbnails and subtitles
        val associated = MetadataAssociator.associate(discoveredItems)

        _progress.value = _progress.value.copy(
            isRunning = false,
            statusMessage = if (isCancelled) "Crawl cancelled" else "Crawl complete. Found ${associated.size} videos."
        )

        associated
    }

    private suspend fun crawlUrl(
        urlStr: String,
        depth: Int,
        rootUri: URI,
        queue: ArrayDeque<Pair<String, Int>>
    ) {
        val extension = getExtension(urlStr)

        // If direct media link
        if (videoExtensions.contains(extension)) {
            addDiscoveredItem(urlStr, MediaType.VIDEO, extension)
            return
        } else if (imageExtensions.contains(extension)) {
            addDiscoveredItem(urlStr, MediaType.ARTWORK, extension)
            return
        } else if (subtitleExtensions.contains(extension)) {
            addDiscoveredItem(urlStr, MediaType.SUBTITLE, extension)
            return
        }

        // Otherwise fetch HTML
        val request = NetworkClient.newRequest(urlStr)
        val response = NetworkClient.okHttpClient.newCall(request).execute()

        val contentType = response.header("Content-Type")?.lowercase() ?: ""
        if (!contentType.contains("text/html") && !contentType.contains("application/xhtml")) {
            response.close()
            return
        }

        val html = response.body?.string() ?: ""
        response.close()

        val doc = Jsoup.parse(html, urlStr)

        // Extract webpage metadata (Open Graph / JSON-LD / HTML Title)
        val pageTitle = doc.select("meta[property=og:title]").attr("content").ifBlank {
            doc.title().ifBlank { urlStr.substringAfterLast('/') }
        }
        val pageImage = doc.select("meta[property=og:image]").attr("content").ifBlank { null }
        val pageDesc = doc.select("meta[property=og:description]").attr("content").ifBlank {
            doc.select("meta[name=description]").attr("content")
        }

        // Check for embedded video elements in HTML page
        val videoTags = doc.select("video, source")
        for (vTag in videoTags) {
            val src = vTag.absUrl("src")
            if (src.isNotBlank()) {
                val ext = getExtension(src)
                if (videoExtensions.contains(ext) || ext.isNotBlank()) {
                    val vidArtwork = vTag.absUrl("poster").ifBlank { pageImage }
                    addDiscoveredItem(
                        url = src,
                        type = MediaType.VIDEO,
                        format = ext.ifBlank { "mp4" },
                        defaultTitle = pageTitle,
                        defaultDesc = pageDesc,
                        artworkUrl = vidArtwork
                    )
                }
            }
        }

        // Parse links on page or directory index
        val links = if (DirectoryListingParser.isDirectoryListing(doc)) {
            DirectoryListingParser.parseDirectoryLinks(doc, urlStr)
        } else {
            doc.select("a[href]").mapNotNull { a ->
                val href = a.attr("href")
                if (href.startsWith("#") || href.startsWith("javascript:")) null
                else {
                    try {
                        val abs = a.absUrl("href")
                        if (abs.isNotBlank()) ParsedLink(abs, a.text(), abs.endsWith("/")) else null
                    } catch (_: Exception) { null }
                }
            }
        }

        for (link in links) {
            val targetUrl = link.url
            val ext = getExtension(targetUrl)

            if (videoExtensions.contains(ext)) {
                addDiscoveredItem(targetUrl, MediaType.VIDEO, ext, defaultTitle = link.name)
            } else if (imageExtensions.contains(ext)) {
                addDiscoveredItem(targetUrl, MediaType.ARTWORK, ext)
            } else if (subtitleExtensions.contains(ext)) {
                addDiscoveredItem(targetUrl, MediaType.SUBTITLE, ext)
            } else if (depth < config.maxDepth && isValidSubUrl(targetUrl, rootUri)) {
                if (!visitedUrls.contains(targetUrl)) {
                    queue.add(Pair(targetUrl, depth + 1))
                    _progress.value = _progress.value.copy(
                        foldersDiscovered = _progress.value.foldersDiscovered + 1
                    )
                }
            }
        }
    }

    private fun addDiscoveredItem(
        url: String,
        type: MediaType,
        format: String,
        defaultTitle: String = "",
        defaultDesc: String = "",
        artworkUrl: String? = null
    ) {
        val filename = url.substringAfterLast('/').substringBefore('?')
        val title = FilenameCleaner.resolveTitle(filename, url, defaultTitle.takeIf { it.isNotBlank() })
        val category = FilenameCleaner.extractHierarchicalCategory(url)
        val isAlreadyInLib = existingUrls.contains(url)

        val item = DiscoveredMedia(
            url = url,
            title = title,
            originalFilename = filename,
            mediaType = type,
            format = format,
            category = category,
            artworkUrl = artworkUrl,
            description = defaultDesc,
            isAlreadyInLibrary = isAlreadyInLib,
            isSelected = !isAlreadyInLib && type == MediaType.VIDEO
        )

        discoveredItems.add(item)

        when (type) {
            MediaType.VIDEO -> _progress.value = _progress.value.copy(
                videosDiscovered = _progress.value.videosDiscovered + 1
            )
            MediaType.ARTWORK -> _progress.value = _progress.value.copy(
                imagesDiscovered = _progress.value.imagesDiscovered + 1
            )
            MediaType.SUBTITLE -> _progress.value = _progress.value.copy(
                subtitlesDiscovered = _progress.value.subtitlesDiscovered + 1
            )
            else -> {}
        }
    }

    private fun isValidSubUrl(candidateUrl: String, rootUri: URI): Boolean {
        return try {
            val candidateUri = URI(candidateUrl)
            if (config.sameDomainOnly && !candidateUri.host.equals(rootUri.host, ignoreCase = true)) {
                return false
            }
            if (config.sameRootOnly && !candidateUri.path.startsWith(rootUri.path)) {
                return false
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun getExtension(url: String): String {
        val clean = url.substringBefore('?').substringBefore('#')
        return clean.substringAfterLast('.', "").lowercase()
    }
}
