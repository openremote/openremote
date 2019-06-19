package org.openremote.android;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.iid.FirebaseInstanceId;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.openremote.android.service.GeofenceProvider;
import org.openremote.android.service.TokenService;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {

    private static final Logger LOG = Logger.getLogger(MainActivity.class.getName());

    private static final int WRITE_PERMISSION_FOR_DOWNLOAD = 999;
    private static final int WRITE_PERMISSION_FOR_LOGGING = 1000;
    private static final int WEBVIEW_LOAD_TIMEOUT_DEFAULT = 5000;

    public static final String ACTION_BROADCAST = "ACTION_BROADCAST";
    public static final String PUSH_PROVIDER_DISABLED_KEY = "PushProviderDisabled";
    public static final String CONSOLE_ID_KEY = "consoleId";

    protected final ConnectivityChangeReceiver connectivityChangeReceiver = new ConnectivityChangeReceiver();
    protected WebView webView;
    protected int webViewTimeout = WEBVIEW_LOAD_TIMEOUT_DEFAULT;
    protected boolean webViewLoaded;
    protected ErrorViewHolder errorViewHolder;
    protected SharedPreferences sharedPreferences;
    protected GeofenceProvider geofenceProvider;
    protected String consoleId;
    protected BroadcastReceiver onDownloadCompleteReciever = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                Toast.makeText(getApplicationContext(), R.string.download_completed, Toast.LENGTH_LONG).show();
            }
        }
    };

    protected class ErrorViewHolder {
        View errorView;
        Button btnReload;
        Button btnExit;
        TextView tvErrorTitle;
        TextView tvErrorExplanation;

        public ErrorViewHolder(View errorView) {
            this.errorView = errorView;
            btnReload = errorView.findViewById(R.id.reload);
            btnExit = errorView.findViewById(R.id.exit);
            tvErrorTitle = errorView.findViewById(R.id.errorTitle);
            tvErrorExplanation = errorView.findViewById(R.id.errorExplanation);
        }

        public void show(int errorTitle, int errorDescription, boolean canReload, boolean canExit) {
            LOG.fine("Showing error view");

            btnReload.setVisibility(canReload ? View.VISIBLE : View.GONE);
            btnExit.setVisibility(canExit ? View.VISIBLE : View.GONE);

            tvErrorTitle.setText(errorTitle);
            tvErrorExplanation.setText(errorDescription);
            errorView.setVisibility(View.VISIBLE);

            // Stop and clear web view
            LOG.fine("Clearing web view");
            webView.loadUrl("about:blank");
            webView.setVisibility(View.GONE);
        }

        public void hide() {
            errorView.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
        }
    }

    protected String getClientUrl() {
        String platform = "Android " + Build.VERSION.RELEASE;
        String name = getString(R.string.OR_CONSOLE_NAME);
        String providers = getString(R.string.OR_CONSOLE_PROVIDERS);
        String version = BuildConfig.VERSION_NAME;

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return getString(R.string.OR_BASE_SERVER) + getString(R.string.OR_CONSOLE_URL)
                + "?consolePlatform=" + platform
                + "&consoleName=" + name
                + "&consoleVersion=" + version
                + "&consoleProviders=" + providers;
    }

    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (BuildConfig.DEBUG) {
            // Check write permission for logging purposes

            String writePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            if (ContextCompat.checkSelfPermission(getApplicationContext(), writePermission) != PackageManager.PERMISSION_GRANTED) {
                // Write permission has not been granted yet, request it.
                ActivityCompat.requestPermissions(this, new String[]{writePermission}, WRITE_PERMISSION_FOR_LOGGING);
            } else {
                // Assume logging has already been initialised by main application
            }
        }

        try {
            String timeoutStr = getString(R.string.OR_CONSOLE_LOAD_TIMEOUT);
            webViewTimeout = Integer.parseInt(timeoutStr);
        } catch (NumberFormatException nfe) {
            System.out.println("Could not parse console load timeout value: " + nfe);
        }

        // Enable remote debugging of WebView from Chrome Debugger tools
        if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            LOG.info("Enabling remote debugging");
            WebView.setWebContentsDebuggingEnabled(true);
        }

        setContentView(R.layout.activity_web);

        webView = findViewById(R.id.webview);
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            initializeWebView();
        }
        openIntentUrl(getIntent());

        errorViewHolder = new ErrorViewHolder(findViewById(R.id.errorView));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        openIntentUrl(intent);
    }

    protected void openIntentUrl(Intent intent) {
        if (!intent.hasExtra("appUrl")) {
            String url = getClientUrl();
            LOG.fine("Loading web view: " + url);
            loadUrl(url);
        } else {
            String url = getClientUrl();
            String intentUrl = intent.getStringExtra("appUrl");
            if (intentUrl != null) {
                if (intentUrl.startsWith("http") || intentUrl.startsWith("https")) {
                    url = intentUrl;
                } else {
                    url = url + intentUrl;
                }
            }

            LOG.fine("Loading web view: " + url);
            loadUrl(url);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(connectivityChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        registerReceiver(onDownloadCompleteReciever, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(connectivityChangeReceiver);
        unregisterReceiver(onDownloadCompleteReciever);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (webView != null) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_BACK:
                        if (webView.canGoBack()) {
                            webView.goBack();
                        } else {
                            finish();
                        }
                        return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
    }

    public void reloadWebView(View view) {
        errorViewHolder.hide();
        String url = webView.getUrl();
        if ("about:blank".equals(url)) {
            url = getClientUrl();
            LOG.fine("Reloading web view: " + url);
            loadUrl(url);
        }
    }

    public void exitOnClick(View view) {
        finish();
        System.exit(0);
    }

    @SuppressLint("SetJavaScriptEnabled")
    protected void initializeWebView() {
        LOG.fine("Initializing web view");

        final WebAppInterface webAppInterface = new WebAppInterface(this, webView);

        webView.addJavascriptInterface(webAppInterface, "MobileInterface");
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });
        webView.setLongClickable(false);

        webView.setWebViewClient(new WebViewClient() {


            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {

                // When initialising Keycloak with an invalid offline refresh token (e.g. wrong nonce because
                // server was reinstalled), we detect the failure and then don't show an error view. We clear the stored
                // invalid token. The web app will then start a new login.
                if (request.getUrl().getLastPathSegment() != null &&
                        request.getUrl().getLastPathSegment().equals("token") &&
                        request.getMethod().equals("POST") &&
                        errorResponse.getStatusCode() == 400) {
                    MainActivity.this.storeData(getString(R.string.SHARED_PREF_REFRESH_TOKEN), null);
                    return;
                }

                handleError(errorResponse.getStatusCode(), errorResponse.getReasonPhrase(), request.getUrl().toString(), request.isForMainFrame());
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                if (Boolean.parseBoolean(getString(R.string.SSL_IGNORE))) {
                    LOG.fine("Ignoring SSL certificate error: " + error.getPrimaryError());
                    handler.proceed(); // Ignore SSL certificate errors
                } else {
                    LOG.severe("SSL error: " + error.getPrimaryError());
                    LOG.severe("SSL certificate: " + error.getCertificate());
                    errorViewHolder.show(R.string.httpError, R.string.httpErrorExplain, true, true);
                }
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                handleError(errorCode, description, failingUrl, true);
            }

            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {

                // Remote debugging session from Chrome wants to load about:blank and then fails with "ERROR_UNSUPPORTED_SCHEME", ignore
                if ("net::ERR_CACHE_MISS".contentEquals(error.getDescription())) {
                    return;
                }

                if (request.getUrl().toString().equals("about:blank") && error.getErrorCode() == ERROR_UNSUPPORTED_SCHEME) {
                    return;
                }

                handleError(
                        error.getErrorCode(), error.getDescription().toString(),
                        request.getUrl().toString(), request.isForMainFrame());
            }

            @Override
            public void onPageStarted(WebView view, final String url, Bitmap favicon) {

                Runnable run = new Runnable() {
                    public void run() {
                        if (!webViewLoaded) {
                            handleError(ERROR_TIMEOUT, "Connection timed out", url, true);
                        }
                    }
                };
                Handler myHandler = new Handler(Looper.myLooper());
                myHandler.postDelayed(run, 5000);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                webViewLoaded = true;
            }

            protected void handleError(int errorCode, String description, String failingUrl, boolean isForMainFrame) {

                LOG.warning("Error requesting '" + failingUrl + "': " + errorCode + "(" + description + ")");

                // This will be the URL loaded into the webview itself (false for images etc. of the main page)
                if (isForMainFrame) {

                    // Check page load error URL
                    String errorUrl = getString(R.string.OR_CONSOLE_LOAD_ERROR_URL);
                    if (!TextUtils.isEmpty(errorUrl) && !Objects.equals(failingUrl, errorUrl)) {
                        LOG.info("Loading error URL: " + errorUrl);
                        loadUrl(errorUrl);
                        return;
                    }
                } else {

                    if (Boolean.parseBoolean(getString(R.string.OR_CONSOLE_IGNORE_PAGE_ERRORS))) {
                        return;
                    }

                    //TODO should we always ignore image errors?
                    if (failingUrl != null && (failingUrl.endsWith("png")
                            || failingUrl.endsWith("jpg")
                            || failingUrl.endsWith("ico"))) {
                        LOG.info("Ignoring error loading image resource");
                        return;
                    }
                }

                errorViewHolder.show(R.string.httpError, R.string.httpErrorExplain, true, true);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                String msg = "WebApp console (" + consoleMessage.sourceId() + ":" + consoleMessage.lineNumber() + "): " + consoleMessage.message();
                switch (consoleMessage.messageLevel()) {
                    case DEBUG:
                    case TIP:
                        LOG.fine(msg);
                        break;
                    case LOG:
                        LOG.info(msg);
                        break;
                    default:
                        LOG.severe(msg);
                }
                return true;
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {

                String writePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
                if (ContextCompat.checkSelfPermission(MainActivity.this, writePermission) != PackageManager.PERMISSION_GRANTED) {
                    // Write permission has not been granted yet, request it.
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{writePermission}, WRITE_PERMISSION_FOR_DOWNLOAD);
                } else {
                    DownloadManager.Request request = new
                            DownloadManager.Request(Uri.parse(url));

                    request.setMimeType(mimetype);
                    //------------------------COOKIE!!------------------------
                    String cookies = CookieManager.getInstance().getCookie(url);
                    request.addRequestHeader("cookie", cookies);
                    //------------------------COOKIE!!------------------------
                    request.addRequestHeader("User-Agent", userAgent);
                    request.setDescription("Downloading file...");
                    request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype));
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype));
                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    if (dm != null) {
                        Toast.makeText(getApplicationContext(), R.string.downloading_file, Toast.LENGTH_LONG).show();
                        dm.enqueue(request);
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.error_downloading, Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    protected void loadUrl(String url) {
        webViewLoaded = false;
        webView.loadUrl(url);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == WRITE_PERMISSION_FOR_LOGGING) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Add File logging
                MainApplication.addFileHandler(this);
            }
        } else if (requestCode == WRITE_PERMISSION_FOR_DOWNLOAD) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), R.string.downloading_file, Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == GeofenceProvider.Companion.getLocationReponseCode()) {
            geofenceProvider.onRequestPermissionsResult(requestCode, permissions, grantResults);
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    protected class WebAppInterface {

        private final Activity activity;
        private final TokenService tokenService;
        private final WebView webView;

        public WebAppInterface(Activity activity, WebView webView) {
            tokenService = new TokenService(activity);
            this.activity = activity;
            this.webView = webView;
        }

        @JavascriptInterface
        public void postMessage(String jsonMessage) throws JSONException {
            JSONObject reader = new JSONObject(jsonMessage);
            String messageType = reader.getString("type");
            JSONObject data = reader.optJSONObject("data");
            switch (messageType) {
                case "error":
                    LOG.fine("Received WebApp message, error: " + data.getString("error"));
                    Handler mainHandler = new Handler(getMainLooper());
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            errorViewHolder.show(R.string.fatalError, R.string.fatalErrorExplain, true, true);
                        }
                    });
                    break;
                case "provider":
                    String action = data.getString("action");
                    if (action != null) {
                        String provider = data.getString("provider");
                        if (provider.equalsIgnoreCase("geofence")) {
                            handleGeofenceProviderMessage(data);
                        } else if (provider.equalsIgnoreCase("push")) {
                            handlePushProviderMessage(data);
                        } else if (provider.equalsIgnoreCase("storage")) {
                            handleStorageProviderMessage(data);
                        }
                    }
                    break;
                default:
            }
        }

        protected void handleGeofenceProviderMessage(JSONObject data) throws JSONException {
            String action = data.getString("action");

            if (geofenceProvider == null) {
                geofenceProvider = new GeofenceProvider(activity);
            }

            if (action.equalsIgnoreCase("PROVIDER_INIT")) {
                Map<String, Object> initData = geofenceProvider.initialize();
                notifyClient(initData);
            } else if (action.equalsIgnoreCase("PROVIDER_ENABLE")) {
                String consoleId = data.getString("consoleId");

                if (consoleId != null) {
                    ((MainActivity) activity).consoleId = consoleId;
                    geofenceProvider.enable(MainActivity.this, String.format("%s/api/%s",
                            getString(R.string.OR_BASE_SERVER),
                            getString(R.string.OR_REALM)),
                            consoleId, new GeofenceProvider.GeofenceCallback() {
                                @Override
                                public void accept(@NotNull Map<String, ?> responseData) {
                                    //noinspection unchecked
                                    notifyClient((Map<String, Object>) responseData);
                                }
                            });
                }
            } else if (action.equalsIgnoreCase("PROVIDER_DISABLE")) {
                geofenceProvider.disable();
                Map<String, Object> response = new HashMap<>();
                response.put("action", "PROVIDER_DISABLE");
                response.put("provider", "geofence");
                notifyClient(response);
            } else if (action.equalsIgnoreCase("GEOFENCE_REFRESH")) {
                geofenceProvider.refreshGeofences();
            } else if (action.equalsIgnoreCase("GET_LOCATION")) {
                geofenceProvider.getLocation(MainActivity.this, new GeofenceProvider.GeofenceCallback() {
                    @Override
                    public void accept(@NotNull Map<String, ?> responseData) {
                        //noinspection unchecked
                        notifyClient((Map<String, Object>) responseData);
                    }
                });
            }
        }

        protected void handlePushProviderMessage(JSONObject data) throws JSONException {
            String action = data.getString("action");

            if (action.equalsIgnoreCase("PROVIDER_INIT")) {
                // Push permission is covered by the INTERNET permission and is not a runtime permission
                Map<String, Object> response = new HashMap<>();
                response.put("action", "PROVIDER_INIT");
                response.put("provider", "push");
                response.put("version", "fcm");
                response.put("enabled", false);
                response.put("disabled", sharedPreferences.contains(PUSH_PROVIDER_DISABLED_KEY));
                response.put("requiresPermission", false);
                response.put("hasPermission", true);
                response.put("success", true);
                notifyClient(response);
            } else if (action.equalsIgnoreCase("PROVIDER_ENABLE")) {
                String consoleId = data.getString("consoleId");

                if (consoleId != null) {
                    sharedPreferences.edit()
                            .putString(GeofenceProvider.Companion.getConsoleIdKey(), consoleId)
                            .remove(PUSH_PROVIDER_DISABLED_KEY)
                            .apply();
                }
                // TODO: Implement topic support
                String fcmToken = FirebaseInstanceId.getInstance().getToken();
                Map<String, Object> response = new HashMap<>();
                response.put("action", "PROVIDER_ENABLE");
                response.put("provider", "push");
                response.put("hasPermission", true);
                response.put("success", true);
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("token", fcmToken);
                response.put("data", responseData);
                notifyClient(response);
            } else if (action.equalsIgnoreCase("PROVIDER_DISABLE")) {
                // Cannot disable push notifications
                Map<String, Object> response = new HashMap<>();
                response.put("action", "PROVIDER_DISABLE");
                response.put("provider", "push");
                sharedPreferences.edit().putBoolean(PUSH_PROVIDER_DISABLED_KEY, true).apply();
                notifyClient(response);
            }
        }

        protected void handleStorageProviderMessage(JSONObject data) throws JSONException {
            String action = data.getString("action");

            if (action.equalsIgnoreCase("PROVIDER_INIT")) {
                Map<String, Object> response = new HashMap<>();
                response.put("action", "PROVIDER_INIT");
                response.put("provider", "storage");
                response.put("version", "1.0.0");
                response.put("enabled", true);
                response.put("requiresPermission", false);
                response.put("hasPermission", true);
                response.put("success", true);
                notifyClient(response);
            } else if (action.equalsIgnoreCase("PROVIDER_ENABLE")) {
                // Doesn't require enabling but just in case it gets called lets return a valid response
                Map<String, Object> response = new HashMap<>();
                response.put("action", "PROVIDER_ENABLE");
                response.put("provider", "storage");
                response.put("hasPermission", true);
                response.put("success", true);
                notifyClient(response);
            } else if (action.equalsIgnoreCase("STORE")) {
                try {
                    String key = data.getString("key");
                    String valueJson = data.getString("value");
                    storeData(key, valueJson);
                } catch (JSONException e) {
                    LOG.log(Level.SEVERE, "Failed to store data", e);
                }
            } else if (action.equalsIgnoreCase("RETRIEVE")) {
                try {
                    String key = data.getString("key");
                    String dataJson = retrieveData(key);
                    Map<String, Object> response = new HashMap<>();
                    response.put("action", "RETRIEVE");
                    response.put("provider", "storage");
                    response.put("key", key);
                    response.put("value", dataJson);
                    notifyClient(response);
                } catch (JSONException e) {
                    LOG.log(Level.SEVERE, "Failed to retrieve data", e);
                }
            }
        }
    }

    protected void notifyClient(Map<String, Object> data) {
        try {
            final String jsonString = new ObjectMapper().writeValueAsString(data);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    webView.evaluateJavascript(String.format("OpenRemoteConsole._handleProviderResponse('%s')", jsonString), null);
                }
            });
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    protected void storeData(String key, String data) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (data == null) {
            editor.remove(key);
        } else {
            editor.putString(key, data);
        }
        editor.apply();
    }

    protected String retrieveData(String key) {
        return sharedPreferences.getString(key, null);
    }

    protected void onConnectivityChanged(boolean connectivity) {
        LOG.info("Connectivity changed: " + connectivity);
        // TODO Should check if about:blank && connectivity == true, otherwise no reload necessary
        if (connectivity) {
            reloadWebView(getCurrentFocus());
        } else {
            errorViewHolder.show(R.string.noConnectivity, R.string.noConnectivityExplain, false, true);
        }
    }

    protected class ConnectivityChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager cm
                    = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            onConnectivityChanged(activeNetwork != null && activeNetwork.isConnectedOrConnecting());
        }
    }
}


