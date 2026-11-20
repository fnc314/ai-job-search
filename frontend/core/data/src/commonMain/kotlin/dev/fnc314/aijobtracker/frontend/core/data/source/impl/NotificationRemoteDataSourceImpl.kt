package dev.fnc314.aijobtracker.frontend.core.data.source.impl

import dev.fnc314.aijobtracker.frontend.core.data.model.DevicePlatform
import dev.fnc314.aijobtracker.frontend.core.data.model.PlatformDeviceInfo
import dev.fnc314.aijobtracker.frontend.core.data.source.NetworkDataSource
import dev.fnc314.aijobtracker.frontend.core.data.source.NotificationRemoteDataSource
import dev.fnc314.aijobtracker.frontend.core.network.RemoteNetworkConfig
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class NotificationRemoteDataSourceImpl(
    private val networkDataSource: NetworkDataSource,
) : NotificationRemoteDataSource {

    private fun headers(): Map<String, String> {
        val apiKey = RemoteNetworkConfig.apiKey
        return if (!apiKey.isNullOrBlank()) {
            mapOf(RemoteNetworkConfig.API_KEY_HEADER to apiKey)
        } else {
            emptyMap()
        }
    }

    override suspend fun registerDevice(token: String, platform: DevicePlatform, deviceId: String): Boolean {
        return try {
            val payload = buildJsonObject {
                put("token", token)
                put("platform", platform.name)
                put("deviceId", deviceId)
            }.toString()

            val url = "${RemoteNetworkConfig.baseUrl.trimEnd('/')}/notifications/device/register"
            networkDataSource.postJson(url = url, jsonBody = payload, headers = headers())
            true
        } catch (_: Throwable) {
            false
        }
    }

    override suspend fun deregisterDevice(deviceId: String): Boolean {
        return try {
            val payload = buildJsonObject {
                put("deviceId", deviceId)
            }.toString()

            val url = "${RemoteNetworkConfig.baseUrl.trimEnd('/')}/notifications/device/deregister"
            networkDataSource.postJson(url = url, jsonBody = payload, headers = headers())
            true
        } catch (_: Throwable) {
            false
        }
    }

    override suspend fun sendHeartbeat(
        deviceInfo: PlatformDeviceInfo,
        targetProposalId: String?,
    ): Boolean {
        return try {
            val payload = buildJsonObject {
                put("deviceId", deviceInfo.deviceId)
                put("platform", deviceInfo.platform.name)
                put("make", deviceInfo.make)
                put("model", deviceInfo.model)
                put("osVersion", deviceInfo.osVersion)
                put("appVersion", deviceInfo.appVersion)
                if (!targetProposalId.isNullOrBlank()) {
                    put("targetProposalId", targetProposalId)
                }
            }.toString()

            val url = "${RemoteNetworkConfig.baseUrl.trimEnd('/')}/notifications/device/heartbeat"
            networkDataSource.postJson(url = url, jsonBody = payload, headers = headers())
            true
        } catch (_: Throwable) {
            false
        }
    }
}
