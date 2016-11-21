/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.agent.controller2;

import org.openremote.agent.controller2.model.ControllerState;
import org.openremote.console.controller.Controller;
import org.openremote.console.controller.ControllerConnectionStatus;
import org.openremote.console.controller.auth.UserPasswordCredentials;
import org.openremote.entities.controller.AsyncControllerCallback;
import org.openremote.entities.controller.ControllerResponseCode;

import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class Controller2Adapter {

    private static final Logger LOG = Logger.getLogger(Controller2Adapter.class.getName());

    final static protected long RECONNECT_DELAY_SECONDS = 5;

    final protected AtomicInteger referenceCount = new AtomicInteger(1);

    final protected URL url;
    final protected String username;
    final protected String password;

    final protected Controller controller;
    final protected ScheduledExecutorService connectionScheduler = Executors.newScheduledThreadPool(1);
    final protected AsyncControllerCallback<ControllerConnectionStatus> connectCallback;

    final protected ControllerState controllerState;

    protected volatile boolean connectionInProgress;
    protected volatile boolean forceDisconnect;

    public Controller2Adapter(URL url, String username, String password) {
        this(url, username, password, null);
    }

    public Controller2Adapter(URL url, String username, String password, Controller controller) {
        LOG.fine("Creating adapter: " + url);
        this.url = url;
        this.username = username;
        this.password = password;

        if (controller == null) {
            Controller.Builder cb = new Controller.Builder(url.toString());
            if (username != null && password != null) {
                cb.setCredentials(new UserPasswordCredentials(username, password));
            }
            // TODO: We poll the sensors of max. 25 devices per controller, 200 devices total!
            cb.setConnector(new Controller2HttpConnector(25, 200, 5000));
            controller = cb.build();
        }
        this.controller = controller;
        this.controllerState = new ControllerState(url.toString());

        connectCallback = new AsyncControllerCallback<ControllerConnectionStatus>() {

            @Override
            public void onFailure(ControllerResponseCode controllerResponseCode) {
                connectionInProgress = false;
                LOG.fine("Disconnected from the controller: " + url);
                LOG.fine("Disconnect reason: " + controllerResponseCode);
                if (!forceDisconnect) {
                    doConnection(true);
                }
            }

            @Override
            public void onSuccess(ControllerConnectionStatus controllerConnectionStatus) {
                connectionInProgress = false;
                LOG.fine("Connected to the controller: " + url);
                if (!forceDisconnect) {
                    // It is imperative that further initialization happens on a different thread: The
                    // current thread is the HTTP client's worker thread and it can't be blocked if
                    // you want to make further HTTP calls!
                    connectionScheduler.schedule(() -> {
                            getControllerState().initialize(Controller2Adapter.this.controller);
                        }, 0, TimeUnit.SECONDS
                    );
                }
            }
        };

        // Whenever a connection problem occurs the controller library will disconnect and call the connect
        // onFailure callback with a disconnected status; we then need to handle reconnection
        doConnection(false);
    }

    public int incrementReferenceCount() {
        return referenceCount.getAndIncrement();
    }

    public int decrementReferenceCount() {
        return referenceCount.decrementAndGet();
    }

    public URL getUrl() {
        return url;
    }

    public synchronized void close() {
        LOG.fine("Closing adapter: " + url);
        if (controller != null) {
            forceDisconnect = true;
            try {
                getControllerState().setInitialized(false);
                controller.disconnect();
            } catch (Exception ex) {
                LOG.warning("Ignoring error during adapter disconnect: " + ex);
            }
        }
    }

    public synchronized ControllerState getControllerState() {
        return controllerState;
    }

    protected void doConnection(boolean doDelay) {
        if (connectionInProgress || controller.isConnected()) {
            return;
        }

        connectionInProgress = true;

        // Push connection task onto separate thread to avoid blocking the caller when delaying reconnection
        connectionScheduler.schedule(
            () -> {
                LOG.fine("Connecting to controller: " + url);
                controller.connect(connectCallback);
            },
            doDelay ? RECONNECT_DELAY_SECONDS : 0L,
            TimeUnit.SECONDS
        );
    }
}
