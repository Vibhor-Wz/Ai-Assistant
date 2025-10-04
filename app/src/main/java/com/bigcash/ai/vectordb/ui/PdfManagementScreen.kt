package com.bigcash.ai.vectordb.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PlayArrow
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
import com.bigcash.ai.vectordb.viewmodel.PdfViewModel
import kotlinx.coroutines.launch
import java.io.InputStream
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.util.Log
import androidx.compose.material.icons.filled.Build
import androidx.core.content.FileProvider
import com.bigcash.ai.vectordb.ui.components.PdfListItem
import java.io.File
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Phone
import androidx.compose.ui.viewinterop.AndroidView
import com.bigcash.ai.vectordb.utils.PermissionHelper
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin

/**
 * Main screen for PDF management functionality.
 * This screen provides UI for uploading, viewing, and managing PDF files.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfManagementScreen(
    viewModel: PdfViewModel,
    onNavigateToChat: () -> Unit = {},
    modifier: Modifier,
    permissionHelper: PermissionHelper
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Collect state from ViewModel
    val pdfs by viewModel.pdfs.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val uploadSuccess by viewModel.uploadSuccess.collectAsStateWithLifecycle()
    
    // YouTube Transcript State
    val isFetchingTranscript by viewModel.isFetchingTranscript.collectAsStateWithLifecycle()
    val isSummarizingTranscript by viewModel.isSummarizingTranscript.collectAsStateWithLifecycle()
    val transcriptText by viewModel.transcriptText.collectAsStateWithLifecycle()
    val summaryText by viewModel.summaryText.collectAsStateWithLifecycle()
    val transcriptError by viewModel.transcriptError.collectAsStateWithLifecycle()

    // Audio Recording State
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val recordingError by viewModel.recordingError.collectAsStateWithLifecycle()

    // Speech Recognition State
    val recognizedText by viewModel.recognizedText.collectAsStateWithLifecycle()
    val speechError by viewModel.speechError.collectAsStateWithLifecycle()

    // Audio Summary State
    val isProcessingAudioSummary by viewModel.isProcessingAudioSummary.collectAsStateWithLifecycle()
    val audioSummaryText by viewModel.audioSummaryText.collectAsStateWithLifecycle()
    val audioSummaryError by viewModel.audioSummaryError.collectAsStateWithLifecycle()

    // UI State
    var pdfName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var showUploadDialog by remember { mutableStateOf(false) }
    
    // YouTube UI State
    var showYouTubeDialog by remember { mutableStateOf(false) }
    var showSummaryDialog by remember { mutableStateOf(false) }
    var youtubeUrl by remember { mutableStateOf("") }
    
    // Audio Summary UI State
    var showAudioSummaryDialog by remember { mutableStateOf(false) }
    var showAudioProcessingDialog by remember { mutableStateOf(false) }
    var showLanguageSelectionDialog by remember { mutableStateOf(false) }
    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAudioFileName by remember { mutableStateOf("") }
    var selectedAudioData by remember { mutableStateOf<ByteArray?>(null) }
    var selectedLanguage by remember { mutableStateOf("English") }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedPdfUri = it
            showUploadDialog = true
            // Extract filename from URI and detect file type
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
                        mimeType?.startsWith("audio/") == true -> "audio.mp3"
                        mimeType?.startsWith("image/") == true -> "document.jpg"
                        mimeType?.startsWith("application/pdf") == true -> "document.pdf"
                        else -> "document.pdf"
                    }
                }
            pdfName = fileName
        }
    }

    // Audio file picker launcher for Call Summary
    val audioFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedAudioUri = it
            // Extract filename from URI
            val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    cursor.getString(nameIndex)
                } else null
            } ?: "audio_file.mp3"
            
            selectedAudioFileName = fileName
            
            // Read audio data and show language selection dialog
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    inputStream?.use { stream ->
                        val audioData = stream.readBytes()
                        selectedAudioData = audioData
                        showLanguageSelectionDialog = true
                    }
                } catch (e: Exception) {
                    Log.e("PdfManagementScreen", "Error reading audio file", e)
                }
            }
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

    // Handle YouTube transcript results
    LaunchedEffect(transcriptText, summaryText, transcriptError) {
        when {
            summaryText.isNotEmpty() -> {
                showYouTubeDialog = false
                youtubeUrl = ""
                showSummaryDialog = true
            }
        }
    }

    // Handle audio summary results
    LaunchedEffect(audioSummaryText, audioSummaryError) {
        when {
            audioSummaryText.isNotEmpty() -> {
                showAudioProcessingDialog = false
                showAudioSummaryDialog = true
            }
            audioSummaryError != null -> {
                showAudioProcessingDialog = false
            }
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { filePickerLauncher.launch("audio/*") },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Upload Audio")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Audio")
                        }

                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Upload Any File")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Any File")
                        }
                    }

                    Button(
                        onClick = { showYouTubeDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "YouTube Transcript")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("YouTube Transcript")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            if (isRecording) {
                                viewModel.stopAudioRecording()
                            } else {
                                viewModel.startAudioRecording(permissionHelper)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && !isFetchingTranscript
                    ) {
                        Icon(Icons.Filled.Phone, contentDescription = "Audio Recording")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isRecording) "Stop Recording" else "Start Recording")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { audioFilePickerLauncher.launch("audio/*") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && !isProcessingAudioSummary
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = "Call Summary")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Call Summary")
                    }
                }

                Text(
                    text = "Supports PDF files, images (JPG, PNG, etc.), documents (DOC, TXT, etc.), and YouTube transcripts",
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
                        isLoading = isLoading,
                        showDeleteButton = true
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

    // YouTube URL Input Dialog
    if (showYouTubeDialog) {
        YouTubeUrlDialog(
            url = youtubeUrl,
            onUrlChange = { youtubeUrl = it },
            onConfirm = {
                viewModel.fetchYouTubeTranscript(youtubeUrl, "en")
                // Don't close dialog immediately - let LaunchedEffect handle it
            },
            onDismiss = {
                showYouTubeDialog = false
                youtubeUrl = ""
                viewModel.clearTranscriptData()
            },
            isLoading = isFetchingTranscript || isSummarizingTranscript,
            isSummarizing = isSummarizingTranscript,
            error = transcriptError
        )
    }

    // Summary Display Dialog
    if (showSummaryDialog) {
        SummaryDisplayDialog(
            summary = summaryText,
            onDismiss = {
                showSummaryDialog = false
                viewModel.clearTranscriptData()
            },
        )
    }
    
    // Speech Recognition Result Dialog
    if (recognizedText.isNotEmpty()) {
        SpeechResultDialog(
            recognizedText = recognizedText,
            speechError = speechError,
            onDismiss = {
                viewModel.clearSpeechData()
            }
        )
    }
    
    // Language Selection Dialog
    if (showLanguageSelectionDialog) {
        LanguageSelectionDialog(
            fileName = selectedAudioFileName,
            selectedLanguage = selectedLanguage,
            onLanguageChange = { selectedLanguage = it },
            onConfirm = {
                selectedAudioData?.let { audioData ->
                    showLanguageSelectionDialog = false
                    showAudioProcessingDialog = true
                    viewModel.processAudioFileForSummary(selectedAudioFileName, audioData, selectedLanguage)
                }
            },
            onDismiss = {
                showLanguageSelectionDialog = false
                selectedAudioData = null
                selectedAudioFileName = ""
            }
        )
    }
    
    // Audio Processing Dialog
    if (showAudioProcessingDialog) {
        AudioProcessingDialog(
            fileName = selectedAudioFileName,
            isLoading = isProcessingAudioSummary,
            error = audioSummaryError,
            onDismiss = {
                showAudioProcessingDialog = false
                viewModel.clearAudioSummaryData()
            }
        )
    }
    
    // Audio Summary Dialog
    if (showAudioSummaryDialog) {
        AudioSummaryDialog(
            summary = audioSummaryText,
            isLoading = isProcessingAudioSummary,
            error = audioSummaryError,
            onDismiss = {
                showAudioSummaryDialog = false
                viewModel.clearAudioSummaryData()
            }
        )
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
    } catch (e : Exception) {
        Log.e("PdfManagementScreen", "Error opening file: ${file.name}", e)
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

/**
 * Dialog for entering YouTube URL and fetching transcript.
 */
@Composable
fun YouTubeUrlDialog(
    url: String,
    onUrlChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean,
    isSummarizing: Boolean,
    error: String?
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("YouTube Transcript") },
        text = {
            Column {
                Text("Enter a YouTube URL to fetch its transcript:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    label = { Text("YouTube URL") },
                    placeholder = { Text("https://youtu.be/VIDEO_ID") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                if (isLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Text(
                            text = if (isSummarizing) "Summarizing transcript..." else "Fetching transcript...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = url.isNotBlank() && !isLoading
            ) {
                Text("Fetch Transcript")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for displaying YouTube transcript.
 */
@Composable
fun TranscriptDisplayDialog(
    transcript: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("YouTube Transcript") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                Text(
                    text = "Transcript:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                if (transcript.isNotEmpty()) {
                    Text(
                        text = transcript,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "No transcript available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Dialog for displaying AI-generated YouTube transcript summary.
 */
@Composable
fun SummaryDisplayDialog(
    summary: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    
    // Initialize Markwon for markdown rendering
    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(HtmlPlugin.create())
            .usePlugin(ImagesPlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TaskListPlugin.create(context))
            .build()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📺 YouTube Video Summary")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                Text(
                    text = "AI-Generated Summary:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                if (summary.isNotEmpty()) {
                    // Use Markwon to render markdown
                    AndroidView(
                        factory = { context ->
                            val textView = android.widget.TextView(context)
                            textView.textSize = 14f
                            textView.setTextColor(android.graphics.Color.BLACK)
                            textView.setPadding(0, 0, 0, 0)
                            markwon.setMarkdown(textView, summary)
                            textView
                        },
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "No summary available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Dialog to display recognized speech text.
 */
@Composable
fun SpeechResultDialog(
    recognizedText: String,
    speechError: String?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Speech Recognition Result") },
        text = {
            Column {
                if (speechError != null) {
                    Text(
                        text = "Error: $speechError",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (recognizedText.isNotEmpty()) {
                    Text(
                        text = "Recognized Text:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = recognizedText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "No speech recognized",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Dialog for displaying AI-generated audio summary.
 */
@Composable
fun AudioSummaryDialog(
    summary: String,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    // Initialize Markwon for markdown rendering
    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(HtmlPlugin.create())
            .usePlugin(ImagesPlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TaskListPlugin.create(context))
            .build()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Email, contentDescription = "Audio Summary")
                Text("🎵 Audio Call Summary")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Text(
                            text = "Processing audio and generating summary...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else if (error != null) {
                    Text(
                        text = "Error: $error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (summary.isNotEmpty()) {
                    Text(
                        text = "AI-Generated Summary:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Use Markwon to render markdown
                    AndroidView(
                        factory = { context ->
                            val textView = android.widget.TextView(context)
                            textView.textSize = 14f
                            textView.setTextColor(android.graphics.Color.BLACK)
                            textView.setPadding(0, 0, 0, 0)
                            markwon.setMarkdown(textView, summary)
                            textView
                        },
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "No summary available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Close")
            }
        }
    )
}

/**
 * Dialog for displaying audio processing status.
 */
@Composable
fun AudioProcessingDialog(
    fileName: String,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Email, contentDescription = "Audio Processing")
                Text("🎵 Processing Audio")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                Text(
                    text = "File: $fileName",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Column {
                            Text(
                                text = "Processing audio file...",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Extracting text and generating AI summary",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Processing steps
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProcessingStep(
                            text = "Reading audio file",
                            isCompleted = true
                        )
                        ProcessingStep(
                            text = "Extracting text using AI",
                            isCompleted = isLoading
                        )
                        ProcessingStep(
                            text = "Generating summary",
                            isCompleted = false
                        )
                    }
                } else if (error != null) {
                    Text(
                        text = "Error: $error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "Processing completed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Processing..." else "Close")
            }
        }
    )
}

/**
 * Individual processing step component.
 */
@Composable
private fun ProcessingStep(
    text: String,
    isCompleted: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isCompleted) {
            Icon(
                imageVector = Icons.Filled.Search, // Using search icon as checkmark
                contentDescription = "Completed",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
        }
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Dialog for selecting language for audio summary.
 */
@Composable
fun LanguageSelectionDialog(
    fileName: String,
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Email, contentDescription = "Language Selection")
                Text("🌐 Select Language")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                Text(
                    text = "File: $fileName",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Choose the language for your audio summary:",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Language selection options
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LanguageOption(
                        language = "English",
                        flag = "🇺🇸",
                        isSelected = selectedLanguage == "English",
                        onClick = { onLanguageChange("English") }
                    )
                    
                    LanguageOption(
                        language = "Hindi",
                        flag = "🇮🇳",
                        isSelected = selectedLanguage == "Hindi",
                        onClick = { onLanguageChange("Hindi") }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "The AI will generate a summary in the selected language based on the audio content.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Generate Summary")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Individual language option component.
 */
@Composable
private fun LanguageOption(
    language: String,
    flag: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) 
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else 
            null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = flag,
                style = MaterialTheme.typography.headlineMedium
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = language,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (language == "English") 
                        "Generate summary in English" 
                    else 
                        "हिंदी में सारांश उत्पन्न करें",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Search, // Using search icon as checkmark
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
