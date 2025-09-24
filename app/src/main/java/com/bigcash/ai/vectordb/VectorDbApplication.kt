package com.bigcash.ai.vectordb

import android.app.Application
import android.util.Log
import com.bigcash.ai.vectordb.data.ObjectBox
import com.google.firebase.FirebaseApp

/**
 * Application class for initializing global components like Firebase and ObjectBox.
 * This class is responsible for setting up Firebase and the database when the app starts.
 */
class VectorDbApplication : Application() {
    
    companion object {
        private const val TAG = "VECTOR_DEBUG"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        initializeFirebase()
        
        // Initialize ObjectBox database
        ObjectBox.init(this)
    }
    
    private fun initializeFirebase() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.d(TAG, "🔥 Firebase: Successfully initialized")
            } else {
                Log.d(TAG, "🔥 Firebase: Already initialized")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase: Initialization failed", e)
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        
        // Close ObjectBox database when app is terminated
        ObjectBox.close()
    }
}
