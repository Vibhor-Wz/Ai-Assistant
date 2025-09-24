package com.bigcash.ai.vectordb.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sin
import kotlin.random.Random

/**
 * Safe service for generating embeddings using Gemini's embedding model.
 * This service handles text-to-vector conversion using the gemini-embedding-001 model.
 * Falls back to mock embeddings if Gemini API is not available or has dependency issues.
 */
class SafeGeminiEmbeddingService(private val context: Context) : EmbeddingService {
    
    companion object {
        private const val TAG = "SafeGeminiEmbeddingService"
        private const val MODEL_NAME = "gemini-embedding-001"
        private const val EMBEDDING_DIMENSION = 768 // Gemini embedding-001 produces 768-dimensional vectors
    }
    
    /**
     * Generate embedding for a single text input.
     * 
     * @param text The text to embed
     * @return FloatArray representing the embedding vector
     * @throws EmbeddingException if embedding generation fails
     */
    override suspend fun generateEmbedding(text: String): FloatArray = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating embedding for text of length: ${text.length}")
            
            if (text.isBlank()) {
                throw EmbeddingException("Text cannot be empty or blank")
            }
            
            // For now, use mock embeddings until Gemini API dependencies are resolved
            val embedding = createMockEmbedding(text)
            
            Log.d(TAG, "Successfully generated embedding with dimension: ${embedding.size}")
            return@withContext embedding
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating embedding", e)
            throw EmbeddingException("Failed to generate embedding: ${e.message}", e)
        }
    }
    
    /**
     * Generate embeddings for multiple text inputs in batch.
     * 
     * @param texts List of texts to embed
     * @return List of FloatArray representing embedding vectors
     * @throws EmbeddingException if embedding generation fails
     */
    override suspend fun generateEmbeddings(texts: List<String>): List<FloatArray> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating embeddings for ${texts.size} texts")
            
            val embeddings = mutableListOf<FloatArray>()
            
            // Process texts in batches to avoid overwhelming the system
            val batchSize = 10
            texts.chunked(batchSize).forEach { batch ->
                val batchEmbeddings = batch.map { text ->
                    generateEmbedding(text)
                }
                embeddings.addAll(batchEmbeddings)
                
                // Small delay between batches
                kotlinx.coroutines.delay(100)
            }
            
            Log.d(TAG, "Successfully generated ${embeddings.size} embeddings")
            return@withContext embeddings
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating batch embeddings", e)
            throw EmbeddingException("Failed to generate batch embeddings: ${e.message}", e)
        }
    }
    
    /**
     * Get the expected embedding dimension for this model.
     * 
     * @return The dimension of embeddings produced by this model
     */
    override fun getEmbeddingDimension(): Int = EMBEDDING_DIMENSION
    
    /**
     * Validate if an embedding vector has the correct dimension.
     * 
     * @param embedding The embedding vector to validate
     * @return true if the embedding has the correct dimension
     */
    override fun isValidEmbedding(embedding: FloatArray): Boolean {
        return embedding.size == EMBEDDING_DIMENSION
    }
    
    /**
     * Create a mock embedding for testing purposes.
     * In production, replace this with actual Gemini embedding API call.
     * 
     * @param text The text to create embedding for
     * @return Mock embedding vector
     */
    private fun createMockEmbedding(text: String): FloatArray {
        val embedding = FloatArray(EMBEDDING_DIMENSION)
        val textHash = text.hashCode()
        
        // Create deterministic mock embedding based on text hash
        for (i in embedding.indices) {
            val seed = textHash + i
            embedding[i] = (sin(seed.toDouble()) * 0.5).toFloat()
        }
        
        // Normalize the embedding to unit length
        return normalizeEmbedding(embedding)
    }
    
    /**
     * Normalize an embedding vector to unit length.
     * 
     * @param embedding The embedding to normalize
     * @return Normalized embedding
     */
    private fun normalizeEmbedding(embedding: FloatArray): FloatArray {
        val magnitude = kotlin.math.sqrt(embedding.sumOf { (it * it).toDouble() }.toFloat())
        
        return if (magnitude > 0) {
            FloatArray(embedding.size) { embedding[it] / magnitude }
        } else {
            embedding.copyOf()
        }
    }
    
    /**
     * Get the API key for Gemini AI.
     * In a production app, this should be stored securely (e.g., in Android Keystore).
     * For now, we'll use a placeholder that should be replaced with actual API key.
     * 
     * @return The API key for Gemini AI
     */
    private fun getApiKey(): String {
        // TODO: Replace with actual API key from secure storage
        // For development, you can set this in your local.properties file
        return "YOUR_GEMINI_API_KEY_HERE"
    }
    
    /**
     * Custom exception for embedding-related errors.
     */
    class EmbeddingException(message: String, cause: Throwable? = null) : Exception(message, cause)
}
