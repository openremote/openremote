package io.openremote.orlib.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.firebase.messaging.RemoteMessage
import io.openremote.orlib.ORConstants
import io.openremote.orlib.R
import io.openremote.orlib.models.ORAlertAction
import io.openremote.orlib.models.ORAlertButton
import io.openremote.orlib.ui.OrMainActivity
import java.util.logging.Level
import java.util.logging.Logger


class ORFirebaseMessagingService : com.google.firebase.messaging.FirebaseMessagingService() {

    private var notificationResource: NotificationResource? = null

    class MyBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            LOG.info("TODO: Remove notification")
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationResource = NotificationResource(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        LOG.info("Received message from: " + remoteMessage.from)

        // If the message contains a notification then we assume it has been shown to the user
        if (remoteMessage.notification != null) {
            LOG.info(
                "Message contains notification body: " + remoteMessage.notification!!.getBody()
            )
        } else if (remoteMessage.data.isNotEmpty()) {
            val messageData: Map<String, String> = remoteMessage.data

            // Mark as delivered on the server
            val notificationIdStr = messageData["notification-id"]
            var notificationId: Long? = null
            if (notificationIdStr != null && notificationIdStr.isNotEmpty()) {
                notificationId = notificationIdStr.toLong()
                val consoleId: String? = getSharedPreferences(
                    applicationContext.getString(R.string.app_name),
                    Context.MODE_PRIVATE
                ).getString("consoleId", "")
                if (!consoleId.isNullOrBlank()) {
                    notificationResource?.notificationDelivered(notificationId, consoleId)
                }
            }
            val isSilent = !messageData.containsKey("or-title")
            if (isSilent) {
                when (val action: String? = remoteMessage.data.get("action")) {
                    "GEOFENCE_REFRESH" -> {
                        val geofenceProvider = GeofenceProvider(applicationContext)
                        geofenceProvider.refreshGeofences()
                    }
                    else -> {
                        val broadCastIntent = Intent(ORConstants.ACTION_BROADCAST)
                        broadCastIntent.putExtra("action", action)
                        sendBroadcast(broadCastIntent)
                    }
                }
            } else {
                val title = messageData["or-title"]
                val body = messageData["or-body"]
                var buttonORS: Array<ORAlertButton>? = null
                var actionOR: ORAlertAction? = null

                // Check for action (to be executed when notification is clicked)
                if (messageData.containsKey("action")) {
                    val actionJson = messageData["action"]
                    if (actionJson != null && actionJson.isNotEmpty()) {
                        try {
                            actionOR = jacksonObjectMapper().readValue(
                                actionJson,
                                ORAlertAction::class.java
                            )
                        } catch (e: Exception) {
                            LOG.log(Level.SEVERE, "Failed to de-serialise alert action", e)
                        }
                    }
                }

                // Check for buttons
                if (messageData.containsKey("buttons")) {
                    val buttonsJson = messageData["buttons"]
                    if (buttonsJson != null && buttonsJson.isNotEmpty()) {
                        try {
                            buttonORS = jacksonObjectMapper().readValue(
                                buttonsJson,
                                Array<ORAlertButton>::class.java
                            )
                        } catch (e: Exception) {
                            LOG.log(Level.SEVERE, "Failed to de-serialise alert actions", e)
                        }
                    }
                }
                handleNotification(notificationId, title, body, actionOR, buttonORS)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channelName = "OR Message Service"
        val channel = NotificationChannel(
            getString(R.string.NOTIFICATION_CHANNEL_ID),
            channelName, NotificationManager.IMPORTANCE_HIGH
        )
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        channel.setShowBadge(false)
        channel.setSound(defaultSoundUri, null)
        channel.enableVibration(true)
        channel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
    }

    private fun handleNotification(
        notificationId: Long?,
        title: String?,
        body: String?,
        actionOR: ORAlertAction?,
        buttonORS: Array<ORAlertButton>?
    ) {
        val pm = packageManager
        val notificationIntent = pm.getLaunchIntentForPackage(packageName)
        notificationIntent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = NotificationCompat.Builder(
            this,
            getString(R.string.NOTIFICATION_CHANNEL_ID)
        )
            .setContentTitle(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(createActionIntent(notificationId, "\"CLOSED\"", null))
        if (actionOR != null) {
            notificationBuilder.setContentIntent(createActionIntent(notificationId, null, actionOR))
        }
        if (buttonORS != null) {
            for (alertButton in buttonORS) {
                notificationBuilder.addAction(
                    NotificationCompat.Action(
                        R.drawable.empty,
                        alertButton.title,
                        createActionIntent(
                            notificationId,
                            alertButton.title,
                            alertButton.action
                        )
                    )
                )
            }
        }
        LOG.info(
            "Showing notification id=$notificationId, title=$title, body=$body, action=$actionOR, buttons=" + (buttonORS?.size
                ?: 0)
        )
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        notificationManager?.notify(notificationId.hashCode(), notificationBuilder.build())
    }

    private fun createActionIntent(
        notificationId: Long?,
        acknowledgement: String?,
        ORAlertAction: ORAlertAction?
    ): PendingIntent {
        val actionIntent = Intent(this, ORMessagingActionService::class.java)
        actionIntent.putExtra("notificationId", notificationId)
        actionIntent.putExtra("acknowledgement", acknowledgement)
        actionIntent.action = System.currentTimeMillis().toString()
        if (ORAlertAction?.url != null && ORAlertAction.url.isNotEmpty()) {
            actionIntent.putExtra("appUrl", ORAlertAction.url)
            actionIntent.putExtra("httpMethod", ORAlertAction.httpMethod)
            actionIntent.putExtra("silent", ORAlertAction.silent)
            actionIntent.putExtra("openInBrowser", ORAlertAction.openInBrowser)
            actionIntent.putExtra("data", ObjectMapper().writeValueAsString(ORAlertAction.data))
        }
        return PendingIntent.getService(this, 0, actionIntent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        private val LOG = Logger.getLogger(
            ORFirebaseMessagingService::class.java.name
        )
    }
}
