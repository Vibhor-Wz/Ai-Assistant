package com.bigcash.ai.vectordb.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bigcash.ai.vectordb.data.PdfEntity
import com.bigcash.ai.vectordb.repository.PdfRepository
import com.bigcash.ai.vectordb.service.FirebaseAiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for chat functionality.
 * This class handles the UI state and business logic for chat operations.
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {
    
    // Services
    private val pdfRepository = PdfRepository(application)
    private val firebaseAiService = FirebaseAiService(application)
    
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
        
        // Simulate AI response (for now)
        viewModelScope.launch {
            try {

                val aiResponse = generateAIResponse(message)
                
                val aiMessage = ChatMessage(
                    text = aiResponse,
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
     * @return AI response text based on document search results
     */
    private suspend fun generateAIResponse(query: String): String {
        try {
            // Perform vector search to find relevant documents
            val searchResults = pdfRepository.vectorSearch(query, topK = 3)
            
            return if (searchResults.isNotEmpty()) {
                // Extract document content and generate AI response
                val documentData = buildDocumentData(searchResults)
                firebaseAiService.generateResponseFromDocuments(query, documentData)
            } else {
                // No relevant documents found
                "I couldn't find any documents in your collection that are relevant to your query: '$query'. " +
                "You might want to try uploading some documents first, or rephrase your question to be more specific."
            }
            
        } catch (e: Exception) {
            // Fallback response if vector search fails
            return "I encountered an issue while searching through your documents. " +
                   "Please make sure you have uploaded some documents and try again."
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
            """
            Document: ${pdf.name}
            Relevance Score: ${(score * 100).toInt()}%
            File Content: ${pdf.data}
            Description: ${pdf.description}
            Upload Date: ${pdf.uploadDate}
            """.trimIndent()
        }
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
