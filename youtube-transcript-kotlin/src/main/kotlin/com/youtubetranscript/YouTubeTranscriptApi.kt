package com.youtubetranscript

import com.youtubetranscript.models.TranscriptSegment
import com.youtubetranscript.models.TranscriptList
import com.youtubetranscript.core.TranscriptListFetcher
import com.youtubetranscript.core.VideoIdExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * YouTube Transcript API for Kotlin/Android
 * 
 * A Kotlin library for fetching YouTube video transcripts, similar to Python's youtube-transcript-api.
 * 
 * ## Usage
 * 
 * ```kotlin
 * // Basic usage
 * val transcript = YouTubeTranscriptApi.getTranscript("dQw4w9WgXcQ")
 * 
 * // With specific language
 * val transcript = YouTubeTranscriptApi.getTranscript("dQw4w9WgXcQ", listOf("en", "es"))
 * 
 * // Get available transcripts
 * val transcriptList = YouTubeTranscriptApi.listTranscripts("dQw4w9WgXcQ")
 * ```
 * 
 * @author YouTube Transcript Kotlin
 * @version 1.0.0
 */
object YouTubeTranscriptApi {
    
    /**
     * Fetch transcript for a YouTube video.
     * 
     * @param videoId The YouTube video ID
     * @param languages List of language codes in order of preference (default: ["en"])
     * @param preserveFormatting Whether to preserve HTML formatting in transcript text
     * @return List of transcript segments
     * @throws TranscriptException If transcript cannot be fetched
     */
    suspend fun getTranscript(
        videoId: String,
        languages: List<String> = listOf("en"),
        preserveFormatting: Boolean = false
    ): List<TranscriptSegment> = withContext(Dispatchers.IO) {
        val transcriptList = listTranscripts(videoId)
        
        // Try to find transcript in requested languages first
        val transcript = try {
            transcriptList.findTranscript(languages)
        } catch (e: TranscriptException) {
            // If no transcript found for requested languages, try to get any available transcript
            try {
                val availableLanguages = transcriptList.getAvailableLanguages()
                if (availableLanguages.isNotEmpty()) {
                    // Try to get the first available transcript
                    transcriptList.findTranscript(listOf(availableLanguages.first()))
                } else {
                    // If no languages available, try to get any transcript from the list
                    val allTranscripts = transcriptList.getAllTranscripts()
                    if (allTranscripts.isNotEmpty()) {
                        allTranscripts.first()
                    } else {
                        throw e // Re-throw original exception if no transcripts available at all
                    }
                }
            } catch (fallbackException: TranscriptException) {
                throw e // Re-throw original exception if fallback also fails
            }
        }
        
        transcript.fetch(preserveFormatting)
    }
    
    /**
     * Get a list of available transcripts for a YouTube video.
     * 
     * @param videoId The YouTube video ID
     * @return TranscriptList containing all available transcripts
     * @throws TranscriptException If video cannot be accessed
     */
    suspend fun listTranscripts(videoId: String): TranscriptList = withContext(Dispatchers.IO) {
        val fetcher = TranscriptListFetcher()
        fetcher.fetch(videoId)
    }
    
    /**
     * Extract video ID from various YouTube URL formats.
     * 
     * @param url YouTube URL or video ID
     * @return Extracted video ID
     * @throws IllegalArgumentException If video ID cannot be extracted
     */
    fun extractVideoId(url: String): String {
        return VideoIdExtractor.extractVideoId(url)
    }
    
    /**
     * Check if a video has transcripts available.
     * 
     * @param videoId The YouTube video ID
     * @return True if transcripts are available, false otherwise
     */
    suspend fun hasTranscripts(videoId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val transcriptList = listTranscripts(videoId)
            transcriptList.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get available languages for a video's transcripts.
     * 
     * @param videoId The YouTube video ID
     * @return List of available language codes
     */
    suspend fun getAvailableLanguages(videoId: String): List<String> = withContext(Dispatchers.IO) {
        val transcriptList = listTranscripts(videoId)
        transcriptList.getAvailableLanguages()
    }
}
