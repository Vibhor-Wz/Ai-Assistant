package com.bigcash.ai.vectordb.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bigcash.ai.vectordb.data.PdfEntity
import com.bigcash.ai.vectordb.viewmodel.PdfViewModel
import kotlinx.coroutines.launch
import java.io.InputStream
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.filled.Build
import androidx.core.content.FileProvider
import java.io.File

/**
 * Main screen for PDF management functionality.
 * This screen provides UI for uploading, viewing, and managing PDF files.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfManagementScreen(
    viewModel: PdfViewModel,
    onNavigateToChat: () -> Unit = {},
    modifier: Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Collect state from ViewModel
    val pdfs by viewModel.pdfs.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val uploadSuccess by viewModel.uploadSuccess.collectAsStateWithLifecycle()

    // UI State
    var pdfName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var showUploadDialog by remember { mutableStateOf(false) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedPdfUri = it
            showUploadDialog = true
            // Extract filename from URI
            val fileName =
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex =
                        cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) {
                        cursor.getString(nameIndex)
                    } else null
                } ?: run {
                    // Fallback based on MIME type
                    val mimeType = context.contentResolver.getType(uri)
                    when {
                        mimeType?.startsWith("image/") == true -> "document.jpg"
                        mimeType?.startsWith("application/pdf") == true -> "document.pdf"
                        else -> "document.pdf"
                    }
                }
            pdfName = fileName
        }
    }

    // Show snackbar for errors
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            viewModel.clearError()
        }
    }

    // Show snackbar for upload success
    LaunchedEffect(uploadSuccess) {
        if (uploadSuccess) {
            viewModel.clearUploadSuccess()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Document Vector Database",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Row {
                IconButton(
                    onClick = { 
                        viewModel.clearAllData()
                    },
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Clear Database",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                
                IconButton(
                    onClick = onNavigateToChat,
                    enabled = true
                ) {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = "Chat",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Upload Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Upload Documents",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { filePickerLauncher.launch("application/pdf") },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Upload PDF")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PDF")
                        }

                        Button(
                            onClick = { filePickerLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Upload Image")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Image")
                        }
                    }

                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Upload Any File")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Any File Type")
                    }
                }

                Text(
                    text = "Supports PDF files, images (JPG, PNG, etc.), and documents (DOC, TXT, etc.)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Search Section
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                viewModel.searchPdfs(it)
            },
            label = { Text("Search Documents") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        // PDFs List
        if (isLoading && pdfs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (pdfs.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No documents found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Upload your first PDF or image to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pdfs) { pdf ->
                    PdfListItem(
                        pdf = pdf,
                        onDelete = { viewModel.deletePdf(pdf.id) },
                        onViewOriginal = { 
                            val originalFile = viewModel.getOriginalFile(pdf)
                            originalFile?.let { file ->
                                openFileWithExternalApp(context, file)
                            }
                        },
                        isLoading = isLoading
                    )
                }
            }
        }

    }

    // Upload Dialog
    if (showUploadDialog) {
        UploadDialog(
            pdfName = pdfName,
            onNameChange = { pdfName = it },
            onConfirm = {
                selectedPdfUri?.let { uri ->
                    scope.launch {
                        try {
                            val inputStream: InputStream? =
                                context.contentResolver.openInputStream(uri)
                            inputStream?.use { stream ->
                                val pdfData = stream.readBytes()
                                viewModel.savePdf(pdfName, pdfData)
                                showUploadDialog = false
                                selectedPdfUri = null
                                pdfName = ""
                            }
                        } catch (e: Exception) {
                            // Error will be handled by ViewModel
                        }
                    }
                }
            },
            onDismiss = {
                showUploadDialog = false
                selectedPdfUri = null
                pdfName = ""
            }
        )
    }

    // Error Snackbar
    errorMessage?.let { message ->
        LaunchedEffect(message) {
            // In a real app, you might want to show this as a Snackbar
            // For now, we'll just clear it after a delay
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }
}

/**
 * Dialog for confirming PDF upload with custom name.
 */
@Composable
fun UploadDialog(
    pdfName: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload Document") },
        text = {
            Column {
                Text("Enter a name for the document:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pdfName,
                    onValueChange = onNameChange,
                    label = { Text("Document Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Upload")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * List item for displaying PDF information.
 */
@Composable
fun PdfListItem(
    pdf: PdfEntity,
    onDelete: () -> Unit,
    onViewOriginal: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = pdf.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Size: ${formatFileSize(pdf.fileSize)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (pdf.description.isNotEmpty()) {
                    Text(
                        text = pdf.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Text(
                    text = "Uploaded: ${pdf.uploadDate.toString().substringBefore(" ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                if (pdf.localFilePath.isNotEmpty()) {
                    Text(
                        text = "Local file: ${pdf.localFilePath.substringAfterLast("/")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Row {
                IconButton(
                    onClick = onViewOriginal,
                    enabled = !isLoading && pdf.localFilePath.isNotEmpty()
                ) {
                    Icon(
                        Icons.Filled.Build,
                        contentDescription = "View Original File",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(
                    onClick = onDelete,
                    enabled = !isLoading
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete PDF",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Format file size in human-readable format.
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

/**
 * Open a file with an external app using FileProvider.
 */
private fun openFileWithExternalApp(context: android.content.Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, getMimeType(file.extension))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Fallback: try to open with any app that can handle the file
            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(fallbackIntent)
        }
    } catch (e: Exception) {
        android.util.Log.e("PdfManagementScreen", "Error opening file: ${file.name}", e)
    }
}

/**
 * Get MIME type based on file extension.
 */
private fun getMimeType(extension: String): String {
    return when (extension.lowercase()) {
        "pdf" -> "application/pdf"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "txt" -> "text/plain"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        else -> "*/*"
    }
}
