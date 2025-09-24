package com.bigcash.ai.vectordb.utils

import android.util.Log
import kotlin.math.sqrt

/**
 * Utility functions for vector operations and serialization.
 * This class provides helper methods for working with embeddings and vectors.
 */
object VectorUtils {
    
    private const val TAG = "VectorUtils"
    
    /**
     * Calculate cosine similarity between two vectors.
     * 
     * @param vector1 First vector
     * @param vector2 Second vector
     * @return Cosine similarity score (-1.0 to 1.0)
     */
    fun cosineSimilarity(vector1: FloatArray, vector2: FloatArray): Float {
        if (vector1.size != vector2.size) {
            throw IllegalArgumentException("Vectors must have the same dimension")
        }
        
        var dotProduct = 0.0f
        var norm1 = 0.0f
        var norm2 = 0.0f
        
        for (i in vector1.indices) {
            dotProduct += vector1[i] * vector2[i]
            norm1 += vector1[i] * vector1[i]
            norm2 += vector2[i] * vector2[i]
        }
        
        val magnitude = sqrt(norm1) * sqrt(norm2)
        return if (magnitude > 0) dotProduct / magnitude else 0.0f
    }
    
    /**
     * Calculate Euclidean distance between two vectors.
     * 
     * @param vector1 First vector
     * @param vector2 Second vector
     * @return Euclidean distance
     */
    fun euclideanDistance(vector1: FloatArray, vector2: FloatArray): Float {
        if (vector1.size != vector2.size) {
            throw IllegalArgumentException("Vectors must have the same dimension")
        }
        
        var sum = 0.0f
        for (i in vector1.indices) {
            val diff = vector1[i] - vector2[i]
            sum += diff * diff
        }
        
        return sqrt(sum)
    }
    
    /**
     * Calculate Manhattan distance between two vectors.
     * 
     * @param vector1 First vector
     * @param vector2 Second vector
     * @return Manhattan distance
     */
    fun manhattanDistance(vector1: FloatArray, vector2: FloatArray): Float {
        if (vector1.size != vector2.size) {
            throw IllegalArgumentException("Vectors must have the same dimension")
        }
        
        var sum = 0.0f
        for (i in vector1.indices) {
            sum += kotlin.math.abs(vector1[i] - vector2[i])
        }
        
        return sum
    }
    
    /**
     * Normalize a vector to unit length.
     * 
     * @param vector The vector to normalize
     * @return Normalized vector
     */
    fun normalize(vector: FloatArray): FloatArray {
        val magnitude = sqrt(vector.sumOf { (it * it).toDouble() }.toFloat())
        
        return if (magnitude > 0) {
            FloatArray(vector.size) { vector[it] / magnitude }
        } else {
            vector.copyOf()
        }
    }
    
    /**
     * Calculate the magnitude (length) of a vector.
     * 
     * @param vector The vector
     * @return Vector magnitude
     */
    fun magnitude(vector: FloatArray): Float {
        var sum = 0.0f
        for (value in vector) {
            sum += value * value
        }
        return sqrt(sum)
    }
    
    /**
     * Serialize a FloatArray to a comma-separated string.
     * 
     * @param vector The vector to serialize
     * @return Serialized string
     */
    fun serializeVector(vector: FloatArray): String {
        return vector.joinToString(",")
    }
    
    /**
     * Deserialize a comma-separated string to a FloatArray.
     * 
     * @param serialized The serialized string
     * @return Deserialized vector
     */
    fun deserializeVector(serialized: String): FloatArray {
        return if (serialized.isBlank()) {
            floatArrayOf()
        } else {
            try {
                serialized.split(",").map { it.trim().toFloat() }.toFloatArray()
            } catch (e: NumberFormatException) {
                Log.e(TAG, "Error deserializing vector: $serialized", e)
                floatArrayOf()
            }
        }
    }
    
    /**
     * Serialize a FloatArray to a Base64-encoded string.
     * This is more compact than comma-separated format.
     * 
     * @param vector The vector to serialize
     * @return Base64-encoded string
     */
    fun serializeVectorBase64(vector: FloatArray): String {
        val bytes = ByteArray(vector.size * 4) // 4 bytes per float
        for (i in vector.indices) {
            val bits = java.lang.Float.floatToIntBits(vector[i])
            bytes[i * 4] = (bits shr 24).toByte()
            bytes[i * 4 + 1] = (bits shr 16).toByte()
            bytes[i * 4 + 2] = (bits shr 8).toByte()
            bytes[i * 4 + 3] = bits.toByte()
        }
        return android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
    }
    
    /**
     * Deserialize a Base64-encoded string to a FloatArray.
     * 
     * @param serialized The Base64-encoded string
     * @return Deserialized vector
     */
    fun deserializeVectorBase64(serialized: String): FloatArray {
        return try {
            val bytes = android.util.Base64.decode(serialized, android.util.Base64.DEFAULT)
            val vector = FloatArray(bytes.size / 4)
            for (i in vector.indices) {
                val bits = ((bytes[i * 4].toInt() and 0xFF) shl 24) or
                          ((bytes[i * 4 + 1].toInt() and 0xFF) shl 16) or
                          ((bytes[i * 4 + 2].toInt() and 0xFF) shl 8) or
                          (bytes[i * 4 + 3].toInt() and 0xFF)
                vector[i] = java.lang.Float.intBitsToFloat(bits)
            }
            vector
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing Base64 vector: $serialized", e)
            floatArrayOf()
        }
    }
    
    /**
     * Check if two vectors are approximately equal within a tolerance.
     * 
     * @param vector1 First vector
     * @param vector2 Second vector
     * @param tolerance Tolerance for comparison
     * @return true if vectors are approximately equal
     */
    fun approximatelyEqual(vector1: FloatArray, vector2: FloatArray, tolerance: Float = 1e-6f): Boolean {
        if (vector1.size != vector2.size) return false
        
        for (i in vector1.indices) {
            if (kotlin.math.abs(vector1[i] - vector2[i]) > tolerance) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * Calculate the dot product of two vectors.
     * 
     * @param vector1 First vector
     * @param vector2 Second vector
     * @return Dot product
     */
    fun dotProduct(vector1: FloatArray, vector2: FloatArray): Float {
        if (vector1.size != vector2.size) {
            throw IllegalArgumentException("Vectors must have the same dimension")
        }
        
        var sum = 0.0f
        for (i in vector1.indices) {
            sum += vector1[i] * vector2[i]
        }
        
        return sum
    }
    
    /**
     * Add two vectors element-wise.
     * 
     * @param vector1 First vector
     * @param vector2 Second vector
     * @return Sum vector
     */
    fun add(vector1: FloatArray, vector2: FloatArray): FloatArray {
        if (vector1.size != vector2.size) {
            throw IllegalArgumentException("Vectors must have the same dimension")
        }
        
        return FloatArray(vector1.size) { vector1[it] + vector2[it] }
    }
    
    /**
     * Subtract vector2 from vector1 element-wise.
     * 
     * @param vector1 First vector
     * @param vector2 Second vector
     * @return Difference vector
     */
    fun subtract(vector1: FloatArray, vector2: FloatArray): FloatArray {
        if (vector1.size != vector2.size) {
            throw IllegalArgumentException("Vectors must have the same dimension")
        }
        
        return FloatArray(vector1.size) { vector1[it] - vector2[it] }
    }
    
    /**
     * Multiply a vector by a scalar.
     * 
     * @param vector The vector
     * @param scalar The scalar
     * @return Scaled vector
     */
    fun multiply(vector: FloatArray, scalar: Float): FloatArray {
        return FloatArray(vector.size) { vector[it] * scalar }
    }
    
    /**
     * Calculate the average of multiple vectors.
     * 
     * @param vectors List of vectors
     * @return Average vector
     */
    fun average(vectors: List<FloatArray>): FloatArray {
        if (vectors.isEmpty()) {
            throw IllegalArgumentException("Cannot calculate average of empty vector list")
        }
        
        val dimension = vectors.first().size
        if (vectors.any { it.size != dimension }) {
            throw IllegalArgumentException("All vectors must have the same dimension")
        }
        
        val sum = FloatArray(dimension)
        for (vector in vectors) {
            for (i in vector.indices) {
                sum[i] += vector[i]
            }
        }
        
        return multiply(sum, 1.0f / vectors.size)
    }
    
    /**
     * Find the most similar vector from a list of candidates.
     * 
     * @param queryVector The query vector
     * @param candidateVectors List of candidate vectors
     * @return Index of the most similar vector and its similarity score
     */
    fun findMostSimilar(queryVector: FloatArray, candidateVectors: List<FloatArray>): Pair<Int, Float> {
        if (candidateVectors.isEmpty()) {
            throw IllegalArgumentException("Candidate vectors list cannot be empty")
        }
        
        var bestIndex = 0
        var bestSimilarity = cosineSimilarity(queryVector, candidateVectors[0])
        
        for (i in 1 until candidateVectors.size) {
            val similarity = cosineSimilarity(queryVector, candidateVectors[i])
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity
                bestIndex = i
            }
        }
        
        return Pair(bestIndex, bestSimilarity)
    }
    
    /**
     * Validate that a vector has the expected dimension.
     * 
     * @param vector The vector to validate
     * @param expectedDimension The expected dimension
     * @return true if vector has the correct dimension
     */
    fun validateDimension(vector: FloatArray, expectedDimension: Int): Boolean {
        return vector.size == expectedDimension
    }
    
    /**
     * Check if a vector contains any NaN or infinite values.
     * 
     * @param vector The vector to check
     * @return true if vector contains invalid values
     */
    fun hasInvalidValues(vector: FloatArray): Boolean {
        return vector.any { it.isNaN() || it.isInfinite() }
    }
}