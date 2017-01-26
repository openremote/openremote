/*
 * Copyright 2015, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.openremote.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;
import org.openremote.android.util.UrlUtil;
import org.openremote.shared.event.bus.EventBus;
import org.openremote.shared.event.bus.EventListener;
import org.openremote.shared.event.bus.EventRegistration;
import org.openremote.shared.event.client.ConsoleRefreshedEvent;
import org.openremote.shared.event.client.ShellOpenEvent;
import org.openremote.shared.event.client.ShellReadyEvent;
import org.openremote.shared.event.client.ShowFailureEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ConsoleViewActivity extends Activity {

    private static final Logger LOG = Logger.getLogger(ConsoleViewActivity.class.getName());

    public static final String KEY_CONTROLLER_URL = ConsoleViewActivity.class.getName() + ".CONTROLLER_URL";

    protected List<EventRegistration> eventRegistrations = new ArrayList<>();
    protected WebView consoleView;
    protected ShellMessageBus shellMessageBus;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_console_view);

        consoleView = (WebView) findViewById(R.id.consoleView);

        consoleView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                LOG.warning("Error loading console view: " + failingUrl);
                LOG.warning("Error code: " + errorCode);
                LOG.warning("Error description: " + description);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        EventBus.dispatch(
                            new ShowFailureEvent("Error connecting to controller.")
                        );
                    }
                });
            }
        });

        WebSettings webSettings = consoleView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        consoleView.clearCache(true);
        webSettings.setAppCacheEnabled(false);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        shellMessageBus = new ShellMessageBus(this, consoleView);
        consoleView.addJavascriptInterface(shellMessageBus, "ShellMessageBus");

        String controllerURL = getIntent().getStringExtra(KEY_CONTROLLER_URL);

        eventRegistrations.clear();

        eventRegistrations.add(shellMessageBus);

        eventRegistrations.add(new EventRegistration<>(false, ShellReadyEvent.class, new InitialFlowListener(controllerURL)));

        eventRegistrations.add(new EventRegistration<>(false, ConsoleRefreshedEvent.class, new EventListener<ConsoleRefreshedEvent>() {
            @Override
            public void on(ConsoleRefreshedEvent consoleRefreshedEvent) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.consoleViewLoading).setVisibility(View.GONE);
                        consoleView.setVisibility(View.VISIBLE);
                    }
                });
            }
        }));

        eventRegistrations.add(new EventRegistration<>(false, ShellOpenEvent.class, new EventListener<ShellOpenEvent>() {
            @Override
            public void on(final ShellOpenEvent event) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onBackPressed();
                    }
                });
            }
        }));

        eventRegistrations.add(new EventRegistration<>(false, ShowFailureEvent.class, new EventListener<ShowFailureEvent>() {
            @Override
            public void on(final ShowFailureEvent event) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast toast = Toast.makeText(ConsoleViewActivity.this, event.getText(), Toast.LENGTH_LONG);
                        View view = toast.getView();
                        TextView text = (TextView) view.findViewById(android.R.id.message);
                        text.setTextColor(getResources().getColor(R.color.white));
                        text.setBackgroundColor(Color.TRANSPARENT);
                        view.setBackgroundResource(R.color.red);
                        toast.show();
                        onBackPressed();
                    }
                });
            }
        }));

        controllerURL = UrlUtil.url(controllerURL, null, "embedded.html").toString();
        consoleView.loadUrl(controllerURL);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (consoleView != null) {
            LOG.fine("Pausing console view");
            consoleView.onPause();
        }
        EventBus.removeAll(eventRegistrations);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (consoleView != null) {
            LOG.fine("Resuming console view");
            consoleView.onResume();
        }
        EventBus.addAll(eventRegistrations);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (consoleView != null) {
            consoleView.destroy();
        }
    }
}