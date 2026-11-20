package dev.fnc314.aijobtracker.frontend.features.settings.domain.usecase.impl

import dev.fnc314.aijobtracker.frontend.core.data.platform.PlatformDeviceInfoProvider
import dev.fnc314.aijobtracker.frontend.core.data.source.NotificationRemoteDataSource
import dev.fnc314.aijobtracker.frontend.features.settings.domain.usecase.DeregisterDeviceNotificationsUseCase

internal class DeregisterDeviceNotificationsUseCaseImpl(
    private val notificationRemoteDataSource: NotificationRemoteDataSource,
    private val platformDeviceInfoProvider: PlatformDeviceInfoProvider,
) : DeregisterDeviceNotificationsUseCase {
    override suspend fun invoke(): Boolean {
        val deviceInfo = platformDeviceInfoProvider.getDeviceInfo()
        return notificationRemoteDataSource.deregisterDevice(deviceInfo.deviceId)
    }
}
