package com.youtubetranscript.models

/**
 * Represents a single segment of a YouTube transcript.
 * 
 * This class mirrors the FetchedTranscriptSnippet from the Python library.
 * 
 * @property text The transcript text for this segment
 * @property start The timestamp at which this segment appears on screen (in seconds)
 * @property duration The duration of how long the segment stays on screen (in seconds)
 * 
 * Note: Duration is not the duration of the transcribed speech, but how long 
 * the segment stays on screen. There can be overlaps between segments.
 */
data class TranscriptSegment(
    val text: String,
    val start: Double,
    val duration: Double
) {
    /**
     * Get the end timestamp of this segment.
     */
    val end: Double
        get() = start + duration
    
    /**
     * Get the duration of the actual speech (approximation).
     * This is a rough estimate based on text length.
     */
    val speechDuration: Double
        get() = text.length * 0.05 // Rough estimate: 50ms per character
    
    /**
     * Check if this segment overlaps with another segment.
     */
    fun overlapsWith(other: TranscriptSegment): Boolean {
        return start < other.end && end > other.start
    }
    
    /**
     * Get a formatted timestamp string.
     */
    fun getFormattedTimestamp(): String {
        val minutes = (start / 60).toInt()
        val seconds = (start % 60).toInt()
        return String.format("%d:%02d", minutes, seconds)
    }
    
    override fun toString(): String {
        return "[${getFormattedTimestamp()}] $text"
    }
}
