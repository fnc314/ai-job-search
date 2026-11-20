package dev.fnc314.aijobtracker.frontend.core.data.platform

import dev.fnc314.aijobtracker.frontend.core.data.model.DevicePlatform
import dev.fnc314.aijobtracker.frontend.core.data.model.PlatformDeviceInfo

internal class IosPlatformDeviceInfoProvider : PlatformDeviceInfoProvider {
    override fun getDeviceInfo(): PlatformDeviceInfo {
        return PlatformDeviceInfo(
            deviceId = "ios-device",
            platform = DevicePlatform.DEVICE_PLATFORM_IOS,
            make = "Apple",
            model = "iPhone/iPad",
            osVersion = "iOS",
            appVersion = "1.0.0",
        )
    }
}

public actual fun createPlatformDeviceInfoProvider(): PlatformDeviceInfoProvider =
    IosPlatformDeviceInfoProvider()
