package dev.fnc314.aijobtracker.frontend.core.data.platform

import dev.fnc314.aijobtracker.frontend.core.data.model.DevicePlatform
import dev.fnc314.aijobtracker.frontend.core.data.model.PlatformDeviceInfo

internal class JsPlatformDeviceInfoProvider : PlatformDeviceInfoProvider {
    override fun getDeviceInfo(): PlatformDeviceInfo {
        return PlatformDeviceInfo(
            deviceId = "web-client",
            platform = DevicePlatform.DEVICE_PLATFORM_WEB,
            make = "Web Browser",
            model = "JavaScript",
            osVersion = "Web",
            appVersion = "1.0.0",
        )
    }
}

public actual fun createPlatformDeviceInfoProvider(): PlatformDeviceInfoProvider =
    JsPlatformDeviceInfoProvider()
