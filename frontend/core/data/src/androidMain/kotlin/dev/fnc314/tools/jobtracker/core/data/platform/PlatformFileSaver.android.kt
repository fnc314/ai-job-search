package dev.fnc314.tools.jobtracker.core.data.platform

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

public actual object PlatformFileSaver {
    public actual suspend fun saveText(content: String, filename: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val context = try {
                        val cl = Class.forName("dev.fnc314.tools.jobtracker.app.core.di.MainApplicationKt")
                        val field = cl.getDeclaredField("applicationContext")
                        field.get(null) as? android.content.Context
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
                        ) ?: throw IllegalStateException("Failed to create download URI")
                        resolver.openOutputStream(uri)?.use {
                            it.write(content.toByteArray())
                        }
                    } else {
                        throw IllegalStateException("No application context available")
                    }
                } else {
                    throw IllegalStateException("SAF requires Android Q+")
                }
            }.map { }
        }
}
