package dev.fnc314.aijobtracker.frontend.core.data.platform

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context

/** Actual platform implementation of [PlatformFileSaver] for Android. */
public actual object PlatformFileSaver {
    public actual suspend fun saveText(content: String, filename: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val context = try {
                        val cl = Class.forName("dev.fnc314.aijobtracker.frontend.app.core.di.MainApplicationKt")
                        val field = cl.getDeclaredField("applicationContext")
                        field.get(null) as? Context
                    } catch (_: Exception) {
                        null
                    }
                    if (context != null) {
                        val resolver = context.contentResolver
                        val values = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                            put(MediaStore.MediaColumns.MIME_TYPE, "text/markdown")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, "Downloads")
                        }
                        val uri = resolver.insert(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            values,
                        ) ?: error("Failed to create download URI")
                        resolver.openOutputStream(uri)?.use {
                            it.write(content.toByteArray())
                        }
                    } else {
                        error("No application context available")
                    }
                } else {
                    error("SAF requires Android Q+")
                }
            }.map { }
        }
}
