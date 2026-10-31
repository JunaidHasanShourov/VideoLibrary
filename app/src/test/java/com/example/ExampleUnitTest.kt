package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun metadataAssociator_associatesArtworkInSameDirectory() {
    val items = listOf(
      com.example.model.DiscoveredMedia(
        url = "https://example.com/movies/Action/Big%20Buck%20Bunny.mp4",
        title = "Big Buck Bunny",
        originalFilename = "Big%20Buck%20Bunny.mp4",
        mediaType = com.example.model.MediaType.VIDEO,
        format = "mp4"
      ),
      com.example.model.DiscoveredMedia(
        url = "https://example.com/movies/Action/cover.jpg",
        title = "cover",
        originalFilename = "cover.jpg",
        mediaType = com.example.model.MediaType.ARTWORK,
        format = "jpg"
      )
    )

    val associated = com.example.crawler.MetadataAssociator.associate(items)
    assertEquals(1, associated.size)
    assertEquals("https://example.com/movies/Action/cover.jpg", associated[0].artworkUrl)
  }

  @Test
  fun metadataAssociator_associatesParentDirectoryCover() {
    val items = listOf(
      com.example.model.DiscoveredMedia(
        url = "https://example.com/movies/SciFi/CD1/movie.mp4",
        title = "movie",
        originalFilename = "movie.mp4",
        mediaType = com.example.model.MediaType.VIDEO,
        format = "mp4"
      ),
      com.example.model.DiscoveredMedia(
        url = "https://example.com/movies/SciFi/poster.png",
        title = "poster",
        originalFilename = "poster.png",
        mediaType = com.example.model.MediaType.ARTWORK,
        format = "png"
      )
    )

    val associated = com.example.crawler.MetadataAssociator.associate(items)
    assertEquals(1, associated.size)
    assertEquals("https://example.com/movies/SciFi/poster.png", associated[0].artworkUrl)
  }

  @Test
  fun metadataAssociator_fallbackToPageArtwork() {
    val items = listOf(
      com.example.model.DiscoveredMedia(
        url = "https://example.com/stream/video.mp4",
        title = "video",
        originalFilename = "video.mp4",
        mediaType = com.example.model.MediaType.VIDEO,
        format = "mp4"
      )
    )

    val associated = com.example.crawler.MetadataAssociator.associate(items, pageArtworkUrl = "https://example.com/og_thumb.jpg")
    assertEquals(1, associated.size)
    assertEquals("https://example.com/og_thumb.jpg", associated[0].artworkUrl)
  }

  @Test
  fun mediaFormats_recognizesVideoAndAudioExtensions() {
    assertTrue(com.example.model.MediaFormats.isVideo("mkv"))
    assertTrue(com.example.model.MediaFormats.isVideo("avi"))
    assertTrue(com.example.model.MediaFormats.isVideo("flv"))
    assertTrue(com.example.model.MediaFormats.isVideo("ts"))
    assertTrue(com.example.model.MediaFormats.isVideo("m2ts"))
    assertTrue(com.example.model.MediaFormats.isVideo("vob"))
    assertTrue(com.example.model.MediaFormats.isVideo("wmv"))
    assertTrue(com.example.model.MediaFormats.isVideo("3gp"))
    assertTrue(com.example.model.MediaFormats.isVideo("webm"))
    assertTrue(com.example.model.MediaFormats.isVideo("mp4"))
    assertTrue(com.example.model.MediaFormats.isVideo("m3u8"))
    assertTrue(com.example.model.MediaFormats.isVideo("mpd"))

    assertTrue(com.example.model.MediaFormats.isAudio("flac"))
    assertTrue(com.example.model.MediaFormats.isAudio("opus"))
    assertTrue(com.example.model.MediaFormats.isAudio("ac3"))
    assertTrue(com.example.model.MediaFormats.isAudio("eac3"))
    assertTrue(com.example.model.MediaFormats.isAudio("dts"))
    assertTrue(com.example.model.MediaFormats.isAudio("alac"))
    assertTrue(com.example.model.MediaFormats.isAudio("wav"))
    assertTrue(com.example.model.MediaFormats.isAudio("aac"))
  }
}
