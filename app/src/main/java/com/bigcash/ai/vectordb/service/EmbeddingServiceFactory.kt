package com.bigcash.ai.vectordb.service

import android.content.Context
import android.util.Log

/**
 * Factory for creating embedding services.
 * This allows switching between different embedding implementations.
 */
object EmbeddingServiceFactory {
    
    private const val TAG = "EmbeddingServiceFactory"
    
    // Service types
    const val TYPE_MOCK = "mock"
    const val TYPE_HTTP_GEMINI = "http_gemini"
    
    // Default service type
    private const val DEFAULT_SERVICE_TYPE = TYPE_HTTP_GEMINI
    
    /**
     * Create an embedding service based on the specified type.
     * 
     * @param context Android context
     * @param serviceType Type of service to create
     * @return Embedding service instance
     */
    fun createEmbeddingService(context: Context, serviceType: String = DEFAULT_SERVICE_TYPE): EmbeddingService {
        Log.d(TAG, "Creating embedding service of type: $serviceType")
        
        return when (serviceType) {
            TYPE_MOCK -> MockEmbeddingService(context)
            else -> {
                HttpGeminiEmbeddingService(context)
            }
        }
    }
    
    /**
     * Get the default embedding service.
     * 
     * @param context Android context
     * @return Default embedding service
     */
    fun getDefaultEmbeddingService(context: Context): EmbeddingService {
        return createEmbeddingService(context, DEFAULT_SERVICE_TYPE)
    }
    
    /**
     * Get all available service types.
     * 
     * @return List of available service types
     */
    fun getAvailableServiceTypes(): List<String> {
        return listOf(TYPE_MOCK, TYPE_HTTP_GEMINI)
    }
}

/**
 * Common interface for embedding services.
 */
interface EmbeddingService {
    suspend fun generateEmbedding(text: String): FloatArray
    suspend fun generateEmbeddings(texts: List<String>): List<FloatArray>
    fun getEmbeddingDimension(): Int
    fun isValidEmbedding(embedding: FloatArray): Boolean
}

/**
 * Mock embedding service for testing and fallback scenarios.
 */
class MockEmbeddingService(private val context: Context) : EmbeddingService {
    
    companion object {
        private const val TAG = "MockEmbeddingService"
        private const val EMBEDDING_DIMENSION = 768
    }
    
    override suspend fun generateEmbedding(text: String): FloatArray {
        Log.d(TAG, "Generating mock embedding for text of length: ${text.length}")
        
        val embedding = FloatArray(EMBEDDING_DIMENSION)
        val textHash = text.hashCode()
        
        // Create deterministic mock embedding based on text hash
        for (i in embedding.indices) {
            val seed = textHash + i
            embedding[i] = (kotlin.math.sin(seed.toDouble()) * 0.5).toFloat()
        }
        
        return embedding
    }
    
    override suspend fun generateEmbeddings(texts: List<String>): List<FloatArray> {
        Log.d(TAG, "Generating ${texts.size} mock embeddings")
        return texts.map { generateEmbedding(it) }
    }
    
    override fun getEmbeddingDimension(): Int = EMBEDDING_DIMENSION
    
    override fun isValidEmbedding(embedding: FloatArray): Boolean {
        return embedding.size == EMBEDDING_DIMENSION
    }
}
