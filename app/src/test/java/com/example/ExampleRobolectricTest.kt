package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.crawler.FilenameCleaner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Video Library", appName)
    }

    @Test
    fun `filename cleaner removes technical tags`() {
        val cleaned = FilenameCleaner.clean("Movie_Name_2025_1080p.mp4")
        assertEquals("Movie Name", cleaned)
    }

    @Test
    fun `filename cleaner handles complex release names`() {
        val cleaned = FilenameCleaner.clean("Big.Buck.Bunny.720p.WEB-DL.x264.mkv")
        assertEquals("Big Buck Bunny", cleaned)
    }

    @Test
    fun `resolveTitle uses parent folder for generic filenames`() {
        val title = FilenameCleaner.resolveTitle("movie.mp4", "/Movies/Inception/movie.mp4")
        assertEquals("Inception", title)
    }

    @Test
    fun `extractHierarchicalCategory preserves multi-level folder structure`() {
        val category = FilenameCleaner.extractHierarchicalCategory("/media/English Movies/1999/American Pie/movie.mp4")
        assertEquals("English Movies / 1999 / American Pie", category)
    }

    @Test
    fun `metadata associator extracts subtitle languages correctly`() {
        assertEquals("English", com.example.crawler.MetadataAssociator.extractSubtitleLanguage("movie.en.srt"))
        assertEquals("Bengali", com.example.crawler.MetadataAssociator.extractSubtitleLanguage("movie.bn.srt"))
        assertEquals("Hindi", com.example.crawler.MetadataAssociator.extractSubtitleLanguage("movie.hi.srt"))
    }

    @Test
    fun `audio track formatting displays codec channels and rate`() {
        val format = androidx.media3.common.Format.Builder()
            .setSampleMimeType("audio/eac3")
            .setChannelCount(6)
            .setSampleRate(48000)
            .setLanguage("eng")
            .build()
        val label = com.example.ui.player.formatAudioTrackLabel(format, 0)
        assertTrue(label.contains("E-AC-3"))
        assertTrue(label.contains("5.1 Surround"))
        assertTrue(label.contains("48 kHz"))
    }

    @Test
    fun `video track formatting displays resolution bitrate and frame rate`() {
        val format = androidx.media3.common.Format.Builder()
            .setWidth(1920)
            .setHeight(1080)
            .setAverageBitrate(5_500_000)
            .setFrameRate(60f)
            .build()

        val label = com.example.ui.player.formatVideoTrackLabel(format, 0)
        assertTrue(label.contains("1080p FHD"))
        assertTrue(label.contains("1920x1080"))
        assertTrue(label.contains("5.5 Mbps"))
        assertTrue(label.contains("60fps"))
    }

    @Test
    fun `video scale modes and subtitle size enums have valid configurations`() {
        assertEquals(3, com.example.ui.player.VideoScaleMode.values().size)
        assertEquals(4, com.example.ui.player.SubtitleSize.values().size)
        assertTrue(com.example.ui.player.SubtitleSize.LARGE.sizeSp > com.example.ui.player.SubtitleSize.SMALL.sizeSp)
    }

    @Test
    fun `ffmpeg audio renderer is available on classpath`() {
        val clazz = Class.forName("androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer")
        org.junit.Assert.assertNotNull(clazz)
        val constructor = clazz.getConstructor(
            android.os.Handler::class.java,
            androidx.media3.exoplayer.audio.AudioRendererEventListener::class.java,
            androidx.media3.exoplayer.audio.AudioSink::class.java
        )
        org.junit.Assert.assertNotNull(constructor)
    }

    @Test
    fun `purgeSampleData deletes sample items from database`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.database.AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = com.example.repository.VideoRepository(db)
        repo.insertVideo(
            com.example.data.database.VideoEntity(
                title = "Big Buck Bunny",
                originalFilename = "BigBuckBunny.mp4",
                sourceUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                format = "mp4"
            )
        )
        repo.insertVideo(
            com.example.data.database.VideoEntity(
                title = "My Personal Family Vacation",
                originalFilename = "family_vacation.mp4",
                sourceUrl = "https://mycloud.home/family_vacation.mp4",
                format = "mp4"
            )
        )
        assertEquals(2, repo.getVideoCount())
        repo.purgeSampleData()
        assertEquals(1, repo.getVideoCount())
        val remaining = repo.getAllVideosDirect()
        assertEquals("My Personal Family Vacation", remaining[0].title)
        db.close()
    }
}

