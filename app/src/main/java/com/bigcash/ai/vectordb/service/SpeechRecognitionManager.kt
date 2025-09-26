package com.bigcash.ai.vectordb.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.*

/**
 * Manager class for handling speech recognition using SpeechRecognizer.
 * Provides a clean interface for speech recognition operations with proper lifecycle management.
 */
class SpeechRecognitionManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onStart: () -> Unit,
    private val onEnd: () -> Unit
) {
    
    companion object {
        private const val TAG = "SpeechRecognitionManager"
    }
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    private var isListening = false
    
    init {
        Log.d(TAG, "🎤 SpeechRecognitionManager: Initializing speech recognition manager")
        initializeSpeechRecognizer()
    }
    
    /**
     * Initialize the SpeechRecognizer and create the recognition intent.
     */
    private fun initializeSpeechRecognizer() {
        try {
            Log.d(TAG, "🔧 SpeechRecognitionManager: Initializing SpeechRecognizer")
            
            // Check if speech recognition is available
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.e(TAG, "❌ SpeechRecognitionManager: Speech recognition not available on this device")
                onError("Speech recognition not available on this device")
                return
            }
            
            // Create SpeechRecognizer
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(createRecognitionListener())
            
            // Create recognition intent
            recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to generate summary")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            
            Log.d(TAG, "✅ SpeechRecognitionManager: SpeechRecognizer initialized successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ SpeechRecognitionManager: Error initializing speech recognizer", e)
            onError("Failed to initialize speech recognition: ${e.message}")
        }
    }
    
    /**
     * Create the RecognitionListener for handling speech recognition events.
     */
    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "🎤 SpeechRecognitionManager: Ready for speech")
                isListening = true
                onStart()
            }
            
            override fun onBeginningOfSpeech() {
                Log.d(TAG, "🎤 SpeechRecognitionManager: Beginning of speech detected")
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Optional: Handle volume changes for UI feedback
                // Log.v(TAG, "SpeechRecognitionManager: RMS changed: $rmsdB")
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                Log.d(TAG, "🎤 SpeechRecognitionManager: Buffer received")
            }
            
            override fun onEndOfSpeech() {
                Log.d(TAG, "🎤 SpeechRecognitionManager: End of speech detected")
                isListening = false
            }
            
            override fun onError(error: Int) {
                isListening = false
                val errorMessage = getErrorMessage(error)
                Log.e(TAG, "❌ SpeechRecognitionManager: Recognition error: $error - $errorMessage")
                onError(errorMessage)
                onEnd()
            }
            
            override fun onResults(results: Bundle?) {
                Log.d(TAG, "🎤 SpeechRecognitionManager: Recognition results received")
                isListening = false
                
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0]
                    Log.d(TAG, "✅ SpeechRecognitionManager: Recognized text: '$recognizedText'")
                    onResult(recognizedText)
                } else {
                    Log.w(TAG, "⚠️ SpeechRecognitionManager: No recognition results found")
                    onError("No speech was recognized")
                }
                onEnd()
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    Log.d(TAG, "🎤 SpeechRecognitionManager: Partial result: '${matches[0]}'")
                }
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.d(TAG, "🎤 SpeechRecognitionManager: Event received: $eventType")
            }
        }
    }
    
    /**
     * Start speech recognition.
     */
    fun startListening() {
        try {
            Log.d(TAG, "🎤 SpeechRecognitionManager: Starting speech recognition")
            
            if (isListening) {
                Log.w(TAG, "⚠️ SpeechRecognitionManager: Already listening, ignoring start request")
                return
            }
            
            if (!hasRecordAudioPermission()) {
                Log.e(TAG, "❌ SpeechRecognitionManager: RECORD_AUDIO permission not granted")
                onError("Microphone permission is required for speech recognition")
                return
            }
            
            if (speechRecognizer == null) {
                Log.e(TAG, "❌ SpeechRecognitionManager: SpeechRecognizer not initialized")
                onError("Speech recognition not initialized")
                return
            }
            
            if (recognizerIntent == null) {
                Log.e(TAG, "❌ SpeechRecognitionManager: Recognition intent not initialized")
                onError("Recognition intent not initialized")
                return
            }
            
            speechRecognizer?.startListening(recognizerIntent)
            Log.d(TAG, "✅ SpeechRecognitionManager: Speech recognition started")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ SpeechRecognitionManager: Error starting speech recognition", e)
            onError("Failed to start speech recognition: ${e.message}")
            onEnd()
        }
    }
    
    /**
     * Stop speech recognition.
     */
    fun stopListening() {
        try {
            Log.d(TAG, "🎤 SpeechRecognitionManager: Stopping speech recognition")
            
            if (isListening) {

                speechRecognizer?.stopListening()
                isListening = false
                Log.d(TAG, "✅ SpeechRecognitionManager: Speech recognition stopped")
            } else {
                Log.w(TAG, "⚠️ SpeechRecognitionManager: Not currently listening")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ SpeechRecognitionManager: Error stopping speech recognition", e)
        }
    }
    
    /**
     * Cancel speech recognition.
     */
    fun cancelListening() {
        try {
            Log.d(TAG, "🎤 SpeechRecognitionManager: Canceling speech recognition")
            
            if (isListening) {
                speechRecognizer?.cancel()
                isListening = false
                Log.d(TAG, "✅ SpeechRecognitionManager: Speech recognition canceled")
            } else {
                Log.w(TAG, "⚠️ SpeechRecognitionManager: Not currently listening")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ SpeechRecognitionManager: Error canceling speech recognition", e)
        }
    }
    
    /**
     * Check if currently listening.
     */
    fun isListening(): Boolean {
        return isListening
    }
    
    /**
     * Check if speech recognition is available on the device.
     */
    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Check if RECORD_AUDIO permission is granted.
     */
    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
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
     * Get human-readable error message from error code.
     */
    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech was recognized"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error occurred"
        }
    }
    
    /**
     * Clean up resources.
     */
    fun cleanup() {
        try {
            Log.d(TAG, "🧹 SpeechRecognitionManager: Cleaning up resources")
            
            if (isListening) {
                cancelListening()
            }
            
            speechRecognizer?.destroy()
            speechRecognizer = null
            recognizerIntent = null
            
            Log.d(TAG, "✅ SpeechRecognitionManager: Cleanup completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ SpeechRecognitionManager: Error during cleanup", e)
        }
    }
}