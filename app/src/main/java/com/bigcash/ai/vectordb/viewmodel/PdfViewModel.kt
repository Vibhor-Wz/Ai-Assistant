package com.bigcash.ai.vectordb.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bigcash.ai.vectordb.data.PdfEntity
import com.bigcash.ai.vectordb.repository.PdfRepository
import com.youtubetranscript.YouTubeTranscriptApi
import com.youtubetranscript.TranscriptException
import com.bigcash.ai.vectordb.service.FirebaseAiService
import com.bigcash.ai.vectordb.service.SpeechRecognitionManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for PDF management operations.
 * This class handles the UI state and business logic for PDF operations.
 */
class PdfViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "PdfViewModel"
    }

    private val repository = PdfRepository(application)
    private val firebaseAiService = FirebaseAiService(application)
    private var speechRecognitionManager: SpeechRecognitionManager? = null

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

    // Speech Recognition State
    private val _isRecognizingSpeech = MutableStateFlow(false)
    val isRecognizingSpeech: StateFlow<Boolean> = _isRecognizingSpeech.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _isGeneratingSpeechSummary = MutableStateFlow(false)
    val isGeneratingSpeechSummary: StateFlow<Boolean> = _isGeneratingSpeechSummary.asStateFlow()

    private val _speechSummaryText = MutableStateFlow("")
    val speechSummaryText: StateFlow<String> = _speechSummaryText.asStateFlow()

    private val _speechError = MutableStateFlow<String?>(null)
    val speechError: StateFlow<String?> = _speechError.asStateFlow()

    // Continuous speech recognition state
    private val _isContinuousMode = MutableStateFlow(false)
    val isContinuousMode: StateFlow<Boolean> = _isContinuousMode.asStateFlow()

    init {
        Log.d(TAG, "🚀 PdfViewModel: Initializing ViewModel")
        loadAllPdfs()
        initializeSpeechRecognition()
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
     * Initialize speech recognition manager.
     */
    private fun initializeSpeechRecognition() {
        try {
            Log.d(TAG, "🎤 PdfViewModel: Initializing speech recognition")
            
            speechRecognitionManager = SpeechRecognitionManager(
                context = getApplication(),
                onResult = { recognizedText ->
                    Log.d(TAG, "🎤 PdfViewModel: Speech recognized: '$recognizedText'")
                    // Only process non-empty text
                    if (recognizedText.isNotEmpty()) {
                        // Accumulate text in continuous mode
                        if (_isContinuousMode.value) {
                            val currentText = _recognizedText.value
                            _recognizedText.value = if (currentText.isEmpty()) {
                                recognizedText
                            } else {
                                "$currentText $recognizedText"
                            }
                            Log.d(TAG, "📝 PdfViewModel: Accumulated text: '${_recognizedText.value}'")
                        } else {
                            _recognizedText.value = recognizedText
                            processRecognizedSpeech(recognizedText)
                        }
                    } else {
                        Log.d(TAG, "📝 PdfViewModel: Empty recognized text - skipping")
                    }
                },
                onError = { error ->
                    Log.e(TAG, "❌ PdfViewModel: Speech recognition error: $error")
                    _speechError.value = error
                    _isRecognizingSpeech.value = false
                    // Don't reset continuous mode on error, let it handle restart
                },
                onStart = {
                    Log.d(TAG, "🎤 PdfViewModel: Speech recognition started")
                    _isRecognizingSpeech.value = true
                    _speechError.value = null
                },
                onEnd = {
                    Log.d(TAG, "🎤 PdfViewModel: Speech recognition ended")
                    _isRecognizingSpeech.value = false
                    // Only reset continuous mode if manually stopped
                    if (!_isContinuousMode.value) {
                        _isContinuousMode.value = false
                    }
                }
            )
            
            Log.d(TAG, "✅ PdfViewModel: Speech recognition initialized successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ PdfViewModel: Error initializing speech recognition", e)
            _speechError.value = "Failed to initialize speech recognition: ${e.message}"
        }
    }

    /**
     * Start continuous speech recognition.
     */
    fun startContinuousSpeechRecognition() {
        try {
            Log.d(TAG, "🎤 PdfViewModel: Starting continuous speech recognition")
            
            if (speechRecognitionManager == null) {
                Log.e(TAG, "❌ PdfViewModel: Speech recognition not initialized")
                _speechError.value = "Speech recognition not initialized"
                return
            }
            
            if (!speechRecognitionManager!!.isRecognitionAvailable()) {
                Log.e(TAG, "❌ PdfViewModel: Speech recognition not available on device")
                _speechError.value = "Speech recognition not available on this device"
                return
            }
            
            if (!hasRecordAudioPermission()) {
                Log.e(TAG, "❌ PdfViewModel: RECORD_AUDIO permission not granted")
                _speechError.value = "Microphone permission is required for speech recognition. Please grant the permission in Settings."
                return
            }
            
            
            if (speechRecognitionManager!!.isListening()) {
                Log.w(TAG, "⚠️ PdfViewModel: Already listening, ignoring start request")
                return
            }
            
            _speechError.value = null
            _recognizedText.value = ""
            _speechSummaryText.value = ""
            _isContinuousMode.value = true
            
            speechRecognitionManager!!.startContinuousListening()
            Log.d(TAG, "✅ PdfViewModel: Continuous speech recognition started successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ PdfViewModel: Error starting continuous speech recognition", e)
            _speechError.value = "Failed to start continuous speech recognition: ${e.message}"
            _isRecognizingSpeech.value = false
            _isContinuousMode.value = false
        }
    }

    /**
     * Start single speech recognition.
     */
    fun startSpeechRecognition() {
        try {
            Log.d(TAG, "🎤 PdfViewModel: Starting single speech recognition")
            
            if (speechRecognitionManager == null) {
                Log.e(TAG, "❌ PdfViewModel: Speech recognition not initialized")
                _speechError.value = "Speech recognition not initialized"
                return
            }
            
            if (!speechRecognitionManager!!.isRecognitionAvailable()) {
                Log.e(TAG, "❌ PdfViewModel: Speech recognition not available on device")
                _speechError.value = "Speech recognition not available on this device"
                return
            }
            
            if (!hasRecordAudioPermission()) {
                Log.e(TAG, "❌ PdfViewModel: RECORD_AUDIO permission not granted")
                _speechError.value = "Microphone permission is required for speech recognition. Please grant the permission in Settings."
                return
            }
            
            if (speechRecognitionManager!!.isListening()) {
                Log.w(TAG, "⚠️ PdfViewModel: Already listening, ignoring start request")
                return
            }
            
            _speechError.value = null
            _recognizedText.value = ""
            _speechSummaryText.value = ""
            _isContinuousMode.value = false
            
            speechRecognitionManager!!.startListening()
            Log.d(TAG, "✅ PdfViewModel: Speech recognition started successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ PdfViewModel: Error starting speech recognition", e)
            _speechError.value = "Failed to start speech recognition: ${e.message}"
            _isRecognizingSpeech.value = false
        }
    }


    /**
     * Stop speech recognition.
     */
    fun stopSpeechRecognition() {
        try {
            Log.d(TAG, "🛑 PdfViewModel: Stopping speech recognition")
            
            // Reset continuous mode first
            _isContinuousMode.value = false
            
            if (speechRecognitionManager?.isInContinuousMode() == true) {
                Log.d(TAG, "🛑 PdfViewModel: Stopping continuous speech recognition")
                speechRecognitionManager?.stopContinuousListening()
                
                // Generate summary from accumulated text
                val accumulatedText = _recognizedText.value
                Log.d(TAG, "📝 PdfViewModel: Accumulated text length: ${accumulatedText.length}")
                if (accumulatedText.isNotEmpty()) {
                    Log.d(TAG, "📝 PdfViewModel: Generating summary for accumulated text: '$accumulatedText'")
                    processRecognizedSpeech(accumulatedText)
                } else {
                    Log.w(TAG, "⚠️ PdfViewModel: No accumulated text to summarize")
                }
            } else {
                speechRecognitionManager?.stopListening()
            }
            
            Log.d(TAG, "✅ PdfViewModel: Speech recognition stopped")
        } catch (e: Exception) {
            Log.e(TAG, "❌ PdfViewModel: Error stopping speech recognition", e)
        }
    }

    /**
     * Cancel speech recognition.
     */
    fun cancelSpeechRecognition() {
        try {
            Log.d(TAG, "🎤 PdfViewModel: Canceling speech recognition")
            speechRecognitionManager?.cancelListening()
        } catch (e: Exception) {
            Log.e(TAG, "❌ PdfViewModel: Error canceling speech recognition", e)
        }
    }

    /**
     * Process recognized speech text and generate summary using Gemini.
     *
     * @param recognizedText The text recognized from speech
     */
    private fun processRecognizedSpeech(recognizedText: String) {
        viewModelScope.launch {
            Log.d(TAG, "🎤 PdfViewModel: Processing recognized speech text: '$recognizedText'")
            // Don't overwrite _recognizedText here as it might already contain accumulated text
            _isGeneratingSpeechSummary.value = true
            _speechError.value = null
            _speechSummaryText.value = ""

            try {
                Log.d(TAG, "🤖 PdfViewModel: Generating AI summary for speech text")
                // Generate summary using Firebase AI service
                val summary = firebaseAiService.summarizeSpeechText(recognizedText)
                _speechSummaryText.value = summary
                Log.d(TAG, "✅ PdfViewModel: AI summary generated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "❌ PdfViewModel: Error generating AI summary", e)
                _speechError.value = "Failed to generate summary: ${e.message}"
            } finally {
                _isGeneratingSpeechSummary.value = false
            }
        }
    }


    /**
     * Set speech recognition loading state.
     */
    fun setSpeechRecognitionLoading(isLoading: Boolean) {
        _isRecognizingSpeech.value = isLoading
    }

    /**
     * Get the current recognized text.
     */
    fun getRecognizedText(): String {
        return _recognizedText.value
    }

    /**
     * Get the current speech summary text.
     */
    fun getSpeechSummaryText(): String {
        return _speechSummaryText.value
    }

    /**
     * Check if speech recognition is loading.
     */
    fun isSpeechRecognitionLoading(): Boolean {
        return _isRecognizingSpeech.value || _isGeneratingSpeechSummary.value
    }

    /**
     * Clear speech recognition data.
     */
    fun clearSpeechData() {
        Log.d(TAG, "🧹 PdfViewModel: Clearing speech data")
        _recognizedText.value = ""
        _speechSummaryText.value = ""
        _speechError.value = null
        _isContinuousMode.value = false
        _isRecognizingSpeech.value = false
    }

    /**
     * Force stop speech recognition and prevent restarts.
     */
    fun forceStopSpeechRecognition() {
        try {
            Log.d(TAG, "🛑 PdfViewModel: Force stopping speech recognition")
            
            // Reset all states immediately
            _isContinuousMode.value = false
            _isRecognizingSpeech.value = false
            
            // Stop the recognizer
            speechRecognitionManager?.stopContinuousListening()
            
            // Generate summary from accumulated text
            val accumulatedText = _recognizedText.value
            Log.d(TAG, "📝 PdfViewModel: Force stop - accumulated text length: ${accumulatedText.length}")
            if (accumulatedText.isNotEmpty()) {
                Log.d(TAG, "📝 PdfViewModel: Force stop - generating summary for accumulated text: '$accumulatedText'")
                processRecognizedSpeech(accumulatedText)
            } else {
                Log.w(TAG, "⚠️ PdfViewModel: Force stop - no accumulated text to summarize")
            }
            
            Log.d(TAG, "✅ PdfViewModel: Speech recognition force stopped")
        } catch (e: Exception) {
            Log.e(TAG, "❌ PdfViewModel: Error force stopping speech recognition", e)
        }
    }

    /**
     * Check if RECORD_AUDIO permission is granted.
     */
    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get the required permission for speech recognition.
     */
    fun getRequiredPermission(): String {
        return android.Manifest.permission.RECORD_AUDIO
    }

    
    /**
     * Clean up resources when ViewModel is destroyed.
     */
    override fun onCleared() {
        Log.d(TAG, "🧹 PdfViewModel: Cleaning up ViewModel resources")
        super.onCleared()
        repository.cleanup()
        speechRecognitionManager?.cleanup()
        speechRecognitionManager = null
        Log.d(TAG, "✅ PdfViewModel: Cleanup completed")
    }
}
