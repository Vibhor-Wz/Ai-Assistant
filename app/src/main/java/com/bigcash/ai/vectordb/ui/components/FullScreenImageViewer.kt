package com.bigcash.ai.vectordb.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import android.util.Log

private const val TAG = "VECTOR_DEBUG"

/**
 * Full-screen image viewer with zoom and pan functionality.
 * 
 * @param imageUri The URI of the image to display
 * @param onDismiss Callback when the viewer is dismissed
 * @param modifier Modifier for the dialog
 */
@Composable
fun FullScreenImageViewer(
    imageUri: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Log.d(TAG, "🖼️ FullScreenImageViewer: Opening full-screen viewer for: $imageUri")
    
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    
    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        offsetX += offsetChange.x
        offsetY += offsetChange.y
    }
    
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(300),
        label = "scale"
    )
    
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(300),
        label = "offsetX"
    )
    
    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = tween(300),
        label = "offsetY"
    )
    
    Dialog(
        onDismissRequest = {
            Log.d(TAG, "❌ FullScreenImageViewer: Dialog dismiss requested")
            onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
        ) {
            // Close button
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Color.Red.copy(alpha = 0.8f),
                            CircleShape
                        )
                        .clickable {
                            Log.d(TAG, "❌ FullScreenImageViewer: Close button clicked")
                            onDismiss()
                        }
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Close",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            
            // Zoom controls
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextButton(
                    onClick = {
                        Log.d(TAG, "🔍 FullScreenImageViewer: Zoom in clicked")
                        scale = (scale * 1.2f).coerceIn(0.5f, 5f)
                    }
                ) {
                    Text(
                        text = "+",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                
                TextButton(
                    onClick = {
                        Log.d(TAG, "🔍 FullScreenImageViewer: Zoom out clicked")
                        scale = (scale / 1.2f).coerceIn(0.5f, 5f)
                    }
                ) {
                    Text(
                        text = "-",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                
                TextButton(
                    onClick = {
                        Log.d(TAG, "🔄 FullScreenImageViewer: Reset view clicked")
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    }
                ) {
                    Text(
                        text = "Reset",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            
            // Image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .transformable(state = transformableState)
            ) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Full screen image",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = animatedScale,
                            scaleY = animatedScale,
                            translationX = animatedOffsetX,
                            translationY = animatedOffsetY
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    Log.d(TAG, "❌ FullScreenImageViewer: Double tap detected - dismissing")
                                    onDismiss()
                                }
                            )
                        },
                    contentScale = ContentScale.Fit
                )
            }
            
        }
    }
}
