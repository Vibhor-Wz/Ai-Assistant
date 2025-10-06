package com.bigcash.ai.vectordb.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Helper class to handle runtime permissions for audio recording and call log access.
 */
class PermissionHelper(private val activity: ComponentActivity) {

    companion object {
        const val RECORD_AUDIO_PERMISSION = Manifest.permission.RECORD_AUDIO
        const val READ_CALL_LOG_PERMISSION = Manifest.permission.READ_CALL_LOG
        const val READ_PHONE_STATE_PERMISSION = Manifest.permission.READ_PHONE_STATE
        const val READ_EXTERNAL_STORAGE_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE
        const val MANAGE_EXTERNAL_STORAGE_PERMISSION = Manifest.permission.MANAGE_EXTERNAL_STORAGE
    }

    private var onPermissionResult: ((Boolean) -> Unit)? = null

    private val requestPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        onPermissionResult?.invoke(isGranted)
        onPermissionResult = null
    }

    private val requestMultiplePermissionsLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        onPermissionResult?.invoke(allGranted)
        onPermissionResult = null
    }

    /**
     * Check if audio recording permission is granted.
     */
    fun isAudioPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            RECORD_AUDIO_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request audio recording permission.
     * @param onResult Callback with the permission result
     */
    fun requestAudioPermission(onResult: (Boolean) -> Unit) {
        if (isAudioPermissionGranted()) {
            onResult(true)
            return
        }

        onPermissionResult = onResult
        requestPermissionLauncher.launch(RECORD_AUDIO_PERMISSION)
    }

    /**
     * Check if we should show rationale for audio permission.
     */
    fun shouldShowAudioPermissionRationale(): Boolean {
        return activity.shouldShowRequestPermissionRationale(RECORD_AUDIO_PERMISSION)
    }

    /**
     * Check if call log permissions are granted.
     */
    fun areCallLogPermissionsGranted(): Boolean {
        val callLogGranted = ContextCompat.checkSelfPermission(
            activity,
            READ_CALL_LOG_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
        
        val phoneStateGranted = ContextCompat.checkSelfPermission(
            activity,
            READ_PHONE_STATE_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
        
        val result = callLogGranted && phoneStateGranted
        android.util.Log.d("PermissionHelper", "🔐 Call Log permissions - READ_CALL_LOG: $callLogGranted, READ_PHONE_STATE: $phoneStateGranted, Result: $result")
        
        return result
    }

    /**
     * Request call log permissions.
     * @param onResult Callback with the permission result
     */
    fun requestCallLogPermissions(onResult: (Boolean) -> Unit) {
        if (areCallLogPermissionsGranted()) {
            onResult(true)
            return
        }

        onPermissionResult = onResult
        requestMultiplePermissionsLauncher.launch(
            arrayOf(READ_CALL_LOG_PERMISSION, READ_PHONE_STATE_PERMISSION)
        )
    }

    /**
     * Check if we should show rationale for call log permissions.
     */
    fun shouldShowCallLogPermissionRationale(): Boolean {
        return activity.shouldShowRequestPermissionRationale(READ_CALL_LOG_PERMISSION) ||
               activity.shouldShowRequestPermissionRationale(READ_PHONE_STATE_PERMISSION)
    }

    /**
     * Check if storage permissions are granted.
     * On Android 13+, READ_EXTERNAL_STORAGE is deprecated, so we check for media permissions instead.
     */
    fun areStoragePermissionsGranted(): Boolean {
        val androidVersion = android.os.Build.VERSION.SDK_INT
        
        return if (androidVersion >= 33) {
            // Android 13+ - Check for media permissions instead
            val mediaAudioGranted = ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            
            android.util.Log.d("PermissionHelper", "🔐 Android 13+ - READ_MEDIA_AUDIO: $mediaAudioGranted")
            mediaAudioGranted
        } else {
            // Android 12 and below - Check for READ_EXTERNAL_STORAGE
            val storageGranted = ContextCompat.checkSelfPermission(
                activity,
                READ_EXTERNAL_STORAGE_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
            
            android.util.Log.d("PermissionHelper", "🔐 Android 12- - READ_EXTERNAL_STORAGE: $storageGranted")
            storageGranted
        }
    }

    /**
     * Request storage permissions.
     * On Android 13+, requests READ_MEDIA_AUDIO instead of READ_EXTERNAL_STORAGE.
     * @param onResult Callback with the permission result
     */
    fun requestStoragePermissions(onResult: (Boolean) -> Unit) {
        if (areStoragePermissionsGranted()) {
            onResult(true)
            return
        }

        onPermissionResult = onResult
        
        val androidVersion = android.os.Build.VERSION.SDK_INT
        val permissionToRequest = if (androidVersion >= 33) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            READ_EXTERNAL_STORAGE_PERMISSION
        }
        
        android.util.Log.d("PermissionHelper", "🔐 Requesting storage permission: $permissionToRequest (Android $androidVersion)")
        requestPermissionLauncher.launch(permissionToRequest)
    }

    /**
     * Check if we should show rationale for storage permissions.
     */
    fun shouldShowStoragePermissionRationale(): Boolean {
        return activity.shouldShowRequestPermissionRationale(READ_EXTERNAL_STORAGE_PERMISSION)
    }

    /**
     * Check if all required permissions for call recordings are granted.
     */
    fun areAllCallRecordingPermissionsGranted(): Boolean {
        val callLogGranted = areCallLogPermissionsGranted()
        val storageGranted = areStoragePermissionsGranted()
        val allGranted = callLogGranted && storageGranted
        
        android.util.Log.d("PermissionHelper", "🔐 Permission check - Call Log: $callLogGranted, Storage: $storageGranted, All: $allGranted")
        
        return allGranted
    }

    /**
     * Request all permissions needed for call recordings.
     * @param onResult Callback with the permission result
     */
    fun requestAllCallRecordingPermissions(onResult: (Boolean) -> Unit) {
        val permissions = mutableListOf<String>()
        
        if (!areCallLogPermissionsGranted()) {
            permissions.addAll(listOf(READ_CALL_LOG_PERMISSION, READ_PHONE_STATE_PERMISSION))
        }
        
        if (!areStoragePermissionsGranted()) {
            val androidVersion = android.os.Build.VERSION.SDK_INT
            val storagePermission = if (androidVersion >= 33) {
                android.Manifest.permission.READ_MEDIA_AUDIO
            } else {
                READ_EXTERNAL_STORAGE_PERMISSION
            }
            permissions.add(storagePermission)
        }
        
        if (permissions.isEmpty()) {
            onResult(true)
            return
        }

        android.util.Log.d("PermissionHelper", "🔐 Requesting permissions: ${permissions.joinToString()}")
        onPermissionResult = onResult
        requestMultiplePermissionsLauncher.launch(permissions.toTypedArray())
    }
}
