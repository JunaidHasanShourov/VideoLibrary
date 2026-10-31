package com.example.crawler

import com.example.model.DiscoveredMedia
import com.example.model.MediaType
import java.net.URI

object MetadataAssociator {

    private val POSTER_NAMES = listOf("poster", "cover", "folder", "thumb", "thumbnail", "front", "backdrop")

    private val ARTWORK_DIR_NAMES = setOf("artwork", "artworks", "thumbnails", "covers", "images", "thumbs", "poster", "posters")
    private val SUBTITLE_DIR_NAMES = setOf("subtitles", "subtitle", "subs", "sub")

    private val LANGUAGE_MAP = mapOf(
        "en" to "English", "eng" to "English",
        "bn" to "Bengali", "ben" to "Bengali",
        "hi" to "Hindi", "hin" to "Hindi",
        "es" to "Spanish", "spa" to "Spanish",
        "fr" to "French", "fra" to "French", "fre" to "French",
        "de" to "German", "ger" to "German", "deu" to "German",
        "ja" to "Japanese", "jpn" to "Japanese",
        "zh" to "Chinese", "chi" to "Chinese", "zho" to "Chinese",
        "ar" to "Arabic", "ara" to "Arabic",
        "ru" to "Russian", "rus" to "Russian",
        "pt" to "Portuguese", "por" to "Portuguese",
        "it" to "Italian", "ita" to "Italian",
        "ko" to "Korean", "kor" to "Korean"
    )

    fun associate(
        discoveredItems: List<DiscoveredMedia>,
        pageArtworkUrl: String? = null
    ): List<DiscoveredMedia> {
        val videos = discoveredItems.filter { it.mediaType == MediaType.VIDEO }
        val artworks = discoveredItems.filter { it.mediaType == MediaType.ARTWORK }
        val subtitles = discoveredItems.filter { it.mediaType == MediaType.SUBTITLE }

        return videos.map { video ->
            var matchedArtwork = video.artworkUrl ?: pageArtworkUrl
            var matchedSubtitle = video.subtitleUrl

            val videoBaseName = video.originalFilename.substringBeforeLast('.').lowercase()
            val videoParentDir = getParentDirectory(video.url)
            val videoGrandParentDir = getParentDirectory(videoParentDir)

            // 1. Multi-location Artwork Detection
            if (matchedArtwork.isNullOrBlank()) {
                // Same directory matching video name
                var candidateArtwork = artworks.firstOrNull { art ->
                    val artBase = art.originalFilename.substringBeforeLast('.').lowercase()
                    val artDir = getParentDirectory(art.url)
                    artDir == videoParentDir && (artBase == videoBaseName || artBase.startsWith(videoBaseName))
                }

                // Same directory matching standard poster/cover name
                if (candidateArtwork == null) {
                    candidateArtwork = artworks.firstOrNull { art ->
                        val artBase = art.originalFilename.substringBeforeLast('.').lowercase()
                        val artDir = getParentDirectory(art.url)
                        artDir == videoParentDir && POSTER_NAMES.any { artBase.contains(it) }
                    }
                }

                // Subdirectory (e.g. artwork/, thumbnails/, covers/)
                if (candidateArtwork == null) {
                    candidateArtwork = artworks.firstOrNull { art ->
                        val artDir = getParentDirectory(art.url)
                        val lastSubDir = artDir.substringAfterLast('/').lowercase()
                        val artParentDir = getParentDirectory(artDir)
                        artParentDir == videoParentDir && ARTWORK_DIR_NAMES.contains(lastSubDir)
                    }
                }

                // Parent directory poster/cover (e.g. for CD1/movie.avi)
                if (candidateArtwork == null && videoGrandParentDir.isNotBlank()) {
                    candidateArtwork = artworks.firstOrNull { art ->
                        val artBase = art.originalFilename.substringBeforeLast('.').lowercase()
                        val artDir = getParentDirectory(art.url)
                        artDir == videoGrandParentDir && (artBase == videoBaseName || POSTER_NAMES.any { artBase.contains(it) })
                    }
                }

                matchedArtwork = candidateArtwork?.url
            }

            // 2. Multi-location Subtitle Detection
            if (matchedSubtitle.isNullOrBlank()) {
                // Same directory matching video name or video base name prefix
                var candidateSubtitle = subtitles.firstOrNull { sub ->
                    val subBase = sub.originalFilename.substringBeforeLast('.').lowercase()
                    val subDir = getParentDirectory(sub.url)
                    subDir == videoParentDir && (subBase == videoBaseName || subBase.startsWith(videoBaseName))
                }

                // Subdirectory (e.g. subtitles/, subs/)
                if (candidateSubtitle == null) {
                    candidateSubtitle = subtitles.firstOrNull { sub ->
                        val subDir = getParentDirectory(sub.url)
                        val lastSubDir = subDir.substringAfterLast('/').lowercase()
                        val subParentDir = getParentDirectory(subDir)
                        subParentDir == videoParentDir && SUBTITLE_DIR_NAMES.contains(lastSubDir)
                    }
                }

                // Parent directory
                if (candidateSubtitle == null && videoGrandParentDir.isNotBlank()) {
                    candidateSubtitle = subtitles.firstOrNull { sub ->
                        val subBase = sub.originalFilename.substringBeforeLast('.').lowercase()
                        val subDir = getParentDirectory(sub.url)
                        subDir == videoGrandParentDir && (subBase == videoBaseName || subBase.startsWith(videoBaseName))
                    }
                }

                matchedSubtitle = candidateSubtitle?.url
            }

            video.copy(
                artworkUrl = matchedArtwork,
                subtitleUrl = matchedSubtitle
            )
        }
    }

    fun extractSubtitleLanguage(filename: String): String {
        val parts = filename.substringBeforeLast('.').lowercase().split('.', '-', '_')
        for (part in parts) {
            val lang = LANGUAGE_MAP[part]
            if (lang != null) return lang
        }
        return "Unknown"
    }

    private fun getParentDirectory(urlStr: String): String {
        return try {
            val uri = URI(urlStr)
            val path = uri.path ?: ""
            if (path.contains("/")) path.substringBeforeLast('/') else ""
        } catch (_: Exception) {
            if (urlStr.contains("/")) urlStr.substringBeforeLast('/') else ""
        }
    }
}
