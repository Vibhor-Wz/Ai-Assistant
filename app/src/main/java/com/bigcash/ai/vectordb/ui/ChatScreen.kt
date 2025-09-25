package com.bigcash.ai.vectordb.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import java.io.File

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
                        chatViewModel = chatViewModel
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
                                        text = "AI is thinking...",
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
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Ask me about your documents...") },
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
}

/**
 * Composable for displaying a chat message bubble.
 */
@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    chatViewModel: ChatViewModel
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
                    // User messages - plain text
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
