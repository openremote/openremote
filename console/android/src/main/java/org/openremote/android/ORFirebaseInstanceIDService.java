package org.openremote.android;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import org.openremote.android.service.TokenService;

import java.util.logging.Logger;


/**
 * This runs only once when the application is started for the first time after install. To generate
 * new device tokens and send them to the server, uninstall the app first.
 */
public class ORFirebaseInstanceIDService extends FirebaseInstanceIdService {

    private static final Logger LOG = Logger.getLogger(ORFirebaseInstanceIDService.class.getName());

    private TokenService tokenService;

    @Override
    public void onCreate() {
        super.onCreate();
        tokenService = new TokenService(getApplicationContext());
    }

    public ORFirebaseInstanceIDService() {
    }

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    // [START refresh_token]
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        LOG.fine("Obtained FCM token: " + refreshedToken);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(refreshedToken, FirebaseInstanceId.getInstance().getId());
    }
    // [END refresh_token]

    /**
     * Persist token to third-party servers.
     * <p>
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(final String token, final String id) {
        tokenService.sendOrStoreFCMToken(token, id);
    }
}