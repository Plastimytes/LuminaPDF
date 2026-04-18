package com.luminapdf.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// ── PDF Thumbnail ─────────────────────────────────────────────────────────────

@Composable
fun PdfThumbnailImage(
    thumbnailPath: String,
    fileName: String,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(thumbnailPath) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(thumbnailPath) {
        if (thumbnailPath.isNotBlank()) {
            bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    val file = File(thumbnailPath)
                    if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                }.getOrNull()
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap             = bitmap!!.asImageBitmap(),
            contentDescription = fileName,
            contentScale       = ContentScale.Crop,
            modifier           = modifier
        )
    } else {
        // Placeholder with PDF icon and initials
        Box(
            modifier           = modifier.background(MaterialTheme.colorScheme.surface),
            contentAlignment   = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector        = Icons.Outlined.PictureAsPdf,
                    contentDescription = null,
                    modifier           = Modifier.size(40.dp),
                    tint               = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = fileName.take(2).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ── Empty Library ─────────────────────────────────────────────────────────────

@Composable
fun EmptyLibraryPlaceholder(onAddClick: () -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector        = Icons.Outlined.PictureAsPdf,
            contentDescription = null,
            modifier           = Modifier.size(80.dp),
            tint               = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text      = "Your library is empty",
            style     = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color     = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = "Tap the + button to add your first PDF and start reading",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(28.dp))
        FilledTonalButton(onClick = onAddClick) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add PDF")
        }
    }
}

// ── Reading Time Formatter ────────────────────────────────────────────────────

fun formatReadingTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}
