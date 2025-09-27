package com.bigcash.ai.vectordb.service

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Service for recording audio using MediaRecorder.
 * Records audio in MP3 format suitable for AI transcription.
 */
class AudioRecorderService(private val context: Context) {

    companion object {
        private const val TAG = "AudioRecorderService"
    }

    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var isRecording = false

    /**
     * Start recording audio to a temporary file.
     * @return The file path where audio is being recorded, or null if failed to start
     */
    fun startRecording(): String? {
        try {
            // Create temporary file for recording
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "audio_recording_$timestamp.mp3"
            recordingFile = File(context.cacheDir, fileName)

            // Initialize MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(recordingFile!!.absolutePath)

                prepare()
                start()

                isRecording = true
                Log.d(TAG, "Recording started: ${recordingFile!!.name}")

                return recordingFile!!.absolutePath
            }

        } catch (e: IOException) {
            Log.e(TAG, "Failed to start recording", e)
            cleanup()
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            cleanup()
            return null
        }

        return null
    }

    /**
     * Stop recording and return the recorded audio data.
     * @return Pair of filename and audio data bytes, or null if no recording
     */
    fun stopRecording(): Pair<String, ByteArray>? {
        if (!isRecording || mediaRecorder == null || recordingFile == null) {
            Log.w(TAG, "No active recording to stop")
            return null
        }

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false

            val file = recordingFile!!
            val audioData = file.readBytes()
            val fileName = file.name

            Log.d(TAG, "Recording stopped: $fileName, size: ${audioData.size} bytes")

            // Clean up the temporary file
            file.delete()
            recordingFile = null

            return Pair(fileName, audioData)

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            cleanup()
            return null
        }
    }

    /**
     * Cancel current recording without saving.
     */
    fun cancelRecording() {
        Log.d(TAG, "Canceling recording")
        cleanup()
    }

    /**
     * Check if currently recording.
     */
    fun isRecording(): Boolean = isRecording

    /**
     * Clean up resources.
     */
    private fun cleanup() {
        try {
            if (isRecording) {
                mediaRecorder?.stop()
            }
            mediaRecorder?.release()
            mediaRecorder = null

            recordingFile?.delete()
            recordingFile = null

            isRecording = false
            Log.d(TAG, "Cleanup completed")

        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
            // Force cleanup
            mediaRecorder = null
            recordingFile?.delete()
            recordingFile = null
            isRecording = false
        }
    }

    /**
     * Release resources when service is destroyed.
     */
    fun release() {
        cleanup()
    }
}
