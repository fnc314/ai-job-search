package dev.fnc314.aijobtracker.frontend.core.data.platform

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import dev.fnc314.aijobtracker.frontend.core.data.ApplicationContextHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Actual platform implementation of [PlatformFileSaver] for Android. */
public actual object PlatformFileSaver {
    public actual suspend fun saveText(content: String, filename: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val context = ApplicationContextHolder.application
                        ?: error("No application context available")
                    val mimeType = when {
                        filename.endsWith(".tex", ignoreCase = true) -> "text/x-tex"
                        filename.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                        filename.endsWith(".md", ignoreCase = true) -> "text/markdown"
                        else -> "text/plain"
                    }
                    val resolver = context.contentResolver
                    val values = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "Downloads")
                    }
                    val uri = resolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        values,
                    ) ?: error("Failed to create download URI")
                    resolver.openOutputStream(uri)?.use {
                        it.write(content.toByteArray())
                    } ?: error("Failed to open output stream for $uri")
                } else {
                    error("SAF requires Android Q+")
                }
            }.map { }
        }
}
