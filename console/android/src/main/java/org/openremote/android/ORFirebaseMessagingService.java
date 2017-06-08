package org.openremote.android;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.RemoteMessage;

import org.openremote.android.service.AlertAction;
import org.openremote.android.service.AlertNotification;
import org.openremote.android.service.TokenService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ORFirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    private TokenService tokenService;

    public class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
             Log.i("NOTIFICATION","TODO: Remove Notification");
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
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        tokenService.getAlerts(new Callback<List<AlertNotification>>() {
            @Override
            public void onResponse(Call<List<AlertNotification>> call, Response<List<AlertNotification>> response) {
                List<AlertNotification> alertNotifications = response.body();
                Set<String> actualAlertIds = new HashSet<>();
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                Set<String> previousAlertIds =  sharedPreferences.getStringSet(getString(R.string.SHARED_PREF_OPENED_ALERT), new HashSet<String>());

                for (AlertNotification alertNotification : alertNotifications) {
                    if (!previousAlertIds.contains(alertNotification.getId().toString())) {
                        sendNotification(alertNotification);
                        previousAlertIds.add(alertNotification.getId().toString());
                    }
                    actualAlertIds.add(alertNotification.getId().toString());
                    Log.d("NOTIFICATION", alertNotification.toString());
                }
                previousAlertIds.retainAll(actualAlertIds);
                sharedPreferences.edit().putStringSet(getString(R.string.SHARED_PREF_OPENED_ALERT) ,previousAlertIds ).commit();
            }

            @Override
            public void onFailure(Call<List<AlertNotification>> call, Throwable t) {
                Log.e("NOTIFICATION", "Error retriving alert", t);
            }
        });


        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
    // [END receive_message]




    private void sendNotification(AlertNotification alertNotification) {
        int notificationId = alertNotification.getId().hashCode();
        Intent intent = new Intent(this, ORMessagingActionService.class);
        intent.putExtra("notification", alertNotification);

        PendingIntent pendingIntent = PendingIntent.getService(this, notificationId, intent,
                PendingIntent.FLAG_ONE_SHOT);


        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(alertNotification.getTitle())
                .setContentText(alertNotification.getMessage())
                .setDeleteIntent(pendingIntent)
                .setSmallIcon(R.drawable.app_icon)
                .setWhen(0)
                .setSound(defaultSoundUri);

        for (AlertAction alertAction : alertNotification.getActions()) {
            Intent  actionIntent = new Intent(this, ORMessagingActionService.class);

            actionIntent.putExtra("action", alertAction);
            actionIntent.putExtra("url", alertNotification.getAppUrl());
            actionIntent.putExtra("notification", alertNotification);
            actionIntent.setAction(Long.toString(System.currentTimeMillis()));
            PendingIntent actionPendingIntent = PendingIntent.getService(this, notificationId, actionIntent,
                  pendingIntent.FLAG_ONE_SHOT);
            notificationBuilder = notificationBuilder.addAction(new NotificationCompat.Action(R.drawable.empty, alertAction.getTitle(), actionPendingIntent));
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
}