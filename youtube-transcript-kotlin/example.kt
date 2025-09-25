package com.youtubetranscript.example

import com.youtubetranscript.YouTubeTranscriptApi
import com.youtubetranscript.TranscriptException
import kotlinx.coroutines.runBlocking

/**
 * Example usage of YouTube Transcript Kotlin library.
 * 
 * This file demonstrates various ways to use the library.
 */
fun main() = runBlocking {
    
    // Example 1: Basic transcript fetching
    println("=== Example 1: Basic Usage ===")
    try {
        val transcript = YouTubeTranscriptApi.getTranscript("dQw4w9WgXcQ")
        println("Found ${transcript.size} transcript segments")
        transcript.take(3).forEach { segment ->
            println("${segment.getFormattedTimestamp()}: ${segment.text}")
        }
    } catch (e: TranscriptException) {
        println("Error: ${e.message}")
    }
    
    // Example 2: Language preference
    println("\n=== Example 2: Language Preference ===")
    try {
        val transcript = YouTubeTranscriptApi.getTranscript(
            videoId = "dQw4w9WgXcQ",
            languages = listOf("en", "es", "fr")
        )
        println("Found transcript in preferred language")
        println("First segment: ${transcript.first().text}")
    } catch (e: TranscriptException) {
        println("Error: ${e.message}")
    }
    
    // Example 3: List available transcripts
    println("\n=== Example 3: Available Transcripts ===")
    try {
        val transcriptList = YouTubeTranscriptApi.listTranscripts("dQw4w9WgXcQ")
        println("Available languages: ${transcriptList.getAvailableLanguages()}")
        println("Manual transcripts: ${transcriptList.getManuallyCreatedLanguages()}")
        println("Generated transcripts: ${transcriptList.getGeneratedLanguages()}")
    } catch (e: TranscriptException) {
        println("Error: ${e.message}")
    }
    
    // Example 4: Video ID extraction
    println("\n=== Example 4: Video ID Extraction ===")
    val urls = listOf(
        "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        "https://youtu.be/dQw4w9WgXcQ",
        "https://www.youtube.com/embed/dQw4w9WgXcQ",
        "dQw4w9WgXcQ"
    )
    
    urls.forEach { url ->
        try {
            val videoId = YouTubeTranscriptApi.extractVideoId(url)
            println("$url -> $videoId")
        } catch (e: Exception) {
            println("$url -> Error: ${e.message}")
        }
    }
    
    // Example 5: Check if video has transcripts
    println("\n=== Example 5: Check Transcript Availability ===")
    val videoIds = listOf("dQw4w9WgXcQ", "invalid_id")
    
    videoIds.forEach { videoId ->
        try {
            val hasTranscripts = YouTubeTranscriptApi.hasTranscripts(videoId)
            println("Video $videoId has transcripts: $hasTranscripts")
        } catch (e: Exception) {
            println("Video $videoId: Error checking transcripts - ${e.message}")
        }
    }
    
    // Example 6: Translation
    println("\n=== Example 6: Translation ===")
    try {
        val transcriptList = YouTubeTranscriptApi.listTranscripts("dQw4w9WgXcQ")
        val transcript = transcriptList.findTranscript(listOf("en"))
        
        if (transcript.isTranslatable) {
            println("Original language: ${transcript.language}")
            println("Available translations: ${transcript.translationLanguages.map { it.languageCode }}")
            
            // Translate to Spanish if available
            val spanishTranslation = transcript.translationLanguages.find { it.languageCode == "es" }
            if (spanishTranslation != null) {
                val translatedTranscript = transcript.translate("es")
                val translatedSegments = translatedTranscript.fetch()
                println("Translated to Spanish: ${translatedSegments.first().text}")
            }
        } else {
            println("Transcript is not translatable")
        }
    } catch (e: TranscriptException) {
        println("Error: ${e.message}")
    }
    
    // Example 7: Error handling
    println("\n=== Example 7: Error Handling ===")
    try {
        YouTubeTranscriptApi.getTranscript("invalid_video_id")
    } catch (e: TranscriptException) {
        when (e) {
            is com.youtubetranscript.VideoUnavailableException -> {
                println("Video is unavailable: ${e.message}")
            }
            is com.youtubetranscript.TranscriptsDisabledException -> {
                println("Transcripts are disabled: ${e.message}")
            }
            is com.youtubetranscript.NoTranscriptFoundException -> {
                println("No transcript found: ${e.message}")
                println("Available languages: ${e.availableLanguages}")
            }
            else -> {
                println("Other error: ${e.message}")
            }
        }
    }
}
