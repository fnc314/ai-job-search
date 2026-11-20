package dev.fnc314.aijobtracker.frontend.core.data.platform

import dev.fnc314.aijobtracker.frontend.core.data.model.DevicePlatform
import dev.fnc314.aijobtracker.frontend.core.data.model.PlatformDeviceInfo
import java.net.InetAddress

internal class JvmPlatformDeviceInfoProvider : PlatformDeviceInfoProvider {
    override fun getDeviceInfo(): PlatformDeviceInfo {
        val hostName = try {
            InetAddress.getLocalHost().hostName
        } catch (_: Throwable) {
            "desktop"
        }
        val osName = System.getProperty("os.name") ?: "Desktop OS"
        val osVersion = System.getProperty("os.version") ?: ""
        val osArch = System.getProperty("os.arch") ?: ""

        return PlatformDeviceInfo(
            deviceId = "desktop-$hostName".lowercase().replace(Regex("[^a-z0-9_-]"), "-"),
            platform = DevicePlatform.DEVICE_PLATFORM_DESKTOP,
            make = if (osName.contains("Mac", ignoreCase = true)) "Apple" else "Desktop",
            model = osArch,
            osVersion = "$osName $osVersion",
            appVersion = "1.0.0",
        )
    }
}

public actual fun createPlatformDeviceInfoProvider(): PlatformDeviceInfoProvider =
    JvmPlatformDeviceInfoProvider()
