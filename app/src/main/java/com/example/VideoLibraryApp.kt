package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.database.AppDatabase
import com.example.data.network.NetworkClient
import com.example.data.preferences.UserPreferences
import com.example.downloader.DownloadManager
import com.example.repository.VideoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VideoLibraryApp : Application(), ImageLoaderFactory {

    lateinit var database: AppDatabase
        private set

    lateinit var videoRepository: VideoRepository
        private set

    lateinit var downloadManager: DownloadManager
        private set

    lateinit var userPreferences: UserPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)
        videoRepository = VideoRepository(database)
        downloadManager = DownloadManager.getInstance(this)
        userPreferences = UserPreferences(this)

        CoroutineScope(Dispatchers.IO).launch {
            videoRepository.purgeSampleData()
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(NetworkClient.okHttpClient)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .build()
    }

    companion object {
        lateinit var instance: VideoLibraryApp
            private set
    }
}
