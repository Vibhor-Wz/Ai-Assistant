package com.bigcash.ai.vectordb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bigcash.ai.vectordb.ui.ChatScreen
import com.bigcash.ai.vectordb.ui.PdfManagementScreen
import com.bigcash.ai.vectordb.ui.theme.VectordbTheme
import com.bigcash.ai.vectordb.viewmodel.ChatViewModel
import com.bigcash.ai.vectordb.viewmodel.PdfViewModel

/**
 * Main Activity for the PDF Vector Database application.
 * This activity hosts the main UI for PDF management functionality.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VectordbTheme {
                MainContent(
                    modifier = Modifier
                )
            }
        }
    }
}

/**
 * Main content composable that handles navigation between PDF management and chat screens.
 */
@Composable
fun MainContent(
    modifier: Modifier = Modifier,
    pdfViewModel: PdfViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf(Screen.PdfManagement) }
    
    when (currentScreen) {
        Screen.PdfManagement -> {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                PdfManagementScreen(
                    viewModel = pdfViewModel,
                    onNavigateToChat = { currentScreen = Screen.Chat },
                    modifier = Modifier.padding(innerPadding)
                )
            }

        }
        Screen.Chat -> {
            ChatScreen(
                onNavigateBack = { currentScreen = Screen.PdfManagement },
                chatViewModel = chatViewModel,
                modifier = modifier
            )
        }
    }
}

/**
 * Enum representing the different screens in the app.
 */
enum class Screen {
    PdfManagement,
    Chat
}