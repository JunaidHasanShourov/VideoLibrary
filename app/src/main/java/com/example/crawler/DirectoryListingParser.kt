package com.example.crawler

import org.jsoup.nodes.Document
import java.net.URI

object DirectoryListingParser {

    fun isDirectoryListing(doc: Document): Boolean {
        val title = doc.title().lowercase()
        val h1 = doc.select("h1").text().lowercase()

        if (title.contains("index of") || h1.contains("index of") ||
            title.contains("directory listing") || h1.contains("directory listing")
        ) {
            return true
        }

        // Check for typical Apache / Nginx / IIS table / pre structure
        val preLinks = doc.select("pre a")
        if (preLinks.size >= 3) return true

        val tableLinks = doc.select("table a")
        if (tableLinks.size >= 4 && (doc.text().contains("Last modified") || doc.text().contains("Size") || doc.text().contains("Description"))) {
            return true
        }

        return false
    }

    fun parseDirectoryLinks(doc: Document, baseUrl: String): List<ParsedLink> {
        val results = mutableListOf<ParsedLink>()
        val links = doc.select("a[href]")

        for (link in links) {
            val href = link.attr("href")
            val text = link.text().trim()

            // Skip sorting links, parent directory navigations, queries
            if (href.startsWith("?") || href.startsWith("#") ||
                href.equals("../", ignoreCase = true) || href.equals("..", ignoreCase = true) ||
                text.equals("Parent Directory", ignoreCase = true) ||
                text.equals("[To Parent Directory]", ignoreCase = true)
            ) {
                continue
            }

            try {
                val absoluteUri = URI(baseUrl).resolve(href)
                val fullUrl = absoluteUri.toString()
                val isDir = href.endsWith("/") || (!href.contains(".") && !href.startsWith("http"))

                results.add(
                    ParsedLink(
                        url = fullUrl,
                        name = if (text.isNotBlank()) text.removeSuffix("/") else href.removeSuffix("/").substringAfterLast('/'),
                        isDirectory = isDir
                    )
                )
            } catch (_: Exception) {}
        }

        return results
    }
}

data class ParsedLink(
    val url: String,
    val name: String,
    val isDirectory: Boolean
)
