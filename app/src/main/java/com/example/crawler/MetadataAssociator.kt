package com.example.crawler

import com.example.model.DiscoveredMedia
import com.example.model.MediaType
import java.net.URI

object MetadataAssociator {

    private val POSTER_NAMES = listOf(
        "poster", "cover", "folder", "thumb", "thumbnail", "front", "backdrop",
        "fanart", "banner", "preview", "screenshot", "art", "image", "pic"
    )

    private val ARTWORK_DIR_NAMES = setOf("artwork", "artworks", "thumbnails", "covers", "images", "thumbs", "poster", "posters", "art")
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
            var matchedArtwork = video.artworkUrl?.takeIf { it.isNotBlank() } ?: pageArtworkUrl?.takeIf { it.isNotBlank() }
            var matchedSubtitle = video.subtitleUrl

            val videoBaseName = cleanName(video.originalFilename)
            val videoParentDir = normalizeDirectory(video.url)
            val videoGrandParentDir = if (videoParentDir.contains("/")) videoParentDir.substringBeforeLast('/') else ""

            // 1. Multi-location Artwork Detection
            if (matchedArtwork.isNullOrBlank()) {
                // Same directory matching video name or video base name prefix
                var candidateArtwork = artworks.firstOrNull { art ->
                    val artBase = cleanName(art.originalFilename)
                    val artDir = normalizeDirectory(art.url)
                    artDir == videoParentDir && (artBase == videoBaseName || artBase.startsWith(videoBaseName) || videoBaseName.startsWith(artBase))
                }

                // Same directory matching standard poster/cover name
                if (candidateArtwork == null) {
                    candidateArtwork = artworks.firstOrNull { art ->
                        val artBase = cleanName(art.originalFilename)
                        val artDir = normalizeDirectory(art.url)
                        artDir == videoParentDir && POSTER_NAMES.any { artBase.contains(it) }
                    }
                }

                // Subdirectory (e.g. artwork/, thumbnails/, covers/)
                if (candidateArtwork == null) {
                    candidateArtwork = artworks.firstOrNull { art ->
                        val artDir = normalizeDirectory(art.url)
                        val lastSubDir = artDir.substringAfterLast('/')
                        val artParentDir = if (artDir.contains("/")) artDir.substringBeforeLast('/') else ""
                        artParentDir == videoParentDir && ARTWORK_DIR_NAMES.contains(lastSubDir)
                    }
                }

                // Any artwork in the same directory as fallback
                if (candidateArtwork == null) {
                    candidateArtwork = artworks.firstOrNull { art ->
                        val artDir = normalizeDirectory(art.url)
                        artDir == videoParentDir
                    }
                }

                // Parent directory poster/cover (e.g. for CD1/movie.avi or nested folders)
                if (candidateArtwork == null && videoGrandParentDir.isNotBlank()) {
                    candidateArtwork = artworks.firstOrNull { art ->
                        val artBase = cleanName(art.originalFilename)
                        val artDir = normalizeDirectory(art.url)
                        artDir == videoGrandParentDir && (artBase == videoBaseName || POSTER_NAMES.any { artBase.contains(it) })
                    }
                }

                // Any artwork in the parent directory as secondary fallback
                if (candidateArtwork == null && videoGrandParentDir.isNotBlank()) {
                    candidateArtwork = artworks.firstOrNull { art ->
                        val artDir = normalizeDirectory(art.url)
                        artDir == videoGrandParentDir
                    }
                }

                matchedArtwork = candidateArtwork?.url ?: pageArtworkUrl
            }

            // 2. Multi-location Subtitle Detection
            if (matchedSubtitle.isNullOrBlank()) {
                // Same directory matching video name or video base name prefix
                var candidateSubtitle = subtitles.firstOrNull { sub ->
                    val subBase = cleanName(sub.originalFilename)
                    val subDir = normalizeDirectory(sub.url)
                    subDir == videoParentDir && (subBase == videoBaseName || subBase.startsWith(videoBaseName))
                }

                // Subdirectory (e.g. subtitles/, subs/)
                if (candidateSubtitle == null) {
                    candidateSubtitle = subtitles.firstOrNull { sub ->
                        val subDir = normalizeDirectory(sub.url)
                        val lastSubDir = subDir.substringAfterLast('/')
                        val subParentDir = if (subDir.contains("/")) subDir.substringBeforeLast('/') else ""
                        subParentDir == videoParentDir && SUBTITLE_DIR_NAMES.contains(lastSubDir)
                    }
                }

                // Parent directory
                if (candidateSubtitle == null && videoGrandParentDir.isNotBlank()) {
                    candidateSubtitle = subtitles.firstOrNull { sub ->
                        val subBase = cleanName(sub.originalFilename)
                        val subDir = normalizeDirectory(sub.url)
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

    private fun cleanName(raw: String): String {
        return try {
            java.net.URLDecoder.decode(raw, "UTF-8").substringBeforeLast('.').lowercase().trim()
        } catch (_: Exception) {
            raw.substringBeforeLast('.').lowercase().trim()
        }
    }

    private fun normalizeDirectory(urlStr: String): String {
        return try {
            val decoded = java.net.URLDecoder.decode(urlStr, "UTF-8")
            val clean = decoded.substringBefore('?').substringBefore('#').trimEnd('/')
            if (clean.contains("/")) clean.substringBeforeLast('/').lowercase() else ""
        } catch (_: Exception) {
            val clean = urlStr.substringBefore('?').substringBefore('#').trimEnd('/')
            if (clean.contains("/")) clean.substringBeforeLast('/').lowercase() else ""
        }
    }
}
