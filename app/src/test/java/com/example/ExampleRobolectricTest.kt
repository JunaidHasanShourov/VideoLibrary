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
}
