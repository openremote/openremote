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

        AlertNotification notification = (AlertNotification) intent.getSerializableExtra("notification");
        tokenService.deleteAlert(notification.getId());

        NotificationManager manager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        manager.cancel(notification.getId().hashCode());
        AlertAction alertAction =  (AlertAction) intent.getSerializableExtra("action");

        if(alertAction != null && AlertNotification.ActionType.ACTUATOR.equals(alertAction.getType())) {
            tokenService.executeAction(alertAction);
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
