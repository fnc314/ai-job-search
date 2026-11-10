package dev.fnc314.tools.jobtracker.core.data.platform

import java.awt.Desktop
import java.io.File
import java.net.URI

public actual class PlatformFileOpener public actual constructor() {
    public actual fun openUrl(url: String): Unit {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
            } else {
                ProcessBuilder("open", url).start()
            }
        } catch (e: Exception) {
            println("Failed to open URL: ${e.message}")
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
            val file = File(path)
            if (file.exists()) {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(file)
                } else {
                    ProcessBuilder("open", file.absolutePath).start()
                }
            } else if (path.startsWith("http://") || path.startsWith("https://")) {
                openUrl(path)
            } else {
                println("File does not exist: $path")
            }
        } catch (e: Exception) {
            println("Failed to open file: ${e.message}")
        }
    }
}
