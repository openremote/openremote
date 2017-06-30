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
import android.util.Log;
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

    private final ConnectivityChangeReceiver connectivityChangeReceiver = new ConnectivityChangeReceiver();
    private WebView webView;

    private boolean hasNetwork = false;

    private ErrorViewHolder errorViewHolder;


    class ErrorViewHolder {
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

        void show(int errorTitle, int errorDescription, boolean canReload, boolean canExit) {

            btnReload.setVisibility(canReload ? View.VISIBLE : View.GONE);
            btnExit.setVisibility(canExit ? View.VISIBLE : View.GONE);

            tvErrorTitle.setText(errorTitle);
            tvErrorExplanation.setText(errorDescription);
            errorView.setVisibility(View.VISIBLE);
        }

        private void hide() {
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
            loadIndex();
        }
        errorViewHolder = new ErrorViewHolder(findViewById(R.id.errorView));


    }

    @Override
    protected void onNewIntent(Intent intent) {
       if (intent.hasExtra("url")) {
           webView.loadUrl(getString(R.string.OR_BASE_SERVER) + getString(R.string.OR_CONSOLE_URL) + intent.getStringExtra("url"));
       }
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(
                connectivityChangeReceiver,
                new IntentFilter(
                        ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(connectivityChangeReceiver);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
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
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
    }

    private void loadIndex() {

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
                Log.i("WEBVIEW", "request url :" + request.getUrl().toString());
                Log.i("WEBVIEW", "error :" + errorResponse.getStatusCode());
                if (request.getUrl().toString().startsWith(getString(R.string.OR_BASE_SERVER) + getString(R.string.OR_CONSOLE_URL))) {
                    errorViewHolder.show(R.string.httpError,R.string.httpErrorExplain,true,true);
                }


                //
                //todo: translate common error into user friendly dialog msg ex 400-500 => try again later service not avaible (app clean exit with confirm button)
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                if (Boolean.parseBoolean(getString(R.string.SSL_IGNORE))) {
                    handler.proceed(); // Ignore SSL certificate errors
                } else {
                    super.onReceivedSslError(view, handler, error);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    LOG.info("request url :" + request.getUrl().toString());
                    LOG.info( "error :" + error.getErrorCode());
                    LOG.info( "error :" + error.getDescription());
                    // Remote debugging sessions from Chrome trigger "ERR_CACHE_MISS" that don't hurt, but we should not redirect the view
                    if (isRemoteDebuggingEnabled() && error.getErrorCode() == ERROR_UNKNOWN) {
                        return;
                    }
                }

                //TODO => try again later service not avaible (app clean exit with confirm button) // good message
                //webView.loadUrl("about:blank");
                errorViewHolder.show(R.string.fatalError,R.string.fatalErrorExplain,false,true);
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        String url = getString(R.string.OR_BASE_SERVER) + getString(R.string.OR_CONSOLE_URL);
        if (getIntent().hasExtra("url")) {
            url = url + getIntent().getStringExtra("url");
        }
        webView.loadUrl(url);
    }



    public void reloadPage(View view) {
        errorViewHolder.hide();
        webView.loadUrl(webView.getUrl());
    }

    public void exitOnClick(View view) {
        finish();
    }

    private class WebAppInterface {

       private final TokenService tokenService;

        public WebAppInterface(Activity activity) {
            tokenService = new TokenService(activity);
        }

        @JavascriptInterface
        public void logOut() {
            tokenService.clearToken();
        }

        @JavascriptInterface
        public String getMobileToken() {
            return tokenService.getJsonToken();
        }

        @JavascriptInterface
        public void setMobileToken(String token, String refreshToken, String idToken) {
            tokenService.saveToken(token, refreshToken, idToken);
        }

        @JavascriptInterface
        public void postMessage(String jsonMessage) throws JSONException {
            JSONObject reader = new JSONObject(jsonMessage);
            String messageType = reader.getString("type");
            JSONObject data = reader.getJSONObject("data");
            switch (messageType) {
                case "token" :
                    tokenService.saveToken(data.getString("token"), data.getString("refreshToken") , data.getString("idToken"));
                    break;
                case "error" :
                    //
                default:
            }
        }

        @JavascriptInterface
        public String getMessage(String messageKey) {
            switch (messageKey) {
                case "token" :
                    return tokenService.getJsonToken();
                default:
                    return "{}";
            }
        }
        //TODO receive error with severity
    }


    private void onConnectivityChanged(boolean connectivity) {
        this.hasNetwork = connectivity;
        if (connectivity) {
            //TODO :  pass conectivity information to wwebapp
            reloadPage(getCurrentFocus());
        } else {
            errorViewHolder.show(R.string.noConnectivity,R.string.noConnectivityExplain,false,true);
        }
    }

    private class ConnectivityChangeReceiver extends BroadcastReceiver {

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


