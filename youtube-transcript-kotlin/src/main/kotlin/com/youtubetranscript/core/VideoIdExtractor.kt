package com.youtubetranscript.core

import com.youtubetranscript.TranscriptException

/**
 * Extracts video IDs from various YouTube URL formats.
 * 
 * This class handles all common YouTube URL patterns.
 */
object VideoIdExtractor {
    
    private val patterns = listOf(
        // Standard YouTube URLs
        """(?:https?://)?(?:www\.)?youtube\.com/watch\?v=([a-zA-Z0-9_-]{11})""".toRegex(),
        """(?:https?://)?(?:www\.)?youtube\.com/embed/([a-zA-Z0-9_-]{11})""".toRegex(),
        """(?:https?://)?(?:www\.)?youtube\.com/v/([a-zA-Z0-9_-]{11})""".toRegex(),
        
        // Short URLs
        """(?:https?://)?youtu\.be/([a-zA-Z0-9_-]{11})""".toRegex(),
        
        // Mobile URLs
        """(?:https?://)?(?:m\.)?youtube\.com/watch\?v=([a-zA-Z0-9_-]{11})""".toRegex(),
        
        // Live URLs
        """(?:https?://)?(?:www\.)?youtube\.com/live/([a-zA-Z0-9_-]{11})""".toRegex(),
        
        // Shorts URLs
        """(?:https?://)?(?:www\.)?youtube\.com/shorts/([a-zA-Z0-9_-]{11})""".toRegex(),
        
        // Direct video ID (11 characters)
        """^([a-zA-Z0-9_-]{11})$""".toRegex()
    )
    
    /**
     * Extract video ID from various YouTube URL formats.
     * 
     * @param url YouTube URL or video ID
     * @return Extracted video ID
     * @throws IllegalArgumentException If video ID cannot be extracted
     */
    fun extractVideoId(url: String): String {
        if (url.isBlank()) {
            throw IllegalArgumentException("URL cannot be empty")
        }
        
        val trimmedUrl = url.trim()
        
        for (pattern in patterns) {
            val match = pattern.find(trimmedUrl)
            if (match != null) {
                val videoId = match.groupValues[1]
                if (isValidVideoId(videoId)) {
                    return videoId
                }
            }
        }
        
        throw IllegalArgumentException("Could not extract video ID from: $url")
    }
    
    /**
     * Check if a string is a valid YouTube video ID.
     * 
     * @param videoId The video ID to validate
     * @return True if valid, false otherwise
     */
    fun isValidVideoId(videoId: String): Boolean {
        return videoId.length == 11 && videoId.matches("""[a-zA-Z0-9_-]{11}""".toRegex())
    }
    
    /**
     * Get all supported URL patterns.
     * 
     * @return List of supported URL patterns
     */
    fun getSupportedPatterns(): List<String> {
        return listOf(
            "https://www.youtube.com/watch?v=VIDEO_ID",
            "https://youtu.be/VIDEO_ID",
            "https://www.youtube.com/embed/VIDEO_ID",
            "https://www.youtube.com/v/VIDEO_ID",
            "https://m.youtube.com/watch?v=VIDEO_ID",
            "https://www.youtube.com/live/VIDEO_ID",
            "https://www.youtube.com/shorts/VIDEO_ID",
            "VIDEO_ID" // Direct video ID
        )
    }
}
