package com.example

import android.app.Application
import com.example.data.database.AppDatabase
import com.example.data.preferences.UserPreferences
import com.example.downloader.DownloadManager
import com.example.repository.VideoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VideoLibraryApp : Application() {

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
            videoRepository.populateSampleDataIfEmpty()
        }
    }

    companion object {
        lateinit var instance: VideoLibraryApp
            private set
    }
}
