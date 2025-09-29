package com.bigcash.ai.vectordb.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
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
    
    companion object {
        private const val TAG = "VECTOR_DEBUG"
    }
    
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
            timestamp = System.currentTimeMillis(),
            pdfEntity = null,
            file = null
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
        
        Log.d(TAG, "💬 ChatViewModel: User sent message: '$message'")
        
        _currentQuery.value = message
        _isLoading.value = true
        _errorMessage.value = null
        
        // Add user message
        val userMessage = ChatMessage(
            text = message,
            isUser = true,
            timestamp = System.currentTimeMillis(),
            pdfEntity = null,
            file = null
        )
        _messages.value = _messages.value + userMessage
        Log.d(TAG, "✅ ChatViewModel: User message added to chat")
        
        // Check if this is an image editing request
        if (isImageEditRequest(message)) {
            Log.d(TAG, "🖼️ ChatViewModel: Detected image editing request")
            handleImageEditRequest(message)
        } else {
            // Generate AI response with bridge analysis
            viewModelScope.launch {
                try {
                    Log.d(TAG, "🤖 ChatViewModel: Starting AI response generation")
                    val aiResponseWithResults = generateAIResponse(message)
                    Log.d(TAG, "📝 ChatViewModel: AI response generated, length: ${aiResponseWithResults.response.length}")

                    // Use ResponseBridge to analyze and decide what to return
                    Log.d(TAG, "🌉 ChatViewModel: Analyzing response with ResponseBridge")
                    val bridgeResult = responseBridge.analyzeResponse(
                        aiResponse = aiResponseWithResults.response,
                        searchResults = aiResponseWithResults.searchResults,
                        userQuery = message
                    )
                    
                    // Log the bridge decision for testing
                    Log.d(TAG, "🌉 ChatViewModel: Bridge Decision:")
                    Log.d(TAG, "   📊 Type: ${bridgeResult.responseType}")
                    Log.d(TAG, "   🎯 Confidence: ${bridgeResult.confidence}")
                    Log.d(TAG, "   📁 File: ${bridgeResult.file?.name ?: "None"}")
                    Log.d(TAG, "   📄 PDF Entity: ${bridgeResult.pdfEntity?.name ?: "None"}")
                    Log.d(TAG, "   📝 Content length: ${bridgeResult.content.length}")

                    val aiMessage = ChatMessage(
                        text = bridgeResult.content,
                        isUser = false,
                        timestamp = System.currentTimeMillis(),
                        pdfEntity = bridgeResult.pdfEntity,
                        file = bridgeResult.file
                    )
                    
                    _messages.value = _messages.value + aiMessage
                    Log.d(TAG, "✅ ChatViewModel: AI message added to chat with file info: ${aiMessage.pdfEntity != null || aiMessage.file != null}")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ ChatViewModel: Error generating AI response", e)
                    _errorMessage.value = "Error generating response: ${e.message}"
                    
                    val errorMessage = ChatMessage(
                        text = "Sorry, I encountered an error while processing your request. Please try again.",
                        isUser = false,
                        timestamp = System.currentTimeMillis(),
                        pdfEntity = null,
                        file = null
                    )
                    _messages.value = _messages.value + errorMessage
                    Log.d(TAG, "⚠️ ChatViewModel: Error message added to chat")
                    
                } finally {
                    _isLoading.value = false
                    Log.d(TAG, "🏁 ChatViewModel: AI response generation completed")
                }
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
        Log.d(TAG, "🔍 ChatViewModel: Starting AI response generation for query: '$query'")
        
        try {
            // Perform vector search to find relevant documents
            Log.d(TAG, "🔍 ChatViewModel: Performing vector search with topK=3")
            val searchResults = pdfRepository.vectorSearch(query, topK = 3)
            Log.d(TAG, "📊 ChatViewModel: Vector search found ${searchResults.size} results")
            
            val response = if (searchResults.isNotEmpty()) {
                Log.d(TAG, "📄 ChatViewModel: Building document data from search results")
                // Extract document content and generate AI response
                val documentData = buildDocumentData(searchResults)
                Log.d(TAG, "📝 ChatViewModel: Document data length: ${documentData.length}")
                
                Log.d(TAG, "🤖 ChatViewModel: Generating AI response from documents")
                firebaseAiService.generateResponseFromDocuments(query, documentData)
            } else {
                Log.w(TAG, "⚠️ ChatViewModel: No relevant documents found for query")
                // No relevant documents found
                "I couldn't find any documents in your collection that are relevant to your query: '$query'. " +
                "You might want to try uploading some documents first, or rephrase your question to be more specific."
            }
            
            Log.d(TAG, "✅ ChatViewModel: AI response generated successfully, length: ${response.length}")
            return AiResponseWithResults(response, searchResults)

        } catch (e: Exception) {
            Log.e(TAG, "❌ ChatViewModel: Error in generateAIResponse", e)
            // Fallback response if vector search fails
            val fallbackResponse = "I encountered an issue while searching through your documents. " +
                   "Please make sure you have uploaded some documents and try again."
            Log.d(TAG, "🔄 ChatViewModel: Using fallback response")
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
        Log.d(TAG, "📁 ChatViewModel: Getting original file for PDF: ${pdfEntity.name}")
        Log.d(TAG, "📁 ChatViewModel: Local file path: ${pdfEntity.localFilePath}")
        
        val file = pdfRepository.getOriginalFile(pdfEntity)
        if (file != null) {
            Log.d(TAG, "✅ ChatViewModel: Original file found: ${file.name} (${file.length()} bytes)")
        } else {
            Log.w(TAG, "⚠️ ChatViewModel: Original file not found for PDF: ${pdfEntity.name}")
        }
        
        return file
    }
    
    /**
     * Check if the original file exists for a PDF entity.
     * 
     * @param pdfEntity The PDF entity
     * @return True if the original file exists, false otherwise
     */
    fun hasOriginalFile(pdfEntity: PdfEntity): Boolean {
        Log.d(TAG, "🔍 ChatViewModel: Checking if original file exists for PDF: ${pdfEntity.name}")
        val exists = pdfRepository.hasOriginalFile(pdfEntity)
        Log.d(TAG, "🔍 ChatViewModel: File exists: $exists")
        return exists
    }
    
    /**
     * Clear all messages and reset the chat.
     */
    fun clearChat() {
        Log.d(TAG, "🗑️ ChatViewModel: Clearing chat and resetting")
        _messages.value = emptyList()
        _currentQuery.value = ""
        _errorMessage.value = null
        addWelcomeMessage()
        Log.d(TAG, "✅ ChatViewModel: Chat cleared and welcome message added")
    }
    
    /**
     * Clear error message.
     */
    fun clearError() {
        Log.d(TAG, "🧹 ChatViewModel: Clearing error message")
        _errorMessage.value = null
    }
    
    /**
     * Update the current query text.
     * 
     * @param query The new query text
     */
    fun updateQuery(query: String) {
        Log.d(TAG, "📝 ChatViewModel: Updating query to: '$query'")
        _currentQuery.value = query
    }
    
    /**
     * Send an image message in the chat.
     * 
     * @param imageUri The URI of the selected image
     * @param imageFile The file representing the image
     */
    fun sendImageMessage(imageUri: String, imageFile: java.io.File) {
        Log.d(TAG, "🖼️ ChatViewModel: User sent image: $imageUri")
        
        // Add user image message
        val userMessage = ChatMessage(
            text = "📷 Image",
            isUser = true,
            timestamp = System.currentTimeMillis(),
            pdfEntity = null,
            file = null,
            imageUri = imageUri,
            imageFile = imageFile
        )
        _messages.value = _messages.value + userMessage
        

        Log.d(TAG, "✅ ChatViewModel: Image message added to chat")
    }
    
    /**
     * Check if the message is an image editing request.
     * 
     * @param message The user's message
     * @return True if it's an image editing request
     */
    private fun isImageEditRequest(message: String): Boolean {
        val lowerMessage = message.lowercase()
        val editKeywords = listOf(
            "change background", "change the background", "edit background", "edit the background",
            "modify background", "modify the background", "replace background", "replace the background",
            "edit image", "edit the image", "modify image", "modify the image",
            "change image", "change the image", "transform image", "transform the image"
        )
        
        val hasImageInRecentMessages = _messages.value.takeLast(5).any { 
            it.isUser && it.imageFile != null 
        }
        
        val isEditRequest = editKeywords.any { keyword -> 
            lowerMessage.contains(keyword) 
        }
        
        Log.d(TAG, "🔍 ChatViewModel: Checking image edit request")
        Log.d(TAG, "   📝 Message: '$message'")
        Log.d(TAG, "   🖼️ Has recent image: $hasImageInRecentMessages")
        Log.d(TAG, "   ✏️ Is edit request: $isEditRequest")
        
        return hasImageInRecentMessages && isEditRequest
    }
    
    /**
     * Handle image editing request.
     * 
     * @param message The user's message
     */
    private fun handleImageEditRequest(message: String) {
        Log.d(TAG, "🖼️ ChatViewModel: Handling image edit request")
        
        // Find the most recent image from user messages
        val recentImageMessage = _messages.value.takeLast(5).lastOrNull { 
            it.isUser && it.imageFile != null 
        }
        
        if (recentImageMessage?.imageFile != null) {
            Log.d(TAG, "📷 ChatViewModel: Found recent image: ${recentImageMessage.imageFile.name}")
            
            viewModelScope.launch {
                try {
                    // Convert file to byte array
                    val imageBytes = recentImageMessage.imageFile.readBytes()
                    Log.d(TAG, "📊 ChatViewModel: Image size: ${imageBytes.size} bytes")
                    
                    // Extract edit prompt from message or use default
                    val editPrompt = extractEditPrompt(message) ?: "Change the background of this image"
                    Log.d(TAG, "✏️ ChatViewModel: Edit prompt: '$editPrompt'")
                    
                    // Call the editImage function (try both approaches for better reliability)
                    var editedBitmap = firebaseAiService.editImage(imageBytes, editPrompt)
                    
                    // If the first approach fails, try the chat-based approach
                    if (editedBitmap == null) {
                        Log.w(TAG, "🔄 ChatViewModel: First edit attempt failed, trying chat-based approach")
                        editedBitmap = firebaseAiService.editImageWithChat(imageBytes, editPrompt)
                    }

                    Log.d(TAG, "📊 ChatViewModel: Edited image size: ${editedBitmap?.byteCount ?: 0} bytes")
                    if (editedBitmap != null) {
                        Log.d(TAG, "✅ ChatViewModel: Image edited successfully")
                        
                        // Convert bitmap to byte array for storage
                        val outputStream = java.io.ByteArrayOutputStream()
                        editedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream)
                        val editedImageBytes = outputStream.toByteArray()
                        
                        // Create a temporary file for the edited image
                        val editedImageFile = createTempEditedImageFile(editedImageBytes)
                        
                        if (editedImageFile != null) {
                            val aiMessage = ChatMessage(
                                text = "I've edited your image as requested! Here's the result:",
                                isUser = false,
                                timestamp = System.currentTimeMillis(),
                                pdfEntity = null,
                                file = null,
                                imageUri = android.net.Uri.fromFile(editedImageFile).toString(),
                                imageFile = editedImageFile,
                                generatedImageData = editedImageBytes,
                                isImageGenerated = true,
                                imagePrompt = editPrompt,
                                isImageEdit = true
                            )
                            
                            _messages.value = _messages.value + aiMessage
                            Log.d(TAG, "✅ ChatViewModel: Edited image message added to chat")
                        } else {
                            throw Exception("Failed to create temporary file for edited image")
                        }
                    } else {
                        Log.w(TAG, "⚠️ ChatViewModel: Image editing failed - no result")
                        val errorMessage = ChatMessage(
                            text = "Sorry, I couldn't edit your image. Please try again or make sure you have a recent image in the chat.",
                            isUser = false,
                            timestamp = System.currentTimeMillis(),
                            pdfEntity = null,
                            file = null
                        )
                        _messages.value = _messages.value + errorMessage
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ ChatViewModel: Error editing image", e)
                    _errorMessage.value = "Error editing image: ${e.message}"
                    
                    val errorMessage = ChatMessage(
                        text = "Sorry, I encountered an error while editing your image. Please try again.",
                        isUser = false,
                        timestamp = System.currentTimeMillis(),
                        pdfEntity = null,
                        file = null
                    )
                    _messages.value = _messages.value + errorMessage
                } finally {
                    _isLoading.value = false
                    Log.d(TAG, "🏁 ChatViewModel: Image editing completed")
                }
            }
        } else {
            Log.w(TAG, "⚠️ ChatViewModel: No recent image found for editing")
            _isLoading.value = false
            
            val errorMessage = ChatMessage(
                text = "I don't see any recent image in our conversation to edit. Please upload an image first and then ask me to edit it.",
                isUser = false,
                timestamp = System.currentTimeMillis(),
                pdfEntity = null,
                file = null
            )
            _messages.value = _messages.value + errorMessage
        }
    }
    
    /**
     * Extract edit prompt from user message.
     * 
     * @param message The user's message
     * @return Extracted prompt or null if not found
     */
    private fun extractEditPrompt(message: String): String? {
        val lowerMessage = message.lowercase()
        
        // Look for specific background change requests
        val backgroundPatterns = listOf(
            "change background to (.+)".toRegex(),
            "change the background to (.+)".toRegex(),
            "make background (.+)".toRegex(),
            "make the background (.+)".toRegex(),
            "set background to (.+)".toRegex(),
            "set the background to (.+)".toRegex()
        )
        
        for (pattern in backgroundPatterns) {
            val match = pattern.find(lowerMessage)
            if (match != null) {
                val prompt = match.groupValues[1].trim()
                if (prompt.isNotEmpty()) {
                    return "Change the background to $prompt"
                }
            }
        }
        
        // If no specific prompt found, return null to use default
        return null
    }
    
    /**
     * Create a temporary file for the edited image.
     * 
     * @param imageBytes The edited image as byte array
     * @return Temporary file or null if creation failed
     */
    private fun createTempEditedImageFile(imageBytes: ByteArray): java.io.File? {
        return try {
            val tempFile = java.io.File(getApplication<Application>().cacheDir, "edited_image_${System.currentTimeMillis()}.jpg")
            tempFile.writeBytes(imageBytes)
            Log.d(TAG, "📁 ChatViewModel: Created temp edited image file: ${tempFile.absolutePath}")
            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "❌ ChatViewModel: Error creating temp edited image file", e)
            null
        }
    }

    /**
     * Clean up resources when ViewModel is destroyed.
     */
    override fun onCleared() {
        Log.d(TAG, "🧹 ChatViewModel: Cleaning up resources")
        super.onCleared()
        pdfRepository.cleanup()
        Log.d(TAG, "✅ ChatViewModel: Cleanup completed")
    }
}

/**
 * Data class representing a chat message.
 */
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long,
    val pdfEntity: PdfEntity? = null,
    val file: java.io.File? = null,
    val imageUri: String? = null,
    val imageFile: java.io.File? = null,
    val generatedImageData: ByteArray? = null,
    val isImageGenerated: Boolean = false,
    val imagePrompt: String? = null,
    val isImageEdit: Boolean = false
)
