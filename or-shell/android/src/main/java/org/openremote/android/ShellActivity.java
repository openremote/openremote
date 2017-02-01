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

import android.app.Activity;
import android.content.*;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.model.meta.Device;
import org.jboss.aerogear.android.core.Callback;
import org.openremote.android.announce.ControllerAnnounceListener;
import org.openremote.android.announce.ControllerAnnounceService;
import org.openremote.android.announce.ControllerItem;
import org.openremote.android.util.AndroidLoggingHandler;
import org.openremote.android.util.KeycloakHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ShellActivity extends Activity {

    public static final String KEY_AUTO_CONNECT_CONTROLLER = ShellActivity.class.getName() + ".AUTO_CONNECT_CONTROLLER";
    public static final String KEY_CONTROLLER_URL = ShellActivity.class.getName() + ".CONTROLLER_URL";
    public static final String KEY_AUTO_CONNECT_CHECKED = ShellActivity.class.getName() + ".AUTO_CONNECT_CHECKED";
    public static final String KEY_CONFIG_CHANGE = ShellActivity.class.getName() + ".CONFIG_CHANGE";

    protected ArrayAdapter<ControllerItem> controllerItems;
    protected ControllerAnnounceListener controllerAnnounceListener;
    protected AndroidUpnpService controllerAnnounceService;
    private ServiceConnection controllerAnnounceClientConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            controllerAnnounceService = (AndroidUpnpService) service;

            // Clear the list
            controllerItems.clear();

            // Get ready for future device advertisements
            controllerAnnounceService.getRegistry().addListener(controllerAnnounceListener);

            // Now add all devices to the list we already know about
            for (Device device : controllerAnnounceService.getRegistry().getDevices()) {
                controllerAnnounceListener.deviceAdded(device);
            }

            // Search asynchronously for all devices, they will respond soon
            controllerAnnounceService.getControlPoint().search();
        }

        public void onServiceDisconnected(ComponentName className) {
            controllerAnnounceService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* ############################ DEV SETTINGS ##################################### */
        AndroidLoggingHandler.reset(new AndroidLoggingHandler());
        java.util.logging.Logger.getLogger("org.openremote").setLevel(Level.FINEST);
        java.util.logging.Logger.getLogger("org.openremote.android.shell").setLevel(Level.OFF);
        /* ############################ DEV SETTINGS ##################################### */

        setContentView(R.layout.activity_shell);

        // Prepare the list to collect controller announcements
        controllerItems = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        ListView listView = (ListView) findViewById(R.id.controllerList);
        listView.setAdapter(controllerItems);
        listView.setEmptyView(findViewById(R.id.findControllersButton));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setControllerUrl(controllerItems.getItem(position).getPresentationURI());
            }
        });

        // This will start the UPnP service if it wasn't already started
        controllerAnnounceListener = new ControllerAnnounceListener(ShellActivity.this, controllerItems);
        getApplicationContext().bindService(
            new Intent(this, ControllerAnnounceService.class),
            controllerAnnounceClientConnection,
            Context.BIND_AUTO_CREATE
        );

        findViewById(R.id.findControllersButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findControllers();
            }
        });


        /* TODO At some point we might need this progress/busy indicator again
        ((ProgressBar) findViewById(R.id.controllersLoadingProgress)).getIndeterminateDrawable()
            .setColorFilter(
                getResources().getColor(R.color.green_accent),
                android.graphics.PorterDuff.Mode.SRC_IN
            );

            // Show progress
            findViewById(R.id.findControllersButton).setVisibility(View.GONE);
            findViewById(R.id.controllerList).setVisibility(View.GONE);
            findViewById(R.id.controllersLoading).setVisibility(View.VISIBLE);

            // Hide progress
            findViewById(R.id.controllerList).setVisibility(View.VISIBLE);
            findViewById(R.id.controllersLoading).setVisibility(View.GONE);

        */

        ((EditText) findViewById(R.id.controllerUrl)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                boolean enabled = s.length() > 0;
                Button button = ((Button) findViewById(R.id.connectButton));
                button.setEnabled(enabled);
                button.setTextColor(enabled ? getResources().getColor(R.color.dark_grey) : getResources().getColor(R.color.medium_grey));
            }
        });

        findViewById(R.id.connectButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startConsoleView();
            }
        });

        // Android restarted
        if (savedInstanceState != null) {
            setControllerUrl(savedInstanceState.getString(KEY_CONTROLLER_URL));
            setAutoConnectChecked(savedInstanceState.getBoolean(KEY_AUTO_CONNECT_CHECKED));
        }

        // We restarted
        if (getIntent().getStringExtra(KEY_CONTROLLER_URL) != null) {
            setControllerUrl(getIntent().getStringExtra(KEY_CONTROLLER_URL));
        }
        if (getIntent().getBooleanExtra(KEY_AUTO_CONNECT_CHECKED, false)) {
            setAutoConnectChecked(true);
        }

        String savedAutoConnect = getSavedAutoConnect();
        if (savedAutoConnect != null) {
            setControllerUrl(savedAutoConnect);
            setAutoConnectChecked(true);

            // Immediately go to console view if this isn't a config change (orientation change)
            boolean isConfigChange = getIntent().getBooleanExtra(KEY_CONFIG_CHANGE, false);
            if (!isConfigChange) {
                startConsoleView();
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // We must handle orientation restart, so auto-connect isn't triggered in onCreate()
        getIntent().putExtra(KEY_CONFIG_CHANGE, true);
        getIntent().putExtra(KEY_CONTROLLER_URL, getControllerUrl());
        getIntent().putExtra(KEY_AUTO_CONNECT_CHECKED, isAutoConnectChecked());
        finish();
        startActivity(getIntent());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_shell, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_find_controllers:
                findControllers();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_CONTROLLER_URL, getControllerUrl());
        outState.putBoolean(KEY_AUTO_CONNECT_CHECKED, isAutoConnectChecked());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (controllerAnnounceService != null) {
            controllerAnnounceService.getRegistry().removeListener(controllerAnnounceListener);
        }
        // This will stop the UPnP service if nobody else is bound to it
        getApplicationContext().unbindService(controllerAnnounceClientConnection);
    }

    protected void findControllers() {
        if (controllerAnnounceService != null) {
            Toast toast = Toast.makeText(ShellActivity.this, "Broadcasting controller search message...", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();
            controllerAnnounceService.getControlPoint().search();
        }
    }

    protected String getControllerUrl() {
        return ((EditText) findViewById(R.id.controllerUrl)).getText().toString();
    }

    protected void setControllerUrl(String controllerUrl) {
        ((EditText) findViewById(R.id.controllerUrl)).setText(controllerUrl);
    }

    protected boolean isAutoConnectChecked() {
        return ((CheckBox) findViewById(R.id.autoConnectCheckBox)).isChecked();
    }

    protected void setAutoConnectChecked(boolean autoConnect) {
        ((CheckBox) findViewById(R.id.autoConnectCheckBox)).setChecked(autoConnect);
    }

    protected void startConsoleView() {
        saveAutoConnect(getControllerUrl());


        Intent intent = new Intent(ShellActivity.this, ConsoleViewActivity.class);
        intent.putExtra(ConsoleViewActivity.KEY_CONTROLLER_URL, getControllerUrl());
        startActivity(intent);
    }

    protected String getSavedAutoConnect() {
        return getPreferences(MODE_PRIVATE).getString(KEY_AUTO_CONNECT_CONTROLLER, null);
    }

    protected void saveAutoConnect(String controller) {
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        if (controller != null && controller.length() > 0 && isAutoConnectChecked()) {
            editor.putString(KEY_AUTO_CONNECT_CONTROLLER, controller);
        } else {
            editor.remove(KEY_AUTO_CONNECT_CONTROLLER);
        }
        editor.apply();
    }

}