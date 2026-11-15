package dev.fnc314.tools.jobtracker.core.data.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

/** Actual platform implementation of [PlatformFileReader] for iOS. */
public actual object PlatformFileReader {
    @OptIn(ExperimentalForeignApi::class)
    public actual fun readText(path: String): String? {
        return try {
            NSString.stringWithContentsOfFile(
                path = path,
                encoding = NSUTF8StringEncoding,
                error = null,
            )
        } catch (e: Exception) {
            null
        }
    }
}
