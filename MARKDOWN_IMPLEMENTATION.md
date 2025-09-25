# Markdown Implementation for AI Responses

This document describes the implementation of Markwon markdown rendering for AI responses in the Android chat application.

## Overview

The AI chat assistant now supports rich markdown formatting in responses, making them more readable and organized. This is particularly useful when displaying complex information from documents or providing structured answers.

## Implementation Details

### Dependencies Added

The following Markwon dependencies were added to `app/build.gradle.kts`:

```kotlin
// Markwon for text rendering
implementation("io.noties.markwon:core:4.6.2")
implementation("io.noties.markwon:html:4.6.2")
implementation("io.noties.markwon:image:4.6.2")
implementation("io.noties.markwon:linkify:4.6.2")
implementation("io.noties.markwon:ext-tables:4.6.2")
implementation("io.noties.markwon:ext-strikethrough:4.6.2")
implementation("io.noties.markwon:ext-tasklist:4.6.2")
```

**Note**: The compose plugin was not available in version 4.6.2, so the implementation uses `AndroidView` with `TextView` for markdown rendering.

### Components Created

#### 1. MarkdownText Composable (`MarkdownText.kt`)

A comprehensive markdown rendering composable with the following features:

- **Full Markdown Support**: Headers, bold, italic, code, lists, tables, blockquotes, links
- **Custom Theming**: Integrated with Material Design 3 theming
- **Link Handling**: Automatic link detection and external browser opening
- **Performance Optimized**: Uses `remember` for Markwon instance creation
- **AndroidView Integration**: Uses `AndroidView` with `TextView` for markdown rendering

#### 2. SimpleMarkdownText Composable

A lightweight version for basic markdown rendering when full features aren't needed.

### Integration Points

#### 1. ChatScreen Updates

- **MessageBubble**: Updated to use `MarkdownText` for AI responses while keeping plain text for user messages
- **Conditional Rendering**: User messages remain as plain text, AI messages use markdown

#### 2. AI Service Updates

- **Prompt Enhancement**: Updated Firebase AI service prompts to encourage markdown formatting
- **Formatting Guidelines**: Added specific instructions for AI to use markdown in responses

#### 3. ViewModel Updates

- **Error Messages**: Updated fallback and error messages to use markdown formatting
- **Consistent Styling**: All AI-generated text now uses markdown for better readability

## Supported Markdown Features

### Text Formatting
- **Bold text** using `**text**`
- *Italic text* using `*text*`
- `Code snippets` using backticks
- ~~Strikethrough~~ using `~~text~~`

### Lists
- Numbered lists: `1. Item`
- Bullet lists: `- Item`
- Task lists: `- [x] Completed` or `- [ ] Pending`

### Code Blocks
```kotlin
// Code blocks with syntax highlighting
fun example() {
    return "formatted code"
}
```

### Tables
| Column 1 | Column 2 | Column 3 |
|----------|----------|----------|
| Data 1   | Data 2   | Data 3   |

### Blockquotes
> Important information or quotes from documents

### Links
[Clickable links](https://example.com) that open in external browser

### Headers
# H1 Header
## H2 Header
### H3 Header

## Usage Examples

### Basic Usage
```kotlin
MarkdownText(
    markdown = "**Bold text** and *italic text*",
    color = MaterialTheme.colorScheme.onSurface,
    style = MaterialTheme.typography.bodyMedium
)
```

### With Custom Link Handling
```kotlin
MarkdownText(
    markdown = "Visit [our website](https://example.com)",
    onLinkClick = { url ->
        // Custom link handling
        openUrl(url)
    }
)
```

## Testing

A test screen (`MarkdownTestScreen.kt`) is available to demonstrate all markdown features. This can be used for:

- Development testing
- Feature verification
- UI/UX validation

## Benefits

1. **Improved Readability**: AI responses are now more organized and easier to scan
2. **Better Information Hierarchy**: Headers and lists help structure complex information
3. **Enhanced User Experience**: Rich formatting makes responses more engaging
4. **Document Integration**: Better display of information extracted from documents
5. **Consistent Styling**: All AI responses follow the same formatting standards

## Future Enhancements

- Image support in markdown
- Custom table styling
- Syntax highlighting for code blocks
- Custom markdown extensions for document-specific formatting
- Dark/light theme optimizations

## Performance Considerations

- Markwon instances are cached using `remember` to avoid recreation
- Only AI responses use markdown rendering (user messages remain plain text)
- Link handling is optimized for external browser opening
- Theme creation is optimized to avoid unnecessary recompositions
