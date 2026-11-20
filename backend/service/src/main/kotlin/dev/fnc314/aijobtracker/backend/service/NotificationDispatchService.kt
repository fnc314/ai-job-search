package dev.fnc314.aijobtracker.backend.service

import com.google.cloud.Timestamp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import dev.fnc314.aijobtracker.backend.BackendService
import dev.fnc314.aijobtracker.records.TopLevelDestination
import dev.fnc314.aijobtracker.records.TrackerNotificationPayload
import java.time.Instant

/**
 * Service orchestrating FCM push notification dispatch to registered user devices.
 */
public object NotificationDispatchService {

    /**
     * Dispatches a push notification to all registered devices of a given user (or all users when userId is null).
     *
     * @param payload The structured notification payload.
     * @param targetUserId Optional target user ID; if null, dispatches to all users with active devices.
     * @return Count of successfully dispatched notifications.
     */
    public fun dispatch(
        payload: TrackerNotificationPayload,
        targetUserId: String? = null
    ): Int {
        val db = BackendService.remoteDatabase
        var successfulDispatches = 0

        try {
            val userDocs = if (targetUserId != null) {
                listOfNotNull(db.collection("users").document(targetUserId).get().get())
            } else {
                db.collection("users").get().get().documents
            }

            for (userDoc in userDocs) {
                val devices = userDoc.reference.collection("devices").get().get().documents
                for (device in devices) {
                    val token = device.getString("token")
                    if (token.isNullOrBlank()) continue

                    try {
                        val messageBuilder = Message.builder()
                            .setToken(token)
                            .setNotification(
                                Notification.builder()
                                    .setTitle(payload.title)
                                    .setBody(payload.body)
                                    .build()
                            )
                            .putData("destination", payload.destination.name)
                            .putData("route", payload.route)
                            .putData("proposalId", payload.proposalId)
                            .putData("stagedChangesCount", payload.stagedChangesCount.toString())
                            .putData("click_action", "FLUTTER_NOTIFICATION_CLICK")

                        val response = FirebaseMessaging.getInstance().send(messageBuilder.build())
                        println("Successfully sent FCM notification to device ${device.id}: $response")
                        successfulDispatches++
                    } catch (e: Exception) {
                        System.err.println("Failed to send FCM to device ${device.id} (${device.getString("token")}): ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("Error querying devices for FCM notification: ${e.message}")
        }

        return successfulDispatches
    }

    /**
     * Convenient helper to construct and dispatch a notification when a new proposal is staged.
     */
    public fun notifyNewProposal(
        proposalId: String,
        stagedChangesCount: Int,
        summaryText: String = "$stagedChangesCount status transition(s) staged from recent emails."
    ): Int {
        val payload = TrackerNotificationPayload.newBuilder()
            .setDestination(TopLevelDestination.TOP_LEVEL_DESTINATION_EDITS)
            .setRoute("trackeredits")
            .setProposalId(proposalId)
            .setTitle("Review Staged Email Updates")
            .setBody(summaryText)
            .setStagedChangesCount(stagedChangesCount)
            .setDispatchedAt(
                com.google.protobuf.Timestamp.newBuilder()
                    .setSeconds(Instant.now().epochSecond)
                    .build()
            )
            .build()

        return dispatch(payload)
    }
}
