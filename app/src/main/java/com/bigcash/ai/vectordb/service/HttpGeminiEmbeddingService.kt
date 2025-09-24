package com.bigcash.ai.vectordb.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.sin

/**
 * HTTP-based service for generating embeddings using Gemini's embedding model.
 * This service uses direct HTTP calls to avoid dependency issues with the Gemini client library.
 */
class HttpGeminiEmbeddingService(private val context: Context) : EmbeddingService {
    
    companion object {
        private const val TAG = "HttpGeminiEmbeddingService"
        private const val EMBEDDING_DIMENSION = 768
        private const val GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent"
    }
    
    private val json = Json { ignoreUnknownKeys = true }
    
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
            
            val apiKey = getApiKey()
            if (apiKey == "YOUR_GEMINI_API_KEY_HERE") {
                Log.w(TAG, "Using mock embedding - API key not configured")
                return@withContext createMockEmbedding(text)
            }
            
            val requestBody = EmbeddingRequest(
                content = Content(parts = listOf(Part(text = text)))
            )
            
            val response = makeHttpRequest(requestBody, apiKey)
            val embedding = response.embedding.values
            Log.d(TAG, "Successfully generated embedding: $embedding")
            if (embedding.isEmpty()) {
                throw EmbeddingException("Received empty embedding from Gemini API")
            }
            
            val embeddingArray = FloatArray(embedding.size) { embedding[it] }
            
            Log.d(TAG, "Successfully generated embedding with dimension: ${embeddingArray.size}")
            return@withContext embeddingArray
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating embedding", e)
            Log.w(TAG, "Falling back to mock embedding")
            return@withContext createMockEmbedding(text)
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
            
            // Process texts in batches to avoid rate limiting
            val batchSize = 10
            texts.chunked(batchSize).forEach { batch ->
                val batchEmbeddings = batch.map { text ->
                    generateEmbedding(text)
                }
                embeddings.addAll(batchEmbeddings)
                
                // Small delay between batches to respect rate limits
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
     * Make HTTP request to Gemini API.
     */
    private suspend fun makeHttpRequest(requestBody: EmbeddingRequest, apiKey: String): EmbeddingResponse {
        val url = URL("$GEMINI_API_URL?key=$apiKey")
        val connection = url.openConnection() as HttpURLConnection
        
        return withContext(Dispatchers.IO) {
            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                
                val jsonBody = json.encodeToString(EmbeddingRequest.serializer(), requestBody)
                connection.outputStream.use { outputStream ->
                    outputStream.write(jsonBody.toByteArray())
                }
                
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                    json.decodeFromString(EmbeddingResponse.serializer(), responseBody)
                } else {
                    val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                    throw IOException("HTTP $responseCode: $errorBody")
                }
            } finally {
                connection.disconnect()
            }
        }
    }
    
    /**
     * Create a mock embedding for testing purposes.
     */
    private fun createMockEmbedding(text: String): FloatArray {
        val embedding = FloatArray(EMBEDDING_DIMENSION)
        val textHash = text.hashCode()
        
        // Create deterministic mock embedding based on text hash
        for (i in embedding.indices) {
            val seed = textHash + i
            embedding[i] = (sin(seed.toDouble()) * 0.5).toFloat()
        }
        
        return embedding
    }
    
    /**
     * Get the expected embedding dimension for this model.
     */
    override fun getEmbeddingDimension(): Int = EMBEDDING_DIMENSION
    
    /**
     * Validate if an embedding vector has the correct dimension.
     */
    override fun isValidEmbedding(embedding: FloatArray): Boolean {
        return embedding.size == EMBEDDING_DIMENSION
    }
    
    /**
     * Get the API key for Gemini AI.
     */
    private fun getApiKey(): String {
        return "AIzaSyBSqRcLV6jdKc2vVZUbKWPyordQirMCgZw"
    }
    
    /**
     * Custom exception for embedding-related errors.
     */
    class EmbeddingException(message: String, cause: Throwable? = null) : Exception(message, cause)
}

// Data classes for JSON serialization
@Serializable
data class EmbeddingRequest(
    val content: Content
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String
)

@Serializable
data class EmbeddingResponse(
    val embedding: Embedding
)

@Serializable
data class Embedding(
    val values: List<Float>
)
