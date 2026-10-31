package com.example.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.DownloadStatus

@Entity(
    tableName = "videos",
    indices = [
        Index(value = ["sourceUrl"], unique = false),
        Index(value = ["title"]),
        Index(value = ["categoryName"]),
        Index(value = ["isFavorite"]),
        Index(value = ["isDownloaded"]),
        Index(value = ["lastPlayed"]),
        Index(value = ["isLiveStream"])
    ]
)
data class VideoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val originalFilename: String,
    val description: String = "",
    val sourceUrl: String,
    val localPath: String? = null,
    val artworkUri: String? = null,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val fileSize: Long = 0L,
    val format: String = "mp4",
    val rating: Float = 0f,
    val categoryId: Long? = null,
    val categoryName: String = "General",
    val dateAdded: Long = System.currentTimeMillis(),
    val lastPlayed: Long = 0L,
    val playbackPositionMs: Long = 0L,
    val watchedPercentage: Float = 0f,
    val isFavorite: Boolean = false,
    val isDownloaded: Boolean = false,
    val isLiveStream: Boolean = false,
    val subtitleUri: String? = null,
    val tags: String = "" // comma-separated tags
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val path: String = "",
    val videoCount: Int = 0
)

@Entity(
    tableName = "downloads",
    indices = [
        Index(value = ["videoId"]),
        Index(value = ["status"])
    ]
)
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val videoId: Long,
    val title: String,
    val url: String,
    val localFilePath: String,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val speedBytesPerSec: Long = 0L,
    val errorMessage: String? = null,
    val createdTime: Long = System.currentTimeMillis(),
    val updatedTime: Long = System.currentTimeMillis()
)
