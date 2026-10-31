package com.example.crawler

object FilenameCleaner {

    private val TECHNICAL_PATTERNS = listOf(
        // Resolutions
        Regex("""(?i)\b(2160p|4k|uhd|1080p|1080i|720p|576p|480p|360p)\b"""),
        // Codecs & formats
        Regex("""(?i)\b(x264|x265|h264|h265|hevc|avc|xvid|divx|av1|10bit|8bit)\b"""),
        // Audio
        Regex("""(?i)\b(aac|ac3|dts|dts-hd|truehd|atmos|mp3|flac|ddp5\.1|dd5\.1|5\.1|7\.1)\b"""),
        // Sources & releases
        Regex("""(?i)\b(web-dl|webrip|bluray|blu-ray|bdrip|brrip|dvdrip|hdtv|remux|proper|repack)\b"""),
        // Years when part of technical release tag
        Regex("""\b(19\d\d|20\d\d)\b"""),
        // Common release groups / tags in brackets or after hyphen
        Regex("""\[.*?\]"""),
        Regex("""\{.*?\}""")
    )

    private val GENERIC_FILENAMES = setOf(
        "movie", "video", "sample", "main", "play", "index", "track", "out",
        "file", "download", "default", "stream", "final", "part1", "part2",
        "cd1", "cd2", "disc1", "disc2", "clip", "preview", "media"
    )

    private val GENERIC_DIRECTORY_NAMES = setOf(
        "video", "videos", "media", "files", "storage", "pub", "downloads",
        "download", "content", "ftp", "www", "html", "public_html", "var",
        "mnt", "data", "movies", "root", "share", "shared"
    )

    fun clean(filename: String): String {
        // Strip extension
        var name = filename.substringBeforeLast('.')

        // Replace separators (dots, underscores, pluses) with spaces first so word boundaries work
        name = name.replace(Regex("""[._+]"""), " ")
        name = name.replace(Regex("""\s+-\s+"""), " - ")

        // Replace technical tokens with spaces
        for (pattern in TECHNICAL_PATTERNS) {
            name = pattern.replace(name, " ")
        }

        // Collapse multiple spaces
        name = name.trim().replace(Regex("""\s+"""), " ")

        // Capitalize words nicely
        if (name.isBlank()) {
            return filename.substringBeforeLast('.')
        }

        return name.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                if (word.length > 1 && word.all { it.isUpperCase() }) {
                    word // keep acronyms like BBC, NASA, S01E02
                } else {
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
            }
    }

    /**
     * Resolves the best available title:
     * 1. Webpage title (if meaningful and not generic site/filename)
     * 2. Cleaned filename
     * 3. Fallback to parent directory name if the filename is generic (e.g. movie.mp4, video.mkv)
     */
    fun resolveTitle(filename: String, pathOrUrl: String = "", pageTitle: String? = null): String {
        val baseName = filename.substringBeforeLast('.').lowercase().trim()
        val isGeneric = baseName.isBlank() || GENERIC_FILENAMES.contains(baseName) || baseName.matches(Regex("""^(part|cd|disc)\s*\d+$"""))

        if (!pageTitle.isNullOrBlank() && !pageTitle.endsWith(".mp4", ignoreCase = true) && !pageTitle.contains("Index of", ignoreCase = true) && pageTitle.length > 2) {
            return pageTitle.trim()
        }

        if (isGeneric && pathOrUrl.isNotBlank()) {
            val parentName = getMeaningfulParentDirName(pathOrUrl)
            if (parentName.isNotBlank()) {
                val cleanedParent = clean(parentName)
                if (cleanedParent.isNotBlank()) {
                    return cleanedParent
                }
            }
        }

        return clean(filename)
    }

    private fun getMeaningfulParentDirName(pathOrUrl: String): String {
        return try {
            val decoded = java.net.URLDecoder.decode(pathOrUrl, "UTF-8")
            val segments = decoded.split("/").filter { it.isNotBlank() }
            // Traverse from last parent folder backwards
            for (i in (segments.size - 2) downTo 0) {
                val seg = segments[i].trim()
                val lower = seg.lowercase()
                if (lower !in GENERIC_DIRECTORY_NAMES && !lower.startsWith("cd") && !lower.startsWith("disc")) {
                    return seg
                }
            }
            ""
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Preserves multi-level nested folder structure (e.g. "English Movies / 1999 / American Pie")
     */
    fun extractHierarchicalCategory(pathOrUrl: String): String {
        try {
            val decoded = java.net.URLDecoder.decode(pathOrUrl, "UTF-8")
            val path = if (decoded.contains("://")) {
                decoded.substringAfter("://").substringAfter("/", "")
            } else {
                decoded
            }
            val segments = path.split("/").filter { it.isNotBlank() }
            if (segments.size <= 1) return "General"

            // Omit the last segment (the video filename itself)
            val dirSegments = segments.dropLast(1)

            val meaningfulSegments = dirSegments.filter { seg ->
                val lower = seg.lowercase().trim()
                lower !in GENERIC_DIRECTORY_NAMES && lower.length > 1
            }

            if (meaningfulSegments.isNotEmpty()) {
                return meaningfulSegments.takeLast(3).joinToString(" / ") { seg ->
                    val cleaned = seg.replace(Regex("""[._\-%20]"""), " ").trim()
                    cleaned.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
                        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    }
                }
            }
        } catch (_: Exception) {}
        return "General"
    }

    fun extractCategoryFromPath(pathOrUrl: String): String {
        return extractHierarchicalCategory(pathOrUrl)
    }
}
