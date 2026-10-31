package com.example.data.database

import androidx.room.TypeConverter
import com.example.model.DownloadStatus

class Converters {
    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus?): String = status?.name ?: DownloadStatus.QUEUED.name

    @TypeConverter
    fun toDownloadStatus(value: String?): DownloadStatus = try {
        if (value != null) DownloadStatus.valueOf(value) else DownloadStatus.QUEUED
    } catch (_: Exception) {
        DownloadStatus.QUEUED
    }
}
