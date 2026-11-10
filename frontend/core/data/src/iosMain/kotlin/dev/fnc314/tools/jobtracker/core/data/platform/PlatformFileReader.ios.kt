package dev.fnc314.tools.jobtracker.core.data.platform

import kotlin.io.path.Path
import kotlin.io.path.readText

public actual object PlatformFileReader {
    public actual fun readText(path: String): String? {
        return try {
            val filePath = Path(path)
            filePath.readText()
        } catch (e: Exception) {
            null
        }
    }
}
