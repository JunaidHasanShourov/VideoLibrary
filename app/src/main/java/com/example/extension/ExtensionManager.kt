package com.example.extension

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ExtensionType(val displayName: String) {
    CRAWLER("Web / Directory Crawler"),
    FTP("FTP / FTPS Scanner"),
    METADATA("Metadata Provider"),
    SUBTITLE("Subtitle Resolver")
}

data class MediaExtension(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val author: String,
    val type: ExtensionType,
    val isEnabled: Boolean = true
)

object ExtensionManager {

    private val defaultExtensions = listOf(
        MediaExtension(
            id = "ext_web_crawler",
            name = "HTTP/HTTPS Directory Crawler",
            description = "Scans Apache, Nginx, and Caddy auto-index listings and HTML pages for video files recursively.",
            version = "1.2.0",
            author = "Video Library Core",
            type = ExtensionType.CRAWLER,
            isEnabled = true
        ),
        MediaExtension(
            id = "ext_ftp_scanner",
            name = "FTP/FTPS Media Scanner",
            description = "Connects to accessible FTP and FTPS media servers with support for deep folder traversal.",
            version = "1.1.0",
            author = "Video Library Core",
            type = ExtensionType.FTP,
            isEnabled = true
        ),
        MediaExtension(
            id = "ext_metadata_extractor",
            name = "Smart Metadata & Artwork Associator",
            description = "Extracts OpenGraph, page titles, hierarchical categories, and discovers nearby posters and subtitles.",
            version = "2.0.0",
            author = "Video Library Core",
            type = ExtensionType.METADATA,
            isEnabled = true
        ),
        MediaExtension(
            id = "ext_subtitle_resolver",
            name = "Multi-Language Subtitle Resolver",
            description = "Matches SRT and VTT subtitles from same or nested directories with automatic language tagging.",
            version = "1.0.5",
            author = "Video Library Core",
            type = ExtensionType.SUBTITLE,
            isEnabled = true
        )
    )

    private val _extensions = MutableStateFlow(defaultExtensions)
    val extensions: StateFlow<List<MediaExtension>> = _extensions.asStateFlow()

    fun toggleExtension(id: String) {
        _extensions.value = _extensions.value.map { ext ->
            if (ext.id == id) ext.copy(isEnabled = !ext.isEnabled) else ext
        }
    }

    fun isExtensionEnabled(id: String): Boolean {
        return _extensions.value.find { it.id == id }?.isEnabled ?: false
    }
}
