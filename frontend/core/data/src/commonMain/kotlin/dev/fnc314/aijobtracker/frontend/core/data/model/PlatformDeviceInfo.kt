package dev.fnc314.aijobtracker.frontend.core.data.model

import kotlinx.serialization.Serializable

/**
 * Supported client platform targets for push notifications and device heartbeats.
 */
@Serializable
public enum class DevicePlatform {
    DEVICE_PLATFORM_UNSPECIFIED,
    DEVICE_PLATFORM_ANDROID,
    DEVICE_PLATFORM_IOS,
    DEVICE_PLATFORM_DESKTOP,
    DEVICE_PLATFORM_WEB;

    public companion object {
        public fun fromString(value: String?): DevicePlatform {
            return when (value?.uppercase()) {
                "ANDROID", "DEVICE_PLATFORM_ANDROID" -> DEVICE_PLATFORM_ANDROID
                "IOS", "DEVICE_PLATFORM_IOS" -> DEVICE_PLATFORM_IOS
                "DESKTOP", "DEVICE_PLATFORM_DESKTOP" -> DEVICE_PLATFORM_DESKTOP
                "WEB", "DEVICE_PLATFORM_WEB" -> DEVICE_PLATFORM_WEB
                else -> DEVICE_PLATFORM_UNSPECIFIED
            }
        }
    }
}

/**
 * Bare-bones hardware and OS metadata representing a client device.
 * Excludes user identifying information.
 */
@Serializable
public data class PlatformDeviceInfo(
    public val deviceId: String,
    public val platform: DevicePlatform,
    public val make: String,
    public val model: String,
    public val osVersion: String,
    public val appVersion: String,
)
