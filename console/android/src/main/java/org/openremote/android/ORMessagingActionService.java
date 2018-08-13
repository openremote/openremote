package org.openremote.android;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.openremote.android.service.TokenService;

public class ORMessagingActionService extends IntentService {

    private TokenService tokenService;

    public ORMessagingActionService() {
        super("org.openremote.android.ORMessagingActionService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        tokenService = new TokenService(getApplicationContext());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        this.sendBroadcast(it);

        if (intent.hasExtra("notificationId")) {
            String notificationId = intent.getStringExtra("notificationId");
            String acknowledgement = intent.getStringExtra("acknowledgement");

            String consoleId = getSharedPreferences(getApplicationContext().getString(R.string.app_name), Context.MODE_PRIVATE).getString("consoleId", "");
            if (!TextUtils.isEmpty(consoleId)) {
                tokenService.notificationAcknowledged(Long.parseLong(notificationId), consoleId, acknowledgement);

            }

            NotificationManager manager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
            manager.cancel(notificationId.hashCode());
        }

        if (intent.hasExtra("appUrl")) {
            Intent activityIntent = new Intent(this, MainActivity.class);
            activityIntent.setAction(Intent.ACTION_MAIN);
            activityIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activityIntent.putExtra("appUrl", intent.getStringExtra("appUrl"));
            activityIntent.putExtra("silent", intent.getBooleanExtra("silent", false));
            activityIntent.putExtra("httpMethod", intent.getStringExtra("httpMethod"));
            activityIntent.putExtra("openInBrowser", intent.getBooleanExtra("openInBrowser", false));
            activityIntent.putExtra("data", intent.getStringExtra("data"));
            startActivity(activityIntent);
        }
    }
}
