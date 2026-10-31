package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY dateAdded DESC")
    fun getAllVideosFlow(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos ORDER BY dateAdded DESC")
    suspend fun getAllVideos(): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getVideoById(id: Long): VideoEntity?

    @Query("SELECT * FROM videos WHERE id = :id")
    fun getVideoFlowById(id: Long): Flow<VideoEntity?>

    @Query("SELECT * FROM videos WHERE sourceUrl = :url LIMIT 1")
    suspend fun getVideoBySourceUrl(url: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE isFavorite = 1 ORDER BY dateAdded DESC")
    fun getFavoriteVideosFlow(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE playbackPositionMs > 0 AND watchedPercentage < 0.95 ORDER BY lastPlayed DESC LIMIT 10")
    fun getContinueWatchingFlow(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE lastPlayed > 0 ORDER BY lastPlayed DESC LIMIT 10")
    fun getRecentlyPlayedFlow(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos ORDER BY dateAdded DESC LIMIT 10")
    fun getRecentlyAddedFlow(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isDownloaded = 1 ORDER BY dateAdded DESC")
    fun getDownloadedVideosFlow(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isLiveStream = 1 ORDER BY dateAdded DESC")
    fun getLiveStreamsFlow(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE categoryName = :category ORDER BY title ASC")
    fun getVideosByCategoryFlow(category: String): Flow<List<VideoEntity>>

    @Query("""
        SELECT * FROM videos 
        WHERE title LIKE '%' || :query || '%' 
           OR originalFilename LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
           OR categoryName LIKE '%' || :query || '%'
           OR tags LIKE '%' || :query || '%'
           OR sourceUrl LIKE '%' || :query || '%'
        ORDER BY title ASC
    """)
    fun searchVideosFlow(query: String): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>): List<Long>

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Delete
    suspend fun deleteVideo(video: VideoEntity)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteVideoById(id: Long)

    @Query("UPDATE videos SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("""
        UPDATE videos 
        SET playbackPositionMs = :positionMs, 
            watchedPercentage = :watchedPercentage, 
            lastPlayed = :lastPlayed 
        WHERE id = :id
    """)
    suspend fun updatePlaybackProgress(id: Long, positionMs: Long, watchedPercentage: Float, lastPlayed: Long)

    @Query("UPDATE videos SET isDownloaded = :isDownloaded, localPath = :localPath WHERE id = :id")
    suspend fun updateDownloadStatus(id: Long, isDownloaded: Boolean, localPath: String?)

    @Query("SELECT COUNT(*) FROM videos")
    suspend fun getVideoCount(): Int

    @Query("DELETE FROM videos")
    suspend fun deleteAllVideos()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)

    @Query("UPDATE categories SET name = :newName WHERE name = :oldName")
    suspend fun renameCategory(oldName: String, newName: String)
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdTime DESC")
    fun getAllDownloadsFlow(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE videoId = :videoId LIMIT 1")
    suspend fun getDownloadByVideoId(videoId: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED', 'DOWNLOADING')")
    suspend fun getActiveDownloads(): List<DownloadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity): Long

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Query("UPDATE downloads SET downloadedBytes = :downloaded, totalBytes = :total, speedBytesPerSec = :speed, status = :status, updatedTime = :now WHERE id = :id")
    suspend fun updateProgress(id: Long, downloaded: Long, total: Long, speed: Long, status: DownloadStatus, now: Long = System.currentTimeMillis())

    @Query("UPDATE downloads SET status = :status, errorMessage = :errorMessage, updatedTime = :now WHERE id = :id")
    suspend fun updateStatus(id: Long, status: DownloadStatus, errorMessage: String? = null, now: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteDownload(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadById(id: Long)

    @Query("DELETE FROM downloads WHERE videoId = :videoId")
    suspend fun deleteDownloadByVideoId(videoId: Long)
}
