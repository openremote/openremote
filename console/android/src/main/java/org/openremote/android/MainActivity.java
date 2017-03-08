package org.openremote.android;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;


public class MainActivity extends Activity {


    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setContentView(R.layout.activity_web);
        webView = (WebView) findViewById(R.id.webview);
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        }
        else {
            loadIndex();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
    }

    private void loadIndex() {

        webView.addJavascriptInterface(new WebAppInterface(this),"MobileInterface");
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl(getString(R.string.OR_BASE_SERVER)+getString(R.string.OR_CONSOLE_URL));

    }



    private class WebAppInterface {
        private final SharedPreferences sharedPref;

        public WebAppInterface(Activity activity) {
            sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        }

        @JavascriptInterface
        public String getMobileToken() {
            Log.d("TOKEN", "get Token");
            return sharedPref.getString(getString(R.string.SHARED_PREF_TOKENS), null);
       }

        @JavascriptInterface
        public void setMobileToken(String token, String  refreshToken, String idToken) {
            Log.d("TOKEN", "Set Token");
            String mToken = "{ \"token\" :\"" +token +"\", \"refreshToken\": \""+refreshToken+ "\", \"idToken\": \""+idToken+"\"}";
            sharedPref.edit().putString(getString(R.string.SHARED_PREF_TOKENS), mToken).commit();
        }
    }
}


