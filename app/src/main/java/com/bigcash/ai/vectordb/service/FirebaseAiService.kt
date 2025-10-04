package com.bigcash.ai.vectordb.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    
    private val imageGenerativeModel: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-2.5-flash-image-preview",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.TEXT, ResponseModality.IMAGE)
            }
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
            "mp3" -> "audio/mp3"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "aiff" -> "audio/aiff"
            "aac" -> "audio/aac"
            "flac" -> "aaudio/flac"

            // Video
            "mp4" -> "video/mp4"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"

            else -> "application/octet-stream"
        }
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
            FileType.AUDIO -> createAudioPrompt()
        }
    }

    private fun createAudioPrompt(): String {
        return """
            You are an AI audio analysis and summarization system.  
            Your task is to process the given audio transcript and decide how to respond based on the content type.  
 
            1. **Audio Type Detection**  
            - If the audio is **generic voice content** (e.g., casual speech, music, short generic sentences, background noise, unrelated chatter, or single unrelated phrases), then:  
            → Do not generate a meeting summary.  
            → Instead, provide a **simple and concise generic response** describing the content.  
 
            - If the audio is a **meeting/discussion/conversational context** (two or more people talking, or a single speaker presenting/explaining a topic in detail, such as business, planning, brainstorming, or structured context), then:  
            → Identify it as a "Meeting/Contextual Discussion".  
            → Generate a **clear, structured summary** covering:  
            - Main topics discussed  
            - Key points raised by speakers  
            - Decisions or outcomes (if any)  
            - Action items or follow-ups (if mentioned)  
 
            2. **Output Rules**  
            - Always first state the **detected audio type**: "Generic Audio" or "Meeting/Discussion".  
            - Then provide the appropriate response (generic description or structured summary).  
            - Keep responses concise but informative.  
 
            3. **Tone**  
            - Neutral, professional, and easy to understand.  
            - Do not invent details not supported by the audio transcript.  
        """.trimIndent()
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
                "document_text": "Full content of the PDF exactly as it appears, preserving text, tables, images, spacing, and formatting"
            }
        8. Output only valid JSON, without extra explanation or commentary and no "'''" in start and end of json.
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
        2. Preserve the original structure and context:
           - Maintain lines, paragraphs, labels, tables, and any visible formatting.
           - Include any detected images or graphics as descriptive placeholders (e.g., "[Image: description]") inline.
           - Preserve the natural flow and sequence of content as it appears in the image.
        3. Include everything visible in the image, whether it is a document (e.g., Aadhaar, PAN, passbook, bank statement), a form, an advertisement, or any other content.
        4. Represent the entire content as a single string in JSON to maintain context.
        Required Output:
        Provide structured JSON in the following format:
    
        {
            "metadata": {
                "file_name": "$fileName",
                "file_size": ${fileSize},
                "file_type": "Image"
            },
            "document_text": "Full content of the image exactly as it appears, preserving text, tables, images, and layout"
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
                    "file_type": "PDF"
                },
                "document_text": "Full content of the PDF exactly as it appears, preserving text, tables, images, spacing, and formatting"
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
        2. Preserve the original sequence, layout, and flow of content:
           - Maintain lines, paragraphs, labels, tables, lists, and images in their original positions.
           - For images or graphics, include descriptive placeholders inline (e.g., "[Image: description]").
           - Do NOT split content into separate sections unless naturally present.
        3. Include all visible content in the order it appears in the file.
        4. Represent the entire content as a single string in JSON to maintain context.
        6. For lists, preserve the original order and formatting.
        7. Provide structured JSON output in the following format:
            Required Output:
            Provide structured JSON in the following format:
        
            {
                "metadata": {
                    "file_name": "$fileName",
                    "file_size": ${fileSize},
                    "file_type": "<detected type>"
                },
                "document_text": "Full content of the file exactly as it appears, preserving text, tables, images, and layout"
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
    1. If the retrieved documents clearly answer the query, summarize and provide the answer using markdown formatting for better readability.
    2. If the documents only partially match, provide what is available and explain that some details are missing.
    3. If the documents are unrelated or don't contain any useful information, DO NOT mention document names or unrelated topics. 
       Simply give a natural, generic response such as:
       - "I couldn't find your address in the documents provided."
       - "No document contains that type of information."
    4. Keep the tone conversational, clear, and user-friendly.
    5. Do not ask the user follow-up questions — only answer based on what you have.
    6. If the user query is for a **document/file** itself (e.g., "Give me my PAN card", "Show me my Aadhaar"), 
       do not provide any text answer, just give a generic response.
    7. **Special Rule for Numbers:**
       - If the user query explicitly asks for a **number** (e.g., "PAN number", "Aadhaar number", "XYZ number"), 
         then provide only the number as text (**TEXT_ONLY**).
       - If the query asks for the **document/card itself** (e.g., "PAN card", "Aadhaar card"), 
         then provide the document/file (**FULL_FILE**).
       - If it’s ambiguous or could be both (e.g., "Give me my Aadhaar"), treat it as **FULL_FILE**.
    
    RESPONSE TYPE INDICATOR:
    At the end of your response, add a response type indicator in the following format:
    
    [RESPONSE_TYPE: <TYPE>]
    
     Where <TYPE> should be one of:
     - TEXT_ONLY: If the user is asking for specific information that can be extracted from the documents (e.g., "What's my address?", "What's my PAN number?")
     - FULL_FILE: If the user is asking for the complete document/file itself (e.g., "Give me my Aadhaar card", "Show me my PAN document", "Send me my bank statement")
     - MIXED: If the user's request is ambiguous or could benefit from both information and file access (e.g., "What documents do I have?", "Tell me about my files")
    
    Examples:
    - Query: "What's my PAN number?" → [RESPONSE_TYPE: TEXT_ONLY]
    - Query: "Give me my Aadhaar card" → [RESPONSE_TYPE: FULL_FILE]
    - Query: "Show me my PAN" → [RESPONSE_TYPE: FULL_FILE]
    - Query: "What's my Aadhaar number?" → [RESPONSE_TYPE: TEXT_ONLY]
    
    FORMATTING GUIDELINES:
    - Use **bold** for important points and key information
    - Use *italics* for emphasis
    - Use bullet points (-) for lists
    - Use numbered lists (1., 2., etc.) for step-by-step instructions
    - Use > blockquotes for important quotes or excerpts from documents
    - Use ## headings for major sections when organizing complex information
    - Use [links](url) when referencing external resources (if applicable)
    
    Important: 
    - Never include file names or irrelevant document details in your answer when they don't actually help answer the query.
    - Always prioritize clarity and avoid confusing the user with unrelated content.
    - Use markdown formatting to make your response more readable and organized.
    - Always end your response with the [RESPONSE_TYPE: <TYPE>] indicator.
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
     * Generate a summary from text using Firebase AI.
     * This function takes any text content and generates a comprehensive summary.
     *
     * @param text The text content to summarize
     * @param language The language for summary generation ("English" or "Hindi")
     * @return AI-generated summary of the text
     */
    suspend fun generateSummaryFromText(text: String, language: String = "English"): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "📝 FirebaseAiService: Generating summary from text")
        Log.d(TAG, "📊 FirebaseAiService: Text length: ${text.length} characters")

        try {
            // Ensure user is authenticated
            if (!ensureAuthenticated()) {
                Log.w(TAG, "⚠️ FirebaseAiService: Authentication failed, using fallback")
                return@withContext generateTextSummaryFallback(text)
            }

//            val prompt = buildTextSummaryPrompt(text)
            val prompt = if (language.lowercase() == "hindi") {
                buildHindiSummaryPrompt(text)
            } else {
                buildTextSummaryPrompt(text)
            }
            Log.d(TAG, "📋 FirebaseAiService: Generated text summary prompt")

            val response = generativeModel.generateContent(prompt)
            val summary = response.text ?: generateTextSummaryFallback(text)

            Log.d(TAG, "✅ FirebaseAiService: Text summary generated successfully")
            Log.d(TAG, "📊 FirebaseAiService: Summary length: ${summary.length} characters")

            return@withContext summary

        } catch (e: Exception) {
            Log.e(TAG, "❌ FirebaseAiService: Error generating text summary", e)
            return@withContext generateTextSummaryFallback(text)
        }
    }

    /**
     * Summarize a YouTube transcript using Firebase AI.
     * This function takes a raw YouTube transcript and generates a comprehensive summary.
     *
     * @param transcript The raw transcript text from YouTube
     * @param videoTitle Optional video title for context
     * @return AI-generated summary of the transcript
     */
    suspend fun summarizeYouTubeTranscript(
        transcript: String,
        videoTitle: String? = null
    ): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "🎥 FirebaseAiService: Summarizing YouTube transcript")
        Log.d(TAG, "📝 FirebaseAiService: Transcript length: ${transcript.length} characters")
        Log.d(TAG, "📺 FirebaseAiService: Video title: $videoTitle")

        try {
            // Ensure user is authenticated
            if (!ensureAuthenticated()) {
                Log.w(TAG, "⚠️ FirebaseAiService: Authentication failed, using fallback")
                return@withContext generateTranscriptFallback(transcript, videoTitle)
            }

            val prompt = buildTranscriptSummaryPrompt(transcript, videoTitle)
            Log.d(TAG, "📋 FirebaseAiService: Generated transcript summary prompt")

            val response = generativeModel.generateContent(prompt)
            val summary = response.text ?: generateTranscriptFallback(transcript, videoTitle)

            Log.d(TAG, "✅ FirebaseAiService: Transcript summary generated successfully")
            Log.d(TAG, "📊 FirebaseAiService: Summary length: ${summary.length} characters")

            return@withContext summary

        } catch (e: Exception) {
            Log.e(TAG, "❌ FirebaseAiService: Error summarizing transcript", e)
            return@withContext generateTranscriptFallback(transcript, videoTitle)
        }
    }

    /**
     * Build a comprehensive prompt for summarizing YouTube transcripts.
     *
     * @param transcript The raw transcript text
     * @param videoTitle Optional video title
     * @return Formatted prompt for the AI model
     */
    private fun buildTranscriptSummaryPrompt(transcript: String, videoTitle: String?): String {
        val titleContext = if (videoTitle != null) {
            "Video Title: $videoTitle\n\n"
        } else {
            ""
        }

        return """
        You are an AI assistant specialized in summarizing YouTube video transcripts. 
        Your task is to create a comprehensive, well-structured summary of the provided transcript.
        
        $titleContext
        TRANSCRIPT TO SUMMARIZE:
        $transcript
        
        YOUR TASK:
        1. **Create a comprehensive summary** that captures the main points, key topics, and important details from the transcript.
        2. **Structure the summary** with clear headings and sections for better readability.
        3. **Identify the main themes** and topics discussed in the video.
        4. **Extract key insights** and important information that would be valuable to someone who didn't watch the video.
        5. **Maintain the original context** and meaning while making it more concise and organized.
        6. **Use markdown formatting** to make the summary visually appealing and easy to read.
        
        SUMMARY STRUCTURE:
        ## 📺 Video Summary
        
        ### 🎯 Main Topics
        - List the main topics and themes discussed
        
        ### 🔑 Key Points
        - Highlight the most important points and insights
        
        ### 📝 Detailed Summary
        - Provide a comprehensive summary of the content
        
        ### 💡 Key Takeaways
        - List the main takeaways and actionable insights
        
        ### 🏷️ Tags
        - Suggest relevant tags or categories for this content
        
        FORMATTING GUIDELINES:
        - Use **bold** for important points and key information
        - Use *italics* for emphasis
        - Use bullet points (-) for lists
        - Use numbered lists (1., 2., etc.) for step-by-step information
        - Use > blockquotes for important quotes or key statements
        - Use ## headings for major sections
        - Use ### headings for subsections
        - Use emojis to make sections more visually appealing
        
        IMPORTANT:
        - Make the summary comprehensive but concise
        - Preserve the original meaning and context
        - Use clear, professional language
        - Organize information logically
        - Make it useful for someone who wants to understand the video content without watching it
        - If the transcript contains multiple languages, note this and summarize accordingly
        - If there are timestamps or technical details, include them where relevant
        """.trimIndent()
    }

    /**
     * Generate a fallback summary when AI service is unavailable.
     */
    private fun generateTranscriptFallback(transcript: String, videoTitle: String?): String {
        val wordCount = transcript.split("\\s+".toRegex()).size
        val charCount = transcript.length
        val lines = transcript.split("\n").size
        
        return """
        ## 📺 YouTube Video Summary
        
        ${if (videoTitle != null) "**Video Title:** $videoTitle\n" else ""}
        
        ### 📊 Transcript Statistics
        - **Word Count:** $wordCount words
        - **Character Count:** $charCount characters  
        - **Lines:** $lines lines
        
        ### 📝 Content Overview
        This is a transcript from a YouTube video. The content appears to be substantial with $wordCount words, suggesting it covers detailed information or discussion.
        
        ### 🔍 Key Characteristics
        - **Content Type:** Video transcript
        - **Length:** ${if (wordCount > 1000) "Long-form content" else if (wordCount > 500) "Medium-length content" else "Short content"}
        - **Language:** ${if (transcript.any { it.code in 0x0900..0x097F }) "Contains Hindi/Devanagari script" else "English or other language"}
        
        ### 📋 Raw Transcript Preview
        ${transcript.take(500)}${if (transcript.length > 500) "..." else ""}
        
        ### ⚠️ Note
        This is a basic analysis generated without AI processing. For a detailed summary, ensure Firebase AI service is properly configured.
        
        **Full Transcript Length:** ${transcript.length} characters
        """.trimIndent()
    }

    /**
     * Build a comprehensive prompt for summarizing general text content.
     *
     * @param text The text content to summarize
     * @return Formatted prompt for the AI model
     */
//    private fun buildTextSummaryPrompt(text: String): String {
//        return """
//        You are an AI assistant specialized in creating comprehensive summaries of text content.
//        Your task is to analyze the provided text and create a well-structured, informative summary.
//
//        TEXT TO SUMMARIZE:
//        $text
//
//        YOUR TASK:
//        1. **Create a comprehensive summary** that captures the main points, key topics, and important details from the text.
//        2. **Structure the summary** with clear headings and sections for better readability.
//        3. **Identify the main themes** and topics discussed in the content.
//        4. **Extract key insights** and important information that would be valuable to someone who didn't read the original text.
//        5. **Maintain the original context** and meaning while making it more concise and organized.
//        6. **Use markdown formatting** to make the summary visually appealing and easy to read.
//
//        SUMMARY STRUCTURE:
//        ## 📝 Content Summary
//
//        ### 🎯 Main Topics
//        - List the main topics and themes discussed
//
//        ### 🔑 Key Points
//        - Highlight the most important points and insights
//
//        ### 📋 Detailed Summary
//        - Provide a comprehensive summary of the content
//
//        ### 💡 Key Takeaways
//        - List the main takeaways and actionable insights
//
//        ### 🏷️ Content Type
//        - Identify what type of content this appears to be (meeting notes, call transcript, document, etc.)
//
//        FORMATTING GUIDELINES:
//        - Use **bold** for important points and key information
//        - Use *italics* for emphasis
//        - Use bullet points (-) for lists
//        - Use numbered lists (1., 2., etc.) for step-by-step information
//        - Use > blockquotes for important quotes or key statements
//        - Use ## headings for major sections
//        - Use ### headings for subsections
//
//        CONTENT ANALYSIS GUIDELINES:
//        - If this appears to be a call transcript or meeting notes, structure it accordingly
//        - If this appears to be a document or article, summarize the main arguments and conclusions
//        - If this appears to be technical content, highlight the technical details and procedures
//        - If this appears to be conversational content, capture the main discussion points and decisions
//        - Use clear, professional language
//        - Organize information logically
//        - Make it useful for someone who wants to understand the content without reading the original
//        - If the content contains multiple languages, note this and summarize accordingly
//        - If there are timestamps or technical details, include them where relevant
//        """.trimIndent()
//    }









    private fun buildTextSummaryPrompt(text: String): String {
        return """
    You are an assistant that writes **clear, natural, and easy-to-read summaries** of any text.
    Your goal is to help someone quickly understand what the text is about —
    as if you were explaining it to a friend who didn’t read it.

    TEXT TO SUMMARIZE:
    $text

    YOUR TASK:
    1. Write a **simple, natural summary** that covers the main ideas, important details, and any useful insights.
    2. Use a **friendly, human tone** — not robotic or academic.
    3. Keep the summary **concise but complete** — enough for someone to fully understand the content.
    4. If the text is a conversation or call, describe what was discussed and what was decided.
    5. If it’s a document, article, or notes, summarize the key sections and main message.
    6. Keep formatting light and easy to scan.

    FORMAT:
    ## Summary

    ### Main Idea
    - A short overview of what this text is mainly about.

    ### Important Points
    - A few bullet points summarizing the key details, topics, or decisions.

    ### Extra Notes
    - Any useful insights, context, or follow-ups worth mentioning.

    STYLE GUIDE:
    - Use **plain, natural language**.
    - Avoid overusing markdown or bold unless needed.
    - Don’t make it sound like an AI wrote it.
    - Focus on *clarity and readability*.
    - If the text includes emotions, tone, or sentiment, capture it briefly in your own words.

    GOAL:
    Make the summary sound like it was written by a helpful human who read and understood the content carefully.
    """.trimIndent()
    }
















//    private fun buildTextSummaryPrompt(text: String): String {
//        return """
//    You are an assistant that writes **clear, natural summaries** of any text or transcript.
//    Your goal is to help someone quickly understand what was said or written —
//    as if you were explaining it to a friend who didn’t read it.
//
//    TEXT TO SUMMARIZE:
//    $text
//
//    YOUR TASK:
//    1. Write a **simple, human-readable summary** that covers the main ideas and useful details.
//    2. If the text includes **multiple speakers** (like a meeting or call),
//       - Identify each speaker by name if provided (e.g., "John:", "Agent:", "Customer:").
//       - Summarize what each speaker said in your own words.
//       - Highlight key exchanges, questions, and decisions.
//    3. If the text is not conversational (e.g., article, notes, document), write a normal summary.
//    4. Use a **friendly, natural tone** — not robotic or overly formal.
//    5. Keep it concise but complete — focus on clarity and usefulness.
//
//    FORMAT:
//    ## Summary
//
//    ### Main Idea
//    - Briefly explain what the text or discussion was about.
//
//    ### Key Details
//    - Important points, topics, or actions mentioned.
//    - Keep sentences short and easy to read.
//
//    ### Speakers (if applicable)
//    - **Speaker 1 (Name or Role):** Main points or statements.
//    - **Speaker 2 (Name or Role):** Responses or opinions.
//    - Include more speakers only if relevant.
//
//    ### Notes or Takeaways
//    - Key insights, outcomes, or next steps.
//    - Mention tone or sentiment briefly if helpful (e.g., friendly, tense, excited).
//
//    STYLE GUIDE:
//    - Use plain, natural language.
//    - Avoid over-formatting or heavy markdown.
//    - Keep the flow conversational.
//    - Only include speakers when they add clarity.
//
//    GOAL:
//    Produce a short, natural summary that sounds like a human wrote it after carefully reading or listening to the content.
//    """.trimIndent()
//    }












    private fun buildHindiSummaryPrompt(text: String): String {
        return """
    आप एक सहायक हैं जो किसी भी टेक्स्ट या बातचीत का **स्पष्ट, आसान और स्वाभाविक हिंदी सारांश** लिखते हैं।
    आपका उद्देश्य यह है कि कोई व्यक्ति जल्दी से समझ सके कि इस टेक्स्ट या बातचीत में क्या कहा गया है,
    जैसे आप किसी दोस्त को समझा रहे हों जिसने इसे नहीं पढ़ा या सुना हो।

    सारांश बनाने के लिए टेक्स्ट:
    $text

    आपका कार्य:
    1. एक **सादा और स्वाभाविक हिंदी सारांश** लिखें जिसमें मुख्य बातें और ज़रूरी विवरण हों।
    2. अगर टेक्स्ट में **कई स्पीकर्स** (जैसे कॉल या मीटिंग) हैं:
       - हर स्पीकर का नाम या रोल पहचानें (जैसे “राहुल:”, “एजेंट:”, “ग्राहक:”)
       - हर स्पीकर ने क्या कहा उसका सारांश अपने शब्दों में लिखें।
       - मुख्य चर्चा, सवाल और निर्णयों को शामिल करें।
    3. अगर टेक्स्ट बातचीत नहीं है (जैसे कोई आर्टिकल या नोट्स), तो सामान्य सारांश लिखें।
    4. भाषा को **दोस्ताना, सरल और प्राकृतिक** रखें — मशीन जैसी नहीं।
    5. सारांश को **संक्षिप्त लेकिन पूरा** बनाएं।

    प्रारूप:
    ## सारांश

    ### मुख्य विषय
    - यह टेक्स्ट या बातचीत मुख्य रूप से किस बारे में है।

    ### ज़रूरी बिंदु
    - मुख्य बातें, विषय या कार्य जिन पर चर्चा हुई।

    ### स्पीकर्स (अगर हों)
    - **स्पीकर 1 (नाम या भूमिका):** क्या कहा।
    - **स्पीकर 2 (नाम या भूमिका):** क्या जवाब दिया या राय दी।

    ### नोट्स / निष्कर्ष
    - महत्वपूर्ण नतीजे, सुझाव या अगला कदम।
    - अगर बातचीत में कोई भावना या टोन है (जैसे सौहार्दपूर्ण, ग़ुस्से वाला, औपचारिक), तो उसका संक्षिप्त ज़िक्र करें।

    लेखन शैली:
    - आसान और रोज़मर्रा की हिंदी का उपयोग करें।
    - बहुत अधिक मार्कडाउन या तकनीकी फॉर्मेटिंग न करें।
    - सिर्फ़ वही जानकारी शामिल करें जो ज़रूरी और स्पष्ट हो।

    उद्देश्य:
    एक ऐसा सारांश बनाना जो ऐसा लगे जैसे किसी इंसान ने ध्यान से पढ़कर या सुनकर लिखा हो —
    न कि किसी मशीन ने जनरेट किया हो।
    """.trimIndent()
    }





    /**
     * Generate a fallback summary when AI service is unavailable for general text.
     */
    private fun generateTextSummaryFallback(text: String): String {
        val wordCount = text.split("\\s+".toRegex()).size
        val charCount = text.length
        val lines = text.split("\n").size
        
        return """
        ## 📝 Content Summary
        
        ### 📊 Content Statistics
        - **Word Count:** $wordCount words
        - **Character Count:** $charCount characters  
        - **Lines:** $lines lines
        
        ### 📋 Content Overview
        This appears to be text content with $wordCount words, suggesting it contains substantial information or discussion.
        
        ### 🔍 Key Characteristics
        - **Content Type:** Text document or transcript
        - **Length:** ${if (wordCount > 1000) "Long-form content" else if (wordCount > 500) "Medium-length content" else "Short content"}
        - **Language:** ${if (text.any { it.code in 0x0900..0x097F }) "Contains Hindi/Devanagari script" else "English or other language"}
        
        ### 📄 Content Preview
        ${text.take(500)}${if (text.length > 500) "..." else ""}
        
        ### ⚠️ Note
        This is a basic analysis generated without AI processing. For a detailed summary, ensure Firebase AI service is properly configured.
        
        **Full Content Length:** ${text.length} characters
        """.trimIndent()
    }


    /**
     * Generate image variations from an existing image.
     *
     * @param imageData Original image as byte array
     * @param variationPrompt Optional prompt for specific variations
     * @return Generated image variation as Bitmap
     */
    suspend fun editImage(imageData: ByteArray, variationPrompt: String? = null): Bitmap? = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔄 FirebaseAiService: Generating image variations")

        try {
            if (!ensureAuthenticated()) {
                Log.w(TAG, "⚠️ FirebaseAiService: Authentication failed for image variations")
                return@withContext null
            }

            // Convert byte array to Bitmap
            val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            if (bitmap == null) {
                Log.e(TAG, "❌ FirebaseAiService: Failed to decode image from byte array")
                return@withContext null
            }

            val prompt = variationPrompt ?: "Change the background of this image"
            Log.d(TAG, "📝 FirebaseAiService: Using prompt: '$prompt'")

            // Use the official API pattern with content block
            val response = imageGenerativeModel.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )
            
            val generatedImageAsBitmap = response
                .candidates.first().content.parts.filterIsInstance<ImagePart>().firstOrNull()?.image
            
            if (generatedImageAsBitmap != null) {
                Log.d(TAG, "✅ FirebaseAiService: Image variation generated successfully")
            } else {
                Log.w(TAG, "⚠️ FirebaseAiService: No image generated in response")
            }
            
            return@withContext generatedImageAsBitmap

        } catch (e: Exception) {
            Log.e(TAG, "❌ FirebaseAiService: Error generating image variations", e)
            return@withContext null
        }
    }

    /**
     * Generate multiple variations of an image.
     * 
     * @param imageData Original image as byte array
     * @param variationPrompt Prompt for variations
     * @param numberOfVariations Number of variations to generate (default: 3)
     * @return List of generated image variations as Bitmaps
     */
    suspend fun generateMultipleImageVariations(
        imageData: ByteArray, 
        variationPrompt: String? = null,
        numberOfVariations: Int = 2
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔄 FirebaseAiService: Generating $numberOfVariations image variations")
        
        val variations = mutableListOf<Bitmap>()
        
        try {
            if (!ensureAuthenticated()) {
                Log.w(TAG, "⚠️ FirebaseAiService: Authentication failed for image variations")
                return@withContext emptyList()
            }

            // Convert byte array to Bitmap
            val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            if (bitmap == null) {
                Log.e(TAG, "❌ FirebaseAiService: Failed to decode image from byte array")
                return@withContext emptyList()
            }

            val basePrompt = variationPrompt ?: "Create variations of this image"
            Log.d(TAG, "📝 FirebaseAiService: Using base prompt: '$basePrompt'")

            // Generate multiple variations by making multiple API calls
            repeat(numberOfVariations) { index ->
                try {
                    val variationNumber = index + 1
                    val specificPrompt = "$basePrompt - Variation $variationNumber"
                    Log.d(TAG, "🎨 FirebaseAiService: Generating variation $variationNumber")
                    
                    // Use the chat-based approach for better variation
                    val chat = imageGenerativeModel.startChat()
                    
                    val prompt = content {
                        image(bitmap)
                        text(specificPrompt)
                    }
                    
                    val response = chat.sendMessage(prompt)
                    
                    val generatedImage = response
                        .candidates.first().content.parts.filterIsInstance<ImagePart>().firstOrNull()?.image
                    
                    if (generatedImage != null) {
                        variations.add(generatedImage)
                        Log.d(TAG, "✅ FirebaseAiService: Variation $variationNumber generated successfully")
                    } else {
                        Log.w(TAG, "⚠️ FirebaseAiService: No image generated for variation $variationNumber")
                    }
                    
                    // Add a small delay between requests to avoid rate limiting
                    delay(1000)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ FirebaseAiService: Error generating variation ${index + 1}", e)
                }
            }
            
            Log.d(TAG, "✅ FirebaseAiService: Generated ${variations.size} variations out of $numberOfVariations requested")
            return@withContext variations
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ FirebaseAiService: Error generating multiple image variations", e)
            return@withContext emptyList()
        }
    }

    /**
     * Enum for supported file types.
     */
    enum class FileType {
        PDF,
        IMAGE,
        DOCUMENT,
        UNSUPPORTED,
        AUDIO
    }
}
