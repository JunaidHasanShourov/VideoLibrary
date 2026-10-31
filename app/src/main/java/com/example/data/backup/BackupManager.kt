package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.data.database.VideoEntity
import com.example.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

enum class ImportMode {
    MERGE,
    REPLACE,
    SKIP_DUPLICATES
}

object BackupManager {

    suspend fun exportToJson(context: Context, repository: VideoRepository, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val videos = repository.getAllVideosDirect()
            val root = JSONObject()
            root.put("app", "VideoLibrary")
            root.put("schemaVersion", 2)
            root.put("exportedAt", System.currentTimeMillis())

            val videosArray = JSONArray()
            for (v in videos) {
                val obj = JSONObject().apply {
                    put("title", v.title)
                    put("originalFilename", v.originalFilename)
                    put("sourceUrl", v.sourceUrl)
                    put("localPath", v.localPath ?: "")
                    put("durationMs", v.durationMs)
                    put("playbackPositionMs", v.playbackPositionMs)
                    put("fileSize", v.fileSize)
                    put("format", v.format)
                    put("rating", v.rating.toDouble())
                    put("artworkUri", v.artworkUri ?: "")
                    put("subtitleUri", v.subtitleUri ?: "")
                    put("categoryName", v.categoryName)
                    put("isFavorite", v.isFavorite)
                    put("isDownloaded", v.isDownloaded)
                    put("isLiveStream", v.isLiveStream)
                    put("description", v.description)
                    put("tags", v.tags)
                }
                videosArray.put(obj)
            }
            root.put("videos", videosArray)

            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os).use { writer ->
                    writer.write(root.toString(2))
                }
            }
            Result.success(videos.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromJson(
        context: Context,
        repository: VideoRepository,
        uri: Uri,
        mode: ImportMode
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val content = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        content.append(line)
                        line = reader.readLine()
                    }
                }
            }

            val root = JSONObject(content.toString())
            val videosArray = root.optJSONArray("videos") ?: return@withContext Result.failure(Exception("No videos array found in backup file"))

            val existingVideos = repository.getAllVideosDirect()
            val existingUrls = existingVideos.map { it.sourceUrl }.toSet()

            if (mode == ImportMode.REPLACE) {
                repository.clearAll()
            }

            var importedCount = 0
            for (i in 0 until videosArray.length()) {
                val obj = videosArray.getJSONObject(i)
                val sourceUrl = obj.optString("sourceUrl")
                if (sourceUrl.isBlank()) continue

                if (mode == ImportMode.SKIP_DUPLICATES && existingUrls.contains(sourceUrl)) {
                    continue
                }

                val filename = obj.optString("originalFilename").ifBlank { sourceUrl.substringAfterLast('/') }
                val video = VideoEntity(
                    title = obj.optString("title", "Untitled Video"),
                    originalFilename = filename,
                    sourceUrl = sourceUrl,
                    localPath = obj.optString("localPath").takeIf { it.isNotBlank() },
                    durationMs = obj.optLong("durationMs", 0L),
                    playbackPositionMs = obj.optLong("playbackPositionMs", 0L),
                    fileSize = obj.optLong("fileSize", 0L),
                    format = obj.optString("format", "mp4"),
                    rating = obj.optDouble("rating", 0.0).toFloat(),
                    artworkUri = obj.optString("artworkUri").takeIf { it.isNotBlank() },
                    subtitleUri = obj.optString("subtitleUri").takeIf { it.isNotBlank() },
                    categoryName = obj.optString("categoryName", "General"),
                    isFavorite = obj.optBoolean("isFavorite", false),
                    isDownloaded = obj.optBoolean("isDownloaded", false),
                    isLiveStream = obj.optBoolean("isLiveStream", false),
                    description = obj.optString("description", ""),
                    tags = obj.optString("tags", "")
                )

                repository.insertVideo(video)
                importedCount++
            }

            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
