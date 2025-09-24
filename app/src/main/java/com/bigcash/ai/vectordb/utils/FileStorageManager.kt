package com.bigcash.ai.vectordb.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for managing local file storage of uploaded documents.
 * This class handles saving, retrieving, and deleting files from the app's internal storage.
 */
class FileStorageManager(private val context: Context) {
    
    companion object {
        private const val TAG = "FileStorageManager"
        private const val UPLOADS_FOLDER = "uploaded_documents"
    }
    
    private val uploadsDir: File by lazy {
        val dir = File(context.filesDir, UPLOADS_FOLDER)
        if (!dir.exists()) {
            dir.mkdirs()
            Log.d(TAG, "Created uploads directory: ${dir.absolutePath}")
        }
        dir
    }
    
    /**
     * Save a file to local storage.
     *
     * @param fileName The original name of the file
     * @param fileData The file data as ByteArray
     * @return The local file path if successful, null otherwise
     */
    fun saveFile(fileName: String, fileData: ByteArray): String? {
        return try {
            // Generate a unique filename to avoid conflicts
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileExtension = getFileExtension(fileName)
            val baseName = getFileNameWithoutExtension(fileName)
            val uniqueFileName = "${baseName}_${timestamp}${fileExtension}"
            
            val file = File(uploadsDir, uniqueFileName)
            
            FileOutputStream(file).use { fos ->
                fos.write(fileData)
            }
            
            Log.d(TAG, "File saved successfully: ${file.absolutePath}")
            file.absolutePath
        } catch (e: IOException) {
            Log.e(TAG, "Error saving file: $fileName", e)
            null
        }
    }
    
    /**
     * Retrieve a file from local storage.
     *
     * @param filePath The local file path
     * @return The file if it exists, null otherwise
     */
    fun getFile(filePath: String): File? {
        return try {
            val file = File(filePath)
            if (file.exists() && file.isFile) {
                Log.d(TAG, "File retrieved successfully: $filePath")
                file
            } else {
                Log.w(TAG, "File not found: $filePath")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving file: $filePath", e)
            null
        }
    }
    
    /**
     * Delete a file from local storage.
     *
     * @param filePath The local file path
     * @return True if the file was deleted successfully, false otherwise
     */
    fun deleteFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                val deleted = file.delete()
                if (deleted) {
                    Log.d(TAG, "File deleted successfully: $filePath")
                } else {
                    Log.w(TAG, "Failed to delete file: $filePath")
                }
                deleted
            } else {
                Log.w(TAG, "File does not exist: $filePath")
                true // Consider it deleted if it doesn't exist
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file: $filePath", e)
            false
        }
    }
    
    /**
     * Check if a file exists in local storage.
     *
     * @param filePath The local file path
     * @return True if the file exists, false otherwise
     */
    fun fileExists(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            file.exists() && file.isFile
        } catch (e: Exception) {
            Log.e(TAG, "Error checking file existence: $filePath", e)
            false
        }
    }
    
    /**
     * Get the size of a file in bytes.
     *
     * @param filePath The local file path
     * @return The file size in bytes, or -1 if the file doesn't exist
     */
    fun getFileSize(filePath: String): Long {
        return try {
            val file = File(filePath)
            if (file.exists() && file.isFile) {
                file.length()
            } else {
                -1
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file size: $filePath", e)
            -1
        }
    }
    
    /**
     * Get all uploaded files.
     *
     * @return List of file paths for all uploaded files
     */
    fun getAllUploadedFiles(): List<String> {
        return try {
            uploadsDir.listFiles()?.map { it.absolutePath } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all uploaded files", e)
            emptyList()
        }
    }
    
    /**
     * Clean up old files (optional utility method for maintenance).
     *
     * @param maxAgeInDays Maximum age of files in days
     * @return Number of files deleted
     */
    fun cleanupOldFiles(maxAgeInDays: Int = 30): Int {
        var deletedCount = 0
        try {
            val cutoffTime = System.currentTimeMillis() - (maxAgeInDays * 24 * 60 * 60 * 1000L)
            uploadsDir.listFiles()?.forEach { file ->
                if (file.isFile && file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        deletedCount++
                        Log.d(TAG, "Deleted old file: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old files", e)
        }
        return deletedCount
    }
    
    /**
     * Get the file extension from a filename.
     */
    private fun getFileExtension(fileName: String): String {
        val lastDotIndex = fileName.lastIndexOf('.')
        return if (lastDotIndex > 0 && lastDotIndex < fileName.length - 1) {
            fileName.substring(lastDotIndex)
        } else {
            ""
        }
    }
    
    /**
     * Get the filename without extension.
     */
    private fun getFileNameWithoutExtension(fileName: String): String {
        val lastDotIndex = fileName.lastIndexOf('.')
        return if (lastDotIndex > 0) {
            fileName.substring(0, lastDotIndex)
        } else {
            fileName
        }
    }
}
