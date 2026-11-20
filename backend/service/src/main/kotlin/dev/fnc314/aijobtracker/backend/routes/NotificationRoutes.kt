package dev.fnc314.aijobtracker.backend.routes

import com.google.cloud.Timestamp
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.SetOptions
import com.google.firebase.auth.FirebaseAuth
import dev.fnc314.aijobtracker.backend.BackendService
import dev.fnc314.aijobtracker.records.DeviceHeartbeat
import dev.fnc314.aijobtracker.records.DevicePlatform
import dev.fnc314.aijobtracker.records.DeviceRegistration
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

private val notificationJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
public data class NotificationApiResponse(
    public val status: String,
    public val message: String,
    public val details: Map<String, String> = emptyMap(),
)

/**
 * Registers notification endpoints:
 * - `POST /notifications/device/register`
 * - `POST /notifications/device/deregister`
 * - `POST /notifications/device/heartbeat`
 * - `GET /notifications/devices`
 * - `DELETE /notifications/devices`
 */
public fun Route.notificationRoutes(): Unit {
    post("/notifications/device/register") {
        val uid = extractUid(call)
        if (uid == null) {
            respondUnauthorized(call)
            return@post
        }

        try {
            val bodyText = call.receiveText()
            val json = Json.parseToJsonElement(bodyText).jsonObject
            val deviceId = json["deviceId"]?.jsonPrimitive?.contentOrNull?.trim()
            val token = json["token"]?.jsonPrimitive?.contentOrNull?.trim()
            val platformStr = json["platform"]?.jsonPrimitive?.contentOrNull?.trim() ?: "DEVICE_PLATFORM_ANDROID"

            if (deviceId.isNullOrBlank() || token.isNullOrBlank()) {
                call.respondText(
                    notificationJson.encodeToString(NotificationApiResponse("ERROR", "deviceId and token are required")),
                    ContentType.Application.Json,
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }

            val platform = runCatching { DevicePlatform.valueOf(platformStr) }.getOrDefault(DevicePlatform.DEVICE_PLATFORM_ANDROID)
            val db = BackendService.remoteDatabase

            // Enforce max 5 devices invariant: if >= 5 active devices, evict oldest
            val devicesCollection = db.collection("users").document(uid).collection("devices")
            val existingDevices = devicesCollection.get().get().documents
            val isExistingDevice = existingDevices.any { it.id == deviceId }

            if (!isExistingDevice && existingDevices.size >= 5) {
                // Find oldest / least recently active device
                val oldestDevice = existingDevices.minByOrNull { doc ->
                    doc.getTimestamp("lastInteractedAt")?.seconds
                        ?: doc.getTimestamp("updatedAt")?.seconds
                        ?: 0L
                }
                oldestDevice?.reference?.delete()?.get()
            }

            val now = Timestamp.now()
            val registrationData = mutableMapOf<String, Any>(
                "deviceId" to deviceId,
                "token" to token,
                "platform" to platform.name,
                "updatedAt" to now,
                "lastInteractedAt" to now
            )

            devicesCollection.document(deviceId).set(registrationData, SetOptions.merge()).get()

            call.respondText(
                notificationJson.encodeToString(
                    NotificationApiResponse(
                        status = "SUCCESS",
                        message = "Device registered successfully",
                        details = mapOf("deviceId" to deviceId, "platform" to platform.name)
                    )
                ),
                ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        } catch (e: Exception) {
            call.respondText(
                notificationJson.encodeToString(NotificationApiResponse("ERROR", e.message ?: "Failed to register device")),
                ContentType.Application.Json,
                status = HttpStatusCode.InternalServerError
            )
        }
    }

    post("/notifications/device/deregister") {
        val uid = extractUid(call)
        if (uid == null) {
            respondUnauthorized(call)
            return@post
        }

        try {
            val bodyText = call.receiveText()
            val json = Json.parseToJsonElement(bodyText).jsonObject
            val deviceId = json["deviceId"]?.jsonPrimitive?.contentOrNull?.trim()

            if (deviceId.isNullOrBlank()) {
                call.respondText(
                    notificationJson.encodeToString(NotificationApiResponse("ERROR", "deviceId is required")),
                    ContentType.Application.Json,
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }

            val db = BackendService.remoteDatabase
            val docRef = db.collection("users").document(uid).collection("devices").document(deviceId)
            docRef.delete().get()

            call.respondText(
                notificationJson.encodeToString(
                    NotificationApiResponse(
                        status = "SUCCESS",
                        message = "Device deregistered successfully",
                        details = mapOf("deviceId" to deviceId)
                    )
                ),
                ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        } catch (e: Exception) {
            call.respondText(
                notificationJson.encodeToString(NotificationApiResponse("ERROR", e.message ?: "Failed to deregister device")),
                ContentType.Application.Json,
                status = HttpStatusCode.InternalServerError
            )
        }
    }

    post("/notifications/device/heartbeat") {
        val uid = extractUid(call)
        if (uid == null) {
            respondUnauthorized(call)
            return@post
        }

        try {
            val bodyText = call.receiveText()
            val json = Json.parseToJsonElement(bodyText).jsonObject
            val deviceId = json["deviceId"]?.jsonPrimitive?.contentOrNull?.trim()
            val make = json["make"]?.jsonPrimitive?.contentOrNull?.trim() ?: "unknown"
            val model = json["model"]?.jsonPrimitive?.contentOrNull?.trim() ?: "unknown"
            val osVersion = json["osVersion"]?.jsonPrimitive?.contentOrNull?.trim() ?: "unknown"
            val appVersion = json["appVersion"]?.jsonPrimitive?.contentOrNull?.trim() ?: "unknown"
            val targetProposalId = json["targetProposalId"]?.jsonPrimitive?.contentOrNull?.trim()

            if (deviceId.isNullOrBlank()) {
                call.respondText(
                    notificationJson.encodeToString(NotificationApiResponse("ERROR", "deviceId is required")),
                    ContentType.Application.Json,
                    status = HttpStatusCode.BadRequest
                )
                return@post
            }

            val epochSeconds = Instant.now().epochSecond
            val now = Timestamp.now()
            val db = BackendService.remoteDatabase
            val deviceDoc = db.collection("users").document(uid).collection("devices").document(deviceId)

            // Update parent device lastInteractedAt
            deviceDoc.set(mapOf("lastInteractedAt" to now), SetOptions.merge()).get()

            // Heartbeat entry
            val heartbeatData = mutableMapOf<String, Any>(
                "deviceId" to deviceId,
                "make" to make,
                "model" to model,
                "osVersion" to osVersion,
                "appVersion" to appVersion,
                "timestamp" to now,
            )
            if (!targetProposalId.isNullOrBlank()) {
                heartbeatData["targetProposalId"] = targetProposalId
            }

            val heartbeatsCol = deviceDoc.collection("heartbeats")
            heartbeatsCol.document(epochSeconds.toString()).set(heartbeatData).get()

            // Retention cap: strictly max 20 entries
            val existingHeartbeats = heartbeatsCol.orderBy("timestamp").get().get().documents
            if (existingHeartbeats.size > 20) {
                val excess = existingHeartbeats.size - 20
                for (i in 0 until excess) {
                    existingHeartbeats[i].reference.delete().get()
                }
            }

            call.respondText(
                notificationJson.encodeToString(
                    NotificationApiResponse(
                        status = "SUCCESS",
                        message = "Heartbeat recorded successfully",
                        details = mapOf("deviceId" to deviceId, "epoch" to epochSeconds.toString())
                    )
                ),
                ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        } catch (e: Exception) {
            call.respondText(
                notificationJson.encodeToString(NotificationApiResponse("ERROR", e.message ?: "Failed to record heartbeat")),
                ContentType.Application.Json,
                status = HttpStatusCode.InternalServerError
            )
        }
    }

    get("/notifications/devices") {
        val uid = extractUid(call)
        if (uid == null) {
            respondUnauthorized(call)
            return@get
        }

        try {
            val db = BackendService.remoteDatabase
            val devices = db.collection("users").document(uid).collection("devices").get().get().documents
            val deviceList = devices.map { doc ->
                mapOf(
                    "deviceId" to doc.id,
                    "platform" to (doc.getString("platform") ?: "UNKNOWN"),
                    "updatedAt" to (doc.getTimestamp("updatedAt")?.toString() ?: ""),
                    "lastInteractedAt" to (doc.getTimestamp("lastInteractedAt")?.toString() ?: "")
                )
            }

            call.respondText(
                Json.encodeToString(deviceList),
                ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        } catch (e: Exception) {
            call.respondText(
                notificationJson.encodeToString(NotificationApiResponse("ERROR", e.message ?: "Failed to list devices")),
                ContentType.Application.Json,
                status = HttpStatusCode.InternalServerError
            )
        }
    }

    delete("/notifications/devices") {
        val uid = extractUid(call)
        if (uid == null) {
            respondUnauthorized(call)
            return@delete
        }

        try {
            val db = BackendService.remoteDatabase
            val devices = db.collection("users").document(uid).collection("devices").get().get().documents
            for (doc in devices) {
                doc.reference.delete().get()
            }

            call.respondText(
                notificationJson.encodeToString(
                    NotificationApiResponse(
                        status = "SUCCESS",
                        message = "All devices purged for user",
                        details = mapOf("count" to devices.size.toString())
                    )
                ),
                ContentType.Application.Json,
                status = HttpStatusCode.OK
            )
        } catch (e: Exception) {
            call.respondText(
                notificationJson.encodeToString(NotificationApiResponse("ERROR", e.message ?: "Failed to purge devices")),
                ContentType.Application.Json,
                status = HttpStatusCode.InternalServerError
            )
        }
    }
}

private fun extractUid(call: io.ktor.server.application.ApplicationCall): String? {
    val authHeader = call.request.headers["Authorization"]?.trim()
    if (authHeader?.startsWith("Bearer ", ignoreCase = true) == true) {
        val token = authHeader.substring(7).trim()
        return try {
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(token)
            decodedToken.uid
        } catch (_: Exception) {
            val apiKey = call.request.headers["X-Api-Key"]?.trim()
            val expectedKey = System.getenv("JOB_TRACKER_API_KEY")?.trim()
            if (!apiKey.isNullOrBlank() && (expectedKey.isNullOrBlank() || apiKey == expectedKey)) {
                "admin-user"
            } else {
                null
            }
        }
    }

    val apiKey = call.request.headers["X-Api-Key"]?.trim()
    val expectedKey = System.getenv("JOB_TRACKER_API_KEY")?.trim()
    if (!apiKey.isNullOrBlank() && (expectedKey.isNullOrBlank() || apiKey == expectedKey)) {
        return "admin-user"
    }

    return null
}

private suspend fun respondUnauthorized(call: io.ktor.server.application.ApplicationCall) {
    call.respondText(
        notificationJson.encodeToString(NotificationApiResponse("ERROR", "Unauthorized: Valid Firebase ID token or API key required")),
        ContentType.Application.Json,
        status = HttpStatusCode.Unauthorized
    )
}
