package com.bigcash.ai.vectordb.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bigcash.ai.vectordb.data.PdfEntity
import com.bigcash.ai.vectordb.repository.PdfRepository
import com.youtubetranscript.YouTubeTranscriptApi
import com.youtubetranscript.TranscriptException
import com.bigcash.ai.vectordb.service.FirebaseAiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for PDF management operations.
 * This class handles the UI state and business logic for PDF operations.
 */
class PdfViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PdfRepository(application)
    private val firebaseAiService = FirebaseAiService(application)

    // UI State
    private val _pdfs = MutableStateFlow<List<PdfEntity>>(emptyList())
    val pdfs: StateFlow<List<PdfEntity>> = _pdfs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _uploadSuccess = MutableStateFlow(false)
    val uploadSuccess: StateFlow<Boolean> = _uploadSuccess.asStateFlow()

    // YouTube Transcript State
    private val _isFetchingTranscript = MutableStateFlow(false)
    val isFetchingTranscript: StateFlow<Boolean> = _isFetchingTranscript.asStateFlow()

    private val _isSummarizingTranscript = MutableStateFlow(false)
    val isSummarizingTranscript: StateFlow<Boolean> = _isSummarizingTranscript.asStateFlow()

    private val _transcriptText = MutableStateFlow("")
    val transcriptText: StateFlow<String> = _transcriptText.asStateFlow()

    private val _summaryText = MutableStateFlow("")
    val summaryText: StateFlow<String> = _summaryText.asStateFlow()

    private val _transcriptError = MutableStateFlow<String?>(null)
    val transcriptError: StateFlow<String?> = _transcriptError.asStateFlow()

    init {
        loadAllPdfs()
    }

    /**
     * Load all PDFs from the database.
     */
    fun loadAllPdfs() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val pdfList = repository.getAllPdfs()
                _pdfs.value = pdfList
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load PDFs: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Save a PDF to the database.
     *
     * @param name The name of the PDF
     * @param data The PDF file data
     * @param description Optional description
     */
    fun savePdf(name: String, data: ByteArray, description: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _uploadSuccess.value = false

            try {
                val pdfEntity = repository.createPdfEntity(name, data, description)
                pdfEntity?.let{
                    repository.savePdf(it)
                    _uploadSuccess.value = true
                    loadAllPdfs()
                }
                if(pdfEntity == null) {
                    _errorMessage.value = "Failed to save PDF"
                }

            } catch (e: Exception) {
                _errorMessage.value = "Failed to save PDF: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Delete a PDF from the database.
     *
     * @param id The ID of the PDF to delete
     */
    fun deletePdf(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                repository.deletePdf(id)
                loadAllPdfs() // Refresh the list
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete PDF: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Search PDFs by name.
     *
     * @param query The search query
     */
    fun searchPdfs(query: String) {
        if (query.isEmpty()) {
            loadAllPdfs()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val searchResults = repository.searchPdfsByName(query)
                _pdfs.value = searchResults
            } catch (e: Exception) {
                _errorMessage.value = "Failed to search PDFs: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Clear upload success state.
     */
    fun clearUploadSuccess() {
        _uploadSuccess.value = false
    }

    /**
     * Get a PDF by ID.
     *
     * @param id The ID of the PDF
     * @return The PDF entity or null if not found
     */
    suspend fun getPdfById(id: Long): PdfEntity? {
        return repository.getPdfById(id)
    }
    
    /**
     * Get the original file from local storage.
     *
     * @param pdfEntity The PDF entity containing the local file path
     * @return The original file if it exists, null otherwise
     */
    fun getOriginalFile(pdfEntity: PdfEntity): java.io.File? {
        return repository.getOriginalFile(pdfEntity)
    }
    
    /**
     * Check if the original file exists in local storage.
     *
     * @param pdfEntity The PDF entity containing the local file path
     * @return True if the file exists, false otherwise
     */
    fun hasOriginalFile(pdfEntity: PdfEntity): Boolean {
        return repository.hasOriginalFile(pdfEntity)
    }
    
    /**
     * Get the size of the original file in local storage.
     *
     * @param pdfEntity The PDF entity containing the local file path
     * @return The file size in bytes, or -1 if the file doesn't exist
     */
    fun getOriginalFileSize(pdfEntity: PdfEntity): Long {
        return repository.getOriginalFileSize(pdfEntity)
    }
    
    /**
     * Clear all data from the database.
     * This is useful when schema changes require a fresh start.
     * WARNING: This will delete all data permanently.
     */
    fun clearAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                repository.clearAllData()
                _pdfs.value = emptyList()
                _uploadSuccess.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Failed to clear data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Check if the database is empty.
     * 
     * @return True if no data exists, false otherwise
     */
    fun isDatabaseEmpty(): Boolean {
        return repository.isDatabaseEmpty()
    }

    /**
     * Fetch YouTube transcript and generate AI summary.
     *
     * @param youtubeUrl The YouTube video URL
     * @param language The language code for the transcript (default: "en")
     */
    fun fetchYouTubeTranscript(youtubeUrl: String, language: String = "en") {
        viewModelScope.launch {
            _isFetchingTranscript.value = true
            _transcriptError.value = null
            _transcriptText.value = ""
            _summaryText.value = ""

            try {
                // Extract video ID from URL
                val videoId = YouTubeTranscriptApi.extractVideoId(youtubeUrl)
                
                // Fetch the raw transcript using the new module
                val transcriptSegments = YouTubeTranscriptApi.getTranscript(
                    videoId = videoId,
                    languages = listOf(language)
                )
                
                // Convert segments to text
                val transcript = transcriptSegments.joinToString(" ") { it.text }
                _transcriptText.value = transcript

                // Start summarization process
                _isSummarizingTranscript.value = true
                try {
                    val summary = firebaseAiService.summarizeYouTubeTranscript(transcript)
                    _summaryText.value = summary
                } catch (e: Exception) {
                    // If summarization fails, we still have the raw transcript
                    _transcriptError.value = "Failed to generate summary: ${e.message}"
                } finally {
                    _isSummarizingTranscript.value = false
                }

            } catch (e: TranscriptException) {
                _transcriptError.value = "Failed to fetch transcript: ${e.message}"
            } catch (e: Exception) {
                _transcriptError.value = "Failed to fetch transcript: ${e.message}"
            } finally {
                _isFetchingTranscript.value = false
            }
        }
    }

    /**
     * Clear YouTube transcript data.
     */
    fun clearTranscriptData() {
        _transcriptText.value = ""
        _summaryText.value = ""
        _transcriptError.value = null
        _isFetchingTranscript.value = false
        _isSummarizingTranscript.value = false
    }

    /**
     * Get the current transcript text.
     */
    fun getTranscriptText(): String {
        return _transcriptText.value
    }

    /**
     * Get the current summary text.
     */
    fun getSummaryText(): String {
        return _summaryText.value
    }

    /**
     * Check if transcript is being fetched.
     */
    fun isTranscriptLoading(): Boolean {
        return _isFetchingTranscript.value || _isSummarizingTranscript.value
    }

    
    /**
     * Clean up resources when ViewModel is destroyed.
     */
    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }
}
