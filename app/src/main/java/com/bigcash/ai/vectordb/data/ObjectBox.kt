package com.bigcash.ai.vectordb.data

import android.content.Context
import io.objectbox.BoxStore

/**
 * ObjectBox database setup and management.
 * This class handles the initialization of ObjectBox and provides access to the BoxStore.
 */
object ObjectBox {
    private var boxStore: BoxStore? = null

    /**
     * Initialize ObjectBox with the given context.
     * This should be called in the Application class or early in the app lifecycle.
     *
     * @param context Application context
     */
    fun init(context: Context) {
        if (boxStore == null) {
            boxStore = MyObjectBox.builder()
                .androidContext(context.applicationContext)
                .build()
        }
    }

    /**
     * Get the BoxStore instance.
     * Make sure to call init() first.
     *
     * @return BoxStore instance
     * @throws IllegalStateException if ObjectBox is not initialized
     */
    fun get(): BoxStore {
        return boxStore ?: throw IllegalStateException("ObjectBox is not initialized. Call init() first.")
    }

    /**
     * Close the BoxStore and release resources.
     * This should be called when the app is being destroyed.
     */
    fun close() {
        boxStore?.close()
        boxStore = null
    }
    
    /**
     * Clear all data from the database.
     * This is useful for development or when schema changes require a fresh start.
     * WARNING: This will delete all data permanently.
     */
    fun clearAllData() {
        boxStore?.let { store ->
            try {
                // Clear all PDF entities
                store.boxFor(PdfEntity::class.java).removeAll()
                
                // Clear all TextChunk entities
                store.boxFor(TextChunkEntity::class.java).removeAll()
                
                android.util.Log.d("ObjectBox", "✅ All data cleared successfully")
            } catch (e: Exception) {
                android.util.Log.e("ObjectBox", "❌ Error clearing data", e)
            }
        }
    }
    
    /**
     * Check if the database is empty.
     * 
     * @return True if no data exists, false otherwise
     */
    fun isEmpty(): Boolean {
        return boxStore?.let { store ->
            val pdfCount = store.boxFor(PdfEntity::class.java).count()
            val textChunkCount = store.boxFor(TextChunkEntity::class.java).count()
            pdfCount == 0L && textChunkCount == 0L
        } ?: true
    }
}
