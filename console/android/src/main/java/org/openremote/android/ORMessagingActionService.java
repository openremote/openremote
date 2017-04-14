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
        AlertNotification notification = (AlertNotification) intent.getSerializableExtra("notification");
        tokenService.deleteAlert(notification.getId());

        NotificationManager manager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        manager.cancel(notification.getId().hashCode());

        if(intent.hasExtra("action")) {

            AlertAction alertAction =  (AlertAction) intent.getSerializableExtra("action");
            if (AlertNotification.ActionType.ACTUATOR.equals(alertAction.getType())) {
                tokenService.executeAction(alertAction);
            } else {
                //TODO : open app and pass rawJson to webView
            }
        }


    }
}
