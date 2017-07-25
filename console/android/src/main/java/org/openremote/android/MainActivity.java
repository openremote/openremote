package org.openremote.android;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.openremote.android.service.TokenService;

import java.util.logging.Logger;

public class MainActivity extends Activity {

    private static final Logger LOG = Logger.getLogger(MainActivity.class.getName());

    protected final ConnectivityChangeReceiver connectivityChangeReceiver = new ConnectivityChangeReceiver();
    protected WebView webView;
    protected ErrorViewHolder errorViewHolder;

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
        }

        public void hide() {
            errorView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
            String url = getString(R.string.OR_BASE_SERVER) + getString(R.string.OR_CONSOLE_URL);
            if (getIntent().hasExtra("url")) {
                url = url + getIntent().getStringExtra("url");
            }
            LOG.fine("Loading web view: " + url);
            webView.loadUrl(url);
        }

        errorViewHolder = new ErrorViewHolder(findViewById(R.id.errorView));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.hasExtra("url")) {
            String url = getString(R.string.OR_BASE_SERVER) + getString(R.string.OR_CONSOLE_URL) + intent.getStringExtra("url");
            LOG.fine("Loading web view: " + url);
            webView.loadUrl(url);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(connectivityChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(connectivityChangeReceiver);
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
        if ("about:blank".equals(url))
            url = getString(R.string.OR_BASE_SERVER) + getString(R.string.OR_CONSOLE_URL);
        LOG.fine("Reloading web view: " + url);
        webView.loadUrl(url);
    }

    public void exitOnClick(View view) {
        finish();
        System.exit(0);
    }

    protected void initializeWebView() {
        LOG.fine("Initializing web view");

        webView.addJavascriptInterface(new WebAppInterface(this), "MobileInterface");
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAppCacheEnabled(true);
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // Remote debugging sessions from Chrome trigger "ERR_CACHE_MISS" that don't hurt, but we should not redirect the view
                    if (isRemoteDebuggingEnabled() && error.getErrorCode() == ERROR_UNKNOWN) {
                        return;
                    }
                    LOG.warning("Error requesting '" + request.getUrl() + "': " + error.getErrorCode());
                }
                errorViewHolder.show(R.string.fatalError, R.string.fatalErrorExplain, false, true);
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
    }

    protected class WebAppInterface {

        private final TokenService tokenService;

        public WebAppInterface(Activity activity) {
            tokenService = new TokenService(activity);
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
                    tokenService.saveToken(data.getString("token"), data.getString("refreshToken"), data.getString("idToken"));
                    break;
                case "logout":
                    tokenService.clearToken();
                    break;
                case "status":
                    // TODO: status that we want to receive from webview | waiting to know if it's relevant to show error if bad connectivity meybe a conter or timmout retry should be consider
                    // currently
                    // websocket drop with error
                    // websocket connect back (success)
                default:
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


