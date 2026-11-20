package dev.fnc314.aijobtracker.frontend.core.data.source

import dev.fnc314.aijobtracker.frontend.core.data.model.DevicePlatform
import dev.fnc314.aijobtracker.frontend.core.data.model.PlatformDeviceInfo

/**
 * Multiplatform client interface for device push notification registration, deregistration, and heartbeat analytics.
 */
public interface NotificationRemoteDataSource {
    /**
     * Registers or updates a device push token with the backend.
     */
    public suspend fun registerDevice(token: String, platform: DevicePlatform, deviceId: String): Boolean

    /**
     * Deregisters a device push token from the backend.
     */
    public suspend fun deregisterDevice(deviceId: String): Boolean

    /**
     * Sends an interactive heartbeat upon user tap of a notification.
     */
    public suspend fun sendHeartbeat(
        deviceInfo: PlatformDeviceInfo,
        targetProposalId: String? = null,
    ): Boolean
}
