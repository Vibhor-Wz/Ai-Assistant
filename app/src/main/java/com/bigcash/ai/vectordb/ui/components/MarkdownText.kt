package com.bigcash.ai.vectordb.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import io.noties.markwon.Markwon
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import android.widget.TextView
import androidx.compose.ui.viewinterop.AndroidView

/**
 * A composable that renders markdown text using Markwon.
 * Supports various markdown features including links, images, tables, and more.
 */
@Composable
fun MarkwonText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    onLinkClick: (String) -> Unit = { url ->
        // Default behavior: open links in external browser
        // This will be handled in the AndroidView context
    }
) {
    val context = LocalContext.current
    val textColor = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface
    
    // Create Markwon instance with all necessary plugins
    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(CorePlugin.create())
            .usePlugin(HtmlPlugin.create())
            .usePlugin(ImagesPlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TaskListPlugin.create(context))
            .build()
    }
    
    AndroidView(
        factory = { context ->
            TextView(context).apply {
                setTextColor(textColor.toArgb())
                textSize = style.fontSize.value
                typeface = when (style.fontFamily) {
                    FontFamily.Monospace -> android.graphics.Typeface.MONOSPACE
                    FontFamily.Serif -> android.graphics.Typeface.SERIF
                    FontFamily.SansSerif -> android.graphics.Typeface.SANS_SERIF
                    FontFamily.Cursive -> android.graphics.Typeface.DEFAULT
                    else -> android.graphics.Typeface.DEFAULT
                }
                
                // Set up link movement method for clickable links
                movementMethod = android.text.method.LinkMovementMethod.getInstance()
            }
        },
        update = { textView ->
            markwon.setMarkdown(textView, markdown)
        },
        modifier = modifier
    )
}

/**
 * A simple markdown text composable for basic markdown rendering.
 * Use this for simpler use cases where you don't need advanced features.
 */
@Composable
fun SimpleMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current
) {
    val context = LocalContext.current
    val textColor = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface
    
    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(CorePlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .build()
    }
    
    AndroidView(
        factory = { context ->
            TextView(context).apply {
                setTextColor(textColor.toArgb())
                textSize = style.fontSize.value
                typeface = when (style.fontFamily) {
                    FontFamily.Monospace -> android.graphics.Typeface.MONOSPACE
                    FontFamily.Serif -> android.graphics.Typeface.SERIF
                    FontFamily.SansSerif -> android.graphics.Typeface.SANS_SERIF
                    FontFamily.Cursive -> android.graphics.Typeface.DEFAULT
                    else -> android.graphics.Typeface.DEFAULT
                }
                
                // Set up link movement method for clickable links
                movementMethod = android.text.method.LinkMovementMethod.getInstance()
            }
        },
        update = { textView ->
            markwon.setMarkdown(textView, markdown)
        },
        modifier = modifier
    )
}
