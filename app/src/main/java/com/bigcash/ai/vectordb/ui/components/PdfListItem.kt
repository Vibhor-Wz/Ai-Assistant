package com.bigcash.ai.vectordb.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bigcash.ai.vectordb.data.PdfEntity

/**
 * List item for displaying PDF information.
 * This is a reusable component that can be used in both PDF management and chat screens.
 */
@Composable
fun PdfListItem(
    pdf: PdfEntity,
    onViewOriginal: () -> Unit,
    onDelete: (() -> Unit)? = null, // Optional delete functionality
    isLoading: Boolean = false,
    showDeleteButton: Boolean = true
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
                
                // Only show delete button if explicitly requested and delete function is provided
                if (showDeleteButton && onDelete != null) {
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
