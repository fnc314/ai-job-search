package dev.fnc314.tools.jobtracker.platform

import android.content.Intent
import android.provider.Browser
import androidx.core.content.ContextCompat.startActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class PlatformFileOpener {
    actual fun openUrl(url: String) {
        // TODO: Get context from Android application
        println("Opening URL: $url")
    }

    actual fun openPdf(url: String) {
        println("Opening PDF: $url")
    }

    actual fun openFile(path: String) {
        println("Opening file: $path")
    }
}
