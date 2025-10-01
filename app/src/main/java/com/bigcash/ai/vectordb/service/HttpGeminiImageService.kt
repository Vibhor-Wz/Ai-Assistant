package com.bigcash.ai.vectordb.service

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.io.encoding.Base64
import kotlin.math.sin

/**
 * HTTP-based service for generating embeddings using Gemini's embedding model.
 * This service uses direct HTTP calls to avoid dependency issues with the Gemini client library.
 */
class HttpGeminiImageService(private val context: Context) : EmbeddingService {
    val BASE_64_FLAGS = android.util.Base64.NO_WRAP

    companion object {
        private const val TAG = "HttpGeminiImageService"
        private const val EMBEDDING_DIMENSION = 768

        private const val GEMINI_IMAGE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image-preview:generateContent"

    }


    /**
     * Edit or generate an image using Gemini’s image model.
     *
     * @param imageData Input image bytes (JPEG/PNG)
     * @param editPrompt Instructional prompt for editing
     * @return ByteArray of edited/generated image (PNG by default) or null if failed
     */
    suspend fun editImageWithPrompt(imageData: Bitmap, editPrompt: String): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "✏️ Editing image with prompt: '$editPrompt'")

                val apiKey = getApiKey()
                if (apiKey.isBlank() || apiKey == "YOUR_GEMINI_API_KEY_HERE") {
                    Log.e(TAG, "❌ No valid Gemini API key configured")
                    return@withContext null
                }

                // Base64 encode the image
                val imgBase64 = encodeBitmapToBase64Jpeg(imageData)

                // Build JSON body
                val requestJson = buildJsonObject {
                    putJsonArray("contents") {
                        addJsonObject {
                            putJsonArray("parts") {
                                addJsonObject { put("text", editPrompt) }
                                addJsonObject {
                                    putJsonObject("inline_data") {
                                        put("mime_type", "image/jpeg")
                                        put("data", imgBase64)
                                    }
                                }
                            }
                        }
                    }
                }.toString()

                val url = URL(GEMINI_IMAGE_URL)
                val connection = url.openConnection() as HttpURLConnection

                return@withContext try {
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("x-goog-api-key", apiKey)
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.doOutput = true

                    connection.outputStream.use { it.write(requestJson.toByteArray()) }

                    val responseCode = connection.responseCode
                    val responseBody = if (responseCode == HttpURLConnection.HTTP_OK) {
                        connection.inputStream.bufferedReader().use { it.readText() }
                    } else {
                        val errorBody =
                            connection.errorStream?.bufferedReader()?.use { it.readText() }
                                ?: "Unknown error"
                        throw IOException("HTTP $responseCode: $errorBody")
                    }

                    // Extract base64-encoded "data" field
                    val regex = """"data"\s*:\s*"([^"]+)"""".toRegex()
                    val match = regex.find(responseBody)
                    val base64Image = match?.groups?.get(1)?.value

                    if (base64Image != null) {
                        Base64.decode(
                            base64Image, BASE_64_FLAGS
                        )
                    } else {
                        Log.e(TAG, "❌ No image data found in response")
                        null
                    }
                } finally {
                    connection.disconnect()
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error editing image", e)
                null
            }
        }

    private fun encodeBitmapToBase64Jpeg(input: Bitmap): String {
        ByteArrayOutputStream().let {
            input.compress(Bitmap.CompressFormat.JPEG, 80, it)
            return android.util.Base64.encodeToString(it.toByteArray(), BASE_64_FLAGS)
        }
    }

    override suspend fun generateEmbedding(text: String): FloatArray {
        TODO("Not yet implemented")
    }

    override suspend fun generateEmbeddings(texts: List<String>): List<FloatArray> {
        TODO("Not yet implemented")
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



    // Data classes for JSON serialization
    @Serializable
    data class EmbeddingRequest(
        val content: Content
    )


    @Serializable
    data class Embedding(
        val values: List<Float>
    )
}