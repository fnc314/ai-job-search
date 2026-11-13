package dev.fnc314.tools.jobtracker.core.data.platform

import kotlinx.cinterop.BetaInteropException
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScope
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCString
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithBytes
import platform.Foundation.writeToURL

public actual object PlatformFileSaver {
    public actual suspend fun saveText(content: String, filename: String): Result<Unit> =
        runCatching {
            val urls = NSFileManager.defaultManager.URLsForDirectory(
                NSDocumentDirectory,
                NSUserDomainMask,
            )
            if (urls.isEmpty()) throw IllegalStateException("No document directory")
            val docUrl = urls[0] as NSURL
            val fileUrl = docUrl.URLByAppendingPathComponent(filename)
            val data = content.encodeToByteArray().toNSData()
            data.writeToURL(fileUrl, true)
        }

    private fun ByteArray.toNSData(): NSData = memScope {
        this@toNSData.usePinned {
            NSData.dataWithBytes(it.ptr, this@toNSData.size.toULong())
        }
    }
}
