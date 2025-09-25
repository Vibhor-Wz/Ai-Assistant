package com.bigcash.ai.vectordb.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bigcash.ai.vectordb.data.PdfEntity
import com.bigcash.ai.vectordb.repository.PdfRepository
import com.bigcash.ai.vectordb.service.FirebaseAiService
import com.bigcash.ai.vectordb.service.ResponseBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Data class for AI response with search results
 */
data class AiResponseWithResults(
    val response: String,
    val searchResults: List<Pair<PdfEntity, Float>>
)

/**
 * ViewModel for chat functionality.
 * This class handles the UI state and business logic for chat operations.
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {
    
    // Services
    private val pdfRepository = PdfRepository(application)
    private val firebaseAiService = FirebaseAiService(application)
    private val responseBridge = ResponseBridge(application)
    
    // UI State
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _currentQuery = MutableStateFlow("")
    val currentQuery: StateFlow<String> = _currentQuery.asStateFlow()
    
    init {
        // Add a welcome message
        addWelcomeMessage()
    }
    
    /**
     * Add a welcome message to the chat.
     */
    private fun addWelcomeMessage() {
        val welcomeMessage = ChatMessage(
            text = "Hello! I'm your AI assistant. I can help you search through your documents using natural language queries. Try asking me something like 'What documents do I have about machine learning?'",
            isUser = false,
            timestamp = System.currentTimeMillis()
        )
        _messages.value = listOf(welcomeMessage)
    }
    
    /**
     * Send a message in the chat.
     * 
     * @param message The message text to send
     */
    fun sendMessage(message: String) {
        if (message.isBlank()) return
        
        _currentQuery.value = message
        _isLoading.value = true
        _errorMessage.value = null
        
        // Add user message
        val userMessage = ChatMessage(
            text = message,
            isUser = true,
            timestamp = System.currentTimeMillis()
        )
        _messages.value = _messages.value + userMessage
        
        // Generate AI response with bridge analysis
        viewModelScope.launch {
            try {
                val aiResponseWithResults = generateAIResponse(message)
                
                // Use ResponseBridge to analyze and decide what to return
                val bridgeResult = responseBridge.analyzeResponse(
                    aiResponse = aiResponseWithResults.response,
                    searchResults = aiResponseWithResults.searchResults,
                    userQuery = message
                )
                
                // Log the bridge decision for testing
                android.util.Log.d("ChatViewModel", "🌉 Bridge Decision:")
                android.util.Log.d("ChatViewModel", "   Type: ${bridgeResult.responseType}")
                android.util.Log.d("ChatViewModel", "   Confidence: ${bridgeResult.confidence}")
                android.util.Log.d("ChatViewModel", "   File: ${bridgeResult.file?.name ?: "None"}")
                android.util.Log.d("ChatViewModel", "   PDF Entity: ${bridgeResult.pdfEntity?.name ?: "None"}")
                
                val aiMessage = ChatMessage(
                    text = bridgeResult.content,
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
                
                _messages.value = _messages.value + aiMessage
                
            } catch (e: Exception) {
                _errorMessage.value = "Error generating response: ${e.message}"
                
                val errorMessage = ChatMessage(
                    text = "Sorry, I encountered an error while processing your request. Please try again.",
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
                _messages.value = _messages.value + errorMessage
                
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Generate AI response for a given query using vector search and Firebase AI.
     * 
     * @param query The user's query
     * @return AI response with search results
     */
    private suspend fun generateAIResponse(query: String): AiResponseWithResults {
        try {
            // Perform vector search to find relevant documents
            val searchResults = pdfRepository.vectorSearch(query, topK = 3)
            
            val response = if (searchResults.isNotEmpty()) {
                // Extract document content and generate AI response
                val documentData = buildDocumentData(searchResults)
                firebaseAiService.generateResponseFromDocuments(query, documentData)
            } else {
                // No relevant documents found
                "I couldn't find any documents in your collection that are relevant to your query: '$query'. " +
                "You might want to try uploading some documents first, or rephrase your question to be more specific."
            }
            
            return AiResponseWithResults(response, searchResults)
            
        } catch (e: Exception) {
            // Fallback response if vector search fails
            val fallbackResponse = "I encountered an issue while searching through your documents. " +
                   "Please make sure you have uploaded some documents and try again."
            return AiResponseWithResults(fallbackResponse, emptyList())
        }
    }
    
    /**
     * Build document data string from search results for AI processing.
     * 
     * @param searchResults List of PDF entities with similarity scores
     * @return Formatted document data string
     */
    private fun buildDocumentData(searchResults: List<Pair<PdfEntity, Float>>): String {
        return searchResults.joinToString("\n\n") { (pdf, score) ->
            val hasOriginalFile = pdf.localFilePath.isNotEmpty()
            val fileInfo = if (hasOriginalFile) {
                "Original file available: Yes (${pdf.localFilePath.substringAfterLast("/")})"
            } else {
                "Original file available: No"
            }
            
            """
            Document: ${pdf.name}
            Relevance Score: ${(score * 100).toInt()}%
            File Content: ${pdf.data}
            Description: ${pdf.description}
            Upload Date: ${pdf.uploadDate}
            $fileInfo
            """.trimIndent()
        }
    }
    
    /**
     * Get the original file for a PDF entity.
     * 
     * @param pdfEntity The PDF entity
     * @return The original file if it exists, null otherwise
     */
    fun getOriginalFile(pdfEntity: PdfEntity): java.io.File? {
        return pdfRepository.getOriginalFile(pdfEntity)
    }
    
    /**
     * Check if the original file exists for a PDF entity.
     * 
     * @param pdfEntity The PDF entity
     * @return True if the original file exists, false otherwise
     */
    fun hasOriginalFile(pdfEntity: PdfEntity): Boolean {
        return pdfRepository.hasOriginalFile(pdfEntity)
    }
    
    /**
     * Clear all messages and reset the chat.
     */
    fun clearChat() {
        _messages.value = emptyList()
        _currentQuery.value = ""
        _errorMessage.value = null
        addWelcomeMessage()
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Update the current query text.
     * 
     * @param query The new query text
     */
    fun updateQuery(query: String) {
        _currentQuery.value = query
    }
    
    /**
     * Clean up resources when ViewModel is destroyed.
     */
    override fun onCleared() {
        super.onCleared()
        pdfRepository.cleanup()
    }
}

/**
 * Data class representing a chat message.
 */
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long
)
