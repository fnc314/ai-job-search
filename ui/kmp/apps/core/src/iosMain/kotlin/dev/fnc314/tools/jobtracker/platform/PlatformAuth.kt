package dev.fnc314.tools.jobtracker.platform

internal actual class PlatformAuth actual constructor() {
    actual suspend fun signIn(): Boolean = false
    actual fun signOut() {}
    actual fun isAuthenticated(): Boolean = false
}

internal actual class PlatformFileOpener {
    actual fun openUrl(url: String) {
        println("Opening URL: $url")
    }
    actual fun openPdf(url: String) {
        println("Opening PDF: $url")
    }
    actual fun openFile(path: String) {
        println("Opening file: $path")
    }
}
