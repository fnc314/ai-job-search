package dev.fnc314.tools.jobtracker.core.data.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.khronos.webgl.Uint8Array
import org.w3c.dom.files.Blob
import org.w3c.dom.url.URL

public actual object PlatformFileSaver {
    public actual suspend fun saveText(content: String, filename: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                val blob = Blob(arrayOf(content), BlobPropertyBag("text/markdown"))
                val url = URL.createObjectURL(blob)
                downloadUrl(url, filename)
                URL.revokeObjectURL(url)
            }
        }
}

private fun downloadUrl(url: String, filename: String) {
    val document = js("document").dynamic
    val a = document.createElement("a")
    a.href = url
    a.download = filename
    a.style.display = "none"
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
}
