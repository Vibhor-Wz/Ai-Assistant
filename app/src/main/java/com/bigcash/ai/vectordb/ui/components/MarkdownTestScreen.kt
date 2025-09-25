package com.bigcash.ai.vectordb.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Test screen to demonstrate markdown rendering capabilities.
 * This can be used for testing and development purposes.
 */
@Composable
fun MarkdownTestScreen(
    modifier: Modifier = Modifier
) {
    val sampleMarkdown = """
        # Markdown Test Screen
        
        This screen demonstrates the **markdown rendering** capabilities of the AI chat assistant.
        
        ## Features Supported
        
        ### Text Formatting
        - **Bold text** for emphasis
        - *Italic text* for subtle emphasis
        - `Code snippets` for technical terms
        - ~~Strikethrough~~ for corrections
        
        ### Lists
        1. **Numbered lists** for step-by-step instructions
        2. Easy to follow and organized
        3. Great for procedures and guides
        
        - **Bullet points** for general lists
        - Easy to scan and read
        - Perfect for features and benefits
        
        ### Code Blocks
        ```kotlin
        fun exampleFunction() {
            println("This is a code block")
            return "Formatted code"
        }
        ```
        
        ### Blockquotes
        > This is a blockquote that can be used to highlight important information or quotes from documents.
        > It's particularly useful for emphasizing key points from retrieved documents.
        
        ### Tables
        | Feature | Status | Description |
        |---------|--------|-------------|
        | Bold | ✅ | **Bold text** rendering |
        | Italic | ✅ | *Italic text* rendering |
        | Code | ✅ | `Code` rendering |
        | Lists | ✅ | Bullet and numbered lists |
        | Links | ✅ | [Clickable links](https://example.com) |
        
        ### Links
        You can include [clickable links](https://example.com) that will open in the external browser.
        
        ### Task Lists
        - [x] Implement markdown rendering
        - [x] Add support for basic formatting
        - [x] Test with various markdown features
        - [ ] Add image support (future enhancement)
        - [ ] Add table styling (future enhancement)
        
        ---
        
        This markdown rendering makes AI responses much more **readable** and **organized**, especially when dealing with complex information from documents.
    """.trimIndent()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Markdown Rendering Test",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            MarkwonText(
                markdown = sampleMarkdown,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
