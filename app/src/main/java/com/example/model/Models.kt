package com.example.model

enum class MediaType {
    VIDEO,
    LIVE_STREAM,
    ARTWORK,
    SUBTITLE,
    DIRECTORY,
    UNKNOWN
}

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class SortOption(val displayName: String) {
    TITLE_ASC("Title A-Z"),
    TITLE_DESC("Title Z-A"),
    RECENTLY_ADDED("Recently Added"),
    RECENTLY_PLAYED("Recently Played"),
    DURATION("Duration"),
    RATING("Rating"),
    FILE_SIZE("File Size")
}

enum class FilterOption(val displayName: String) {
    ALL("All"),
    FAVORITES("Favorites"),
    DOWNLOADED("Downloaded"),
    STREAMING("Streaming"),
    LIVE_STREAMS("Live Streams"),
    WATCHED("Watched"),
    UNWATCHED("Unwatched"),
    HAS_SUBTITLES("Has Subtitles")
}

data class CrawlerConfig(
    val maxDepth: Int = 3,
    val maxPages: Int = 200,
    val maxVideos: Int = 100,
    val requestDelayMs: Long = 250L,
    val sameDomainOnly: Boolean = true,
    val sameRootOnly: Boolean = true,
    val allowedExtensions: Set<String> = setOf("mp4", "webm", "mkv", "mov", "m4v", "avi", "ts", "m3u8")
)

data class DiscoveredMedia(
    val url: String,
    val title: String,
    val originalFilename: String,
    val mediaType: MediaType,
    val format: String,
    val category: String = "General",
    val artworkUrl: String? = null,
    val subtitleUrl: String? = null,
    val fileSize: Long = 0L,
    val durationMs: Long = 0L,
    val description: String = "",
    val isAlreadyInLibrary: Boolean = false,
    val isSelected: Boolean = true
)
