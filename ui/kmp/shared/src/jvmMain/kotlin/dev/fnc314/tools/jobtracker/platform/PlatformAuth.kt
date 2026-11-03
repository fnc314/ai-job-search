package dev.fnc314.tools.jobtracker.platform

import java.awt.Desktop

actual class PlatformAuth actual constructor() {
    actual suspend fun signIn(): Boolean = false
    actual fun signOut() {}
    actual fun isAuthenticated(): Boolean = false
}

actual class PlatformFileOpener {
    actual fun openUrl(url: String) {
        try {
            Desktop.getDesktop().browse(java.net.URI(url))
        } catch (e: Exception) {
            println("Failed to open URL: ${e.message}")
        }
    }
    actual fun openPdf(url: String) {
        openUrl(url)
    }
    actual fun openFile(path: String) {
        try {
            Desktop.getDesktop().open(java.io.File(path))
        } catch (e: Exception) {
            println("Failed to open file: ${e.message}")
        }
    }
}
