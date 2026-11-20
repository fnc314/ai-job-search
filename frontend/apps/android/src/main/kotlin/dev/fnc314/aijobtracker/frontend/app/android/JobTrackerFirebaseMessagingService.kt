package dev.fnc314.aijobtracker.frontend.app.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dev.fnc314.aijobtracker.frontend.core.data.model.DevicePlatform
import dev.fnc314.aijobtracker.frontend.core.data.platform.PlatformDeviceInfoProvider
import dev.fnc314.aijobtracker.frontend.core.data.source.NotificationRemoteDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform.getKoin

/**
 * Android Firebase Cloud Messaging background receiver and token registration handler.
 */
public class JobTrackerFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notificationRemoteDataSource: NotificationRemoteDataSource by lazy { getKoin().get() }
    private val platformDeviceInfoProvider: PlatformDeviceInfoProvider by lazy { getKoin().get() }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            val deviceInfo = platformDeviceInfoProvider.getDeviceInfo()
            notificationRemoteDataSource.registerDevice(
                token = token,
                platform = DevicePlatform.DEVICE_PLATFORM_ANDROID,
                deviceId = deviceInfo.deviceId,
            )
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Job Tracker"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "New proposal staged for review"
        val route = remoteMessage.data["route"] ?: "edits"
        val proposalId = remoteMessage.data["proposalId"]

        showNotification(title, body, route, proposalId)
    }

    private fun showNotification(
        title: String,
        body: String,
        route: String,
        proposalId: String?,
    ) {
        val channelId = "job_tracker_proposals"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Job Application Proposals",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notifications for newly parsed email proposals requiring human review"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_ROUTE, route)
            if (!proposalId.isNullOrBlank()) {
                putExtra(EXTRA_PROPOSAL_ID, proposalId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    public companion object {
        public const val EXTRA_NOTIFICATION_ROUTE: String = "extra_notification_route"
        public const val EXTRA_PROPOSAL_ID: String = "extra_proposal_id"
    }
}
