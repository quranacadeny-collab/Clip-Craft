package com.example

import com.example.data.model.YouTubeUrlParser
import com.example.media.CropRect
import com.example.ui.formatDurationMs
import com.example.ui.formatFileSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `extractVideoId parses watch shorts and youtu_be links`() {
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeUrlParser.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=10s")
        )
        assertEquals(
            "dQw4w9WgXcQ",
            YouTubeUrlParser.extractVideoId("https://youtu.be/dQw4w9WgXcQ")
        )
        assertEquals(
            "abcDEF12345",
            YouTubeUrlParser.extractVideoId("https://youtube.com/shorts/abcDEF12345?feature=share")
        )
        assertNull(YouTubeUrlParser.extractVideoId("https://example.com/not-youtube"))
    }

    @Test
    fun `cropRect fullFrame and formatting helpers work`() {
        assertTrue(CropRect().isFullFrame())
        assertFalse(CropRect(0.2f, 0f, 0.8f, 1f).isFullFrame())
        assertEquals("01:05", formatDurationMs(65_000L))
        assertEquals("2.0 MB", formatFileSize(2L * 1024L * 1024L))
    }
}
