package com.bigcash.ai.vectordb.service

import android.util.Log

/**
 * Service for splitting text into chunks suitable for embedding generation.
 * This service handles text chunking strategies for optimal RAG performance.
 */
class TextChunkingService {
    
    companion object {
        private const val TAG = "TextChunkingService"
        private const val DEFAULT_CHUNK_SIZE = 1000 // characters
        private const val DEFAULT_OVERLAP_SIZE = 200 // characters
        private const val MIN_CHUNK_SIZE = 100 // minimum chunk size
        private const val MAX_CHUNK_SIZE = 2000 // maximum chunk size
    }
    
    /**
     * Configuration for text chunking.
     */
    data class ChunkingConfig(
        val chunkSize: Int = DEFAULT_CHUNK_SIZE,
        val overlapSize: Int = DEFAULT_OVERLAP_SIZE,
        val minChunkSize: Int = MIN_CHUNK_SIZE,
        val maxChunkSize: Int = MAX_CHUNK_SIZE,
        val preserveSentences: Boolean = true,
        val preserveParagraphs: Boolean = true
    )
    
    /**
     * Represents a text chunk with metadata.
     */
    data class TextChunk(
        val text: String,
        val startIndex: Int,
        val endIndex: Int,
        val chunkIndex: Int,
        val metadata: String = ""
    )
    
    /**
     * Split text into chunks using the specified configuration.
     * 
     * @param text The text to chunk
     * @param config The chunking configuration
     * @return List of TextChunk objects
     */
    fun chunkText(text: String, config: ChunkingConfig = ChunkingConfig()): List<TextChunk> {
        Log.d(TAG, "Chunking text of length: ${text.length}")
        
        if (text.isBlank()) {
            return emptyList()
        }
        
        val chunks = mutableListOf<TextChunk>()
        var currentIndex = 0
        var chunkIndex = 0
        
        while (currentIndex < text.length) {
            val chunkEnd = findChunkEnd(text, currentIndex, config)
            val chunkText = text.substring(currentIndex, chunkEnd).trim()
            
            if (chunkText.length >= config.minChunkSize) {
                chunks.add(
                    TextChunk(
                        text = chunkText,
                        startIndex = currentIndex,
                        endIndex = chunkEnd,
                        chunkIndex = chunkIndex,
                        metadata = extractMetadata(chunkText)
                    )
                )
                chunkIndex++
            }
            
            // Move to next chunk with overlap
            currentIndex = if (chunkEnd >= text.length) {
                break
            } else {
                maxOf(currentIndex + config.chunkSize - config.overlapSize, chunkEnd - config.overlapSize)
            }
        }
        
        Log.d(TAG, "Created ${chunks.size} chunks")
        return chunks
    }
    
    /**
     * Find the optimal end position for a chunk.
     * 
     * @param text The full text
     * @param startIndex The start index of the current chunk
     * @param config The chunking configuration
     * @return The end index for the chunk
     */
    private fun findChunkEnd(text: String, startIndex: Int, config: ChunkingConfig): Int {
        val maxEndIndex = minOf(startIndex + config.maxChunkSize, text.length)
        val preferredEndIndex = minOf(startIndex + config.chunkSize, text.length)
        
        if (preferredEndIndex >= text.length) {
            return text.length
        }
        
        if (!config.preserveSentences && !config.preserveParagraphs) {
            return preferredEndIndex
        }
        
        // Try to break at sentence boundaries first
        if (config.preserveSentences) {
            val sentenceEnd = findSentenceEnd(text, startIndex, preferredEndIndex, maxEndIndex)
            if (sentenceEnd > startIndex + config.minChunkSize) {
                return sentenceEnd
            }
        }
        
        // Try to break at paragraph boundaries
        if (config.preserveParagraphs) {
            val paragraphEnd = findParagraphEnd(text, startIndex, preferredEndIndex, maxEndIndex)
            if (paragraphEnd > startIndex + config.minChunkSize) {
                return paragraphEnd
            }
        }
        
        // Fall back to word boundary
        val wordEnd = findWordEnd(text, startIndex, preferredEndIndex, maxEndIndex)
        return if (wordEnd > startIndex + config.minChunkSize) {
            wordEnd
        } else {
            preferredEndIndex
        }
    }
    
    /**
     * Find the end of a sentence within the given range.
     */
    private fun findSentenceEnd(text: String, startIndex: Int, preferredEnd: Int, maxEnd: Int): Int {
        val searchEnd = minOf(preferredEnd + 100, maxEnd) // Look a bit beyond preferred end
        
        for (i in preferredEnd downTo startIndex) {
            if (i < text.length && text[i] in ".!?") {
                // Check if this is actually the end of a sentence
                if (i + 1 >= text.length || text[i + 1].isWhitespace()) {
                    return i + 1
                }
            }
        }
        
        return startIndex
    }
    
    /**
     * Find the end of a paragraph within the given range.
     */
    private fun findParagraphEnd(text: String, startIndex: Int, preferredEnd: Int, maxEnd: Int): Int {
        val searchEnd = minOf(preferredEnd + 100, maxEnd)
        
        for (i in preferredEnd downTo startIndex) {
            if (i < text.length && text[i] == '\n') {
                // Check if this is a paragraph break (double newline or followed by whitespace)
                if (i + 1 >= text.length || text[i + 1] == '\n' || text[i + 1].isWhitespace()) {
                    return i + 1
                }
            }
        }
        
        return startIndex
    }
    
    /**
     * Find the end of a word within the given range.
     */
    private fun findWordEnd(text: String, startIndex: Int, preferredEnd: Int, maxEnd: Int): Int {
        val searchEnd = minOf(preferredEnd + 50, maxEnd)
        
        for (i in preferredEnd downTo startIndex) {
            if (i < text.length && text[i].isWhitespace()) {
                return i
            }
        }
        
        return startIndex
    }
    
    /**
     * Extract metadata from a text chunk (e.g., headings, keywords).
     */
    private fun extractMetadata(chunkText: String): String {
        val metadata = mutableListOf<String>()
        
        // Extract potential headings (lines that are short and end with colon or are all caps)
        val lines = chunkText.split('\n').take(3) // Check first 3 lines
        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.length < 100 && (trimmed.endsWith(':') || trimmed.all { it.isUpperCase() || it.isWhitespace() })) {
                metadata.add("heading: $trimmed")
            }
        }
        
        // Extract potential keywords (words that appear multiple times)
        val words = chunkText.lowercase()
            .split(Regex("\\W+"))
            .filter { it.length > 3 }
            .groupingBy { it }
            .eachCount()
            .filter { it.value > 1 }
            .keys
            .take(5)
        
        if (words.isNotEmpty()) {
            metadata.add("keywords: ${words.joinToString(", ")}")
        }
        
        return metadata.joinToString("; ")
    }
    
    /**
     * Get default chunking configuration optimized for RAG.
     */
    fun getDefaultConfig(): ChunkingConfig {
        return ChunkingConfig(
            chunkSize = DEFAULT_CHUNK_SIZE,
            overlapSize = DEFAULT_OVERLAP_SIZE,
            preserveSentences = true,
            preserveParagraphs = true
        )
    }
    
    /**
     * Get chunking configuration optimized for small documents.
     */
    fun getSmallDocumentConfig(): ChunkingConfig {
        return ChunkingConfig(
            chunkSize = 500,
            overlapSize = 100,
            preserveSentences = true,
            preserveParagraphs = false
        )
    }
    
    /**
     * Get chunking configuration optimized for large documents.
     */
    fun getLargeDocumentConfig(): ChunkingConfig {
        return ChunkingConfig(
            chunkSize = 1500,
            overlapSize = 300,
            preserveSentences = true,
            preserveParagraphs = true
        )
    }
}
