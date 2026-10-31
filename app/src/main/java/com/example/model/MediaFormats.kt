package com.example.model

object MediaFormats {
    /**
     * Extensive set of supported video extensions across containers, codecs and stream manifests.
     */
    val VIDEO_EXTENSIONS: Set<String> = setOf(
        // MP4 / MPEG-4 family
        "mp4", "m4v", "m4p", "m4b", "mp4v",
        // Matroska / WebM
        "mkv", "webm",
        // QuickTime
        "mov", "qt",
        // AVI & DivX
        "avi", "divx",
        // Flash Video
        "flv", "f4v", "f4p",
        // MPEG Transport Stream
        "ts", "mts", "m2ts", "trp",
        // MPEG Program Stream / DVD
        "mpg", "mpeg", "mpe", "mpv", "m2v", "vob",
        // Windows Media Video / ASF
        "wmv", "asf",
        // 3GPP Mobile
        "3gp", "3g2", "3gpp", "3gpp2",
        // Ogg Video
        "ogv", "ogg",
        // RealMedia
        "rm", "rmvb",
        // Streaming Manifests & Playlists
        "m3u8", "mpd", "ism", "ismv", "isma"
    )

    /**
     * Supported audio extensions for extraction and media playback.
     */
    val AUDIO_EXTENSIONS: Set<String> = setOf(
        "mp3", "aac", "m4a", "flac", "wav", "wave", "ogg", "oga", "opus",
        "ac3", "eac3", "ec3", "dts", "dtshd", "dts-hd", "alac", "aiff", "aif",
        "amr", "awb", "wma", "pcm"
    )

    val ALL_MEDIA_EXTENSIONS: Set<String> = VIDEO_EXTENSIONS + AUDIO_EXTENSIONS

    fun isVideo(extension: String): Boolean = VIDEO_EXTENSIONS.contains(extension.lowercase())

    fun isAudio(extension: String): Boolean = AUDIO_EXTENSIONS.contains(extension.lowercase())

    fun isPlayableMedia(extension: String): Boolean = ALL_MEDIA_EXTENSIONS.contains(extension.lowercase())

    fun isStreamingManifest(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains(".m3u8") ||
                lower.contains(".mpd") ||
                lower.contains("/manifest") ||
                lower.contains(".ism") ||
                lower.startsWith("rtsp://")
    }
}
