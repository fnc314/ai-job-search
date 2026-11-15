package dev.fnc314.tools.jobtracker.core.data.platform

import java.io.File

/** Actual platform implementation of [PlatformFileReader] for JVM / Desktop. */
public actual object PlatformFileReader {
    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    public actual fun readText(path: String): String? {
        return try {
            val file = File(path)
            if (file.exists() && file.isFile) {
                file.readText(Charsets.UTF_8)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
