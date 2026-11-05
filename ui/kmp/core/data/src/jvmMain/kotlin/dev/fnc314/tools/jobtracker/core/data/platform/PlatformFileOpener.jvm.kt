package dev.fnc314.tools.jobtracker.core.data.platform

import java.awt.Desktop

public actual class PlatformFileOpener public actual constructor() {
    public actual fun openUrl(url: String): Unit {
        try {
            Desktop.getDesktop().browse(java.net.URI(url))
        } catch (e: Exception) {
            println("Failed to open URL: ${e.message}")
        }
    }
    public actual fun openPdf(url: String): Unit {
        openUrl(url)
    }
    public actual fun openFile(path: String): Unit {
        try {
            Desktop.getDesktop().open(java.io.File(path))
        } catch (e: Exception) {
            println("Failed to open file: ${e.message}")
        }
    }
}
