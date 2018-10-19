package org.openremote.android;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.util.logging.*;

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Fix and configure JUL
        AndroidLoggingHandler.reset(new AndroidLoggingHandler());

        if (BuildConfig.DEBUG) {
            java.util.logging.Logger.getLogger("org.openremote").setLevel(Level.FINEST);
            java.util.logging.Logger.getLogger("io.openremote").setLevel(Level.FINEST);

            // Get write permission
            String writePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            if (ContextCompat.checkSelfPermission(getApplicationContext(), writePermission) == PackageManager.PERMISSION_GRANTED) {
                addFileHandler(getApplicationContext());
            }
        }

        Logger LOG = Logger.getLogger(MainApplication.class.getName());
        LOG.info(">>> Starting OpenRemote application");
    }

    static void addFileHandler(Context context) {

        try {

            File logDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "openremote");

            if (!logDirectory.exists() && !logDirectory.mkdirs()) {
                throw new Exception("Failed to make log directory");
            }

            String logFilePattern = logDirectory.getAbsolutePath() + File.separator + "openremote.log";

            FileHandler fileHandler = null;
            fileHandler = new FileHandler(logFilePattern, 10000000, 10, true);
            fileHandler.setFormatter(new SimpleFormatter());
            Logger rootLogger = LogManager.getLogManager().getLogger("");
            rootLogger.addHandler(fileHandler);

            rootLogger.info("Added logging file handler");

            // Force scan of first log file as might not show via MTP otherwise
            MediaScannerConnection.scanFile(
                context,
                new String[]{logFilePattern + ".0"},
                null,
                null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
