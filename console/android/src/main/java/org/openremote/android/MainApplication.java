package org.openremote.android;

import android.app.Application;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Fix and configure JUL
        AndroidLoggingHandler.reset(new AndroidLoggingHandler());
        if (Boolean.parseBoolean(getApplicationContext().getString(R.string.DEBUG_LOGGING)))  {
            java.util.logging.Logger.getLogger("org.openremote").setLevel(Level.FINEST);
        }

        Logger LOG = Logger.getLogger(MainApplication.class.getName());
        LOG.info(">>> Starting OpenRemote application");
    }
}
