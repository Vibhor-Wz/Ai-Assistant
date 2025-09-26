package com.bigcash.ai.vectordb.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.Manifest
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
import android.util.Log
import androidx.compose.material.icons.filled.Build
import androidx.core.content.FileProvider
import com.bigcash.ai.vectordb.ui.components.PdfListItem
import java.io.File
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Create

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
    
    // YouTube Transcript State
    val isFetchingTranscript by viewModel.isFetchingTranscript.collectAsStateWithLifecycle()
    val isSummarizingTranscript by viewModel.isSummarizingTranscript.collectAsStateWithLifecycle()
    val transcriptText by viewModel.transcriptText.collectAsStateWithLifecycle()
    val summaryText by viewModel.summaryText.collectAsStateWithLifecycle()
    val transcriptError by viewModel.transcriptError.collectAsStateWithLifecycle()
    
    // Speech Recognition State
    val isRecognizingSpeech by viewModel.isRecognizingSpeech.collectAsStateWithLifecycle()
    val recognizedText by viewModel.recognizedText.collectAsStateWithLifecycle()
    val isGeneratingSpeechSummary by viewModel.isGeneratingSpeechSummary.collectAsStateWithLifecycle()
    val speechSummaryText by viewModel.speechSummaryText.collectAsStateWithLifecycle()
    val speechError by viewModel.speechError.collectAsStateWithLifecycle()
    val isContinuousMode by viewModel.isContinuousMode.collectAsStateWithLifecycle()

    // UI State
    var pdfName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var showUploadDialog by remember { mutableStateOf(false) }
    
    // YouTube UI State
    var showYouTubeDialog by remember { mutableStateOf(false) }
    var showSummaryDialog by remember { mutableStateOf(false) }
    var youtubeUrl by remember { mutableStateOf("") }
    
    // Speech Recognition UI State
    var showSpeechSummaryDialog by remember { mutableStateOf(false) }

    // Permission launcher for RECORD_AUDIO
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("PdfManagementScreen", "✅ Microphone permission granted")
            viewModel.startContinuousSpeechRecognition()
        } else {
            Log.w("PdfManagementScreen", "❌ Microphone permission denied")
            // Error will be handled by ViewModel
        }
    }

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

    // Handle speech recognition results - show dialog when summary is ready
    LaunchedEffect(speechSummaryText, isRecognizingSpeech) {
        Log.d("PdfManagementScreen", "📱 LaunchedEffect triggered - speechSummaryText: '${speechSummaryText}', isRecognizingSpeech: $isRecognizingSpeech")
        if (speechSummaryText.isNotEmpty() && !isRecognizingSpeech) {
            Log.d("PdfManagementScreen", "📱 Showing speech summary dialog")
            showSpeechSummaryDialog = true
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

                    Button(
                        onClick = { showYouTubeDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "YouTube Transcript")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("YouTube Transcript")
                    }

                    // Toggle Speech Recognition Button
                    Button(
                        onClick = {
                            if (isRecognizingSpeech && isContinuousMode) {
                                // Currently listening - force stop to prevent restarts
                                Log.d("PdfManagementScreen", "🛑 Force stopping speech recognition")
                                viewModel.forceStopSpeechRecognition()
                            } else {
                                // Not listening - start
                                if (viewModel.hasRecordAudioPermission()) {
                                    Log.d("PdfManagementScreen", "▶️ Starting continuous speech recognition")
                                    viewModel.startContinuousSpeechRecognition()
                                } else {
                                    Log.d("PdfManagementScreen", "🎤 Requesting microphone permission")
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && !isGeneratingSpeechSummary,
                        colors = if (isRecognizingSpeech && isContinuousMode) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        } else {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        if (isRecognizingSpeech && isContinuousMode) {
                            Icon(Icons.Filled.Delete, contentDescription = "Stop Recording")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop Recording")
                        } else {
                            Icon(Icons.Filled.Add, contentDescription = "Start Recording")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Recording")
                        }
                    }


                // Speech recognition status indicator
                if (isRecognizingSpeech || isGeneratingSpeechSummary) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isRecognizingSpeech) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isContinuousMode) "Listening continuously..." else "Listening...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (isGeneratingSpeechSummary) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Generating summary...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else if (!viewModel.hasRecordAudioPermission()) {
                    // Permission status indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "⚠️ Microphone permission required",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Text(
                    text = "Supports PDF files, images (JPG, PNG, etc.), documents (DOC, TXT, etc.), YouTube transcripts, and voice recognition",
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

    // Speech Recognition Summary Dialog
    if (showSpeechSummaryDialog) {
        SpeechSummaryDialog(
            recognizedText = recognizedText,
            summary = speechSummaryText,
            isLoading = isGeneratingSpeechSummary,
            error = speechError,
            onDismiss = {
                showSpeechSummaryDialog = false
            },
            onClear = {
                viewModel.clearSpeechData()
                showSpeechSummaryDialog = false
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
 * Dialog for displaying speech recognition results and AI-generated summary.
 */
@Composable
fun SpeechSummaryDialog(
    recognizedText: String,
    summary: String,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onClear: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Voice Recognition Summary")
                TextButton(onClick = onClear) {
                    Text("Clear")
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 500.dp)
            ) {
                item {
                    if (recognizedText.isNotEmpty()) {
                        Text(
                            text = "Recognized Text:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = recognizedText,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                
                if (error != null) {
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Text(
                            text = "Generating summary...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else if (summary.isNotEmpty()) {
                    Text(
                        text = "AI Summary:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    )
                }
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