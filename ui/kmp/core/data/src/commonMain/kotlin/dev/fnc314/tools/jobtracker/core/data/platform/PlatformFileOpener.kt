package dev.fnc314.tools.jobtracker.core.data.platform

public expect class PlatformFileOpener() {
    public fun openUrl(url: String): Unit
    public fun openPdf(url: String): Unit
    public fun openFile(path: String): Unit
}
