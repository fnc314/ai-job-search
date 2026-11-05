package dev.fnc314.tools.jobtracker.platform

internal actual class PlatformAuth actual constructor() {
    actual suspend fun signIn(): Boolean = false
    actual fun signOut() {}
    actual fun isAuthenticated(): Boolean = false
}

internal actual class PlatformFileOpener {
    actual fun openUrl(url: String) {
        window.open(url, "_blank")
    }
    actual fun openPdf(url: String) {
        window.open(url, "_blank")
    }
    actual fun openFile(path: String) {
        window.open(path, "_blank")
    }
}
