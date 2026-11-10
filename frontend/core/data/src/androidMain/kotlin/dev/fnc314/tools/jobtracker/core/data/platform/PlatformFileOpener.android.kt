package dev.fnc314.tools.jobtracker.core.data.platform

import android.content.Intent
import android.net.Uri
import dev.fnc314.tools.jobtracker.core.data.ApplicationContextHolder
import java.io.File

public actual class PlatformFileOpener public actual constructor() {
    public actual fun openUrl(url: String): Unit {
        try {
            val context = ApplicationContextHolder.application ?: return
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            println("Failed to open URL on Android: ${e.message}")
        }
    }

    public actual fun openPdf(url: String): Unit {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            openUrl(url)
        } else {
            openFile(url)
        }
    }

    public actual fun openFile(path: String): Unit {
        try {
            val context = ApplicationContextHolder.application ?: return
            if (path.startsWith("http://") || path.startsWith("https://")) {
                openUrl(path)
                return
            }
            val file = File(path)
            val uri = Uri.fromFile(file)
            val mimeType = when {
                path.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                path.endsWith(".md", ignoreCase = true) || path.endsWith(".txt", ignoreCase = true) -> "text/plain"
                else -> "*/*"
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            println("Failed to open file on Android: ${e.message}")
        }
    }
}
