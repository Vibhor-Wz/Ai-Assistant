package com.bigcash.ai.vectordb.viewmodel

import android.app.Application
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
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
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.util.Log

/**
 * ViewModel for PDF management operations.
 * This class handles the UI state and business logic for PDF operations.
 */
class PdfViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "PdfViewModel"
        private const val AUDIO_DEBUG_TAG = "AUDIO_RECORDING"
    }

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

    // Audio Recording State
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isProcessingAudio = MutableStateFlow(false)
    val isProcessingAudio: StateFlow<Boolean> = _isProcessingAudio.asStateFlow()

    private val _audioSummary = MutableStateFlow("")
    val audioSummary: StateFlow<String> = _audioSummary.asStateFlow()

    private val _audioError = MutableStateFlow<String?>(null)
    val audioError: StateFlow<String?> = _audioError.asStateFlow()

    // Audio recording components
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null

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
     * Start audio recording.
     */
    fun startAudioRecording() {
        Log.d(AUDIO_DEBUG_TAG, "🎤 Starting audio recording process")
        viewModelScope.launch {
            try {
                Log.d(AUDIO_DEBUG_TAG, "📱 Setting recording state to true")
                _isRecording.value = true
                _audioError.value = null
                
                // Create audio file
                val timestamp = System.currentTimeMillis()
                audioFile = File(getApplication<Application>().cacheDir, "audio_recording_${timestamp}.wav")
                Log.d(AUDIO_DEBUG_TAG, "📁 Created audio file: ${audioFile?.absolutePath}")
                
                // Initialize MediaRecorder
                Log.d(AUDIO_DEBUG_TAG, "🔧 Initializing MediaRecorder for Android API ${Build.VERSION.SDK_INT}")
                mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(getApplication())
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }
                
                Log.d(AUDIO_DEBUG_TAG, "⚙️ Configuring MediaRecorder settings")
                mediaRecorder?.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    setOutputFile(audioFile?.absolutePath)
                    
                    Log.d(AUDIO_DEBUG_TAG, "🎯 Preparing MediaRecorder")
                    prepare()
                    Log.d(AUDIO_DEBUG_TAG, "▶️ Starting audio recording")
                    start()
                }
                
                Log.d(AUDIO_DEBUG_TAG, "✅ Audio recording started successfully")
                
            } catch (e: Exception) {
                Log.e(AUDIO_DEBUG_TAG, "❌ Failed to start audio recording", e)
                _audioError.value = "Failed to start recording: ${e.message}"
                _isRecording.value = false
            }
        }
    }

    /**
     * Stop audio recording and process the audio.
     */
    fun stopAudioRecording() {
        Log.d(AUDIO_DEBUG_TAG, "⏹️ Stopping audio recording")
        viewModelScope.launch {
            try {
                Log.d(AUDIO_DEBUG_TAG, "🛑 Stopping MediaRecorder")
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                _isRecording.value = false
                Log.d(AUDIO_DEBUG_TAG, "📱 Recording state set to false")
                
                // Process the recorded audio
                audioFile?.let { file ->
                    Log.d(AUDIO_DEBUG_TAG, "📁 Checking audio file: ${file.absolutePath}")
                    Log.d(AUDIO_DEBUG_TAG, "📊 File exists: ${file.exists()}, Size: ${file.length()} bytes")
                    
                    if (file.exists() && file.length() > 0) {
                        Log.d(AUDIO_DEBUG_TAG, "✅ Audio file is valid, starting processing")
                        processAudioFile(file)
                    } else {
                        Log.w(AUDIO_DEBUG_TAG, "⚠️ No audio was recorded or file is empty")
                        _audioError.value = "No audio was recorded"
                    }
                }
                
                Log.d(AUDIO_DEBUG_TAG, "✅ Audio recording stopped successfully")
                
            } catch (e: Exception) {
                Log.e(AUDIO_DEBUG_TAG, "❌ Failed to stop audio recording", e)
                _audioError.value = "Failed to stop recording: ${e.message}"
                _isRecording.value = false
            }
        }
    }

    /**
     * Process the recorded audio file and generate summary.
     */
    private suspend fun processAudioFile(audioFile: File) {
        Log.d(AUDIO_DEBUG_TAG, "🔄 Starting audio file processing")
        Log.d(AUDIO_DEBUG_TAG, "📁 Processing file: ${audioFile.absolutePath}")
        Log.d(AUDIO_DEBUG_TAG, "📊 File size: ${audioFile.length()} bytes")
        
        _isProcessingAudio.value = true
        _audioError.value = null
        
        try {
            // Read audio file as byte array
            Log.d(AUDIO_DEBUG_TAG, "📖 Reading audio file as byte array")
            val audioData = audioFile.readBytes()
            val fileName = audioFile.name
            Log.d(AUDIO_DEBUG_TAG, "📄 File name: $fileName")
            Log.d(AUDIO_DEBUG_TAG, "📊 Audio data size: ${audioData.size} bytes")
            
            // Generate content using Firebase AI
            Log.d(AUDIO_DEBUG_TAG, "🤖 Sending audio to Firebase AI for processing")
            val summary = firebaseAiService.generateContentFromFile(fileName, audioData, FirebaseAiService.FileType.AUDIO)
            Log.d(AUDIO_DEBUG_TAG, "✅ AI processing completed")
            Log.d(AUDIO_DEBUG_TAG, "📝 Summary length: ${summary.length} characters")
            _audioSummary.value = summary
            
            // Clean up the temporary audio file
            Log.d(AUDIO_DEBUG_TAG, "🗑️ Cleaning up temporary audio file")
            val deleted = audioFile.delete()
            Log.d(AUDIO_DEBUG_TAG, "🗑️ File deleted: $deleted")
            
            Log.d(AUDIO_DEBUG_TAG, "✅ Audio processing completed successfully")
            
        } catch (e: Exception) {
            Log.e(AUDIO_DEBUG_TAG, "❌ Failed to process audio file", e)
            _audioError.value = "Failed to process audio: ${e.message}"
        } finally {
            _isProcessingAudio.value = false
            Log.d(AUDIO_DEBUG_TAG, "📱 Processing state set to false")
        }
    }

    /**
     * Clear audio data.
     */
    fun clearAudioData() {
        Log.d(AUDIO_DEBUG_TAG, "🧹 Clearing audio data")
        _audioSummary.value = ""
        _audioError.value = null
        _isRecording.value = false
        _isProcessingAudio.value = false
        Log.d(AUDIO_DEBUG_TAG, "✅ Audio data cleared")
    }

    /**
     * Get the current audio summary.
     */
    fun getAudioSummary(): String {
        val summary = _audioSummary.value
        Log.d(AUDIO_DEBUG_TAG, "📖 Getting audio summary: ${summary.length} characters")
        return summary
    }

    /**
     * Check if audio is being processed.
     */
    fun isAudioProcessing(): Boolean {
        val isProcessing = _isProcessingAudio.value
        Log.d(AUDIO_DEBUG_TAG, "🔍 Checking audio processing state: $isProcessing")
        return isProcessing
    }
    
    /**
     * Clean up resources when ViewModel is destroyed.
     */
    override fun onCleared() {
        Log.d(AUDIO_DEBUG_TAG, "🧹 Cleaning up PdfViewModel resources")
        super.onCleared()
        
        // Clean up audio recording resources
        if (mediaRecorder != null) {
            Log.d(AUDIO_DEBUG_TAG, "🎤 Releasing MediaRecorder")
            try {
                mediaRecorder?.release()
            } catch (e: Exception) {
                Log.e(AUDIO_DEBUG_TAG, "❌ Error releasing MediaRecorder", e)
            }
        }
        
        // Clean up audio file
        audioFile?.let { file ->
            Log.d(AUDIO_DEBUG_TAG, "🗑️ Deleting audio file: ${file.absolutePath}")
            val deleted = file.delete()
            Log.d(AUDIO_DEBUG_TAG, "🗑️ Audio file deleted: $deleted")
        }
        
        repository.cleanup()
        Log.d(AUDIO_DEBUG_TAG, "✅ PdfViewModel cleanup completed")
    }
}
