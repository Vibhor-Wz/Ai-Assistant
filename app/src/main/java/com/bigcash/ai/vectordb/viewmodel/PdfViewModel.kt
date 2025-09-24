package com.bigcash.ai.vectordb.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bigcash.ai.vectordb.data.PdfEntity2
import com.bigcash.ai.vectordb.repository.PdfRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for PDF management operations.
 * This class handles the UI state and business logic for PDF operations.
 */
class PdfViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PdfRepository(application)

    // UI State
    private val _pdfs = MutableStateFlow<List<PdfEntity2>>(emptyList())
    val pdfs: StateFlow<List<PdfEntity2>> = _pdfs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _uploadSuccess = MutableStateFlow(false)
    val uploadSuccess: StateFlow<Boolean> = _uploadSuccess.asStateFlow()

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
    suspend fun getPdfById(id: Long): PdfEntity2? {
        return repository.getPdfById(id)
    }
    
    /**
     * Get the original file from local storage.
     *
     * @param pdfEntity The PDF entity containing the local file path
     * @return The original file if it exists, null otherwise
     */
    fun getOriginalFile(pdfEntity: PdfEntity2): java.io.File? {
        return repository.getOriginalFile(pdfEntity)
    }
    
    /**
     * Check if the original file exists in local storage.
     *
     * @param pdfEntity The PDF entity containing the local file path
     * @return True if the file exists, false otherwise
     */
    fun hasOriginalFile(pdfEntity: PdfEntity2): Boolean {
        return repository.hasOriginalFile(pdfEntity)
    }
    
    /**
     * Get the size of the original file in local storage.
     *
     * @param pdfEntity The PDF entity containing the local file path
     * @return The file size in bytes, or -1 if the file doesn't exist
     */
    fun getOriginalFileSize(pdfEntity: PdfEntity2): Long {
        return repository.getOriginalFileSize(pdfEntity)
    }

    
    /**
     * Clean up resources when ViewModel is destroyed.
     */
    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }
}
