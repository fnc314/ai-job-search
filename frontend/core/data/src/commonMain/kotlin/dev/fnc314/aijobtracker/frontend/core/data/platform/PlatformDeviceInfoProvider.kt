package dev.fnc314.aijobtracker.frontend.core.data.platform

import dev.fnc314.aijobtracker.frontend.core.data.model.PlatformDeviceInfo

/**
 * Multiplatform hardware & OS inspection interface resolving bare-bones device metadata.
 */
public interface PlatformDeviceInfoProvider {
    /** Resolves the hardware and operating system metadata for the host device. */
    public fun getDeviceInfo(): PlatformDeviceInfo
}

/**
 * Platform factory resolving the native [PlatformDeviceInfoProvider].
 */
public expect fun createPlatformDeviceInfoProvider(): PlatformDeviceInfoProvider
