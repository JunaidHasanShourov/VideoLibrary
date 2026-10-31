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

    suspend fun populateSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        if (videoDao.getVideoCount() == 0) {
            val samples = listOf(
                VideoEntity(
                    title = "Big Buck Bunny",
                    originalFilename = "big_buck_bunny_720p_surround.mp4",
                    description = "A large and lovable rabbit deals with bullying forest creatures in this Blender open-source film.",
                    sourceUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    artworkUri = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800",
                    durationMs = 596000L,
                    format = "mp4",
                    rating = 4.8f,
                    categoryName = "Animation",
                    isFavorite = true,
                    tags = "Blender, Animation, CGI, Open Movie"
                ),
                VideoEntity(
                    title = "Elephants Dream",
                    originalFilename = "elephants_dream_1080p.mp4",
                    description = "The world's first open-source movie made entirely with open-source software, directed by Bassam Kurdali.",
                    sourceUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    artworkUri = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800",
                    durationMs = 653000L,
                    format = "mp4",
                    rating = 4.5f,
                    categoryName = "Sci-Fi",
                    isFavorite = false,
                    tags = "Sci-Fi, Blender, Surreal"
                ),
                VideoEntity(
                    title = "For Bigger Blazes",
                    originalFilename = "for_bigger_blazes.mp4",
                    description = "Cinematic video reel capturing high-octane action and vibrant cinematography.",
                    sourceUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    artworkUri = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800",
                    durationMs = 15000L,
                    format = "mp4",
                    rating = 4.2f,
                    categoryName = "Action",
                    isFavorite = true,
                    tags = "Action, Reel, Cinema"
                ),
                VideoEntity(
                    title = "Tears of Steel",
                    originalFilename = "tears_of_steel_1080p.mp4",
                    description = "A group of warriors and scientists gather at the Oude Kerk in Amsterdam to stage a crucial historical event in a dystopian future.",
                    sourceUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                    artworkUri = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=800",
                    durationMs = 734000L,
                    format = "mp4",
                    rating = 4.9f,
                    categoryName = "Sci-Fi",
                    isFavorite = true,
                    playbackPositionMs = 245000L,
                    watchedPercentage = 0.33f,
                    lastPlayed = System.currentTimeMillis() - 3600000L,
                    tags = "Sci-Fi, VFX, Amsterdam, Robotics"
                ),
                VideoEntity(
                    title = "We Are Going On Bullrun",
                    originalFilename = "we_are_going_on_bullrun.mp4",
                    description = "Cross-country automotive adventure exploring stunning open roads and coastlines.",
                    sourceUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                    artworkUri = "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=800",
                    durationMs = 47000L,
                    format = "mp4",
                    rating = 4.0f,
                    categoryName = "Adventure",
                    tags = "Cars, Road Trip, Documentary"
                ),
                VideoEntity(
                    title = "Sintel",
                    originalFilename = "sintel_trailer_1080p.mp4",
                    description = "A lonely young woman searches for a dragon she befriended and rescued in an epic fantasy world.",
                    sourceUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                    artworkUri = "https://images.unsplash.com/photo-1514539079130-25950c84af65?w=800",
                    durationMs = 52000L,
                    format = "mp4",
                    rating = 4.7f,
                    categoryName = "Fantasy",
                    isFavorite = false,
                    tags = "Fantasy, Dragon, Quest"
                ),
                VideoEntity(
                    title = "Tears of Steel (Live HLS Stream)",
                    originalFilename = "tears_of_steel_stream.m3u8",
                    description = "24/7 public HLS live streaming broadcast sample for streaming verification.",
                    sourceUrl = "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
                    artworkUri = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=800",
                    durationMs = 0L,
                    format = "m3u8",
                    rating = 4.9f,
                    categoryName = "Live Streams",
                    isLiveStream = true,
                    tags = "Live, HLS, Broadcast, Stream"
                )
            )

            insertVideos(samples)
        }
    }
}
