package dev.fnc314.tools.jobtracker.core.data.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

public actual object PlatformFileSaver {
    public actual suspend fun saveText(content: String, filename: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val downloads = File(System.getProperty("user.home"), "Downloads")
                if (!downloads.exists()) downloads.mkdirs()
                File(downloads, filename).writeText(content)
            }
        }
}
