package com.bigcash.ai.vectordb.service

import android.content.Context
import android.util.Log

/**
 * Service for extracting text from PDF and image documents using Google ML Kit.
 * Supports both PDF and image file formats with comprehensive text extraction.
 */
class PdfTextExtractor(private val context: Context) {
    
    companion object {
        private const val TAG = "VECTOR_DEBUG" // Single tag for filtering all vector-related logs
        private const val MIN_TEXT_LENGTH = 5 // Minimum characters to consider as valid text
    }
    
    private val mlKitExtractor = MlKitTextExtractor(context)
    
    /**
     * Extract text from PDF or image byte array using Google ML Kit.
     *
     * @param fileName The name of the file (used for type detection)
     * @param fileData File data as byte array
     * @return Extracted text using ML Kit
     */
    suspend fun extractTextFromPdf(fileName: String, fileData: ByteArray): String {
        Log.d(TAG, "📖 TextExtractor: Starting ML Kit text extraction")
        Log.d(TAG, "📊 TextExtractor: File: $fileName, Size: ${fileData.size} bytes")
        
        return try {
            val extractedText = mlKitExtractor.extractTextFromFile(fileName, fileData)
            
            if (extractedText.length >= MIN_TEXT_LENGTH) {
                Log.d(TAG, "✅ TextExtractor: ML Kit extraction successful: ${extractedText.length} characters")
                Log.d(TAG, "📝 TextExtractor: Text preview: ${extractedText.take(100)}${if (extractedText.length > 100) "..." else ""}")
                extractedText
            } else {
                Log.d(TAG, "⚠️ TextExtractor: Extracted text too short (${extractedText.length} chars, min: $MIN_TEXT_LENGTH)")
                Log.d(TAG, "📝 TextExtractor: Short text: '$extractedText'")
                extractedText
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ TextExtractor: Error in ML Kit text extraction", e)
            Log.d(TAG, "📊 TextExtractor: Returning empty string due to error")
            ""
        }
    }
    
    /**
     * Clean up resources.
     */
    fun cleanup() {
        Log.d(TAG, "🧹 TextExtractor: Cleaning up ML Kit resources")
        mlKitExtractor.cleanup()
    }
    
    
    /**
     * Clean and normalize extracted text.
     *
     * @param text Raw extracted text
     * @return Cleaned text
     */
    private fun cleanText(text: String): String {
        return text
            .replace(Regex("\\s+"), " ") // Replace multiple whitespaces with single space
            .replace(Regex("[\\r\\n]+"), "\n") // Normalize line breaks
            .trim()
    }
}
