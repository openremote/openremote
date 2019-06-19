package org.openremote.android;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.Nullable;
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

        Long notificationId = intent.getLongExtra("notificationId", 0L);
        String acknowledgement = intent.getStringExtra("acknowledgement");

        NotificationManager manager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(notificationId.hashCode());
        }

        String consoleId = getSharedPreferences(getApplicationContext().getString(R.string.OR_CONSOLE_NAME), Context.MODE_PRIVATE).getString("consoleId", "");
        if (!TextUtils.isEmpty(consoleId)) {
            tokenService.notificationAcknowledged(notificationId, consoleId, acknowledgement);
        }

        String appUrl = intent.getStringExtra("appUrl");

        if (!TextUtils.isEmpty(appUrl)) {
            boolean openInBrowser = intent.getBooleanExtra("openInBrowser", false);
            boolean silent = intent.getBooleanExtra("silent", false);
            String data = intent.getStringExtra("data");
            String httpMethod = intent.getStringExtra("httpMethod");
            httpMethod = TextUtils.isEmpty(httpMethod) ? "GET" : httpMethod;

            if (openInBrowser) {
                // Don't load the app just send straight to browser
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.setData(Uri.parse(appUrl));
                startActivity(i);
            } else if (silent) {
                // Do silent HTTP request
                tokenService.executeRequest(httpMethod, appUrl, data);
            } else {
                Intent activityIntent = new Intent("android.intent.action.MAIN");
                activityIntent.setAction(Intent.ACTION_MAIN);
                activityIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activityIntent.putExtra("appUrl", appUrl);
                startActivity(activityIntent);
            }
        }
    }
}
