package com.example.crawler

import com.example.model.CrawlerConfig
import com.example.model.DiscoveredMedia
import com.example.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPSClient
import java.io.IOException

data class FtpCredentials(
    val host: String,
    val port: Int = 21,
    val username: String = "anonymous",
    val password: String = "",
    val rootDir: String = "/",
    val isFtps: Boolean = false
)

class FtpCrawler(
    private val config: CrawlerConfig,
    private val existingUrls: Set<String> = emptySet()
) {
    private val _progress = MutableStateFlow(CrawlerProgress())
    val progress: StateFlow<CrawlerProgress> = _progress.asStateFlow()

    @Volatile
    private var isCancelled = false

    private val videoExtensions = com.example.model.MediaFormats.VIDEO_EXTENSIONS
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp")
    private val subtitleExtensions = setOf("srt", "vtt", "ass")

    fun cancel() {
        isCancelled = true
        _progress.value = _progress.value.copy(isRunning = false, statusMessage = "FTP Scan cancelled")
    }

    suspend fun crawl(credentials: FtpCredentials): List<DiscoveredMedia> = withContext(Dispatchers.IO) {
        isCancelled = false
        val discovered = mutableListOf<DiscoveredMedia>()

        _progress.value = CrawlerProgress(
            currentUrl = "${credentials.host}:${credentials.port}${credentials.rootDir}",
            isRunning = true,
            statusMessage = "Connecting to FTP server..."
        )

        val ftpClient: FTPClient = if (credentials.isFtps) {
            FTPSClient()
        } else {
            FTPClient()
        }

        try {
            ftpClient.connectTimeout = 10000
            ftpClient.setDataTimeout(java.time.Duration.ofSeconds(10))
            ftpClient.connect(credentials.host, credentials.port)

            val loginSuccess = ftpClient.login(
                credentials.username.ifBlank { "anonymous" },
                credentials.password
            )

            if (!loginSuccess) {
                _progress.value = _progress.value.copy(
                    isRunning = false,
                    statusMessage = "Authentication required — invalid FTP credentials."
                )
                try { ftpClient.disconnect() } catch (_: Exception) {}
                return@withContext emptyList()
            }

            ftpClient.enterLocalPassiveMode()
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

            scanDirectory(
                ftpClient = ftpClient,
                credentials = credentials,
                currentPath = credentials.rootDir.ifBlank { "/" },
                depth = 0,
                discovered = discovered
            )

            try {
                ftpClient.logout()
                ftpClient.disconnect()
            } catch (_: Exception) {}

        } catch (e: IOException) {
            _progress.value = _progress.value.copy(
                isRunning = false,
                statusMessage = "FTP Connection failed: ${e.message}"
            )
            return@withContext emptyList()
        } catch (e: Exception) {
            _progress.value = _progress.value.copy(
                isRunning = false,
                statusMessage = "Error scanning FTP: ${e.message}"
            )
            return@withContext emptyList()
        }

        val associated = MetadataAssociator.associate(discovered)
        _progress.value = _progress.value.copy(
            isRunning = false,
            statusMessage = "FTP scan complete. Found ${associated.size} videos."
        )

        associated
    }

    private fun scanDirectory(
        ftpClient: FTPClient,
        credentials: FtpCredentials,
        currentPath: String,
        depth: Int,
        discovered: MutableList<DiscoveredMedia>
    ) {
        if (isCancelled || depth > config.maxDepth) return

        val normalizedPath = if (currentPath.endsWith("/")) currentPath else "$currentPath/"
        _progress.value = _progress.value.copy(
            currentFolder = normalizedPath,
            currentDepth = depth,
            pagesScanned = _progress.value.pagesScanned + 1,
            statusMessage = "Browsing $normalizedPath"
        )

        val files: Array<FTPFile> = try {
            ftpClient.listFiles(normalizedPath) ?: emptyArray()
        } catch (_: Exception) {
            emptyArray()
        }

        for (file in files) {
            if (isCancelled) break
            val name = file.name
            if (name == "." || name == "..") continue

            val fullPath = "$normalizedPath$name"

            if (file.isDirectory) {
                _progress.value = _progress.value.copy(
                    foldersDiscovered = _progress.value.foldersDiscovered + 1
                )
                scanDirectory(ftpClient, credentials, fullPath, depth + 1, discovered)
            } else if (file.isFile) {
                val ext = name.substringAfterLast('.', "").lowercase()
                val scheme = if (credentials.isFtps) "ftps" else "ftp"
                val fileUrl = "$scheme://${credentials.host}:${credentials.port}$fullPath"
                val isAlreadyInLib = existingUrls.contains(fileUrl)

                if (videoExtensions.contains(ext)) {
                    val title = FilenameCleaner.resolveTitle(name, fullPath)
                    val category = FilenameCleaner.extractHierarchicalCategory(fullPath)
                    discovered.add(
                        DiscoveredMedia(
                            url = fileUrl,
                            title = title,
                            originalFilename = name,
                            mediaType = MediaType.VIDEO,
                            format = ext,
                            category = category,
                            fileSize = file.size,
                            isAlreadyInLibrary = isAlreadyInLib,
                            isSelected = !isAlreadyInLib
                        )
                    )
                    _progress.value = _progress.value.copy(
                        videosDiscovered = _progress.value.videosDiscovered + 1
                    )
                } else if (imageExtensions.contains(ext)) {
                    discovered.add(
                        DiscoveredMedia(
                            url = fileUrl,
                            title = name,
                            originalFilename = name,
                            mediaType = MediaType.ARTWORK,
                            format = ext,
                            fileSize = file.size
                        )
                    )
                    _progress.value = _progress.value.copy(
                        imagesDiscovered = _progress.value.imagesDiscovered + 1
                    )
                } else if (subtitleExtensions.contains(ext)) {
                    discovered.add(
                        DiscoveredMedia(
                            url = fileUrl,
                            title = name,
                            originalFilename = name,
                            mediaType = MediaType.SUBTITLE,
                            format = ext,
                            fileSize = file.size
                        )
                    )
                    _progress.value = _progress.value.copy(
                        subtitlesDiscovered = _progress.value.subtitlesDiscovered + 1
                    )
                }
            }
        }
    }
}
