package com.youtubetranscript.models

import com.youtubetranscript.TranscriptException

/**
 * Represents a list of available transcripts for a YouTube video.
 * 
 * This class mirrors the TranscriptList class from the Python library.
 * 
 * @property videoId The YouTube video ID
 * @property manuallyCreatedTranscripts Map of manually created transcripts by language code
 * @property generatedTranscripts Map of auto-generated transcripts by language code
 * @property translationLanguages List of available translation languages
 */
class TranscriptList(
    val videoId: String,
    private val manuallyCreatedTranscripts: Map<String, Transcript>,
    private val generatedTranscripts: Map<String, Transcript>,
    val translationLanguages: List<TranslationLanguage>
) {
    
    /**
     * Get all available transcripts as a list.
     */
    fun getAllTranscripts(): List<Transcript> {
        return manuallyCreatedTranscripts.values + generatedTranscripts.values
    }
    
    /**
     * Find a transcript for the given language codes.
     * Manually created transcripts are preferred over generated ones.
     * 
     * @param languageCodes List of language codes in order of preference
     * @return The first matching transcript
     * @throws TranscriptException If no transcript is found
     */
    fun findTranscript(languageCodes: List<String>): Transcript {
        for (languageCode in languageCodes) {
            // Try manually created first
            manuallyCreatedTranscripts[languageCode]?.let { return it }
            // Then try generated
            generatedTranscripts[languageCode]?.let { return it }
        }
        
        throw TranscriptException(
            "No transcript found for languages: $languageCodes",
            videoId,
            getAvailableLanguages()
        )
    }
    
    /**
     * Find a manually created transcript for the given language codes.
     * 
     * @param languageCodes List of language codes in order of preference
     * @return The first matching manually created transcript
     * @throws TranscriptException If no manually created transcript is found
     */
    fun findManuallyCreatedTranscript(languageCodes: List<String>): Transcript {
        for (languageCode in languageCodes) {
            manuallyCreatedTranscripts[languageCode]?.let { return it }
        }
        
        throw TranscriptException(
            "No manually created transcript found for languages: $languageCodes",
            videoId,
            getAvailableLanguages()
        )
    }
    
    /**
     * Find a generated transcript for the given language codes.
     * 
     * @param languageCodes List of language codes in order of preference
     * @return The first matching generated transcript
     * @throws TranscriptException If no generated transcript is found
     */
    fun findGeneratedTranscript(languageCodes: List<String>): Transcript {
        for (languageCode in languageCodes) {
            generatedTranscripts[languageCode]?.let { return it }
        }
        
        throw TranscriptException(
            "No generated transcript found for languages: $languageCodes",
            videoId,
            getAvailableLanguages()
        )
    }
    
    /**
     * Get all available language codes.
     */
    fun getAvailableLanguages(): List<String> {
        return (manuallyCreatedTranscripts.keys + generatedTranscripts.keys).distinct().sorted()
    }
    
    /**
     * Get manually created language codes.
     */
    fun getManuallyCreatedLanguages(): List<String> {
        return manuallyCreatedTranscripts.keys.sorted()
    }
    
    /**
     * Get generated language codes.
     */
    fun getGeneratedLanguages(): List<String> {
        return generatedTranscripts.keys.sorted()
    }
    
    /**
     * Check if transcripts are available.
     */
    fun isNotEmpty(): Boolean {
        return manuallyCreatedTranscripts.isNotEmpty() || generatedTranscripts.isNotEmpty()
    }
    
    /**
     * Get the number of available transcripts.
     */
    fun size(): Int {
        return manuallyCreatedTranscripts.size + generatedTranscripts.size
    }
    
    override fun toString(): String {
        val manuallyCreated = manuallyCreatedTranscripts.values.joinToString("\n") { " - $it" }
        val generated = generatedTranscripts.values.joinToString("\n") { " - $it" }
        val translations = translationLanguages.joinToString("\n") { 
            " - ${it.languageCode} (\"${it.language}\")" 
        }
        
        return """
            For this video ($videoId) transcripts are available in the following languages:
            
            (MANUALLY CREATED)
            ${if (manuallyCreated.isNotEmpty()) manuallyCreated else "None"}
            
            (GENERATED)
            ${if (generated.isNotEmpty()) generated else "None"}
            
            (TRANSLATION LANGUAGES)
            ${if (translations.isNotEmpty()) translations else "None"}
        """.trimIndent()
    }
}
