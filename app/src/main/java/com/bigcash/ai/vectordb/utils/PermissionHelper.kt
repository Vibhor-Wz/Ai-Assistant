package com.bigcash.ai.vectordb.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Helper class to handle runtime permissions for audio recording.
 */
class PermissionHelper(private val activity: ComponentActivity) {

    companion object {
        const val RECORD_AUDIO_PERMISSION = Manifest.permission.RECORD_AUDIO
    }

    private var onPermissionResult: ((Boolean) -> Unit)? = null

    private val requestPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        onPermissionResult?.invoke(isGranted)
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
}
