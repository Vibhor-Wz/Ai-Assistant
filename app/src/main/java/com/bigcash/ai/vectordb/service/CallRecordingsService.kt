package com.bigcash.ai.vectordb.service

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Service for fetching call recordings from the device using MediaStore API.
 * This service is compatible with Android 10-14 and handles scoped storage restrictions.
 */
class CallRecordingsService(private val context: Context) {

    companion object {
        private const val TAG = "CallRecordingsService"
        private const val VECTOR_DEBUG_TAG = "VECTOR_DEBUG"
        
        // Audio formats commonly used for call recordings
        private val CALL_RECORDING_FORMATS = listOf("amr", "3gp", "m4a", "wav")
        
        // MIME types for call recording formats
        private val CALL_RECORDING_MIME_TYPES = listOf(
            "audio/amr",
            "audio/3gpp", 
            "audio/mp4",
            "audio/wav"
        )
    }

    /**
     * Data class representing a call recording.
     */
    data class CallRecording(
        val id: String,
        val phoneNumber: String,
        val contactName: String?,
        val callType: String,
        val duration: Long,
        val date: Date,
        val filePath: String?,
        val fileSize: Long = 0L,
        val mimeType: String? = null,
        val displayName: String? = null
    )

    /**
     * Fetch all call recordings from the device using MediaStore API.
     * This method is compatible with Android 10-14 and handles scoped storage.
     * 
     * @return List of call recordings found on the device
     */
    suspend fun fetchCallRecordings(): List<CallRecording> = withContext(Dispatchers.IO) {
        Log.d(VECTOR_DEBUG_TAG, "📞 CallRecordingsService: Starting to fetch call recordings via MediaStore")
        
        val recordings = mutableListOf<CallRecording>()
        
        try {
            // Query MediaStore for audio files
            val audioFiles = queryAudioFilesFromMediaStore()
            Log.d(VECTOR_DEBUG_TAG, "📁 Found ${audioFiles.size} audio files via MediaStore")
            
            // Convert to CallRecording objects
            audioFiles.forEachIndexed { index, audioFile ->
                val recording = CallRecording(
                    id = "mediastore_${index}",
                    phoneNumber = "Unknown",
                    contactName = extractContactNameFromFileName(audioFile.displayName ?: ""),
                    callType = determineCallType(audioFile),
                    duration = audioFile.duration,
                    date = Date(audioFile.dateModified * 1000), // Convert to milliseconds
                    filePath = audioFile.filePath,
                    fileSize = audioFile.size,
                    mimeType = audioFile.mimeType,
                    displayName = audioFile.displayName
                )
                recordings.add(recording)
                Log.d(VECTOR_DEBUG_TAG, "📞 Found audio file: ${audioFile.displayName} (${audioFile.mimeType})")
            }

            // Sort by modification date (newest first)
            recordings.sortByDescending { it.date }

            Log.d(VECTOR_DEBUG_TAG, "✅ CallRecordingsService: Found ${recordings.size} audio files via MediaStore")

        } catch (e: Exception) {
            Log.e(VECTOR_DEBUG_TAG, "❌ CallRecordingsService: Error fetching call recordings", e)
        }

        return@withContext recordings
    }

    /**
     * Query audio files from MediaStore API.
     * 
     * @return List of audio file information
     */
    private fun queryAudioFilesFromMediaStore(): List<AudioFileInfo> {
        val audioFiles = mutableListOf<AudioFileInfo>()
        
        try {
            // Build selection query for call recording formats
            val selection = buildSelectionQuery()
            val selectionArgs = buildSelectionArgs()
            
            Log.d(VECTOR_DEBUG_TAG, "🔍 MediaStore query selection: $selection")
            Log.d(VECTOR_DEBUG_TAG, "🔍 MediaStore query args: ${selectionArgs.joinToString()}")
            
            val cursor: Cursor? = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.SIZE,
                    MediaStore.Audio.Media.DATE_MODIFIED,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.MIME_TYPE,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST
                ),
                selection,
                selectionArgs,
                "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"
            )
            
            cursor?.use { c ->
                val idIndex = c.getColumnIndex(MediaStore.Audio.Media._ID)
                val dataIndex = c.getColumnIndex(MediaStore.Audio.Media.DATA)
                val displayNameIndex = c.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val sizeIndex = c.getColumnIndex(MediaStore.Audio.Media.SIZE)
                val dateModifiedIndex = c.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
                val durationIndex = c.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val mimeTypeIndex = c.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                val titleIndex = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistIndex = c.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                
                Log.d(VECTOR_DEBUG_TAG, "📱 MediaStore cursor count: ${c.count}")
                
                while (c.moveToNext()) {
                    val id = c.getLong(idIndex)
                    val filePath = c.getString(dataIndex)
                    val displayName = c.getString(displayNameIndex)
                    val size = c.getLong(sizeIndex)
                    val dateModified = c.getLong(dateModifiedIndex)
                    val duration = c.getLong(durationIndex)
                    val mimeType = c.getString(mimeTypeIndex)
                    val title = c.getString(titleIndex)
                    val artist = c.getString(artistIndex)
                    
                    // Verify file exists and is accessible
                    if (filePath != null && File(filePath).exists()) {
                        // MediaStore duration is in milliseconds, convert to seconds
                        var durationInSeconds = if (duration > 0) duration / 1000 else 0L
                        
                        // If duration is 0 or seems incorrect, try to get it from file metadata
                        if (durationInSeconds == 0L) {
                            durationInSeconds = getDurationFromFile(filePath)
                        }
                        
                        val audioFile = AudioFileInfo(
                            id = id,
                            filePath = filePath,
                            displayName = displayName,
                            size = size,
                            dateModified = dateModified,
                            duration = durationInSeconds, // Store in seconds
                            mimeType = mimeType,
                            title = title,
                            artist = artist
                        )
                        audioFiles.add(audioFile)
                        Log.d(VECTOR_DEBUG_TAG, "🎵 MediaStore file: $displayName ($mimeType) - ${formatFileSize(size)} - Duration: ${formatDuration(durationInSeconds)} (MediaStore: ${duration}ms)")
                    } else {
                        Log.w(VECTOR_DEBUG_TAG, "⚠️ File not accessible: $filePath")
                    }
                }
            }
            
            Log.d(VECTOR_DEBUG_TAG, "📱 MediaStore found ${audioFiles.size} accessible audio files")
            
        } catch (e: Exception) {
            Log.e(VECTOR_DEBUG_TAG, "❌ Error querying MediaStore", e)
        }
        
        return audioFiles
    }

    /**
     * Build selection query for MediaStore.
     */
    private fun buildSelectionQuery(): String {
        val mimeTypeConditions = CALL_RECORDING_MIME_TYPES.joinToString(" OR ") { 
            "${MediaStore.Audio.Media.MIME_TYPE} = ?" 
        }
        val extensionConditions = CALL_RECORDING_FORMATS.joinToString(" OR ") {
            "${MediaStore.Audio.Media.DATA} LIKE ?"
        }
        
        return "($mimeTypeConditions) OR ($extensionConditions)"
    }

    /**
     * Build selection arguments for MediaStore query.
     */
    private fun buildSelectionArgs(): Array<String> {
        val mimeTypeArgs = CALL_RECORDING_MIME_TYPES.toTypedArray()
        val extensionArgs = CALL_RECORDING_FORMATS.map { "%$it" }.toTypedArray()
        
        return mimeTypeArgs + extensionArgs
    }

    /**
     * Determine call type based on file information.
     */
    private fun determineCallType(audioFile: AudioFileInfo): String {
        val fileName = audioFile.displayName?.lowercase() ?: ""
        val filePath = audioFile.filePath?.lowercase() ?: ""
        
        return when {
            fileName.contains("call") || filePath.contains("call") -> "Call Recording"
            fileName.contains("voice") || filePath.contains("voice") -> "Voice Note"
            fileName.contains("recording") || filePath.contains("recording") -> "Recording"
            fileName.contains("whatsapp") || filePath.contains("whatsapp") -> "WhatsApp Audio"
            fileName.contains("telegram") || filePath.contains("telegram") -> "Telegram Audio"
            else -> "Audio File"
        }
    }

    /**
     * Extract contact name from filename.
     * This tries to extract meaningful names from audio file names.
     */
    private fun extractContactNameFromFileName(fileName: String): String? {
        val nameWithoutExtension = fileName.substringBeforeLast(".")
        
        // Common patterns for call recordings
        val patterns = listOf(
            // WhatsApp patterns: "Voice note from John Doe"
            Regex("Voice note from (.+)", RegexOption.IGNORE_CASE),
            // Call recording patterns: "Call_John_Doe_20231201"
            Regex("Call_(.+)_\\d+", RegexOption.IGNORE_CASE),
            // Simple patterns: "John_Doe_call"
            Regex("(.+)_call", RegexOption.IGNORE_CASE),
            // Date patterns: "20231201_John_Doe"
            Regex("\\d+_(.+)", RegexOption.IGNORE_CASE),
            // Generic patterns: "John_Doe"
            Regex("([A-Za-z]+_[A-Za-z]+)", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            val match = pattern.find(nameWithoutExtension)
            if (match != null) {
                val extractedName = match.groupValues[1]
                    .replace("_", " ")
                    .replace("-", " ")
                    .trim()
                if (extractedName.isNotEmpty() && extractedName.length > 2) {
                    return extractedName
                }
            }
        }
        
        // If no pattern matches, return the filename without extension
        return if (nameWithoutExtension.length > 2) nameWithoutExtension else null
    }

    /**
     * Format duration in a human-readable format.
     */
    fun formatDuration(duration: Long): String {
        val hours = duration / 3600
        val minutes = (duration % 3600) / 60
        val seconds = duration % 60

        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%d:%02d", minutes, seconds)
        }
    }

    /**
     * Format file size in a human-readable format.
     */
    fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1 -> String.format("%.1f GB", gb)
            mb >= 1 -> String.format("%.1f MB", mb)
            kb >= 1 -> String.format("%.1f KB", kb)
            else -> "$bytes bytes"
        }
    }

    /**
     * Get duration from file metadata as fallback.
     * This is a simple implementation that estimates duration based on file size and format.
     */
    private fun getDurationFromFile(filePath: String): Long {
        try {
            val file = File(filePath)
            val fileSize = file.length()
            val extension = file.extension.lowercase()
            
            // Estimate duration based on file size and format
            // These are rough estimates for different audio formats
            val bytesPerSecond = when (extension) {
                "wav" -> 176400L // 44.1kHz, 16-bit, stereo
                "mp3" -> 16000L  // 128kbps average
                "m4a" -> 16000L  // 128kbps average
                "aac" -> 16000L  // 128kbps average
                "amr" -> 8000L   // 8kbps typical for voice
                "3gp" -> 8000L   // 8kbps typical for voice
                else -> 16000L   // Default estimate
            }
            
            val estimatedDuration = fileSize / bytesPerSecond
            Log.d(VECTOR_DEBUG_TAG, "📊 Estimated duration for $filePath: ${estimatedDuration}s (${fileSize} bytes, $extension)")
            return estimatedDuration
        } catch (e: Exception) {
            Log.w(VECTOR_DEBUG_TAG, "⚠️ Error estimating duration for $filePath: ${e.message}")
            return 0L
        }
    }



    /**
     * Data class for audio file information from MediaStore.
     */
    private data class AudioFileInfo(
        val id: Long,
        val filePath: String,
        val displayName: String?,
        val size: Long,
        val dateModified: Long,
        val duration: Long,
        val mimeType: String?,
        val title: String?,
        val artist: String?
    )
}