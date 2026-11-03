package dev.fnc314.tools.jobtracker.platform

expect class PlatformFileOpener() {
    fun openUrl(url: String)
    fun openPdf(url: String)
    fun openFile(path: String)
}
