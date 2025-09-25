package com.bigcash.ai.vectordb.service

import android.content.Context
import android.util.Log
import com.bigcash.ai.vectordb.data.PdfEntity
import com.bigcash.ai.vectordb.repository.PdfRepository
import com.bigcash.ai.vectordb.utils.FileStorageManager
import java.io.File

/**
 * Bridge service that analyzes AI responses and decides whether to return
 * just the text answer or the full file from local storage.
 */
class ResponseBridge(private val context: Context) {
    
    companion object {
        private const val TAG = "ResponseBridge"
    }
    
    private val pdfRepository = PdfRepository(context)
    private val fileStorageManager = FileStorageManager(context)
    
    /**
     * Result of the bridge analysis
     */
    data class BridgeResult(
        val responseType: ResponseType,
        val content: String,
        val file: File? = null,
        val pdfEntity: PdfEntity? = null,
        val confidence: Float = 0.0f
    )
    
    /**
     * Types of responses the bridge can decide
     */
    enum class ResponseType {
        TEXT_ONLY,      // Return only the AI-generated text
        FULL_FILE,      // Return the complete file from local storage
        MIXED          // Return both text and file reference
    }
    
    /**
     * Analyze the AI response and decide what to return to the user.
     *
     * @param aiResponse The AI-generated response
     * @param searchResults The PDF entities that were used to generate the response
     * @param userQuery The original user query
     * @return BridgeResult containing the decision and content
     */
    fun analyzeResponse(
        aiResponse: String,
        searchResults: List<Pair<PdfEntity, Float>>,
        userQuery: String
    ): BridgeResult {
        Log.d(TAG, "🔍 Bridge: Analyzing response for query: '$userQuery'")
        Log.d(TAG, "📝 Bridge: AI Response length: ${aiResponse.length}")
        Log.d(TAG, "📊 Bridge: Search results count: ${searchResults.size}")
        
        // Parse the response type indicator from the AI response
        val responseTypeIndicator = parseResponseTypeIndicator(aiResponse)
        Log.d(TAG, "🎯 Bridge: AI Response Type Indicator: $responseTypeIndicator")
        
        // Make the bridge decision based on AI's response type indicator
        val decision = makeBridgeDecisionFromIndicator(responseTypeIndicator, searchResults, aiResponse)
        Log.d(TAG, "⚖️ Bridge: Decision - Type: ${decision.responseType}, Confidence: ${decision.confidence}")
        
        return decision
    }
    
    /**
     * Parse the response type indicator from the AI response.
     * Looks for [RESPONSE_TYPE: <TYPE>] pattern at the end of the response.
     *
     * @param aiResponse The AI-generated response
     * @return The response type indicator or null if not found
     */
    private fun parseResponseTypeIndicator(aiResponse: String): String? {
        val pattern = Regex("\\[RESPONSE_TYPE:\\s*(\\w+)\\]", RegexOption.IGNORE_CASE)
        val match = pattern.find(aiResponse)
        val responseType = match?.groupValues?.get(1)?.uppercase()
        
        Log.d(TAG, "🔍 Parsing Response Type Indicator:")
        Log.d(TAG, "   Full response: '$aiResponse'")
        Log.d(TAG, "   Pattern match: ${match?.value}")
        Log.d(TAG, "   Extracted type: $responseType")
        
        return responseType
    }
    
    /**
     * Make bridge decision based on AI's response type indicator.
     *
     * @param responseTypeIndicator The response type from AI (TEXT_ONLY, FULL_FILE, MIXED)
     * @param searchResults The search results from vector search
     * @param aiResponse The original AI response
     * @return BridgeResult with the decision
     */
    private fun makeBridgeDecisionFromIndicator(
        responseTypeIndicator: String?,
        searchResults: List<Pair<PdfEntity, Float>>,
        aiResponse: String
    ): BridgeResult {
        
        Log.d(TAG, "⚖️ Making Bridge Decision from AI Indicator:")
        Log.d(TAG, "   AI Response Type: $responseTypeIndicator")
        Log.d(TAG, "   Search results count: ${searchResults.size}")
        
        // Clean the AI response by removing the response type indicator
        val cleanResponse = aiResponse.replace(Regex("\\[RESPONSE_TYPE:\\s*\\w+\\]", RegexOption.IGNORE_CASE), "").trim()
        
        when (responseTypeIndicator) {
            "TEXT_ONLY" -> {
                Log.d(TAG, "🎯 Decision Path: AI indicated TEXT_ONLY -> Return text only")
                return BridgeResult(
                    responseType = ResponseType.TEXT_ONLY,
                    content = cleanResponse,
                    confidence = 0.8f
                )
            }
            
            "FULL_FILE" -> {
                Log.d(TAG, "🎯 Decision Path: AI indicated FULL_FILE -> Return full file")
                val bestMatch = findBestMatchingFile("", searchResults) // Use best match regardless of document type
                if (bestMatch != null) {
                    val file = pdfRepository.getOriginalFile(bestMatch)
                    if (file != null) {
                        Log.d(TAG, "📁 Bridge: Returning full file - ${file.name}")
                        return BridgeResult(
                            responseType = ResponseType.FULL_FILE,
                            content = cleanResponse,
                            file = file,
                            pdfEntity = bestMatch,
                            confidence = 0.9f
                        )
                    } else {
                        Log.w(TAG, "⚠️ Bridge: File not found for ${bestMatch.name}")
                    }
                } else {
                    Log.w(TAG, "⚠️ Bridge: No matching file found")
                }
                
                // Fallback to text if file not available
                return BridgeResult(
                    responseType = ResponseType.TEXT_ONLY,
                    content = cleanResponse + "\n\nNote: The requested file is not available locally.",
                    confidence = 0.5f
                )
            }
            
            "MIXED" -> {
                Log.d(TAG, "🎯 Decision Path: AI indicated MIXED -> Return mixed response")
                val bestMatch = searchResults.firstOrNull()?.first
                val file = bestMatch?.let { pdfRepository.getOriginalFile(it) }
                
                return BridgeResult(
                    responseType = ResponseType.MIXED,
                    content = cleanResponse,
                    file = file,
                    pdfEntity = bestMatch,
                    confidence = 0.7f
                )
            }
            
            else -> {
                Log.d(TAG, "🎯 Decision Path: Unknown or missing indicator -> Return text only")
                // Fallback to text only if AI didn't provide indicator
                return BridgeResult(
                    responseType = ResponseType.TEXT_ONLY,
                    content = cleanResponse,
                    confidence = 0.6f
                )
            }
        }
    }
    
    /**
     * Find the best matching file based on document type and search results
     */
    private fun findBestMatchingFile(documentType: String, searchResults: List<Pair<PdfEntity, Float>>): PdfEntity? {
        if (searchResults.isEmpty()) return null
        
        // Look for exact matches first
        val exactMatch = searchResults.find { (pdf, _) ->
            pdf.name.lowercase().contains(documentType) || 
            pdf.description.lowercase().contains(documentType)
        }
        
        if (exactMatch != null) {
            Log.d(TAG, "🎯 Bridge: Found exact match - ${exactMatch.first.name}")
            return exactMatch.first
        }
        
        // Return the most relevant result
        val bestMatch = searchResults.maxByOrNull { it.second }
        Log.d(TAG, "🎯 Bridge: Using best match - ${bestMatch?.first?.name}")
        return bestMatch?.first
    }
    
}
