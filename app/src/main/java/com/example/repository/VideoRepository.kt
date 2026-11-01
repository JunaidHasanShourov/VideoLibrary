package com.example.repository

import com.example.data.database.AppDatabase
import com.example.data.database.CategoryEntity
import com.example.data.database.VideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class VideoRepository(private val database: AppDatabase) {

    private val videoDao = database.videoDao()
    private val categoryDao = database.categoryDao()

    fun getAllVideosFlow(): Flow<List<VideoEntity>> = videoDao.getAllVideosFlow()

    suspend fun getAllVideosDirect(): List<VideoEntity> = withContext(Dispatchers.IO) {
        videoDao.getAllVideos()
    }

    fun getVideoFlowById(id: Long): Flow<VideoEntity?> = videoDao.getVideoFlowById(id)

    suspend fun getVideoById(id: Long): VideoEntity? = withContext(Dispatchers.IO) {
        videoDao.getVideoById(id)
    }

    suspend fun getVideoBySourceUrl(url: String): VideoEntity? = withContext(Dispatchers.IO) {
        videoDao.getVideoBySourceUrl(url)
    }

    fun getContinueWatchingFlow(): Flow<List<VideoEntity>> = videoDao.getContinueWatchingFlow()

    fun getRecentlyPlayedFlow(): Flow<List<VideoEntity>> = videoDao.getRecentlyPlayedFlow()

    fun getRecentlyAddedFlow(): Flow<List<VideoEntity>> = videoDao.getRecentlyAddedFlow()

    fun getFavoriteVideosFlow(): Flow<List<VideoEntity>> = videoDao.getFavoriteVideosFlow()

    fun getDownloadedVideosFlow(): Flow<List<VideoEntity>> = videoDao.getDownloadedVideosFlow()

    fun getLiveStreamsFlow(): Flow<List<VideoEntity>> = videoDao.getLiveStreamsFlow()

    fun getVideosByCategoryFlow(category: String): Flow<List<VideoEntity>> =
        videoDao.getVideosByCategoryFlow(category)

    fun searchVideosFlow(query: String): Flow<List<VideoEntity>> = videoDao.searchVideosFlow(query)

    suspend fun insertVideo(video: VideoEntity): Long = withContext(Dispatchers.IO) {
        val id = videoDao.insertVideo(video)
        ensureCategoryExists(video.categoryName)
        id
    }

    suspend fun insertVideos(videos: List<VideoEntity>): List<Long> = withContext(Dispatchers.IO) {
        val ids = videoDao.insertVideos(videos)
        videos.map { it.categoryName }.distinct().forEach { ensureCategoryExists(it) }
        ids
    }

    suspend fun updateVideo(video: VideoEntity) = withContext(Dispatchers.IO) {
        videoDao.updateVideo(video)
        ensureCategoryExists(video.categoryName)
    }

    suspend fun deleteVideoById(id: Long) = withContext(Dispatchers.IO) {
        videoDao.deleteVideoById(id)
    }

    suspend fun setFavorite(id: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        videoDao.setFavorite(id, isFavorite)
    }

    suspend fun updatePlaybackProgress(id: Long, positionMs: Long, durationMs: Long) = withContext(Dispatchers.IO) {
        val percentage = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
        videoDao.updatePlaybackProgress(
            id = id,
            positionMs = positionMs,
            watchedPercentage = percentage,
            lastPlayed = System.currentTimeMillis()
        )
    }

    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>> = categoryDao.getAllCategoriesFlow()

    suspend fun ensureCategoryExists(name: String) = withContext(Dispatchers.IO) {
        if (name.isBlank()) return@withContext
        val existing = categoryDao.getCategoryByName(name)
        if (existing == null) {
            categoryDao.insertCategory(CategoryEntity(name = name))
        }
    }

    suspend fun renameCategory(oldName: String, newName: String) = withContext(Dispatchers.IO) {
        categoryDao.renameCategory(oldName, newName)
    }

    suspend fun deleteCategoryById(id: Long) = withContext(Dispatchers.IO) {
        categoryDao.deleteCategoryById(id)
    }

    suspend fun getVideoCount(): Int = withContext(Dispatchers.IO) {
        videoDao.getVideoCount()
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        videoDao.deleteAllVideos()
    }

    suspend fun purgeSampleData() = withContext(Dispatchers.IO) {
        val all = videoDao.getAllVideos()
        val sampleVideos = all.filter {
            it.sourceUrl.contains("gtv-videos-bucket", ignoreCase = true) ||
            it.sourceUrl.contains("commondatastorage.googleapis.com", ignoreCase = true) ||
            it.title.equals("Big Buck Bunny", ignoreCase = true) ||
            it.title.equals("Elephants Dream", ignoreCase = true) ||
            it.title.equals("For Bigger Blazes", ignoreCase = true) ||
            it.title.equals("Tears of Steel", ignoreCase = true) ||
            it.title.equals("We Are Going On Bullrun", ignoreCase = true) ||
            it.title.equals("Sintel", ignoreCase = true) ||
            it.title.contains("Live HLS Stream", ignoreCase = true) ||
            it.title.equals("Subaru Outback On Street and Dirt", ignoreCase = true)
        }
        sampleVideos.forEach { videoDao.deleteVideoById(it.id) }
    }
}

