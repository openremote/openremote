package org.openremote.android;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.RemoteMessage;

import org.openremote.android.service.AlertAction;
import org.openremote.android.service.AlertNotification;
import org.openremote.android.service.TokenService;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ORFirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

    private static final Logger LOG = Logger.getLogger(ORFirebaseMessagingService.class.getName());

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
            if (remoteMessage.getData().containsKey("action")) {
                String action = remoteMessage.getData().get("action");
                Intent broadCastIntent = new Intent(MainActivity.ACTION_BROADCAST);
                broadCastIntent.putExtra("action", action);
                sendBroadcast(broadCastIntent);
            } else {
                // Check if message contains a data payload with or-title or-body and actions

                LOG.fine("Message data payload: " + remoteMessage.getData());
                String title = remoteMessage.getData().get("or-title");
                String body = remoteMessage.getData().get("or-body");
                String actionsJson = remoteMessage.getData().get("actions");
                AlertAction[] actions = null;

                if (actionsJson != null && actionsJson.length() > 0) {
                    try {
                        actions = new ObjectMapper().readValue(actionsJson, AlertAction[].class);
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "Failed to de-serialise alert actions", e);
                    }
                }
                handleNotification(title, body, actions);
            }
        }

//        tokenService.getAlerts(new Callback<List<AlertNotification>>() {
//            @Override
//            public void onResponse(Call<List<AlertNotification>> call, Response<List<AlertNotification>> response) {
//                List<AlertNotification> alertNotifications = response.body();
//                Set<String> actualAlertIds = new HashSet<>();
//                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//                Set<String> previousAlertIds =  sharedPreferences.getStringSet(getString(R.string.SHARED_PREF_OPENED_ALERT), new HashSet<String>());
//
//                for (AlertNotification alertNotification : alertNotifications) {
//                    if (!previousAlertIds.contains(alertNotification.getId().toString())) {
//                        sendNotification(alertNotification);
//                        previousAlertIds.add(alertNotification.getId().toString());
//                    }
//                    actualAlertIds.add(alertNotification.getId().toString());
//                    LOG.fine("Retrieved alert notifications from service: " + alertNotification);
//                }
//                previousAlertIds.retainAll(actualAlertIds);
//                sharedPreferences.edit().putStringSet(getString(R.string.SHARED_PREF_OPENED_ALERT) ,previousAlertIds ).commit();
//            }
//
//            @Override
//            public void onFailure(Call<List<AlertNotification>> call, Throwable t) {
//                LOG.log(Level.SEVERE, "Error retrieving alert", t);
//            }
//        });


        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
    // [END receive_message]


    private void handleNotification(String title, String body, AlertAction[] actions) {
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // TODO: ID to come from backend
        int id = title.hashCode() + body.hashCode();

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(title)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setContentText(body)
                .setSmallIcon(R.drawable.notification_icon)
                .setWhen(0)
                .setPriority(Notification.PRIORITY_MAX)
                .setSound(defaultSoundUri);

        for (AlertAction alertAction : actions) {
            Intent actionIntent = new Intent(this, ORMessagingActionService.class);

            actionIntent.putExtra("notificationId", id);
            actionIntent.putExtra("action", alertAction);
            actionIntent.setAction(Long.toString(System.currentTimeMillis()));
            PendingIntent actionPendingIntent = PendingIntent.getService(this,
                    0,
                    actionIntent,
                    PendingIntent.FLAG_ONE_SHOT);
            notificationBuilder = notificationBuilder.addAction(new NotificationCompat.Action(R.drawable.empty,
                    alertAction.getTitle(),
                    actionPendingIntent));
        }

        LOG.fine("Showing notification (" + actions.length + " actions): " + body);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notificationBuilder.build());
    }


    private void sendNotification(AlertNotification alertNotification) {
        int notificationId = alertNotification.getId().hashCode();
        Intent intent = new Intent(this, ORMessagingActionService.class);
        intent.putExtra("notification", alertNotification);

        PendingIntent pendingIntent = PendingIntent.getService(this,
                notificationId,
                intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(alertNotification.getTitle())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(alertNotification.getMessage()))
                .setContentText(alertNotification.getMessage())
                .setDeleteIntent(pendingIntent)
                .setSmallIcon(R.drawable.notification_icon)
                .setWhen(0)
                .setPriority(Notification.PRIORITY_MAX)
                .setSound(defaultSoundUri);

        for (AlertAction alertAction : alertNotification.getActions()) {
            Intent actionIntent = new Intent(this, ORMessagingActionService.class);

            actionIntent.putExtra("action", alertAction);
            actionIntent.putExtra("url", alertAction.getAppUrl());
            actionIntent.putExtra("notification", alertNotification);
            actionIntent.setAction(Long.toString(System.currentTimeMillis()));
            PendingIntent actionPendingIntent = PendingIntent.getService(this,
                    notificationId,
                    actionIntent,
                    PendingIntent.FLAG_ONE_SHOT);
            notificationBuilder = notificationBuilder.addAction(new NotificationCompat.Action(R.drawable.empty,
                    alertAction.getTitle(),
                    actionPendingIntent));
        }

        LOG.fine("Showing notification (" + alertNotification.getActions().size() + " actions): " + alertNotification.getMessage());

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
}