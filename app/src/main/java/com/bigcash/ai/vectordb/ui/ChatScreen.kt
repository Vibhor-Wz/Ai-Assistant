package com.bigcash.ai.vectordb.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bigcash.ai.vectordb.viewmodel.ChatMessage
import com.bigcash.ai.vectordb.viewmodel.ChatViewModel
import com.bigcash.ai.vectordb.ui.components.PdfListItem
import com.bigcash.ai.vectordb.ui.components.MarkwonText
import com.bigcash.ai.vectordb.ui.components.FullScreenImageViewer
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import java.io.File
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter
import java.io.FileOutputStream
import java.io.InputStream
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.material.icons.filled.Build
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory

// TAG constants for logging
private const val TAG = "VECTOR_DEBUG"

/**
 * Chat screen for AI-powered document search and interaction.
 * This screen provides a conversational interface for querying documents.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    chatViewModel: ChatViewModel = viewModel()
) {
    
    val context = LocalContext.current
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val isLoading by chatViewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by chatViewModel.errorMessage.collectAsStateWithLifecycle()

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // Full-screen image viewer state
    var showFullScreenImage by remember { mutableStateOf(false) }
    var fullScreenImageUri by remember { mutableStateOf("") }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            Log.d(TAG, "🖼️ ChatScreen: Image selected from gallery: $selectedUri")
            // Create a temporary file to store the image
            val tempFile = createTempImageFile(context, selectedUri)
            tempFile?.let { file ->
                chatViewModel.sendImageMessage(selectedUri.toString(), file)
            }
        }
    }

    // Log screen initialization
    LaunchedEffect(Unit) {
        Log.d(TAG, "🚀 ChatScreen: Screen initialized")
        Log.d(TAG, "📊 ChatScreen: Initial message count: ${messages.size}")
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            Log.d(TAG, "📜 ChatScreen: Auto-scrolling to message ${messages.size - 1}")
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI Chat Assistant",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { 
                            Log.d(TAG, "🗑️ ChatScreen: Clearing chat")
                            chatViewModel.clearChat()
                            Log.d(TAG, "✅ ChatScreen: Chat cleared")
                        }
                    ) {
                        Text(
                            text = "Clear",
                            fontSize = 14.sp
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier.padding(paddingValues).fillMaxSize()
        ) {

            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        modifier = Modifier.fillMaxWidth(),
                        chatViewModel = chatViewModel,
                        onImageClick = { imageUri ->
                            Log.d(TAG, "🖼️ ChatScreen: Image clicked, opening full-screen viewer")
                            fullScreenImageUri = imageUri
                            showFullScreenImage = true
                        }
                    )
                }

                // Loading indicator
                if (isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isImageEditRequest(messageText)) {
                                            "AI is editing your image..."
                                        } else {
                                            "AI is thinking..."
                                        },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

            }

            // Error message
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Message Input
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Plus icon for gallery
                    IconButton(
                        onClick = {
                            Log.d(TAG, "📷 ChatScreen: Opening gallery")
                            galleryLauncher.launch("image/*")
                        },
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Image",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { 
                            val hasImage = messages.any { it.isUser && it.imageFile != null && !it.isImageGenerated }
                            Text(
                                if (hasImage) {
                                    "Ask me anything about the image..."
                                } else {
                                    "Ask me about your documents..."
                                }
                            )
                        },
                        modifier = Modifier.weight(1f),
                        maxLines = 3,
                        enabled = !isLoading
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank() && !isLoading) {
                                Log.d(TAG, "📤 ChatScreen: Sending message: '$messageText'")
                                chatViewModel.sendMessage(messageText)
                                messageText = ""
                                Log.d(TAG, "✅ ChatScreen: Message sent and input cleared")
                            } else {
                                Log.d(TAG, "⚠️ ChatScreen: Cannot send message - text: '${messageText.isNotBlank()}', loading: $isLoading")
                            }
                        },
                        enabled = messageText.isNotBlank() && !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (messageText.isNotBlank() && !isLoading) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Full-screen image viewer
    if (showFullScreenImage) {
        Log.d(TAG, "🖼️ ChatScreen: Showing full-screen image viewer for: $fullScreenImageUri")
        FullScreenImageViewer(
            imageUri = fullScreenImageUri,
            onDismiss = {
                Log.d(TAG, "❌ ChatScreen: Closing full-screen image viewer")
                showFullScreenImage = false
                fullScreenImageUri = ""
                Log.d(TAG, "✅ ChatScreen: Full-screen image viewer state updated")
            }
        )
    }
}

/**
 * Composable for displaying a chat message bubble.
 */
@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    chatViewModel: ChatViewModel,
    onImageClick: (String) -> Unit = {}
) {
    
    val context = LocalContext.current
    
    // Log message details
    LaunchedEffect(message) {
        Log.d(TAG, "💬 MessageBubble: Rendering message")
        Log.d(TAG, "   📝 Text length: ${message.text.length}")
        Log.d(TAG, "   👤 Is user: ${message.isUser}")
        Log.d(TAG, "   📄 Has PDF entity: ${message.pdfEntity != null}")
        Log.d(TAG, "   📁 Has file: ${message.file != null}")
        if (message.pdfEntity != null) {
            Log.d(TAG, "   📄 PDF name: ${message.pdfEntity.name}")
            Log.d(TAG, "   📁 PDF local path: ${message.pdfEntity.localFilePath}")
        }
        if (message.file != null) {
            Log.d(TAG, "   📁 File name: ${message.file.name}")
            Log.d(TAG, "   📊 File size: ${message.file.length()} bytes")
        }
    }
    
    Row(
        modifier = modifier,
        horizontalArrangement = if (message.isUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        }
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (message.isUser) {
                    // User messages - text and images
                    if (message.imageUri != null && message.imageFile != null) {
                        // Display image with download button
                        Column {
                            Image(
                                painter = rememberAsyncImagePainter(Uri.parse(message.imageUri)),
                                contentDescription = "Selected image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        Log.d(TAG, "🖼️ MessageBubble: User image clicked")
                                        onImageClick(message.imageUri!!)
                                    },
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = {
                                        Log.d(TAG, "💾 MessageBubble: Downloading image")
                                        downloadImageToGallery(context, message.imageFile!!)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = "Download Image",
                                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = message.text,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    // AI messages - markdown text
                    MarkwonText(
                        markdown = message.text,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    // Show edited image if this is an image edit response
                    if (message.isImageEdit && message.imageUri != null && message.imageFile != null) {
                        Log.d(TAG, "🖼️ MessageBubble: Displaying edited image")
                        Spacer(modifier = Modifier.height(8.dp))
                        Column {
                            Image(
                                painter = rememberAsyncImagePainter(Uri.parse(message.imageUri)),
                                contentDescription = "Edited image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        Log.d(TAG, "🖼️ MessageBubble: AI edited image clicked")
                                        onImageClick(message.imageUri!!)
                                    },
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = {
                                        Log.d(TAG, "💾 MessageBubble: Downloading edited image")
                                        downloadImageToGallery(context, message.imageFile!!)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = "Download Edited Image",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    
                    // Show file card if file is available and response is not TEXT_ONLY
                    if (message.pdfEntity != null) {
                        Log.d(TAG, "📄 MessageBubble: Displaying PDF file card for: ${message.pdfEntity.name}")
                        Spacer(modifier = Modifier.height(8.dp))
                        PdfListItem(
                            pdf = message.pdfEntity,
                            onViewOriginal = {
                                Log.d(TAG, "👆 MessageBubble: User clicked view original file")
                                val fileToOpen = message.file ?: chatViewModel.getOriginalFile(message.pdfEntity)
                                if (fileToOpen != null) {
                                    Log.d(TAG, "📁 MessageBubble: Opening file: ${fileToOpen.name}")
                                    openFileWithExternalApp(context, fileToOpen)
                                } else {
                                    Log.w(TAG, "⚠️ MessageBubble: No file available to open")
                                }
                            },
                            onDelete = null, // No delete functionality in chat
                            showDeleteButton = false // Hide delete button
                        )
                    } else {
                        Log.d(TAG, "📄 MessageBubble: No PDF entity to display")
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = formatTimestamp(message.timestamp),
                    color = if (message.isUser) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Format timestamp for display.
 */
private fun formatTimestamp(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return formatter.format(date)
}

/**
 * Check if the message is an image editing request.
 * This is a simplified version for UI display purposes.
 */
private fun isImageEditRequest(message: String): Boolean {
    val lowerMessage = message.lowercase()
    val editKeywords = listOf(
        "change background", "change the background", "edit background", "edit the background",
        "modify background", "modify the background", "replace background", "replace the background",
        "edit image", "edit the image", "modify image", "modify the image",
        "change image", "change the image", "transform image", "transform the image"
    )
    
    return editKeywords.any { keyword -> 
        lowerMessage.contains(keyword) 
    }
}

/**
 * Open a file with an external app using FileProvider.
 */
private fun openFileWithExternalApp(context: android.content.Context, file: File) {

    
    try {
        Log.d(TAG, "📁 FileOpener: Attempting to open file: ${file.name}")
        Log.d(TAG, "📁 FileOpener: File path: ${file.absolutePath}")
        Log.d(TAG, "📁 FileOpener: File size: ${file.length()} bytes")
        Log.d(TAG, "📁 FileOpener: File exists: ${file.exists()}")
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        Log.d(TAG, "🔗 FileOpener: Generated URI: $uri")
        
        val mimeType = getMimeType(file.extension)
        Log.d(TAG, "📄 FileOpener: MIME type: $mimeType")
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        if (intent.resolveActivity(context.packageManager) != null) {
            Log.d(TAG, "✅ FileOpener: Found app to handle file, starting activity")
            context.startActivity(intent)
        } else {
            Log.w(TAG, "⚠️ FileOpener: No specific app found, trying fallback")
            // Fallback: try to open with any app that can handle the file
            val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(fallbackIntent)
            Log.d(TAG, "🔄 FileOpener: Fallback intent started")
        }
    } catch (e: Exception) {
        Log.e(TAG, "❌ FileOpener: Error opening file: ${file.name}", e)
    }
}

/**
 * Get MIME type based on file extension.
 */
private fun getMimeType(extension: String): String {

    
    val mimeType = when (extension.lowercase()) {
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
    
    Log.d(TAG, "📄 MimeTypeHelper: Extension '$extension' -> MIME type '$mimeType'")
    return mimeType
}

/**
 * Create a temporary file to store the selected image.
 */
private fun createTempImageFile(context: android.content.Context, uri: Uri): File? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val tempFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(tempFile)
        
        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        
        Log.d(TAG, "📁 ImageHandler: Created temp file: ${tempFile.absolutePath}")
        tempFile
    } catch (e: Exception) {
        Log.e(TAG, "❌ ImageHandler: Error creating temp file", e)
        null
    }
}

/**
 * Download image to external storage (gallery).
 */
private fun downloadImageToGallery(context: android.content.Context, imageFile: File) {
    try {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "AI_Chat_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AI_Chat")
        }
        
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        
        uri?.let { imageUri ->
            context.contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                imageFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            Toast.makeText(context, "Image saved to gallery", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "✅ ImageHandler: Image saved to gallery: $imageUri")
        } ?: run {
            Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "❌ ImageHandler: Failed to create image URI")
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
        Log.e(TAG, "❌ ImageHandler: Error saving image", e)
    }
}
