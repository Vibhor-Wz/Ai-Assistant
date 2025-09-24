package com.bigcash.ai.vectordb.repository

import android.content.Context
import android.util.Log
import com.bigcash.ai.vectordb.data.ObjectBox
import com.bigcash.ai.vectordb.data.PdfEntity
import com.bigcash.ai.vectordb.data.PdfEntity_
import com.bigcash.ai.vectordb.service.EmbeddingServiceFactory
import com.bigcash.ai.vectordb.service.PdfTextExtractor
import com.bigcash.ai.vectordb.utils.FileStorageManager
import io.objectbox.Box
import io.objectbox.query.QueryBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Repository class for PDF database operations.
 * This class handles all database interactions for PDF entities using ObjectBox.
 * Now includes local text extraction and embedding generation.
 */
class PdfRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "VECTOR_DEBUG" // Single tag for filtering all vector-related logs
    }
    
    private val pdfBox: Box<PdfEntity> = ObjectBox.get().boxFor(PdfEntity::class.java)
    private val pdfTextExtractor = PdfTextExtractor(context)
    private val embeddingService = EmbeddingServiceFactory.getDefaultEmbeddingService(context)
    private val fileStorageManager = FileStorageManager(context)
    
    /**
     * Save a PDF entity to the database.
     *
     * @param pdfEntity The PDF entity to save
     * @return The ID of the saved entity
     */
    suspend fun savePdf(pdfEntity: PdfEntity): Long = withContext(Dispatchers.IO) {
        pdfBox.put(pdfEntity)
    }
    
    /**
     * Get all PDFs from the database.
     *
     * @return List of all PDF entities
     */
    suspend fun getAllPdfs(): List<PdfEntity> = withContext(Dispatchers.IO) {
        pdfBox.all
    }
    
    /**
     * Get a PDF by its ID.
     *
     * @param id The ID of the PDF to retrieve
     * @return The PDF entity or null if not found
     */
    suspend fun getPdfById(id: Long): PdfEntity? = withContext(Dispatchers.IO) {
        pdfBox.get(id)
    }
    
    /**
     * Delete a PDF from the database and local storage.
     *
     * @param id The ID of the PDF to delete
     * @return True if the PDF was deleted, false if not found
     */
    suspend fun deletePdf(id: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            // Get the PDF entity first to get the local file path
            val pdfEntity = pdfBox.get(id)
            if (pdfEntity != null) {
                // Delete the local file if it exists
                if (pdfEntity.localFilePath.isNotEmpty()) {
                    val fileDeleted = fileStorageManager.deleteFile(pdfEntity.localFilePath)
                    Log.d(TAG, "Local file deletion result: $fileDeleted for path: ${pdfEntity.localFilePath}")
                }
                
                // Delete from database
                pdfBox.remove(id)
                Log.d(TAG, "PDF deleted from database: $id")
                true
            } else {
                Log.w(TAG, "PDF not found for deletion: $id")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting PDF: $id", e)
            false
        }
    }
    
    /**
     * Search PDFs by name.
     *
     * @param name The name or part of the name to search for
     * @return List of PDF entities matching the search criteria
     */
    suspend fun searchPdfsByName(name: String): List<PdfEntity> = withContext(Dispatchers.IO) {
        val q = pdfBox.query()
            .contains(PdfEntity_.name, name, QueryBuilder.StringOrder.CASE_INSENSITIVE)
            .build()
        val find = q.find()
        q.close()
        return@withContext find
    }
    
    /**
     * Generate a mock vector embedding for demonstration purposes.
     * This is kept as a fallback method.
     *
     * @param size The size of the embedding vector (default: 512)
     * @return FloatArray representing the vector embedding
     */
    fun generateMockEmbedding(size: Int = 768): FloatArray {
        return FloatArray(size) { Random.nextFloat() * 2 - 1 } // Values between -1 and 1
    }
    
    /**
     * Extract text from PDF data.
     *
     * @param data PDF file data as ByteArray
     * @return Extracted text
     */
    suspend fun extractTextFromPdf(fileName: String, data: ByteArray): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "📖 Repository: Starting text extraction from file: $fileName")
        Log.d(TAG, "📊 Repository: File data size: ${data.size} bytes")
        Log.d(TAG, "📊 Repository: File data : $data")

        try {
            val extractedText = pdfTextExtractor.extractTextFromPdf(fileName, data)
            Log.d(TAG, "✅ Repository: Text extraction completed successfully")
            Log.d(TAG, "📊 Repository: Extracted text length: ${extractedText.length}")
            extractedText
        } catch (e: Exception) {
            Log.e(TAG, "❌ Repository: Error extracting text from file", e)
            ""
        }
    }
    
    /**
     * Legacy method for backward compatibility.
     * @deprecated Use extractTextFromPdf(fileName, data) instead
     */
    @Deprecated("Use extractTextFromPdf(fileName, data) for better file type detection")
    suspend fun extractTextFromPdf(data: ByteArray): String = withContext(Dispatchers.IO) {
        return@withContext extractTextFromPdf("unknown.pdf", data)
    }
    
    /**
     * Generate embedding from text using the configured embedding service.
     *
     * @param text Input text
     * @return Embedding vector as FloatArray
     */
    suspend fun generateEmbedding(text: String): FloatArray = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔄 Repository: Generating embedding using EmbeddingServiceFactory")
        Log.d(TAG, "📝 Repository: Input text length: ${text.length}")
        try {
            val embedding = embeddingService.generateEmbedding(text)
            Log.d(TAG, "✅ Repository: Embedding generated successfully")
            return@withContext embedding
        } catch (e: Exception) {
            Log.e(TAG, "❌ Repository: Error generating embedding", e)
            Log.d(TAG, "🔄 Repository: Falling back to mock embedding")
            val mockEmbedding = generateMockEmbedding()
            Log.d(TAG, "✅ Repository: Mock embedding generated as fallback")
            return@withContext mockEmbedding
        }
    }
    
    /**
     * Create a PDF entity from file data with real text extraction and embedding generation.
     *
     * @param name The name of the PDF
     * @param data The PDF file data as ByteArray
     * @param description Optional description
     * @return PdfEntity with extracted text and generated embedding
     */
    suspend fun createPdfEntity(name: String, data: ByteArray, description: String = ""): PdfEntity ?  = withContext(Dispatchers.IO) {
        Log.d(TAG, "🚀 Repository: Starting PDF entity creation")
        Log.d(TAG, "📄 Repository: Processing PDF: $name (${data.size} bytes)")
        
        try {
            // Save the original file to local storage
            Log.d(TAG, "💾 Repository: Saving file to local storage")
            val localFilePath = fileStorageManager.saveFile(name, data)
            if (localFilePath == null) {
                Log.e(TAG, "❌ Repository: Failed to save file to local storage")
                return@withContext null
            }
            Log.d(TAG, "✅ Repository: File saved to local storage: $localFilePath")
            
            // Extract text from PDF using ML Kit
            Log.d(TAG, "📖 Repository: Extracting text from file using ML Kit")
            val extractedText = extractTextFromPdf(name, data)
            
            if (extractedText.isEmpty()) {
                Log.w(TAG, "⚠️ Repository: No text extracted from PDF: $name")
            } else {
                Log.d(TAG, "✅ Repository: Extracted ${extractedText.length} characters from PDF: $name")
                Log.d(TAG, "📝 Repository: Text preview: ${extractedText.take(100)}${if (extractedText.length > 100) "..." else ""}")
            }
            
            // Generate embedding from extracted text
            Log.d(TAG, "🧠 Repository: Generating embedding")
            val embedding = if (extractedText.isNotEmpty()) {
                Log.d(TAG, "📊 Repository: Using extracted text for embedding generation")
                generateEmbedding(extractedText)
            } else {
                // Fallback to mock embedding if no text extracted
                Log.w(TAG, "⚠️ Repository: Using mock embedding for PDF with no extracted text: $name")
                generateMockEmbedding()
            }
            
            val pdfEntity = PdfEntity(
                name = name,
                data = extractedText,
                embedding = embedding,
                fileSize = data.size.toLong(),
                description = description.ifEmpty { "Extracted text: ${extractedText.take(100)}..." },
                localFilePath = localFilePath
            )
            
            Log.d(TAG, "✅ Repository: PDF entity created successfully")
            Log.d(TAG, "📊 Repository: Entity embedding dimension: ${embedding.size}")
            Log.d(TAG, "📊 Repository: Entity embedding magnitude: ${kotlin.math.sqrt(embedding.sumOf { (it * it).toDouble() }.toFloat())}")
            Log.d(TAG, "📁 Repository: Local file path: $localFilePath")
            
            return@withContext pdfEntity
        } catch (e: Exception) {
            Log.e(TAG, "❌ Repository: Error creating PDF entity for: $name", e)
            Log.d(TAG, "🔄 Repository: Creating fallback entity with mock embedding")

            Log.d(TAG, "✅ Repository: Fallback entity created")
            return@withContext null
        }
    }
    
    /**
     * Update an existing PDF entity.
     *
     * @param pdfEntity The PDF entity to update
     * @return The updated PDF entity ID
     */
    suspend fun updatePdf(pdfEntity: PdfEntity): Long = withContext(Dispatchers.IO) {
        pdfBox.put(pdfEntity)
    }

    
    /**
     * Perform vector search using ObjectBox's built-in vector search capabilities.
     * This method converts a user query to an embedding and searches for the most similar PDFs.
     *
     * @param queryText The user's natural language query
     * @param topK Number of top results to return (default: 3)
     * @return List of most similar PDFs with similarity scores
     */
    suspend fun vectorSearch(queryText: String, topK: Int = 3): List<Pair<PdfEntity, Float>> = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔍 Repository: Starting ObjectBox vector search")
        Log.d(TAG, "📝 Repository: Query: '$queryText'")
        Log.d(TAG, "📊 Repository: Top K: $topK")
        
        try {
            // Generate embedding for the query
            Log.d(TAG, "🧠 Repository: Generating query embedding")
            val queryEmbedding = generateEmbedding(queryText)
            Log.d(TAG, "✅ Repository: Query embedding generated (dimension: ${queryEmbedding.size})")
            
            // Perform vector search using ObjectBox
            val searchResults = performObjectBoxVectorSearch(queryEmbedding, topK)
            
            Log.d(TAG, "✅ Repository: Vector search completed")
            Log.d(TAG, "📊 Repository: Found $searchResults results")
            
            return@withContext searchResults
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Repository: Error in vector search", e)
            Log.d(TAG, "📊 Repository: Returning empty results due to error")
            return@withContext emptyList()
        }
    }
    
    /**
     * Perform the actual ObjectBox vector search using the query embedding.
     *
     * @param queryEmbedding The embedding vector for the query
     * @param topK Number of top results to return
     * @return List of similar PDFs with similarity scores
     */
    private suspend fun performObjectBoxVectorSearch(queryEmbedding: FloatArray, topK: Int): List<Pair<PdfEntity, Float>> = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔍 Repository: Performing ObjectBox vector search")
        
        try {
            // Get all PDFs from the database
            val allPdfs = pdfBox.all
            Log.d(TAG, "📊 Repository: Total PDFs in database: ${allPdfs.size}")
            
            if (allPdfs.isEmpty()) {
                Log.d(TAG, "📊 Repository: No PDFs found in database")
                return@withContext emptyList()
            }
            val q = pdfBox.query(PdfEntity_.embedding.nearestNeighbors(queryEmbedding, topK))
                .build()
            val resultsWithScores = q.findWithScores()
            q.close()

            return@withContext resultsWithScores.map { result ->
                Pair(result.get(), result.score.toFloat())
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Repository: Error in ObjectBox vector search", e)
            throw e
        }
    }
    
    /**
     * Get the original file from local storage.
     *
     * @param pdfEntity The PDF entity containing the local file path
     * @return The original file if it exists, null otherwise
     */
    fun getOriginalFile(pdfEntity: PdfEntity): java.io.File? {
        return if (pdfEntity.localFilePath.isNotEmpty()) {
            fileStorageManager.getFile(pdfEntity.localFilePath)
        } else {
            Log.w(TAG, "No local file path found for PDF: ${pdfEntity.name}")
            null
        }
    }
    
    /**
     * Check if the original file exists in local storage.
     *
     * @param pdfEntity The PDF entity containing the local file path
     * @return True if the file exists, false otherwise
     */
    fun hasOriginalFile(pdfEntity: PdfEntity): Boolean {
        return if (pdfEntity.localFilePath.isNotEmpty()) {
            fileStorageManager.fileExists(pdfEntity.localFilePath)
        } else {
            false
        }
    }
    
    /**
     * Get the size of the original file in local storage.
     *
     * @param pdfEntity The PDF entity containing the local file path
     * @return The file size in bytes, or -1 if the file doesn't exist
     */
    fun getOriginalFileSize(pdfEntity: PdfEntity): Long {
        return if (pdfEntity.localFilePath.isNotEmpty()) {
            fileStorageManager.getFileSize(pdfEntity.localFilePath)
        } else {
            -1
        }
    }
    
    /**
     * Clean up resources.
     */
    fun cleanup() {
        Log.d(TAG, "🧹 Repository: Cleaning up all resources")
        // Note: EmbeddingServiceFactory services don't require cleanup
        pdfTextExtractor.cleanup()
        // FileStorageManager doesn't require cleanup as it uses lazy initialization
    }
}
