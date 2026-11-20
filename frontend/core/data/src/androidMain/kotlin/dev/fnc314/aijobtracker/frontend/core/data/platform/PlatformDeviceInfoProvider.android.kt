package dev.fnc314.aijobtracker.frontend.core.data.platform

import android.os.Build
import android.provider.Settings
import dev.fnc314.aijobtracker.frontend.core.data.ApplicationContextHolder
import dev.fnc314.aijobtracker.frontend.core.data.model.DevicePlatform
import dev.fnc314.aijobtracker.frontend.core.data.model.PlatformDeviceInfo

internal class AndroidPlatformDeviceInfoProvider : PlatformDeviceInfoProvider {
    override fun getDeviceInfo(): PlatformDeviceInfo {
        val context = ApplicationContextHolder.application
        val deviceId = try {
            if (context != null) {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "android-device"
            } else {
                "android-device"
            }
        } catch (_: Throwable) {
            "android-device"
        }
        val appVersion = try {
            if (context != null) {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                pInfo.versionName ?: "unknown"
            } else {
                "unknown"
            }
        } catch (_: Throwable) {
            "unknown"
        }

        return PlatformDeviceInfo(
            deviceId = deviceId,
            platform = DevicePlatform.DEVICE_PLATFORM_ANDROID,
            make = Build.MANUFACTURER ?: "Google",
            model = Build.MODEL ?: "Android Device",
            osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            appVersion = appVersion,
        )
    }
}

public actual fun createPlatformDeviceInfoProvider(): PlatformDeviceInfoProvider =
    AndroidPlatformDeviceInfoProvider()
