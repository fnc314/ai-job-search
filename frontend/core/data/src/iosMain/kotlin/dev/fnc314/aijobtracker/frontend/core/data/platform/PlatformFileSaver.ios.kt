package dev.fnc314.aijobtracker.frontend.core.data.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithBytes
import platform.Foundation.writeToURL

/** Actual platform implementation of [PlatformFileSaver] for iOS. */
public actual object PlatformFileSaver {
    @OptIn(ExperimentalForeignApi::class)
    public actual suspend fun saveText(content: String, filename: String): Result<Unit> =
        runCatching {
            val urls = NSFileManager.defaultManager.URLsForDirectory(
                NSDocumentDirectory,
                NSUserDomainMask,
            )
            if (urls.isEmpty()) throw IllegalStateException("No document directory")
            val docUrl = urls[0] as NSURL
            val fileUrl = docUrl.URLByAppendingPathComponent(filename)
                ?: throw IllegalStateException("Failed to resolve file URL")
            val data = content.encodeToByteArray().toNSData()
            data.writeToURL(fileUrl, true)
        }

    @OptIn(ExperimentalForeignApi::class)
    private fun ByteArray.toNSData(): NSData {
        if (isEmpty()) return NSData()
        return usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), size.toULong())
        }
    }
}
