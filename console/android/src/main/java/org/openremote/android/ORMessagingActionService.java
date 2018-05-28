package org.openremote.android;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.support.annotation.Nullable;

import org.openremote.android.service.AlertAction;
import org.openremote.android.service.AlertNotification;
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

        if (intent.hasExtra("notification")) {
            AlertNotification notification = (AlertNotification) intent.getSerializableExtra("notification");
            tokenService.deleteAlert(notification.getId());
            NotificationManager manager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
            manager.cancel(notification.getId().hashCode());
        } else if (intent.hasExtra("notificationId")) {
            int id = intent.getIntExtra("notificationId", 0);
            if (id != 0) {
                NotificationManager manager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
                manager.cancel(id);
            }
        }

        AlertAction alertAction =  (AlertAction) intent.getSerializableExtra("action");

        if(alertAction != null) {
            if (AlertNotification.ActionType.ACTUATOR.equals(alertAction.getType())) {
                tokenService.executeAction(alertAction);
            } else if (alertAction.getAppUrl() != null) {
                // TODO: decide how to handle absolute URLs (i.e. open in the normal browser or inside the console?)
                // For now replicated behaviour of iOS for important demo
                Intent activityIntent = new Intent(this, MainActivity.class);
                activityIntent.setAction(Intent.ACTION_MAIN);
                activityIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                            Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activityIntent.putExtra("url", alertAction.getAppUrl());
                startActivity(activityIntent);
            }
        } else if (intent.hasExtra("url")) {
            Intent activityIntent = new Intent(this, MainActivity.class);
            activityIntent.setAction(Intent.ACTION_MAIN);
            activityIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activityIntent.putExtra("url", intent.getStringExtra("url"));
            startActivity(activityIntent);
        }
    }
}
