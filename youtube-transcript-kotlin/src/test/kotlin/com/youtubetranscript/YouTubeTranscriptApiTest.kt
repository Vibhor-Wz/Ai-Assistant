package com.youtubetranscript

import com.youtubetranscript.models.TranscriptSegment
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for YouTubeTranscriptApi.
 */
class YouTubeTranscriptApiTest {
    
    @Test
    fun testExtractVideoId() {
        // Test various URL formats
        assertEquals("dQw4w9WgXcQ", YouTubeTranscriptApi.extractVideoId("dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", YouTubeTranscriptApi.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", YouTubeTranscriptApi.extractVideoId("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", YouTubeTranscriptApi.extractVideoId("https://www.youtube.com/embed/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", YouTubeTranscriptApi.extractVideoId("https://m.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", YouTubeTranscriptApi.extractVideoId("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
    }
    
    @Test
    fun testExtractVideoIdInvalid() {
        // Test invalid URLs
        assertThrows(IllegalArgumentException::class.java) {
            YouTubeTranscriptApi.extractVideoId("")
        }
        
        assertThrows(IllegalArgumentException::class.java) {
            YouTubeTranscriptApi.extractVideoId("invalid-url")
        }
        
        assertThrows(IllegalArgumentException::class.java) {
            YouTubeTranscriptApi.extractVideoId("https://www.youtube.com/watch?v=invalid")
        }
    }
    
    @Test
    fun testTranscriptSegment() {
        val segment = TranscriptSegment(
            text = "Hello world",
            start = 10.5,
            duration = 2.0
        )
        
        assertEquals("Hello world", segment.text)
        assertEquals(10.5, segment.start, 0.001)
        assertEquals(2.0, segment.duration, 0.001)
        assertEquals(12.5, segment.end, 0.001)
        assertEquals("10:10", segment.getFormattedTimestamp())
    }
    
    @Test
    fun testTranscriptSegmentOverlap() {
        val segment1 = TranscriptSegment("First", 10.0, 5.0)
        val segment2 = TranscriptSegment("Second", 12.0, 3.0)
        val segment3 = TranscriptSegment("Third", 20.0, 2.0)
        
        assertTrue(segment1.overlapsWith(segment2))
        assertTrue(segment2.overlapsWith(segment1))
        assertFalse(segment1.overlapsWith(segment3))
        assertFalse(segment3.overlapsWith(segment1))
    }
}
