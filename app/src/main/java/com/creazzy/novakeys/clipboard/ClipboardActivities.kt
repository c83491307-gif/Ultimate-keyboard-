package com.creazzy.novakeys.clipboard

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date

class ScreenshotClipboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ClipboardLibraryScreen() }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ClipboardLibraryScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var images by remember { mutableStateOf(ScreenshotClipboardStore.list(context)) }
    var status by remember { mutableStateOf("") }
    var autoSync by remember { mutableStateOf(ScreenshotClipboardStore.isAutoSyncEnabled(context)) }
    var confirmClear by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(150)) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    var count = 0
                    var failed = 0
                    uris.forEach { uri ->
                        runCatching { ScreenshotClipboardStore.importUri(context, uri); count++ }
                            .onFailure { failed++ }
                    }
                    count to failed
                }
                images = withContext(Dispatchers.IO) { ScreenshotClipboardStore.list(context) }
                status = "Imported ${result.first} image(s)" + if (result.second > 0) "; ${result.second} failed" else ""
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val fullAccess = ScreenshotClipboardStore.canReadWholeImageLibrary(context)
        if (fullAccess) {
            ScreenshotClipboardStore.setAutoSyncEnabled(context, true)
            autoSync = true
            status = "Automatic screenshot import enabled while NovaKeys is running"
        } else {
            ScreenshotClipboardStore.setAutoSyncEnabled(context, false)
            autoSync = false
            status = if (result.values.any { it }) {
                "Android granted selected-photo access only. Use Import screenshots for manual selection."
            } else {
                "Permission not granted. You can still import screenshots manually."
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Screenshot & Image Clipboard") }) }) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Keep as many screenshots and images as device storage allows. Images stay on this device in NovaKeys and appear in the keyboard's recent-image strip.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = {
                picker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }, modifier = Modifier.fillMaxWidth()) { Text("Import one or more screenshots") }

            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Auto-import new screenshots", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Requires full image-library permission. Import runs when the NovaKeys keyboard service is alive; Android may pause it if the service is stopped.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(checked = autoSync, onCheckedChange = { enabled ->
                        if (!enabled) {
                            ScreenshotClipboardStore.setAutoSyncEnabled(context, false)
                            autoSync = false
                            status = "Automatic screenshot import paused"
                        } else if (ScreenshotClipboardStore.canReadWholeImageLibrary(context)) {
                            ScreenshotClipboardStore.setAutoSyncEnabled(context, true)
                            autoSync = true
                            status = "Automatic screenshot import enabled"
                        } else {
                            val permissions = when {
                                Build.VERSION.SDK_INT >= 34 -> arrayOf(
                                    Manifest.permission.READ_MEDIA_IMAGES,
                                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                                )
                                Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                                else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                            permissionLauncher.launch(permissions)
                        }
                    })
                }
            }
            if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.primary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${images.size} saved image(s)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (images.isNotEmpty()) TextButton(onClick = { confirmClear = true }) { Text("Clear all") }
            }
            if (images.isEmpty()) {
                Text("No saved images yet. Import a batch of screenshots or copy text and use Text → Image from the keyboard.")
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(images, key = { it.fileName }) { item ->
                        val thumbnail = remember(item.fileName) { decodeThumbnail(context, item) }
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (thumbnail != null) {
                                    Image(
                                        bitmap = thumbnail.asImageBitmap(),
                                        contentDescription = item.fileName,
                                        modifier = Modifier.size(72.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.fileName, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(item.modifiedAt)),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(formatBytes(item.bytes), style = MaterialTheme.typography.bodySmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(onClick = { shareImage(context, item) }) { Text("Share") }
                                        TextButton(onClick = {
                                            ScreenshotClipboardStore.delete(context, item)
                                            images = ScreenshotClipboardStore.list(context)
                                            status = "Image deleted"
                                        }) { Text("Delete") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Delete all saved images?") },
        text = { Text("This deletes all images stored in NovaKeys' image clipboard. It does not delete the originals in your phone's gallery.") },
        confirmButton = {
            TextButton(onClick = {
                images.forEach { ScreenshotClipboardStore.delete(context, it) }
                images = emptyList()
                confirmClear = false
                status = "Saved image clipboard cleared"
            }) { Text("Delete all") }
        },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
    )
}

class TextToImageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TextToImageScreen() }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun TextToImageScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf(readClipboardText(context)) }
    var generated by remember { mutableStateOf<ClipboardImage?>(null) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Text → Image") }) }) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Paste copied text, including emoji. NovaKeys renders it locally as a clear PNG; the image is added to your recent-image keyboard strip.")
            OutlinedButton(
                onClick = { text = readClipboardText(context); status = "Loaded text from the clipboard" },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Load text from clipboard") }
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { text = it; generated = null; preview = null },
                label = { Text("Text to turn into an image") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                maxLines = 14
            )
            Button(
                enabled = !busy,
                onClick = {
                    busy = true
                    status = "Rendering image…"
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.Default) {
                                val bitmap = TextImageRenderer.render(text)
                                val item = withContext(Dispatchers.IO) { ScreenshotClipboardStore.saveBitmap(context, bitmap) }
                                bitmap to item
                            }
                        }.onSuccess { (bitmap, item) ->
                            preview = bitmap
                            generated = item
                            status = "PNG created and added to the keyboard image clipboard"
                        }.onFailure { error ->
                            status = error.message ?: "Could not create image"
                        }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) "Creating…" else "Create PNG and add to image clipboard") }

            if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.primary)
            preview?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Preview of generated text image",
                    modifier = Modifier.fillMaxWidth().height(380.dp),
                    contentScale = ContentScale.Fit
                )
            }
            generated?.let { item ->
                Button(onClick = { shareImage(context, item) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Share image / send to another app")
                }
                OutlinedButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val uri = ScreenshotClipboardStore.uriFor(context, item)
                    clipboard.setPrimaryClip(ClipData.newUri(context.contentResolver, item.fileName, uri))
                    status = "Image URI copied. For best results, return to the chat and choose the image from NovaKeys' keyboard strip."
                }, modifier = Modifier.fillMaxWidth()) { Text("Copy image to system clipboard") }
            }
        }
    }
}

private fun readClipboardText(context: Context): String {
    return runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        if (!clipboard.hasPrimaryClip()) "" else clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
    }.getOrDefault("")
}

private fun decodeThumbnail(context: Context, item: ClipboardImage): Bitmap? {
    val file = File(ScreenshotClipboardStore.imageDirectory(context), item.fileName)
    val options = BitmapFactory.Options().apply { inSampleSize = 8 }
    return runCatching { BitmapFactory.decodeFile(file.absolutePath, options) }.getOrNull()
}

private fun shareImage(context: Context, item: ClipboardImage) {
    runCatching {
        val uri = ScreenshotClipboardStore.uriFor(context, item)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = ScreenshotClipboardStore.mimeType(item)
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, item.fileName, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share image"))
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}
