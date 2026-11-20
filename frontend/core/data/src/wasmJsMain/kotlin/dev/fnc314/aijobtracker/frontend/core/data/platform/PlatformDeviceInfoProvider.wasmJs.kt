package dev.fnc314.aijobtracker.frontend.core.data.platform

import dev.fnc314.aijobtracker.frontend.core.data.model.DevicePlatform
import dev.fnc314.aijobtracker.frontend.core.data.model.PlatformDeviceInfo

internal class WasmJsPlatformDeviceInfoProvider : PlatformDeviceInfoProvider {
    override fun getDeviceInfo(): PlatformDeviceInfo {
        return PlatformDeviceInfo(
            deviceId = "wasm-client",
            platform = DevicePlatform.DEVICE_PLATFORM_WEB,
            make = "Web Browser",
            model = "WebAssembly",
            osVersion = "Web",
            appVersion = "1.0.0",
        )
    }
}

public actual fun createPlatformDeviceInfoProvider(): PlatformDeviceInfoProvider =
    WasmJsPlatformDeviceInfoProvider()
