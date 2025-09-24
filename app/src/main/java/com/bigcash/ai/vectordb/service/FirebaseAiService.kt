package com.bigcash.ai.vectordb.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Firebase AI Generative Model service for generating detailed content from files.
 * Uses Google's Gemini model to analyze file content and generate comprehensive descriptions.
 */
class FirebaseAiService(private val context: Context) {

    companion object {
        private const val TAG = "MLKitOutput" // Single tag for AI output logging
        private const val VECTOR_DEBUG_TAG = "VECTOR_DEBUG" // For internal debugging

    }

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val generativeModel: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-2.5-flash" // update from 1.5
        )
    }

    init {
        Log.d(VECTOR_DEBUG_TAG, "🤖 FirebaseAiService: Initializing Firebase AI service")
        // Initialize anonymous authentication
        initializeAuth()
    }

    /**
     * Initialize Firebase Authentication with anonymous sign-in.
     */
    private fun initializeAuth() {
        Log.d(VECTOR_DEBUG_TAG, "🔐 FirebaseAiService: Initializing Firebase Authentication")

        // Check if user is already signed in
        if (auth.currentUser != null) {
            Log.d(
                VECTOR_DEBUG_TAG,
                "✅ FirebaseAiService: User already authenticated: ${auth.currentUser?.uid}"
            )
            return
        }

        // Sign in anonymously
        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(
                        VECTOR_DEBUG_TAG,
                        "✅ FirebaseAiService: Anonymous authentication successful"
                    )
                    Log.d(
                        VECTOR_DEBUG_TAG,
                        "👤 FirebaseAiService: User ID: ${auth.currentUser?.uid}"
                    )
                } else {
                    Log.e(
                        VECTOR_DEBUG_TAG,
                        "❌ FirebaseAiService: Anonymous authentication failed",
                        task.exception
                    )
                }
            }
    }

    /**
     * Ensure user is authenticated before making AI requests.
     */
    private suspend fun ensureAuthenticated(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (auth.currentUser == null) {
                Log.d(
                    VECTOR_DEBUG_TAG,
                    "🔐 FirebaseAiService: No authenticated user, signing in anonymously"
                )
                auth.signInAnonymously().await()
                Log.d(VECTOR_DEBUG_TAG, "✅ FirebaseAiService: Anonymous authentication completed")
            }
            true
        } catch (e: Exception) {
            Log.e(VECTOR_DEBUG_TAG, "❌ FirebaseAiService: Authentication failed", e)
            false
        }
    }

    /**
     * Generate detailed content description from file data using Firebase AI.
     *
     * @param fileName Name of the file
     * @param fileData File data as byte array
     * @param fileType Type of file (PDF, IMAGE, etc.)
     * @return Generated detailed content description
     */
    suspend fun generateContentFromFile(
        fileName: String,
        fileData: ByteArray,
        fileType: FileType
    ): String = withContext(Dispatchers.IO) {
        Log.d(VECTOR_DEBUG_TAG, "🤖 FirebaseAiService: Starting AI content generation")
        Log.d(
            VECTOR_DEBUG_TAG,
            "📄 FirebaseAiService: File: $fileName, Type: $fileType, Size: ${fileData.size} bytes"
        )

        try {
            // Ensure user is authenticated
            if (!ensureAuthenticated()) {
                Log.w(
                    VECTOR_DEBUG_TAG,
                    "⚠️ FirebaseAiService: Authentication failed, using fallback"
                )
                val fallbackContent = generateFallbackContent(fileName, fileData, fileType)
                Log.d(TAG, fallbackContent)
                return@withContext fallbackContent
            }

            val prompt = createPrompt(fileName, fileData, fileType)
            Log.d(VECTOR_DEBUG_TAG, "📝 FirebaseAiService: Generated prompt for AI")

            val response = generativeModel.generateContent(
                content {
                    val mimeType = getMimeTypeForFile(fileName)
                    inlineData(fileData, mimeType)
                    text(prompt)
                }
            )
            val generatedContent = response.text ?: ""

            Log.d(VECTOR_DEBUG_TAG, "✅ FirebaseAiService: AI content generation completed")
            Log.d(
                VECTOR_DEBUG_TAG,
                "📊 FirebaseAiService: Generated content length: ${generatedContent.length}"
            )

            // Log the generated content using the required tag
            Log.d(TAG, generatedContent)

            return@withContext generatedContent
        } catch (e: Exception) {
            Log.e(
                VECTOR_DEBUG_TAG,
                "❌ FirebaseAiService: Error generating content with Firebase AI",
                e
            )

            // Fallback to basic content generation if AI fails
            val fallbackContent = generateFallbackContent(fileName, fileData, fileType)
            Log.d(VECTOR_DEBUG_TAG, "🔄 FirebaseAiService: Using fallback content generation")
            Log.d(TAG, fallbackContent)

            return@withContext fallbackContent
        }
    }

    private fun getMimeTypeForFile(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            // Images
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "webp" -> "image/webp"

            // Documents
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "txt" -> "text/plain"
            "rtf" -> "application/rtf"
            "odt" -> "application/vnd.oasis.opendocument.text"

            // Spreadsheets
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ods" -> "application/vnd.oasis.opendocument.spreadsheet"

            // Presentations
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "odp" -> "application/vnd.oasis.opendocument.presentation"

            // Archives
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            "7z" -> "application/x-7z-compressed"

            // Audio
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"

            // Video
            "mp4" -> "video/mp4"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"

            else -> "application/octet-stream"
        }
    }

    private fun ByteArray.toBitmap(): Bitmap {
        return BitmapFactory.decodeByteArray(this, 0, this.size)
    }

    /**
     * Create a comprehensive prompt for the AI model based on file type and content.
     */
    private fun createPrompt(fileName: String, fileData: ByteArray, fileType: FileType): String {
        return when (fileType) {
            FileType.PDF -> createPdfPrompt(fileName, fileData)
            FileType.IMAGE -> createImagePrompt(fileName, fileData)
            FileType.DOCUMENT -> createDocumentPrompt(fileName, fileData)
            FileType.UNSUPPORTED -> createGenericPrompt(fileName, fileData)
        }
    }

    /**
     * Create prompt for PDF files.
     */
    private fun createPdfPrompt(fileName: String, fileData: ByteArray): String {
        val fileSize = fileData.size

        return """
        You are an AI assistant specialized in extracting text from PDF documents using OCR.

        File Information:
        - Name: $fileName
        - Size: ${fileSize} bytes
        - Type: PDF

        Task:
        1. Extract all textual content from the PDF exactly as it appears in reading order.
        2. Preserve the original sequence and flow of content - do not reorganize or group content.
        3. Extract text line by line, maintaining the natural reading progression.
        4. Include all text from headers, footers, sidebars, and main content.
        5. For tables, extract text row by row maintaining the original structure.
        6. For lists, preserve the original order and formatting.
        7. Provide structured JSON output in the following format:
        {
            "metadata": {
                "file_name": "$fileName",
                "file_size": ${fileSize},
                "file_type": "PDF"
            },
            "text_lines": ["list of all text lines in order as they appear in the document"],
            "tables": ["list of tables in JSON-friendly format if any"],
            "other_content": ["any additional detected content"]
        }
        8. Output only valid JSON, without extra explanation or commentary.
        9. Maintain the exact order of content as it appears in the PDF - do not group similar content together.
        ⚠️ IMPORTANT:
        Return only valid JSON.
        Do not include Markdown.
        Do not include triple backticks.
        Do not include commentary.
        """.trimIndent()
    }


    /**
     * Create prompt for image files.
     */
    private fun createImagePrompt(fileName: String, fileData: ByteArray): String {
        val fileSize = fileData.size

        return """
        You are an AI assistant specialized in extracting text from images using OCR.

        File Information:
        - Name: $fileName
        - Size: ${fileSize} bytes
        - Type: Image

        Task:
        1. Perform OCR and extract all textual content exactly as it appears in the image.
        2. Preserve structure (lines, paragraphs, labels, tables if present).
        3. Provide structured JSON output in the following format:
        {
            "metadata": {
                "file_name": "$fileName",
                "file_size": ${fileSize},
                "file_type": "Image"
            },
            "text_lines": ["list of all text lines in order"],
            "tables": ["list of tables in JSON-friendly format if any"],
            "other_content": ["any additional detected content"]
         }
        ⚠️ IMPORTANT:
        Return only valid JSON.
        Do not include Markdown.
        Do not include triple backticks.
        Do not include commentary.
        """.trimIndent()
    }

    /**
     * Create prompt for document files.
     */
    private fun createDocumentPrompt(fileName: String, fileData: ByteArray): String {
        val fileSize = fileData.size
        val mimeType = getMimeTypeForFile(fileName)

        return """
        You are an AI assistant specialized in extracting text from document files.

        File Information:
        - Name: $fileName
        - Size: ${fileSize} bytes
        - Type: Document File
        - MIME Type: $mimeType

        Task:
        1. Extract all textual content from the document exactly as it appears in reading order.
        2. Preserve the original sequence and flow of content - do not reorganize or group content.
        3. Extract text line by line, maintaining the natural reading progression.
        4. Include all text from headers, footers, and main content.
        5. For tables, extract text row by row maintaining the original structure.
        6. For lists, preserve the original order and formatting.
        7. Provide structured JSON output in the following format:
            {
                "metadata": {
                    "file_name": "$fileName",
                    "file_size": ${fileSize},
                    "file_type": "Document"
                },
                "text_lines": ["list of all text lines in order as they appear in the document"],
                "tables": ["list of tables in JSON-friendly format if any"],
                "other_content": ["any additional detected content"]
            }
        8. Output only valid JSON, without any extra text.
        9. Maintain the exact order of content as it appears in the document - do not group similar content together.
        ⚠️ IMPORTANT:
        Return only valid JSON.
        Do not include Markdown.
        Do not include triple backticks.
        Do not include commentary.
        """.trimIndent()
    }


    /**
     * Create prompt for unsupported file types.
     */
    private fun createGenericPrompt(fileName: String, fileData: ByteArray): String {
        val fileSize = fileData.size

        return """
        You are an AI assistant specialized in extracting text from any type of file.

        File Information:
        - Name: $fileName
        - Size: ${fileSize} bytes
        - Type: File (format unknown)

        Task:
        1. Determine the file type and extract all textual content exactly as it appears.
        2. Preserve the original sequence and flow of content - do not reorganize or group content.
        3. Extract text line by line, maintaining the natural reading progression.
        4. Include all text from the file in the order it appears.
        5. For tables, extract text row by row maintaining the original structure.
        6. For lists, preserve the original order and formatting.
        7. Provide structured JSON output in the following format:
            {
                "metadata": {
                    "file_name": "$fileName",
                    "file_size": ${fileSize},
                    "file_type": "<detected type>"
                },
                "text_lines": ["list of all text lines in order as they appear in the file"],
                "tables": ["list of tables in JSON-friendly format if any"],
                "other_content": ["any additional detected content"]
            }
        8. Output only valid JSON, without any extra text.
        9. Maintain the exact order of content as it appears in the file - do not group similar content together.
        ⚠️ IMPORTANT:
        Return only valid JSON.
        Do not include Markdown.
        Do not include triple backticks.
        Do not include commentary.
    """.trimIndent()
    }


    /**
     * Generate fallback content when AI service is unavailable.
     */
    private fun generateFallbackContent(
        fileName: String,
        fileData: ByteArray,
        fileType: FileType
    ): String {
        val fileSize = fileData.size
        val fileHash = fileData.contentHashCode()

        return """
            FILE ANALYSIS REPORT
            ===================
            
            File Details:
            - Name: $fileName
            - Type: ${fileType.name}
            - Size: ${fileSize} bytes
            - Hash: ${fileHash.toString(16)}
            
            Content Analysis:
            This file appears to be a ${fileType.name.lowercase()} document with a size of ${fileSize} bytes. 
            Based on its characteristics, this file likely contains structured information and data.
            
            Document Structure:
            The file size suggests it contains substantial content. For a ${fileType.name} file of this size, 
            we can expect it to include detailed information, possibly with multiple sections or pages.
            
            Key Characteristics:
            - File size indicates moderate to substantial content
            - ${fileType.name} format suggests professional or structured data
            - Suitable for document processing and analysis
            
            Technical Specifications:
            - Format: ${fileType.name}
            - Size Category: ${getSizeCategory(fileSize)}
            - Processing Complexity: ${getComplexityLevel(fileSize)}
            
            Usage Recommendations:
            This file is suitable for:
            - Document analysis and processing
            - Content extraction and indexing
            - Vector database storage
            - Similarity search operations
            
            Note: This analysis was generated using fallback methods due to AI service unavailability.
            For more detailed analysis, ensure Firebase AI service is properly configured.
        """.trimIndent()
    }

    /**
     * Get size category based on file size.
     */
    private fun getSizeCategory(size: Int): String {
        return when {
            size < 10_000 -> "Small"
            size < 100_000 -> "Medium"
            size < 1_000_000 -> "Large"
            else -> "Very Large"
        }
    }

    /**
     * Get complexity level based on file size.
     */
    private fun getComplexityLevel(size: Int): String {
        return when {
            size < 50_000 -> "Simple"
            size < 200_000 -> "Moderate"
            size < 500_000 -> "Complex"
            else -> "Highly Complex"
        }
    }

    /**
     * Generate AI response based on user query and retrieved document data.
     * This function takes a user's natural language query and relevant document content
     * to generate a contextual response using Firebase AI.
     *
     * @param userQuery The user's natural language question/query
     * @param documentData The relevant document content retrieved from vector search
     * @return AI-generated response based on the documents
     */
    suspend fun generateResponseFromDocuments(
        userQuery: String,
        documentData: String
    ): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "🤖 FirebaseAiService: Generating response from documents")
        Log.d(TAG, "📝 FirebaseAiService: User query: '$userQuery'")
        Log.d(TAG, "📄 FirebaseAiService: Document data length: ${documentData.length}")

        try {
            val prompt = buildPrompt(userQuery, documentData)
            Log.d(TAG, "📋 FirebaseAiService: Generated prompt length: ${prompt.length}")

            val response = generativeModel.generateContent(prompt)
            val responseText = response.text ?: "I couldn't generate a response at this time."

            Log.d(TAG, "✅ FirebaseAiService: Response generated successfully")
            Log.d(TAG, "📊 FirebaseAiService: Response length: ${responseText.length}")

            return@withContext responseText

        } catch (e: Exception) {
            Log.e(TAG, "❌ FirebaseAiService: Error generating response from documents", e)
            return@withContext "I encountered an error while processing your request. Please try again."
        }
    }

    /**
     * Build a comprehensive prompt for the AI model based on user query and document data.
     *
     * @param userQuery The user's question
     * @param documentData The relevant document content
     * @return Formatted prompt for the AI model
     */
    private fun buildPrompt(userQuery: String, documentData: String): String {
        return """
        You are an AI assistant that helps the user search and understand their personal document collection. 
        You only know what is inside the retrieved documents listed below. 
        Do not invent or assume facts beyond what the documents contain. 
        
        USER QUERY:
        "$userQuery"
        
        DOCUMENTS RETRIEVED (may be empty or unrelated):
        $documentData
        
        YOUR TASK:
        1. If the retrieved documents clearly answer the query, summarize and provide the answer in plain language.
        2. If the documents only partially match, provide what is available and explain that some details are missing.
        3. If the documents are unrelated or don’t contain any useful information, DO NOT mention document names or unrelated topics. 
           Simply give a natural, generic response such as:
           - "I couldn’t find your address in the documents provided."
           - "No document contains that type of information."
        4. Keep the tone conversational, clear, and user-friendly.
        
        Important: 
        - Never include file names or irrelevant document details in your answer when they don’t actually help answer the query.
        - Always prioritize clarity and avoid confusing the user with unrelated content.
    """.trimIndent()
    }

    /**
     * Generate a quick summary of search results for display in chat.
     * This provides a shorter response when the user just wants to know what documents were found.
     *
     * @param userQuery The user's query
     * @param searchResults List of search results with similarity scores
     * @return Brief summary of found documents
     */
    suspend fun generateSearchSummary(
        userQuery: String,
        searchResults: List<Pair<String, Float>>
    ): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔍 FirebaseAiService: Generating search summary")
        Log.d(TAG, "📊 FirebaseAiService: Found ${searchResults.size} results")

        try {
            val documentsList = searchResults.joinToString("\n") { (name, score) ->
                "• $name (relevance: ${(score * 100).toInt()}%)"
            }

            val prompt = """
            Based on the user's query "$userQuery", I found the following relevant documents:
            
            $documentsList
            
            Please provide a brief, friendly summary of what documents were found and how they relate to the user's question. Keep it concise and conversational.
            """.trimIndent()

            val response = generativeModel.generateContent(prompt)
            val responseText = response.text ?: "I found some relevant documents for your query."

            Log.d(TAG, "✅ FirebaseAiService: Search summary generated")
            return@withContext responseText

        } catch (e: Exception) {
            Log.e(TAG, "❌ FirebaseAiService: Error generating search summary", e)
            return@withContext "I found ${searchResults.size} relevant documents for your query."
        }
    }

    /**
     * Enum for supported file types.
     */
    enum class FileType {
        PDF,
        IMAGE,
        DOCUMENT,
        UNSUPPORTED
    }
}
