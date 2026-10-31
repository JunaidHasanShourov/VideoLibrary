package com.example.downloader

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.database.DownloadEntity
import com.example.data.network.NetworkClient
import com.example.model.DownloadStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

class DownloadManager private constructor(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val downloadDao = db.downloadDao()
    private val videoDao = db.videoDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val activeJobs = ConcurrentHashMap<Long, Job>()

    fun getAllDownloadsFlow(): Flow<List<DownloadEntity>> = downloadDao.getAllDownloadsFlow()

    suspend fun enqueueDownload(videoId: Long, url: String, title: String): Long {
        val existing = downloadDao.getDownloadByVideoId(videoId)
        if (existing != null) {
            if (existing.status == DownloadStatus.COMPLETED) {
                return existing.id
            }
            startDownload(existing.id)
            return existing.id
        }

        // Determine destination file
        val filename = sanitizeFilename(title.ifBlank { "video_$videoId" }) + "_" + System.currentTimeMillis() + ".mp4"
        val storageDir = context.getExternalFilesDir("videos") ?: context.filesDir
        if (!storageDir.exists()) storageDir.mkdirs()
        val destFile = File(storageDir, filename)

        val download = DownloadEntity(
            videoId = videoId,
            title = title,
            url = url,
            localFilePath = destFile.absolutePath,
            status = DownloadStatus.QUEUED
        )

        val id = downloadDao.insertDownload(download)
        startDownload(id)
        return id
    }

    fun startDownload(downloadId: Long) {
        if (activeJobs.containsKey(downloadId)) return

        val job = scope.launch {
            val download = downloadDao.getDownloadById(downloadId) ?: return@launch
            val destFile = File(download.localFilePath)
            val existingBytes = if (destFile.exists()) destFile.length() else 0L

            downloadDao.updateStatus(downloadId, DownloadStatus.DOWNLOADING)

            try {
                val requestBuilder = Request.Builder().url(download.url)
                    .header("User-Agent", NetworkClient.DEFAULT_USER_AGENT)
                    .header("Accept", "*/*")

                // HTTP Range request for resuming interrupted downloads
                if (existingBytes > 0) {
                    requestBuilder.header("Range", "bytes=$existingBytes-")
                }

                val response = NetworkClient.okHttpClient.newCall(requestBuilder.build()).execute()
                if (!response.isSuccessful && response.code != 206) {
                    downloadDao.updateStatus(downloadId, DownloadStatus.FAILED, "HTTP ${response.code}: ${response.message}")
                    response.close()
                    return@launch
                }

                val body = response.body
                if (body == null) {
                    downloadDao.updateStatus(downloadId, DownloadStatus.FAILED, "Empty response body")
                    response.close()
                    return@launch
                }

                val responseLength = body.contentLength()
                val totalBytes = if (response.code == 206) {
                    existingBytes + responseLength
                } else {
                    if (responseLength > 0) responseLength else download.totalBytes
                }

                val append = response.code == 206 && existingBytes > 0
                val outputStream = FileOutputStream(destFile, append)
                val inputStream: InputStream = body.byteStream()

                val buffer = ByteArray(16384)
                var bytesRead: Int
                var downloadedSoFar = if (append) existingBytes else 0L
                var lastUpdateTime = System.currentTimeMillis()
                var bytesSinceLastUpdate = 0L

                try {
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedSoFar += bytesRead
                        bytesSinceLastUpdate += bytesRead

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastUpdateTime
                        if (elapsed >= 1000) {
                            val speed = (bytesSinceLastUpdate * 1000) / elapsed
                            downloadDao.updateProgress(
                                id = downloadId,
                                downloaded = downloadedSoFar,
                                total = totalBytes,
                                speed = speed,
                                status = DownloadStatus.DOWNLOADING
                            )
                            lastUpdateTime = now
                            bytesSinceLastUpdate = 0L
                        }
                    }
                    outputStream.flush()
                } finally {
                    outputStream.close()
                    inputStream.close()
                    response.close()
                }

                // Download completed successfully
                downloadDao.updateProgress(
                    id = downloadId,
                    downloaded = downloadedSoFar,
                    total = downloadedSoFar,
                    speed = 0L,
                    status = DownloadStatus.COMPLETED
                )

                // Update Video Entity
                videoDao.updateDownloadStatus(
                    id = download.videoId,
                    isDownloaded = true,
                    localPath = destFile.absolutePath
                )

            } catch (e: CancellationException) {
                downloadDao.updateStatus(downloadId, DownloadStatus.PAUSED, "Paused by user")
            } catch (e: Exception) {
                downloadDao.updateStatus(downloadId, DownloadStatus.FAILED, e.message ?: "Download error")
            } finally {
                activeJobs.remove(downloadId)
            }
        }

        activeJobs[downloadId] = job
    }

    fun pauseDownload(downloadId: Long) {
        val job = activeJobs.remove(downloadId)
        job?.cancel()
        scope.launch {
            downloadDao.updateStatus(downloadId, DownloadStatus.PAUSED)
        }
    }

    fun cancelDownload(downloadId: Long) {
        val job = activeJobs.remove(downloadId)
        job?.cancel()
        scope.launch {
            val download = downloadDao.getDownloadById(downloadId)
            downloadDao.updateStatus(downloadId, DownloadStatus.CANCELLED)
            if (download != null) {
                val file = File(download.localFilePath)
                if (file.exists()) file.delete()
                videoDao.updateDownloadStatus(download.videoId, false, null)
            }
        }
    }

    fun deleteDownload(downloadId: Long) {
        val job = activeJobs.remove(downloadId)
        job?.cancel()
        scope.launch {
            val download = downloadDao.getDownloadById(downloadId)
            if (download != null) {
                val file = File(download.localFilePath)
                if (file.exists()) file.delete()
                videoDao.updateDownloadStatus(download.videoId, false, null)
                downloadDao.deleteDownloadById(downloadId)
            }
        }
    }

    private fun sanitizeFilename(input: String): String {
        return input.replace(Regex("""[^a-zA-Z0-9._\-]"""), "_").take(40)
    }

    companion object {
        @Volatile
        private var INSTANCE: DownloadManager? = null

        fun getInstance(context: Context): DownloadManager {
            return INSTANCE ?: synchronized(this) {
                val instance = DownloadManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
