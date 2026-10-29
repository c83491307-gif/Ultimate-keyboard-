package com.creazzy.novakeys.clipboard

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

data class ClipboardImage(
    val fileName: String,
    val modifiedAt: Long,
    val bytes: Long
)

object ScreenshotClipboardStore {
    private const val PREFS = "screenshot_clipboard"
    private const val AUTO_SYNC = "auto_sync_enabled"
    private const val LAST_SEEN = "auto_sync_last_seen_sec"

    fun imageDirectory(context: Context): File =
        File(context.filesDir, "screenshot_clipboard").apply { mkdirs() }

    /** No artificial item-count cap. Actual capacity is limited by free device storage. */
    fun list(context: Context, limit: Int = Int.MAX_VALUE): List<ClipboardImage> {
        return imageDirectory(context).listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension.lowercase() in setOf("png", "jpg", "jpeg", "webp", "gif", "heic", "bmp") }
            .sortedByDescending { it.lastModified() }
            .take(limit.coerceAtLeast(0))
            .map { ClipboardImage(it.name, it.lastModified(), it.length()) }
    }

    fun importUri(context: Context, uri: Uri): ClipboardImage {
        val resolver = context.contentResolver
        val sourceName = queryDisplayName(context, uri)
        val extension = validExtension(sourceName) ?: extensionForMime(resolver.getType(uri))
        val destination = File(imageDirectory(context), "image_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$extension")
        try {
            val input = resolver.openInputStream(uri) ?: error("Cannot open selected image")
            input.use { stream -> FileOutputStream(destination).use { output -> stream.copyTo(output) } }
            require(destination.length() > 0L) { "The selected image is empty" }
            return ClipboardImage(destination.name, destination.lastModified(), destination.length())
        } catch (error: Throwable) {
            destination.delete()
            throw error
        }
    }

    /** Imports a newly observed screenshot only once, even when MediaStore emits repeated notifications. */
    fun importObservedScreenshot(context: Context, uri: Uri): ClipboardImage? {
        val key = "origin_" + sha256(uri.toString()).take(32)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(key, false)) return null
        val imported = importUri(context, uri)
        prefs.edit().putBoolean(key, true).apply()
        return imported
    }

    fun saveBitmap(context: Context, bitmap: Bitmap): ClipboardImage {
        val destination = File(imageDirectory(context), "text_image_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.png")
        try {
            FileOutputStream(destination).use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Could not encode PNG" }
            }
            return ClipboardImage(destination.name, destination.lastModified(), destination.length())
        } catch (error: Throwable) {
            destination.delete()
            throw error
        }
    }

    fun delete(context: Context, image: ClipboardImage): Boolean =
        File(imageDirectory(context), image.fileName).delete()

    fun uriFor(context: Context, image: ClipboardImage): Uri {
        val file = File(imageDirectory(context), image.fileName)
        require(file.isFile && file.canonicalFile.parentFile == imageDirectory(context).canonicalFile) {
            "Image is no longer available"
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun mimeType(image: ClipboardImage): String = when (image.fileName.substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "heic" -> "image/heic"
        "bmp" -> "image/bmp"
        else -> "image/png"
    }

    fun isAutoSyncEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(AUTO_SYNC, false)

    fun setAutoSyncEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val edit = prefs.edit().putBoolean(AUTO_SYNC, enabled)
        if (enabled) edit.putLong(LAST_SEEN, System.currentTimeMillis() / 1000L - 1L)
        edit.apply()
    }

    fun canReadWholeImageLibrary(context: Context): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /** Called from a background ContentObserver while the keyboard service is alive. */
    fun scanNewScreenshots(context: Context): Int {
        if (!isAutoSyncEnabled(context) || !canReadWholeImageLibrary(context)) return 0
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val since = prefs.getLong(LAST_SEEN, System.currentTimeMillis() / 1000L - 1L)
        val projection = mutableListOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_ADDED
        )
        val hasRelativePath = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        if (hasRelativePath) projection += MediaStore.Images.Media.RELATIVE_PATH
        else projection += MediaStore.Images.Media.DATA

        var newestSeen = since
        var importedCount = 0
        val selection = "${MediaStore.Images.Media.DATE_ADDED} >= ?"
        val selectionArgs = arrayOf((since - 2L).coerceAtLeast(0L).toString())
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection.toTypedArray(),
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} ASC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val pathColumn = if (hasRelativePath) MediaStore.Images.Media.RELATIVE_PATH else MediaStore.Images.Media.DATA
            val pathIndex = cursor.getColumnIndex(pathColumn)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex).orEmpty()
                val path = if (pathIndex >= 0) cursor.getString(pathIndex).orEmpty() else ""
                val added = cursor.getLong(dateIndex)
                if (added > newestSeen) newestSeen = added
                if ((path.contains("screenshot", true) || name.contains("screenshot", true) ||
                        name.contains("screen_shot", true) || name.contains("screen-shot", true)) &&
                    added >= since - 1L
                ) {
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    runCatching { importObservedScreenshot(context, uri) }
                        .onSuccess { if (it != null) importedCount++ }
                }
            }
        }
        if (newestSeen > since) prefs.edit().putLong(LAST_SEEN, newestSeen).apply()
        return importedCount
    }

    private fun queryDisplayName(context: Context, uri: Uri): String {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else ""
            }.orEmpty()
        }.getOrDefault("")
    }

    private fun validExtension(name: String): String? {
        val extension = name.substringAfterLast('.', "").lowercase()
        return extension.takeIf { it in setOf("png", "jpg", "jpeg", "webp", "gif", "heic", "bmp") }
    }

    private fun extensionForMime(mime: String?): String = when (mime?.lowercase()) {
        "image/jpeg", "image/jpg" -> "jpg"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        "image/heic", "image/heif" -> "heic"
        "image/bmp" -> "bmp"
        else -> "png"
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
