package dev.fnc314.tools.jobtracker.core.data.platform

public actual class PlatformFileOpener public actual constructor() {
    public actual fun openUrl(url: String): Unit {
        println("Opening URL: $url")
    }
    public actual fun openPdf(url: String): Unit {
        println("Opening PDF: $url")
    }
    public actual fun openFile(path: String): Unit {
        println("Opening file: $path")
    }
}
