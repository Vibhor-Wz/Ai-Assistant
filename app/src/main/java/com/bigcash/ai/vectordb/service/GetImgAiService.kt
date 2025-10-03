package com.bigcash.ai.vectordb.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * GetImg AI service for image-to-image generation using Stable Diffusion XL model.
 * Provides image editing capabilities similar to Firebase AI but using GetImg AI's API.
 */
class GetImgAiService(private val context: Context) {

    companion object {
        private const val TAG = "GetImgAiService"
        private const val VECTOR_DEBUG_TAG = "GETIMG_AI_DEBUG"
        private const val API_BASE_URL = "https://api.getimg.ai/v1/stable-diffusion/image-to-image"
        private const val MODEL = "realistic-vision-v1-3"
        private const val CONTROLNET = "none"
    }

    private var apiKey: String = ""

    init {
        Log.d(VECTOR_DEBUG_TAG, "🤖 GetImgAiService: Initializing GetImg AI service")
        // Initialize with a default API key - replace with your actual GetImg AI API key
        apiKey = getDefaultApiKey()
    }

    /**
     * Get default API key for GetImg AI.
     * TODO: Replace with your actual GetImg AI API key
     */
    private fun getDefaultApiKey(): String {
        // Use a placeholder API key - replace with your actual GetImg AI API key
        val defaultApiKey = "key-5MMtCNmRBJmJ4VqNjbSbISbDiZ6S7G6Bw0htWyLZf6DfY7vPR2Gx1BdzCuyyzWNZSn0ipmpediC5h6beu75QDqHovrURtpx"
        Log.d(VECTOR_DEBUG_TAG, "🔑 GetImgAiService: Using default API key: ${defaultApiKey.take(10)}...")
        return defaultApiKey
    }

    /**
     * Set the API key for GetImg AI.
     * @param newApiKey The GetImg AI API key
     */
    fun setApiKey(newApiKey: String) {
        Log.d(VECTOR_DEBUG_TAG, "🔄 GetImgAiService: Updating API key to: ${newApiKey.take(10)}...")
        apiKey = newApiKey
    }

    /**
     * Generate edited image from existing image using GetImg AI Stable Diffusion XL model.
     *
     * @param imageData Original image as byte array
     * @param prompt Text prompt describing the desired transformation
     * @param strength Strength of the transformation (0.0 to 1.0, default: 0.8)
     * @param guidanceScale Guidance scale for the generation (1.0 to 20.0, default: 7.5)
     * @param numInferenceSteps Number of inference steps (1 to 50, default: 20)
     * @param negativePrompt Negative prompt to avoid certain elements (optional)
     * @return Generated edited image as Bitmap, or null if failed
     */
    suspend fun editImage(
        imageData: ByteArray,
        prompt: String,
        strength: Float = 0.1f, // Reduced from 0.8f to preserve original content
        guidanceScale: Float = 7.5f,
        numInferenceSteps: Int = 20,
        negativePrompt: String? = null
    ): Bitmap? = withContext(Dispatchers.IO) {
        Log.d(VECTOR_DEBUG_TAG, "🎨 GetImgAiService: Starting image editing with GetImg AI")
        Log.d(VECTOR_DEBUG_TAG, "📝 GetImgAiService: Prompt: '$prompt'")
        Log.d(VECTOR_DEBUG_TAG, "⚙️ GetImgAiService: Strength: $strength, Guidance: $guidanceScale, Steps: $numInferenceSteps")

        try {
            // Validate API key
            if (apiKey.isEmpty() || apiKey == "YOUR_GETIMG_AI_API_KEY_HERE") {
                Log.e(VECTOR_DEBUG_TAG, "❌ GetImgAiService: API key not set. Please call setApiKey() with your GetImg AI API key")
                return@withContext null
            }

            // Convert image to base64
            val base64Image = convertImageToBase64(imageData)
            if (base64Image == null) {
                Log.e(VECTOR_DEBUG_TAG, "❌ GetImgAiService: Failed to convert image to base64")
                return@withContext null
            }

            Log.d(VECTOR_DEBUG_TAG, "📤 GetImgAiService: Image converted to base64, length: ${base64Image.length}")

            // Validate image before sending
            if (!isValidImageForApi(base64Image)) {
                Log.e(VECTOR_DEBUG_TAG, "❌ GetImgAiService: Image validation failed")
                return@withContext null
            }

            // Prepare request body
            val requestBody = JSONObject().apply {
                put("image", base64Image)
                put("prompt", prompt)
                put("model", MODEL)
                put("controlnet", CONTROLNET)
                put("strength", strength)
                put("guidance_scale", guidanceScale)
                put("num_inference_steps", numInferenceSteps)
                
                // Use default negative prompt to preserve person if none provided
                val finalNegativePrompt = negativePrompt ?: "blurry, low quality, distorted face, deformed person, changed person, different person, altered face, bad anatomy, different identity, face swap, identity change, person replacement"
                put("negative_prompt", finalNegativePrompt)
            }
            val create =
                RequestBody.create("application/json".toMediaTypeOrNull(), requestBody.toString())

            Log.d(VECTOR_DEBUG_TAG, "🚀 GetImgAiService: Submitting request to GetImg AI")
            Log.d(VECTOR_DEBUG_TAG, "📋 GetImgAiService: Request body: ${requestBody.toString()}")
            Log.d(VECTOR_DEBUG_TAG, "📊 GetImgAiService: Base64 image length: ${base64Image.length}")
            Log.d(VECTOR_DEBUG_TAG, "📊 GetImgAiService: Prompt length: ${prompt.length}")

            // Make API request
            val response = makeApiRequest(create)
            if (response != null) {
                Log.d(VECTOR_DEBUG_TAG, "✅ GetImgAiService: Image editing completed successfully")
                return@withContext response
            } else {
                Log.e(VECTOR_DEBUG_TAG, "❌ GetImgAiService: API request failed")
                return@withContext null
            }

        } catch (e: Exception) {
            Log.e(VECTOR_DEBUG_TAG, "❌ GetImgAiService: Error during image editing", e)
            return@withContext null
        }
    }

    /**
     * Generate multiple variations of an image with different prompts.
     *
     * @param imageData Original image as byte array
     * @param prompts List of prompts for different variations
     * @param strength Strength of the transformation for all variations
     * @return List of generated image variations as Bitmaps
     */
    suspend fun generateMultipleImageVariations(
        imageData: ByteArray,
        prompts: List<String>,
        strength: Float = 0.3f // Reduced to preserve original content
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        Log.d(VECTOR_DEBUG_TAG, "🎨 GetImgAiService: Generating ${prompts.size} image variations")

        val variations = mutableListOf<Bitmap>()

        try {
            for ((index, prompt) in prompts.withIndex()) {
                Log.d(VECTOR_DEBUG_TAG, "🔄 GetImgAiService: Generating variation ${index + 1}/${prompts.size}")
                
                val variation = editImage(imageData, prompt, strength)
                if (variation != null) {
                    variations.add(variation)
                    Log.d(VECTOR_DEBUG_TAG, "✅ GetImgAiService: Variation ${index + 1} generated successfully")
                } else {
                    Log.w(VECTOR_DEBUG_TAG, "⚠️ GetImgAiService: Failed to generate variation ${index + 1}")
                }

                // Add delay between requests to avoid rate limiting
                if (index < prompts.size - 1) {
                    kotlinx.coroutines.delay(1000)
                }
            }

            Log.d(VECTOR_DEBUG_TAG, "✅ GetImgAiService: Generated ${variations.size} variations out of ${prompts.size} requested")
            return@withContext variations

        } catch (e: Exception) {
            Log.e(VECTOR_DEBUG_TAG, "❌ GetImgAiService: Error generating multiple variations", e)
            return@withContext emptyList()
        }
    }



    /**
     * Convert image byte array to base64 string.
     *
     * @param imageData Image as byte array
     * @return Base64 encoded image string, or null if conversion fails
     */
    private fun convertImageToBase64(imageData: ByteArray): String? {
        return try {
            // First, determine the image format and convert to JPEG for consistency
            val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            if (bitmap == null) {
                Log.e(VECTOR_DEBUG_TAG, "❌ GetImgAiService: Failed to decode image from byte array")
                return null
            }

            Log.d(VECTOR_DEBUG_TAG, "📐 GetImgAiService: Original image dimensions: ${bitmap.width}x${bitmap.height}")

            // Resize image if it's too large (GetImg AI might have size limits)
            val resizedBitmap = resizeImageIfNeeded(bitmap)
            Log.d(VECTOR_DEBUG_TAG, "📐 GetImgAiService: Resized image dimensions: ${resizedBitmap.width}x${resizedBitmap.height}")

            // Convert bitmap to JPEG byte array for consistent format
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream) // Reduced quality to reduce size
            val jpegData = outputStream.toByteArray()

            Log.d(VECTOR_DEBUG_TAG, "📊 GetImgAiService: JPEG data size: ${jpegData.size} bytes")

            // Convert to base64
            val base64String = Base64.encodeToString(jpegData, Base64.NO_WRAP)
            
            Log.d(VECTOR_DEBUG_TAG, "📸 GetImgAiService: Image converted to base64, size: ${base64String.length} chars")
            base64String
        } catch (e: Exception) {
            Log.e(VECTOR_DEBUG_TAG, "❌ GetImgAiService: Error converting image to base64", e)
            null
        }
    }

    /**
     * Resize image if it's too large for the API.
     * GetImg AI might have dimension limits.
     *
     * @param bitmap Original bitmap
     * @return Resized bitmap if needed, original bitmap otherwise
     */
    private fun resizeImageIfNeeded(bitmap: Bitmap): Bitmap {
        val maxWidth = 1024
        val maxHeight = 1024
        
        val width = bitmap.width
        val height = bitmap.height
        
        // If image is within limits, return original
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }
        
        // Calculate new dimensions maintaining aspect ratio
        val scale = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        Log.d(VECTOR_DEBUG_TAG, "🔄 GetImgAiService: Resizing image from ${width}x${height} to ${newWidth}x${newHeight}")
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Validate if the base64 image is suitable for the API.
     *
     * @param base64Image Base64 encoded image string
     * @return true if image is valid, false otherwise
     */
    private fun isValidImageForApi(base64Image: String): Boolean {
        return try {
            // Check if base64 string is not empty
            if (base64Image.isEmpty()) {
                Log.e(VECTOR_DEBUG_TAG, "❌ GetImgAiService: Base64 image is empty")
                return false
            }

            // Check if base64 string is not too large (API might have size limits)
            if (base64Image.length > 10_000_000) { // 10MB limit
                Log.e(VECTOR_DEBUG_TAG, "❌ GetImgAiService: Base64 image too large: ${base64Image.length} chars")
                return false
            }

            // Try to decode the base64 to verify it's valid
            val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
            if (imageBytes.isEmpty()) {
                Log.e(VECTOR_DEBUG_TAG, "❌ GetImgAiService: Decoded image bytes are empty")
                return false
            }

            // Try to decode as bitmap to verify it's a valid image
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            if (bitmap == null) {
                Log.e(VECTOR_DEBUG_TAG, "❌ GetImgAiService: Failed to decode base64 as bitmap")
                return false
            }

            // Check dimensions
            if (bitmap.width < 64 || bitmap.height < 64) {
                Log.e(VECTOR_DEBUG_TAG, "❌ GetImgAiService: Image too small: ${bitmap.width}x${bitmap.height}")
                return false
            }

            if (bitmap.width > 2048 || bitmap.height > 2048) {
                Log.e(VECTOR_DEBUG_TAG, "❌ GetImgAiService: Image too large: ${bitmap.width}x${bitmap.height}")
                return false
            }

            Log.d(VECTOR_DEBUG_TAG, "✅ GetImgAiService: Image validation passed - ${bitmap.width}x${bitmap.height}, ${imageBytes.size} bytes")
            true

        } catch (e: Exception) {
            Log.e(VECTOR_DEBUG_TAG, "❌ GetImgAiService: Image validation error", e)
            false
        }
    }

    /**
     * Make API request to GetImg AI service.
     *
     * @param requestBody JSON request body as string
     * @return Generated image as Bitmap, or null if failed
     */
    private suspend fun makeApiRequest(requestBody: RequestBody): Bitmap? = withContext(Dispatchers.IO) {
        try {
            Log.d(VECTOR_DEBUG_TAG, "🌐 GetImgAiService: Making API request to GetImg AI")
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(API_BASE_URL)
                .post(requestBody)
                .addHeader("accept", "application/json")
                .addHeader("content-type", "application/json")
                .addHeader("authorization", "Bearer $apiKey")
                .build()

            // Get response
            val response = client.newCall(request).execute()
            Log.d(VECTOR_DEBUG_TAG, "📡 GetImgAiService: Response code: ${response.code}")

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d(VECTOR_DEBUG_TAG, "📥 GetImgAiService: Response body: $responseBody")
                
                if (responseBody != null) {
                    // Parse response JSON
                    val responseJson = JSONObject(responseBody)
                    val base64Image = responseJson.optString("image", "")
                    
                    if (base64Image.isNotEmpty()) {
                        Log.d(VECTOR_DEBUG_TAG, "🖼️ GetImgAiService: Base64 image data length: ${base64Image.length}")
                        
                        // Decode base64 image data directly to Bitmap
                        val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                        val imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        
                        if (imageBitmap != null) {
                            Log.d(VECTOR_DEBUG_TAG, "✅ GetImgAiService: Image decoded successfully from base64")
                            return@withContext imageBitmap
                        } else {
                            Log.e(VECTOR_DEBUG_TAG, "❌ GetImgAiService: Failed to decode base64 image data")
                        }
                    } else {
                        Log.e(VECTOR_DEBUG_TAG, "❌ GetImgAiService: No image data found in response")
                    }
                } else {
                    Log.e(VECTOR_DEBUG_TAG, "❌ GetImgAiService: Empty response body")
                }
            } else {
                val errorBody = response.body?.string()
                Log.e(VECTOR_DEBUG_TAG, "❌ GetImgAiService: API request failed with code ${response.code}")
                Log.e(VECTOR_DEBUG_TAG, "❌ GetImgAiService: Error response: $errorBody")
                
                // Try to parse error details
                try {
                    val errorJson = JSONObject(errorBody ?: "{}")
                    val error = errorJson.optJSONObject("error")
                    if (error != null) {
                        val errorType = error.optString("type", "unknown")
                        val errorCode = error.optString("code", "unknown")
                        val errorMessage = error.optString("message", "unknown")
                        val errorParam = error.optString("param", "unknown")
                        Log.e(VECTOR_DEBUG_TAG, "❌ GetImgAiService: Error details - Type: $errorType, Code: $errorCode, Message: $errorMessage, Param: $errorParam")
                    }
                } catch (e: Exception) {
                    Log.e(VECTOR_DEBUG_TAG, "❌ GetImgAiService: Failed to parse error response", e)
                }
            }

            return@withContext null

        } catch (e: Exception) {
            Log.e(VECTOR_DEBUG_TAG, "❌ GetImgAiService: Error making API request", e)
            return@withContext null
        }
    }



}
