package com.bigcash.ai.vectordb.data

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index
import java.util.Date

/**
 * Entity class representing a text chunk with its embedding stored in ObjectBox database.
 * This entity contains the actual text content and its vector embedding for similarity search.
 */
@Entity
data class TextChunkEntity(
    @Id var id: Long = 0,
    
    /**
     * Reference to the parent document
     */
    @Index var documentId: Long = 0L,
    
    /**
     * The actual text content of this chunk
     */
    var text: String = "",
    
    /**
     * Vector embedding of the text content (768 dimensions for Gemini embedding-001)
     */
    var embedding: FloatArray = floatArrayOf(),
    
    /**
     * Chunk index within the document (for ordering)
     */
    var chunkIndex: Int = 0,
    
    /**
     * Start position of this chunk in the original document
     */
    var startPosition: Int = 0,
    
    /**
     * End position of this chunk in the original document
     */
    var endPosition: Int = 0,
    
    /**
     * Timestamp when this chunk was processed
     */
    var processedDate: Date = Date(),
    
    /**
     * Optional metadata for this chunk (e.g., section, heading)
     */
    var metadata: String = "",
    
    /**
     * Text length in characters
     */
    var textLength: Int = 0
) {
    /**
     * Override equals and hashCode to properly handle FloatArray comparisons
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TextChunkEntity

        if (id != other.id) return false
        if (documentId != other.documentId) return false
        if (text != other.text) return false
        if (!embedding.contentEquals(other.embedding)) return false
        if (chunkIndex != other.chunkIndex) return false
        if (startPosition != other.startPosition) return false
        if (endPosition != other.endPosition) return false
        if (processedDate != other.processedDate) return false
        if (metadata != other.metadata) return false
        if (textLength != other.textLength) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + documentId.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + embedding.contentHashCode()
        result = 31 * result + chunkIndex
        result = 31 * result + startPosition
        result = 31 * result + endPosition
        result = 31 * result + processedDate.hashCode()
        result = 31 * result + metadata.hashCode()
        result = 31 * result + textLength
        return result
    }
}
