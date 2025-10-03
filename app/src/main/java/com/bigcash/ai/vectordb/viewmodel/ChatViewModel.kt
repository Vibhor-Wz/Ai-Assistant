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
import com.bigcash.ai.vectordb.service.GetImgAiService
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
    private val getImgAiService = GetImgAiService(application)
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
        
        // Look for ANY image in the entire conversation history, not just recent messages
        val hasImageInConversation = _messages.value.any { 
            it.isUser && it.imageFile != null && !it.isImageGenerated
        }
        
        // If no image in conversation, definitely not an image edit request
        if (!hasImageInConversation) {
            Log.d(TAG, "🔍 ChatViewModel: No image in conversation")
            return false
        }
        
        // Much more comprehensive and flexible keyword detection
        val editKeywords = listOf(
            // Direct editing commands
            "edit", "modify", "change", "adjust", "enhance", "improve", "fix", "correct", "update",
            "add", "remove", "replace", "resize", "crop", "rotate", "flip", "transform",
            "make it", "turn it", "convert it", "style it", "redesign", "recreate",
            
            // Background related
            "background", "backdrop", "scene", "setting", "environment",
            
            // Color and style related
            "color", "colour", "style", "theme", "mood", "atmosphere", "tone",
            "bright", "dark", "vibrant", "muted", "saturated", "desaturated",
            
            // Object and element related
            "object", "person", "people", "face", "body", "clothing", "clothes",
            "building", "car", "tree", "sky", "water", "landscape", "portrait",
            
            // Artistic terms
            "artistic", "creative", "artistic style", "painting", "drawing", "sketch",
            "photography", "photo", "image", "picture", "photo", "shot",
            
            // Quality and technical terms
            "quality", "resolution", "sharp", "blur", "focus", "exposure",
            "lighting", "shadow", "highlight", "contrast", "brightness",
            
            // Action words that could relate to image editing
            "create", "generate", "make", "produce", "design", "compose",
            "arrange", "organize", "structure", "layout", "composition"
        )
        
        val variationKeywords = listOf(
            "variations", "variation", "multiple", "different", "alternatives", "options",
            "versions", "styles", "approaches", "interpretations", "renditions"
        )
        
        val isEditRequest = editKeywords.any { keyword -> 
            lowerMessage.contains(keyword) 
        }
        
        val isVariationRequest = variationKeywords.any { keyword -> 
            lowerMessage.contains(keyword) 
        }
        
        // Also check for common image editing phrases
        val commonPhrases = listOf(
            "make it look", "turn it into", "change it to", "make it more",
            "make it less", "add some", "remove the", "replace with",
            "instead of", "rather than", "like a", "as if", "in the style of"
        )
        
        val hasCommonPhrase = commonPhrases.any { phrase ->
            lowerMessage.contains(phrase)
        }
        
        val isImageEdit = isEditRequest || isVariationRequest || hasCommonPhrase
        
        Log.d(TAG, "🔍 ChatViewModel: Checking image edit request")
        Log.d(TAG, "   📝 Message: '$message'")
        Log.d(TAG, "   🖼️ Has image in conversation: $hasImageInConversation")
        Log.d(TAG, "   ✏️ Is edit request: $isEditRequest")
        Log.d(TAG, "   🎨 Is variation request: $isVariationRequest")
        Log.d(TAG, "   💬 Has common phrase: $hasCommonPhrase")
        Log.d(TAG, "   ✅ Final decision: $isImageEdit")
        
        return isImageEdit
    }
    
    /**
     * Handle image editing request.
     * 
     * @param message The user's message
     */
    private fun handleImageEditRequest(message: String) {
        Log.d(TAG, "🖼️ ChatViewModel: Handling image edit request")
        
        // Check if this is a variation request
        val lowerMessage = message.lowercase()
        val isVariationRequest = listOf(
            "variations", "variation", "multiple", "different", "alternatives", "options",
            "create variations", "make variations", "generate variations", "show variations"
        ).any { keyword -> lowerMessage.contains(keyword) }

        // Find the most recent ORIGINAL image from user messages (not AI-generated images)
        // Look through the entire conversation history to find the last user-uploaded image
        val recentImageMessage =
            _messages.value.lastOrNull { it.isUser && it.imageFile != null && !it.isImageGenerated }

        if (recentImageMessage?.imageFile != null) {
            Log.d(TAG, "📷 ChatViewModel: Found recent image: ${recentImageMessage.imageFile.name}")
            Log.d(TAG, "🎨 ChatViewModel: Is variation request: $isVariationRequest")
            
            viewModelScope.launch {
                try {
                    // Convert file to byte array
                    val imageBytes = recentImageMessage.imageFile.readBytes()
                    Log.d(TAG, "📊 ChatViewModel: Image size: ${imageBytes.size} bytes")
                    
                    if (isVariationRequest) {
                        // Handle multiple variations
                        handleMultipleVariations(imageBytes, message)
                    } else {
                        // Handle single edit
                        handleSingleEdit(imageBytes, message)
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ ChatViewModel: Error processing image request", e)
                    _errorMessage.value = "Error processing image: ${e.message}"
                    
                    val errorMessage = ChatMessage(
                        text = "Sorry, I encountered an error while processing your image. Please try again.",
                        isUser = false,
                        timestamp = System.currentTimeMillis(),
                        pdfEntity = null,
                        file = null
                    )
                    _messages.value = _messages.value + errorMessage
                } finally {
                    _isLoading.value = false
                    Log.d(TAG, "🏁 ChatViewModel: Image processing completed")
                }
            }
        } else {
            Log.w(TAG, "⚠️ ChatViewModel: No original image found for editing")
            _isLoading.value = false
            
            val errorMessage = ChatMessage(
                text = "I don't see any image in our conversation to edit. Please upload an image first and then ask me to edit it.",
                isUser = false,
                timestamp = System.currentTimeMillis(),
                pdfEntity = null,
                file = null
            )
            _messages.value = _messages.value + errorMessage
        }
    }
    
    /**
     * Handle single image edit request.
     */
    private suspend fun handleSingleEdit(imageBytes: ByteArray, message: String) {
        Log.d(TAG, "✏️ ChatViewModel: Handling single image edit")
        
        // Extract enhanced edit prompt from message
        val editPrompt = extractEditPrompt(message)
        Log.d(TAG, "✏️ ChatViewModel: Enhanced edit prompt: '$editPrompt'")


        val editedBitmap = firebaseAiService.editImage(imageBytes, editPrompt)


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
    }
    
    /**
     * Handle multiple image variations request.
     */
    private suspend fun handleMultipleVariations(imageBytes: ByteArray, message: String) {
        Log.d(TAG, "🎨 ChatViewModel: Handling multiple image variations")
        
        // Extract enhanced variation prompt from message
        val variationPrompt = extractVariationPrompt(message)
        Log.d(TAG, "🎨 ChatViewModel: Enhanced variation prompt: '$variationPrompt'")
        
        // Generate 3 variations
        val variations = firebaseAiService.generateMultipleImageVariations(
            imageBytes, 
            variationPrompt, 
            2
        )
        
        if (variations.isNotEmpty()) {
            Log.d(TAG, "✅ ChatViewModel: Generated ${variations.size} variations")
            
            // Create messages for each variation
            val variationMessages = variations.mapIndexed { index, bitmap ->
                // Convert bitmap to byte array for storage
                val outputStream = java.io.ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream)
                val variationBytes = outputStream.toByteArray()
                
                // Create a temporary file for the variation
                val variationFile = createTempVariationImageFile(variationBytes, index + 1)
                
                if (variationFile != null) {
                    ChatMessage(
                        text = "Variation ${index + 1}:",
                        isUser = false,
                        timestamp = System.currentTimeMillis(),
                        pdfEntity = null,
                        file = null,
                        imageUri = android.net.Uri.fromFile(variationFile).toString(),
                        imageFile = variationFile,
                        generatedImageData = variationBytes,
                        isImageGenerated = true,
                        imagePrompt = variationPrompt,
                        isImageEdit = true
                    )
                } else {
                    null
                }
            }.filterNotNull()
            
            if (variationMessages.isNotEmpty()) {
                // Add all variation messages to chat
                _messages.value = _messages.value + variationMessages
                Log.d(TAG, "✅ ChatViewModel: Added ${variationMessages.size} variation messages to chat")
            } else {
                throw Exception("Failed to create temporary files for variations")
            }
        } else {
            Log.w(TAG, "⚠️ ChatViewModel: No variations generated")
            val errorMessage = ChatMessage(
                text = "Sorry, I couldn't generate variations of your image. Please try again.",
                isUser = false,
                timestamp = System.currentTimeMillis(),
                pdfEntity = null,
                file = null
            )
            _messages.value = _messages.value + errorMessage
        }
    }
    
    /**
     * Extract variation prompt from user message using powerful AI-friendly prompts.
     */
    private fun extractVariationPrompt(message: String): String {
        val trimmedMessage = message.trim()
        
        // If message is empty, return a default creative variation prompt
        if (trimmedMessage.isEmpty()) {
            return "Create multiple creative and diverse variations of this image, each with unique artistic styles, compositions, and visual elements while maintaining the core subject and appeal."
        }
        
        // Create a powerful, context-aware prompt for variations
        val enhancedPrompt = buildString {
            // Add context about being an expert creative director
            append("As an expert creative director and digital artist, ")
            
            // Add the user's request with proper context
            append("please create multiple diverse and creative variations of this image based on: \"$trimmedMessage\". ")
            
            // Add instructions for high-quality variations
            append("Generate variations that are: ")
            append("1) Each unique and distinct from the others, ")
            append("2) High-quality and professional-looking, ")
            append("3) Creative and innovative in their approach, ")
            append("4) Visually appealing and aesthetically pleasing, ")
            append("5) Coherent with the original image's core elements. ")
            
            // Add technical guidance for variations
            append("Vary elements such as: ")
            append("- Artistic styles and visual treatments, ")
            append("- Color palettes and lighting conditions, ")
            append("- Composition and framing, ")
            append("- Backgrounds and environmental settings, ")
            append("- Mood and atmosphere. ")
            append("Ensure each variation offers a fresh perspective while maintaining the image's essential character and appeal.")
        }
        
        Log.d(TAG, "🎨 ChatViewModel: Enhanced variation prompt created")
        Log.d(TAG, "   📝 Original: '$trimmedMessage'")
        Log.d(TAG, "   🚀 Enhanced: '$enhancedPrompt'")
        
        return enhancedPrompt
    }
    
    /**
     * Create a temporary file for a variation image.
     */
    private fun createTempVariationImageFile(imageBytes: ByteArray, variationNumber: Int): java.io.File? {
        return try {
            val tempFile = java.io.File(getApplication<Application>().cacheDir, "variation_${variationNumber}_${System.currentTimeMillis()}.jpg")
            tempFile.writeBytes(imageBytes)
            Log.d(TAG, "📁 ChatViewModel: Created temp variation file: ${tempFile.absolutePath}")
            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "❌ ChatViewModel: Error creating temp variation file", e)
            null
        }
    }
    
    /**
     * Extract edit prompt from user message using powerful AI-friendly prompts.
     * 
     * @param message The user's message
     * @return Enhanced prompt for AI image editing
     */
    private fun extractEditPrompt(message: String): String {
        val trimmedMessage = message.trim()
        
        // If message is empty, return a default creative prompt
        if (trimmedMessage.isEmpty()) {
            return "Please enhance and improve this image with creative modifications while maintaining its core essence and visual appeal."
        }
        
        // Create a powerful, context-aware prompt that leverages AI capabilities
        val enhancedPrompt = buildString {
            // Add context about being an expert image editor
            append("As an expert image editor and digital artist, ")
            
            // Add the user's request with proper context
            append("please modify this image according to the following request: \"$trimmedMessage\". ")
            
            // Add instructions for high-quality output
            append("Ensure the modifications are: ")
            append("1) High-quality and professional-looking, ")
            append("2) Visually appealing and aesthetically pleasing, ")
            append("3) Coherent with the original image's style and composition, ")
            append("4) Creative and innovative while respecting the user's specific requirements. ")
            
            // Add technical guidance
            append("Pay attention to lighting, shadows, colors, and overall visual harmony. ")
            append("If the request involves adding elements, make them look natural and well-integrated. ")
            append("If removing elements, ensure the result looks clean and professional. ")
            append("Maintain the image's resolution and quality throughout the editing process.")
        }
        
        Log.d(TAG, "🎨 ChatViewModel: Enhanced prompt created")
        Log.d(TAG, "   📝 Original: '$trimmedMessage'")
        Log.d(TAG, "   🚀 Enhanced: '$enhancedPrompt'")
        
        return enhancedPrompt
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
