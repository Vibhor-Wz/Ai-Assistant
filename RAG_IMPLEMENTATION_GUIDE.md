# RAG (Retrieval-Augmented Generation) Implementation Guide

This guide explains the complete RAG system implementation for the Android Vector Database application using Gemini embeddings and ObjectBox vector search.

## Overview

The RAG system provides:
- **Text extraction** from PDFs and images using Firebase AI
- **Text chunking** for optimal embedding generation
- **Embedding generation** using Gemini's `gemini-embedding-001` model
- **Vector storage** in ObjectBox with built-in vector search
- **Semantic search** using cosine similarity
- **Modern UI** with Jetpack Compose

## Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Document      │───▶│  Text Chunking   │───▶│   Embeddings    │
│   Upload        │    │   Service        │    │   Generation    │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │                        │
                                ▼                        ▼
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   ObjectBox     │◀───│  Storage Service │◀───│   Gemini AI     │
│   Vector DB     │    │                  │    │   Service       │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │
                                ▼
┌─────────────────┐    ┌──────────────────┐
│   RAG Service   │◀───│  Search Query    │
│   (Orchestrator)│    │  Processing      │
└─────────────────┘    └──────────────────┘
```

## Key Components

### 1. Entity Classes

#### DocumentEntity
Stores document metadata and references to text chunks.

```kotlin
@Entity
data class DocumentEntity(
    @Id var id: Long = 0,
    @Index var name: String = "",
    var filePath: String = "",
    var fileSize: Long = 0L,
    var documentType: String = "",
    var uploadDate: Date = Date(),
    var description: String = "",
    var chunkCount: Int = 0,
    var processingStatus: String = "PENDING"
)
```

#### TextChunkEntity
Stores individual text chunks with their embeddings.

```kotlin
@Entity
data class TextChunkEntity(
    @Id var id: Long = 0,
    @Index var documentId: Long = 0L,
    var text: String = "",
    var embedding: FloatArray = floatArrayOf(),
    var chunkIndex: Int = 0,
    var startPosition: Int = 0,
    var endPosition: Int = 0,
    var processedDate: Date = Date(),
    var metadata: String = "",
    var textLength: Int = 0
)
```

### 2. Services

#### GeminiEmbeddingService
Generates embeddings using Gemini's `gemini-embedding-001` model.

```kotlin
class GeminiEmbeddingService(private val context: Context) {
    suspend fun generateEmbedding(text: String): FloatArray
    suspend fun generateEmbeddings(texts: List<String>): List<FloatArray>
    fun getEmbeddingDimension(): Int = 768
}
```

#### TextChunkingService
Splits text into optimal chunks for embedding generation.

```kotlin
class TextChunkingService {
    fun chunkText(text: String, config: ChunkingConfig = ChunkingConfig()): List<TextChunk>
}
```

#### EmbeddingStorageService
Manages ObjectBox operations for documents and embeddings.

```kotlin
class EmbeddingStorageService {
    suspend fun storeDocument(document: DocumentEntity): DocumentEntity
    suspend fun storeTextChunks(chunks: List<TextChunkEntity>): List<TextChunkEntity>
    suspend fun searchSimilarChunks(queryEmbedding: FloatArray, limit: Int, threshold: Float): List<SimilarityResult>
}
```

#### RagService
Main orchestrator that coordinates the entire RAG pipeline.

```kotlin
class RagService(private val context: Context) {
    suspend fun processDocument(documentName: String, extractedText: String, documentType: String, fileSize: Long, description: String = ""): ProcessingResult
    suspend fun searchDocuments(query: String, topK: Int = 5, similarityThreshold: Float = 0.7f): SearchResult
}
```

### 3. UI Components

#### RagSearchScreen
Main search interface with query input and results display.

#### DocumentUploadScreen
Document upload and processing interface.

#### RagViewModel
ViewModel managing RAG operations and state.

## Usage Examples

### 1. Processing a Document

```kotlin
val ragService = RagService(context)

val result = ragService.processDocument(
    documentName = "research_paper.pdf",
    extractedText = "Extracted text content...",
    documentType = "PDF",
    fileSize = 1024000L,
    description = "Machine learning research paper"
)

if (result.success) {
    println("Document processed with ${result.chunksProcessed} chunks")
}
```

### 2. Searching Documents

```kotlin
val searchResult = ragService.searchDocuments(
    query = "machine learning algorithms",
    topK = 5,
    similarityThreshold = 0.7f
)

searchResult.results.forEach { result ->
    println("Document: ${result.document?.name}")
    println("Similarity: ${result.similarity}")
    println("Text: ${result.chunk.text}")
}
```

### 3. Using the ViewModel

```kotlin
@Composable
fun MyScreen(ragViewModel: RagViewModel = viewModel()) {
    val documents by ragViewModel.documents.collectAsState()
    val uiState by ragViewModel.uiState.collectAsState()
    
    // Process document
    ragViewModel.processDocument(
        documentName = "document.pdf",
        extractedText = "Content...",
        documentType = "PDF",
        fileSize = 1000L
    )
    
    // Search documents
    ragViewModel.searchDocuments("query")
}
```

## Configuration

### 1. API Key Setup

Update the API key in `GeminiEmbeddingService`:

```kotlin
private fun getApiKey(): String {
    // Replace with your actual Gemini API key
    return "YOUR_GEMINI_API_KEY_HERE"
}
```

### 2. Chunking Configuration

Customize text chunking parameters:

```kotlin
val config = ChunkingConfig(
    chunkSize = 1000,        // Characters per chunk
    overlapSize = 200,       // Overlap between chunks
    preserveSentences = true, // Break at sentence boundaries
    preserveParagraphs = true // Break at paragraph boundaries
)
```

### 3. Search Parameters

Adjust search behavior:

```kotlin
val searchResult = ragService.searchDocuments(
    query = "your query",
    topK = 10,                    // Number of results
    similarityThreshold = 0.8f    // Minimum similarity score
)
```

## Performance Considerations

### 1. Embedding Generation
- Batch processing for multiple texts
- Rate limiting between API calls
- Caching of embeddings

### 2. Storage Optimization
- Efficient ObjectBox queries
- Indexed fields for fast lookups
- Vector similarity search optimization

### 3. Memory Management
- Lazy loading of embeddings
- Proper cleanup of resources
- Efficient text chunking

## Error Handling

The system includes comprehensive error handling:

```kotlin
try {
    val result = ragService.processDocument(...)
    if (!result.success) {
        // Handle processing failure
        println("Error: ${result.message}")
    }
} catch (e: RagService.RagException) {
    // Handle RAG-specific errors
    println("RAG Error: ${e.message}")
} catch (e: Exception) {
    // Handle general errors
    println("General Error: ${e.message}")
}
```

## Testing

### 1. Unit Tests
Test individual services in isolation:

```kotlin
@Test
fun testEmbeddingGeneration() {
    val service = GeminiEmbeddingService(context)
    val embedding = runBlocking { service.generateEmbedding("test text") }
    assertEquals(768, embedding.size)
}
```

### 2. Integration Tests
Test the complete RAG pipeline:

```kotlin
@Test
fun testDocumentProcessing() {
    val ragService = RagService(context)
    val result = runBlocking {
        ragService.processDocument("test.pdf", "content", "PDF", 1000L)
    }
    assertTrue(result.success)
    assertTrue(result.chunksProcessed > 0)
}
```

## Troubleshooting

### Common Issues

1. **API Key Errors**
   - Ensure Gemini API key is correctly configured
   - Check API key permissions and quotas

2. **Embedding Dimension Mismatch**
   - Verify embedding dimension is 768 for `gemini-embedding-001`
   - Check vector serialization/deserialization

3. **ObjectBox Errors**
   - Ensure ObjectBox is properly initialized
   - Check entity annotations and relationships

4. **Memory Issues**
   - Monitor embedding storage size
   - Implement proper cleanup for large documents

### Debug Logging

Enable debug logging to troubleshoot issues:

```kotlin
// Add to your Application class
if (BuildConfig.DEBUG) {
    Log.d("RAG_DEBUG", "Debug logging enabled")
}
```

## Future Enhancements

1. **Advanced Chunking Strategies**
   - Semantic chunking based on content
   - Dynamic chunk size based on content type

2. **Enhanced Search**
   - Hybrid search (semantic + keyword)
   - Query expansion and refinement

3. **Performance Optimizations**
   - Embedding caching
   - Incremental processing
   - Background processing

4. **Advanced Features**
   - Document summarization
   - Question answering
   - Multi-modal search (text + images)

## Conclusion

This RAG implementation provides a complete, production-ready system for document processing and semantic search. The modular architecture allows for easy extension and customization while maintaining high performance and reliability.

For questions or issues, refer to the individual service documentation or check the example implementations in the UI components.
