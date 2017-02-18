package org.openremote.android;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.jboss.aerogear.android.core.Callback;
import org.openremote.android.util.KeycloakHelper;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d("KEYCLOAK","Try connect");
        KeycloakHelper.testWebview(new Callback() {
            @Override
            public void onSuccess(Object data) {
                Log.d("KEYCLOAK","Success:" + data.toString());

                WebView view = (WebView) findViewById(R.id.webview);
                WebSettings webSettings = view.getSettings();
                webSettings.setJavaScriptEnabled(true);
                webSettings.setDomStorageEnabled(true);
                //webSettings.setAllowUniversalAccessFromFileURLs(true);

                webSettings.setAllowFileAccessFromFileURLs(true);
                webSettings.setAllowContentAccess(true);
                view.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        return false;
                    }

                });

                view.addJavascriptInterface(new WebAppInterface(data.toString()), "mobileClient");

                Map<String,String> addHeader = new HashMap<String, String>();
                // TODO This only adds the authorization header to the initial request when webview is loaded
                // TODO When we retrieve console resources from the server, all requests must be authorized
                // TODO Use interceptors as outlined here and solution for API level 21:
                // TODO https://stackoverflow.com/questions/7610790/add-custom-headers-to-webview-resource-requests-android
                addHeader.put("Authorization","Bearer "+data.toString());
                addHeader.put("Access-Control-Allow-Origin","*");
                addHeader.put("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
                addHeader.put("Access-Control-Allow-Credentials", "true");
                addHeader.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
                 view.loadUrl("file:///android_asset/index.html",addHeader);
            }

            @Override
            public void onFailure(Exception e) {
                Log.d("KEYCLOAK","failled");

            }
        }, this);
    }

    private class WebAppInterface {
        private String mToken;

        public WebAppInterface(String token) {

            mToken = token;
        }

        @JavascriptInterface
        public String getToken() {
            Log.d("WEBVIEW","getToken");
            return mToken;
        }
    }
}
