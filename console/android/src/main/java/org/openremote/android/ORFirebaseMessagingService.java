package org.openremote.android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.RemoteMessage;
import org.openremote.android.service.AlertAction;
import org.openremote.android.service.AlertButton;
import org.openremote.android.service.GeofenceProvider;
import org.openremote.android.service.TokenService;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ORFirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

    private static final Logger LOG = Logger.getLogger(ORFirebaseMessagingService.class.getName());

    private final String NOTICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID + ".OREindhovenMessage";

    private TokenService tokenService;

    public class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            LOG.info("TODO: Remove notification");
        }

    }

    @Override
    public void onCreate() {
        super.onCreate();
        tokenService = new TokenService(getApplicationContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
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
        LOG.fine("Received message from: " + remoteMessage.getFrom());

        // If the message contains a notification then we assume it has been shown to the user
        if (remoteMessage.getNotification() != null) {
            LOG.fine("Message contains notification body: " + remoteMessage.getNotification().getBody());
        } else if (remoteMessage.getData() != null && !remoteMessage.getData().isEmpty()) {

            Map<String, String> messageData = remoteMessage.getData();

            // Mark as delivered on the server
            String notificationIdStr = messageData.get("notification-id");
            Long notificationId = null;

            if (notificationIdStr != null && !notificationIdStr.isEmpty()) {
                notificationId = Long.parseLong(notificationIdStr);
                String consoleId = getSharedPreferences(getApplicationContext().getString(R.string.app_name), Context.MODE_PRIVATE).getString("consoleId", "");
                if (!TextUtils.isEmpty(consoleId)) {
                    tokenService.notificationDelivered(notificationId, consoleId);
                }
            }

            boolean isSilent = !messageData.containsKey("or-title");

            if (isSilent) {
                String action = remoteMessage.getData().get("action");

                switch (action) {
                    case "GEOFENCE_REFRESH":
                        GeofenceProvider geofenceProvider = new GeofenceProvider(getApplicationContext());
                        geofenceProvider.refreshGeofences();
                        break;
                    default:
                        Intent broadCastIntent = new Intent(MainActivity.ACTION_BROADCAST);
                        broadCastIntent.putExtra("action", action);
                        sendBroadcast(broadCastIntent);
                        break;
                }
            } else {

                String title = messageData.get("or-title");
                String body = messageData.get("or-body");
                AlertButton[] buttons = null;
                AlertAction action = null;

                // Check for action (to be executed when notification is clicked)
                if (messageData.containsKey("action")) {
                    String actionJson = messageData.get("action");

                    if (actionJson != null && !actionJson.isEmpty()) {
                        try {
                            action = new ObjectMapper().readValue(actionJson, AlertAction.class);
                        } catch (Exception e) {
                            LOG.log(Level.SEVERE, "Failed to de-serialise alert action", e);
                        }
                    }
                }

                // Check for buttons
                if (messageData.containsKey("buttons")) {
                    String buttonsJson = messageData.get("buttons");

                    if (buttonsJson != null && !buttonsJson.isEmpty()) {
                        try {
                            buttons = new ObjectMapper().readValue(buttonsJson, AlertButton[].class);
                        } catch (Exception e) {
                            LOG.log(Level.SEVERE, "Failed to de-serialise alert actions", e);
                        }
                    }
                }

                handleNotification(notificationId, title, body, action, buttons);
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        String channelName = "OR Eindhoven Message Service";
        NotificationChannel channel = new NotificationChannel(NOTICATION_CHANNEL_ID,
                channelName, NotificationManager.IMPORTANCE_HIGH);
        channel.setShowBadge(false);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(channel);
    }

    private void handleNotification(Long notificationId, String title, String body, AlertAction action, AlertButton[] buttons) {
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setContentText(body)
                .setSmallIcon(R.drawable.notification_icon)
                .setWhen(0)
                .setPriority(Notification.PRIORITY_MAX)
                .setSound(defaultSoundUri)
                .setDeleteIntent(createActionIntent(notificationId, "\"CLOSED\"", null));

        if (action != null) {
            notificationBuilder.setContentIntent(createActionIntent(notificationId, null, action));
        }

        if (buttons != null) {
            for (AlertButton alertButton : buttons) {
                notificationBuilder.addAction(
                        new NotificationCompat.Action(R.drawable.empty,
                                alertButton.getTitle(),
                                createActionIntent(notificationId, alertButton.getTitle(), alertButton.getAction())));
            }
        }

        LOG.fine("Showing notification (" + (buttons != null ? buttons.length + " buttons): " : "") + body);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(notificationId.hashCode(), notificationBuilder.build());
        }
    }

    private PendingIntent createActionIntent(Long notificationId, String acknowledgement, AlertAction alertAction) {

        Intent actionIntent = new Intent(this, ORMessagingActionService.class);
        actionIntent.putExtra("notificationId", notificationId);
        actionIntent.putExtra("acknowledgement", acknowledgement);
        actionIntent.setAction(Long.toString(System.currentTimeMillis()));

        if (alertAction != null && alertAction.getUrl() != null && !alertAction.getUrl().isEmpty()) {
            actionIntent.putExtra("appUrl", alertAction.getUrl());
            actionIntent.putExtra("httpMethod", alertAction.getHttpMethod());
            actionIntent.putExtra("silent", alertAction.isSilent());
            actionIntent.putExtra("openInBrowser", alertAction.isOpenInBrowser());
            actionIntent.putExtra("data", alertAction.getDataJson());
        }

        return PendingIntent.getService(this, 0, actionIntent, PendingIntent.FLAG_ONE_SHOT);
    }
}