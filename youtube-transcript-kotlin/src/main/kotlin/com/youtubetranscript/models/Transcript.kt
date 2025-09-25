package com.youtubetranscript.models

import com.youtubetranscript.TranscriptException
import com.youtubetranscript.core.TranscriptParser
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Represents a transcript for a specific language.
 * 
 * This class mirrors the Transcript class from the Python library.
 * 
 * @property videoId The YouTube video ID
 * @property language The language name (e.g., "English")
 * @property languageCode The language code (e.g., "en")
 * @property isGenerated Whether this is an auto-generated transcript
 * @property isTranslatable Whether this transcript can be translated
 * @property translationLanguages List of available translation languages
 */
class Transcript(
    private val httpClient: OkHttpClient,
    val videoId: String,
    private val url: String,
    val language: String,
    val languageCode: String,
    val isGenerated: Boolean,
    val isTranslatable: Boolean = false,
    val translationLanguages: List<TranslationLanguage> = emptyList()
) {
    
    /**
     * Fetch the actual transcript data.
     * 
     * @param preserveFormatting Whether to keep HTML text formatting
     * @return List of transcript segments
     * @throws TranscriptException If transcript cannot be fetched
     */
    suspend fun fetch(preserveFormatting: Boolean = false): List<TranscriptSegment> = withContext(Dispatchers.IO) {
        if (url.contains("&exp=xpe")) {
            throw TranscriptException("Transcript requires authentication token", videoId)
        }
        
        val response = httpClient.newCall(
            Request.Builder()
                .url(url)
                .build()
        ).execute()
        
        if (!response.isSuccessful) {
            throw TranscriptException("HTTP error: ${response.code}", videoId)
        }
        
        val xmlContent = response.body?.string() ?: throw TranscriptException("Empty response", videoId)
        val parser = TranscriptParser(preserveFormatting)
        parser.parse(xmlContent)
    }
    
    /**
     * Translate this transcript to another language.
     * 
     * @param languageCode Target language code
     * @return New Transcript object for the translated version
     * @throws TranscriptException If translation is not available
     */
    fun translate(languageCode: String): Transcript {
        if (!isTranslatable) {
            throw TranscriptException("Transcript is not translatable", videoId)
        }
        
        val targetLanguage = translationLanguages.find { it.languageCode == languageCode }
            ?: throw TranscriptException("Translation language not available: $languageCode", videoId)
        
        val translatedUrl = "$url&tlang=$languageCode"
        
        return Transcript(
            httpClient = httpClient,
            videoId = videoId,
            url = translatedUrl,
            language = targetLanguage.language,
            languageCode = languageCode,
            isGenerated = true, // Translations are always generated
            isTranslatable = false, // Translations cannot be further translated
            translationLanguages = emptyList()
        )
    }
    
    override fun toString(): String {
        val translationInfo = if (isTranslatable) " [TRANSLATABLE]" else ""
        return "$languageCode (\"$language\")$translationInfo"
    }
}

/**
 * Represents a translation language option.
 */
data class TranslationLanguage(
    val language: String,
    val languageCode: String
)
