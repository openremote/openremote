package org.openremote.android;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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

import org.json.JSONException;
import org.json.JSONObject;
import org.openremote.android.service.GeofenceProvider;
import org.openremote.android.service.TokenService;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class MainActivity extends Activity {

    private static final Logger LOG = Logger.getLogger(MainActivity.class.getName());

    private final int WRITE_PERMISSION_REQUEST = 999;

    public static final String ACTION_BROADCAST = "ACTION_BROADCAST";

    protected final ConnectivityChangeReceiver connectivityChangeReceiver = new ConnectivityChangeReceiver();
    protected WebView webView;
    protected ErrorViewHolder errorViewHolder;
    protected Context context;
    protected SharedPreferences sharedPreferences;
    protected GeofenceProvider geofenceProvider;

    protected BroadcastReceiver onDownloadCompleteReciever = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                Toast.makeText(getApplicationContext(), R.string.download_completed, Toast.LENGTH_LONG).show();
            }
        }
    };

    protected BroadcastReceiver actionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("action")) {
                String action = intent.getStringExtra("action");
                switch (action) {
                    case "GEOFENCE_REFRESH":
                        geofenceProvider.refreshGeofences();
                        break;
                }
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
            btnReload = (Button) errorView.findViewById(R.id.reload);
            btnExit = (Button) errorView.findViewById(R.id.exit);
            tvErrorTitle = (TextView) errorView.findViewById(R.id.errorTitle);
            tvErrorExplanation = (TextView) errorView.findViewById(R.id.errorExplanation);
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
        String url = getString(R.string.OR_BASE_SERVER) + getString(R.string.OR_CONSOLE_URL) + "?consolePlatform=" + platform;
        return url;
    }

    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        // Enable remote debugging of WebView from Chrome Debugger tools
        if (isRemoteDebuggingEnabled()) {
            LOG.info("Enabling remote debugging");
            WebView.setWebContentsDebuggingEnabled(true);
        }

        setContentView(R.layout.activity_web);

        webView = (WebView) findViewById(R.id.webview);
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            initializeWebView();
            if (!getIntent().hasExtra("url")) {
                String url = getClientUrl();
                LOG.fine("Loading web view: " + url);
                webView.loadUrl(url);
            }
        }
        openIntentUrl(getIntent());

        errorViewHolder = new ErrorViewHolder(findViewById(R.id.errorView));

        registerReceiver(actionReceiver, new IntentFilter(ACTION_BROADCAST));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        openIntentUrl(intent);
    }

    protected void openIntentUrl(Intent intent) {
        if (intent.hasExtra("url")) {
            String url = getClientUrl();
            String intentUrl = intent.getStringExtra("url");
            if (intentUrl != null) {
                if (intentUrl.startsWith("http") || intentUrl.startsWith("https")) {
                    url = intentUrl;
                } else {
                    url = url + intentUrl;
                }
            }

            LOG.fine("Loading web view: " + url);
            webView.loadUrl(url);
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
        unregisterReceiver(actionReceiver);

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
            webView.loadUrl(url);
        }
    }

    public void exitOnClick(View view) {
        finish();
        System.exit(0);
    }

    protected void initializeWebView() {
        LOG.fine("Initializing web view");

        final WebAppInterface webAppInterface = new WebAppInterface(this, webView);

        webView.addJavascriptInterface(webAppInterface, "MobileInterface");
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
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
                //TODO should we ignore images?
                if (request.getUrl().getLastPathSegment() != null &&
                        (request.getUrl().getLastPathSegment().endsWith("png")
                                || request.getUrl().getLastPathSegment().endsWith("jpg")
                                || request.getUrl().getLastPathSegment().endsWith("ico"))
                        )
                    return;

                // When initialising Keycloak with an invalid offline refresh token (e.g. wrong nonce because
                // server was reinstalled), we detect the failure and then don't show an error view. We clear the stored
                // invalid token. The web app will then start a new login.
                if (request.getUrl().getLastPathSegment() != null &&
                        request.getUrl().getLastPathSegment().equals("token") &&
                        request.getMethod().equals("POST") &&
                        errorResponse.getStatusCode() == 400) {
                    webAppInterface.tokenService.clearToken();
                    return;
                }

                LOG.warning("Error requesting '" + request.getUrl() + "', response code: " + errorResponse.getStatusCode());
                errorViewHolder.show(R.string.httpError, R.string.httpErrorExplain, true, true);
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

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                //TODO should we ignore images?
                if (request.getUrl().getLastPathSegment() != null &&
                        (request.getUrl().getLastPathSegment().endsWith("png")
                                || request.getUrl().getLastPathSegment().endsWith("jpg")
                                || request.getUrl().getLastPathSegment().endsWith("ico"))
                        )
                    return;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    // Remote debugging sessions from Chrome trigger "ERR_CACHE_MISS" that don't hurt, but we should not redirect the view
                    if (isRemoteDebuggingEnabled() && error.getErrorCode() == ERROR_UNKNOWN) {
                        return;
                    }

                    // Remote debugging session from Chrome wants to load about:blank and then fails with "ERROR_UNSUPPORTED_SCHEME", ignore
                    if (request.getUrl().toString().equals("about:blank") && error.getErrorCode() == ERROR_UNSUPPORTED_SCHEME) {
                        return;
                    }

                    LOG.warning("Error requesting '" + request.getUrl() + "': " + error.getErrorCode());
                }
                errorViewHolder.show(R.string.fatalError, R.string.fatalErrorExplain, false, true);
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
                if (ContextCompat.checkSelfPermission(context, writePermission) != PackageManager.PERMISSION_GRANTED) {
                    // Location permission has not been granted yet, request it.
                    ActivityCompat.requestPermissions((MainActivity) context, new String[]{writePermission}, WRITE_PERMISSION_REQUEST);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == WRITE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), R.string.downloading_file, Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == GeofenceProvider.Companion.getLocationReponseCode()) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    Map<String, Object> initData = geofenceProvider.initialize();
                    final String jsonString = new ObjectMapper().writeValueAsString(initData);
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            webView.evaluateJavascript(String.format("openremote.INSTANCE.console.handleProviderResponse('%s')", jsonString), null);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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
        public void logOut() {
            tokenService.clearToken();
        }

        @JavascriptInterface
        public void postMessage(String jsonMessage) throws JSONException {
            JSONObject reader = new JSONObject(jsonMessage);
            String messageType = reader.getString("type");
            JSONObject data = reader.optJSONObject("data");
            switch (messageType) {
                case "token":
                    LOG.fine("Received WebApp message, storing offline refresh token");
                    tokenService.saveToken(data.getString("refreshToken"));
                    break;
                case "logout":
                    LOG.fine("Received WebApp message, logout");
                    tokenService.clearToken();
                    break;
                case "error":
                    LOG.fine("Received WebApp message, error: " + data.getString("error"));
                    Handler mainHandler = new Handler(context.getMainLooper());
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
                            handleGeofenceProviderMessage(action, data.has("data") ? data.getJSONObject("data") : null);
                        } else if (provider.equalsIgnoreCase("push")) {
                            handlePushProviderMessage(action, data.has("data") ? data.getJSONObject("data") : null);
                        }
                    }
                    break;
                default:
            }
        }

        protected void handleGeofenceProviderMessage(String action, JSONObject data) throws JSONException {
            if (action.equalsIgnoreCase("PROVIDER_INIT")) {
                geofenceProvider = new GeofenceProvider(activity);
                if (!geofenceProvider.checkPermission()) {
                    geofenceProvider.registerPermissions();
                } else {
                    Map<String, Object> initData = geofenceProvider.initialize();
                    notifyClient(initData);
                }
            } else if (action.equalsIgnoreCase("PROVIDER_ENABLE")) {
                if (data != null) {
                    String consoleId = data.getString("consoleId");
                    if (consoleId != null) {
                        Map<String, Object> enableData = geofenceProvider.enable(String.format("%s/%s",
                                getString(R.string.OR_BASE_SERVER),
                                getString(R.string.OR_REALM)),
                                consoleId);
                        notifyClient(enableData);
                    }
                }
            } else if (action.equalsIgnoreCase("PROVIDER_DISABLE")) {
                geofenceProvider.disable();
                Map<String, Object> response = new HashMap<>();
                response.put("action", "PROVIDER_DISABLE");
                response.put("provider", "geofence");
                notifyClient(response);
            } else if (action.equalsIgnoreCase("GEOFENCE_REFRESH")) {
                geofenceProvider.refreshGeofences();
            }
        }

        protected void handlePushProviderMessage(String action, JSONObject data) throws JSONException {
            if (action.equalsIgnoreCase("PROVIDER_INIT")) {
                // Push permission is covered by the INTERNET permission and is not a runtime permission
                Map<String, Object> response = new HashMap<>();
                response.put("action", "PROVIDER_INIT");
                response.put("provider", "push");
                response.put("version", "fcm");
                response.put("requiresPermission", false);
                response.put("success", true);
                notifyClient(response);
            } else if (action.equalsIgnoreCase("PROVIDER_ENABLE")) {
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
                notifyClient(response);
            }
        }

        protected void notifyClient(Map<String, Object> data) {
            try {
                final String jsonString = new ObjectMapper().writeValueAsString(data);

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        webView.evaluateJavascript(String.format("openremote.INSTANCE.console.handleProviderResponse('%s')", jsonString), null);
                    }
                });
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        @JavascriptInterface
        public String getMessage(String messageKey) {
            switch (messageKey) {
                case "token":
                    return tokenService.getJsonToken();
                default:
                    return "{}";
            }
        }
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

    protected boolean isRemoteDebuggingEnabled() {
        return Boolean.valueOf(getString(R.string.ENABLE_REMOTE_DEBUGGING));
    }
}


