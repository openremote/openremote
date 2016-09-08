/*
 * OpenRemote, the Home of the Digital Home.
 * Copyright 2008-2014, OpenRemote Inc.
 *
 * See the contributors.txt file in the distribution for a
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

import com.ning.http.client.*;
import org.openremote.console.controller.AsyncControllerDiscoveryCallback;
import org.openremote.console.controller.ControllerConnectionStatus;
import org.openremote.console.controller.auth.Credentials;
import org.openremote.console.controller.connector.ControllerDiscoveryResponseHandler;
import org.openremote.console.controller.connector.ControllerDiscoveryServer;
import org.openremote.console.controller.connector.HttpConnector;
import org.openremote.entities.controller.AsyncControllerCallback;
import org.openremote.entities.controller.ControllerResponseCode;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An asynchronous multi-threaded connector.
 */
public class Controller2HttpConnector extends HttpConnector {

    private static final Logger LOG = Logger.getLogger(Controller2HttpConnector.class.getName());

    final protected AsyncHttpClient client;
    protected Realm authRealm;

    final protected ExecutorService discoveryThreadPool = Executors.newFixedThreadPool(3);
    protected ControllerDiscoveryServer discoveryServer;

    private AsyncControllerCallback<ControllerConnectionStatus> connectCallback;

    public Controller2HttpConnector(int maxConnectionsPerHost, int maxConnectionsTotal, int timeoutMillis) {
        AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
        builder
            .setAllowPoolingConnections(true)
            .setMaxConnectionsPerHost(maxConnectionsPerHost)
            .setMaxConnections(maxConnectionsTotal)
            .setConnectTimeout(timeoutMillis)
            .setRequestTimeout(timeoutMillis);
        client = new AsyncHttpClient(builder.build());
    }

    public void startDiscovery(int tcpPort, Integer discoveryDuration, ControllerDiscoveryResponseHandler responseHandler) {
        if (discoveryServer != null)
            return;
        discoveryServer = new ControllerDiscoveryServer(tcpPort, discoveryDuration, responseHandler);
        discoveryThreadPool.submit(discoveryServer);
    }

    @Override
    public void stopDiscovery() {
        if (discoveryServer != null) {
            discoveryServer.cancel();
        }
        discoveryServer = null;
    }

    @Override
    public boolean isDiscoveryRunning() {
        return discoveryServer != null;
    }

    @Override
    protected void doRequest(URI uri, Map<String, String> headers, String content,
                             final ControllerCallback callback, Integer timeout) {
        if (callback.getCommand() == RestCommand.DISCOVERY) {
            int tcpPort = (Integer) callback.getData();
            startDiscovery(
                tcpPort,
                timeout,
                new ControllerDiscoveryResponseHandler() {
                    @Override
                    public void sendStartMessage() {
                        // Called when discovery is started
                        AsyncControllerDiscoveryCallback discoveryCallback = (AsyncControllerDiscoveryCallback) callback.getWrappedCallback();
                        discoveryCallback.onDiscoveryStarted();
                    }

                    @Override
                    public void sendFinishMessage() {
                        // This will be called when discovery is stopped
                        AsyncControllerDiscoveryCallback discoveryCallback = (AsyncControllerDiscoveryCallback) callback.getWrappedCallback();
                        discoveryCallback.onDiscoveryStopped();
                    }

                    @Override
                    public void sendSuccessMessage(byte[] responseBody) {
                        // Called each time a controller is discovered
                        handleResponse(callback, 0, null, responseBody);
                    }

                    @Override
                    public void sendFailureMessage(Exception ex) {
                        // Called when discovery cannot be started
                        AsyncControllerDiscoveryCallback discoveryCallback = (AsyncControllerDiscoveryCallback) callback.getWrappedCallback();
                        discoveryCallback.onStartDiscoveryFailed(ControllerResponseCode.UNKNOWN_ERROR);
                    }
                }
            );
            return;
        }

        if (callback.getCommand() == RestCommand.STOP_DISCOVERY) {
            stopDiscovery();
            return;
        }

        if (callback.getCommand() == RestCommand.DISCONNECT) {
            doDisconnect();
            return;
        }

        boolean doHead = false;
        boolean doGet = false;

        if (callback.getCommand() == RestCommand.GET_RESOURCE_DETAILS) {
            doHead = true;
        }

        if (callback.getCommand() == RestCommand.GET_XML) {
            doGet = true;
        }

        AsyncCompletionHandler<Void> handler = new AsyncCompletionHandler<Void>() {

            @Override
            public Void onCompleted(Response response) throws Exception {
                LOG.fine("Response status: " + response.getStatusCode());
                if (response.getStatusCode() >= 300) {
                    callback.getWrappedCallback().onFailure(
                        ControllerResponseCode.getResponseCode(response.getStatusCode())
                    );
                    return null;
                }

                if (callback.getCommand() == RestCommand.LOGOUT) {
                    authRealm = null;
                }

                if (callback.getCommand() == RestCommand.CONNECT) {
                    connectCallback = (AsyncControllerCallback<ControllerConnectionStatus>) callback.getWrappedCallback();
                }

                Map<String, String> responseHeadersMap = new HashMap<>();
                Set<String> headerNames = response.getHeaders().keySet();
                for (String headerName : headerNames) {
                    responseHeadersMap.put(headerName, response.getHeader(headerName));

                }

                byte[] responseBytes = response.getResponseBodyAsBytes();
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Response body: " + (new String(responseBytes, Charset.forName("utf-8"))));
                }

                Controller2HttpConnector.this.handleResponse(
                    callback,
                    response.getStatusCode(),
                    responseHeadersMap,
                    responseBytes
                );
                return null;
            }

            @Override
            public void onThrowable(Throwable t) {
                if (callback.getCommand() == RestCommand.DO_SENSOR_POLLING
                    && t.getCause() != null && t.getCause() instanceof TimeoutException) {
                    callback.getWrappedCallback().onSuccess(null);
                } else {
                    LOG.log(Level.INFO, "Unknown request error", t);
                    callback.getWrappedCallback().onFailure(ControllerResponseCode.UNKNOWN_ERROR);
                }
            }
        };

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Request body: " + content);
        }

        try {
            if (doHead) {
                LOG.fine("HEAD request: " + uri);
                client.prepareHead(uri.toString())
                    .setRealm(authRealm)
                    .setRequestTimeout(timeout > 0 ? timeout : client.getConfig().getRequestTimeout())
                    .execute(handler);
            } else if (doGet) {
                LOG.fine("GET request: " + uri);
                client.prepareGet(uri.toString())
                    .setHeaders(prepareHeaders(headers))
                    .setRealm(authRealm)
                    .setRequestTimeout(timeout > 0 ? timeout : client.getConfig().getRequestTimeout())
                    .execute(handler);
            } else {
                LOG.fine("POST request: " + uri);
                AsyncHttpClient.BoundRequestBuilder request = client.preparePost(uri.toString())
                    .setHeaders(prepareHeaders(headers))
                    .setHeader("Accept", "application/json")
                    .setRealm(authRealm)
                    .setRequestTimeout(timeout > 0 ? timeout : client.getConfig().getRequestTimeout());
                if (content != null) {
                    request.setHeader("Content-Type", "application/json")
                        .setBody(content.getBytes(Charset.forName("utf-8")));
                }
                request.execute(handler);
            }
        } catch (Exception ex) {
            LOG.log(Level.INFO, "Error creating request: " + uri, ex);
            callback.getWrappedCallback().onFailure(ControllerResponseCode.UNKNOWN_ERROR);
        }
    }

    @Override
    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
        if (credentials != null) {
            this.authRealm = new Realm.RealmBuilder()
                .setPrincipal(credentials.getUsername())
                .setPassword(credentials.getPassword())
                .build();
        } else {
            this.authRealm = null;
        }
    }

    @Override
    public void logout(AsyncControllerCallback<Boolean> callback) {
        credentials = null;
        if (controllerUrl != null) {
            doRequest(buildRequestUri(RestCommand.LOGOUT), null, null, new ControllerCallback(
                RestCommand.LOGOUT, callback), getTimeout());
        } else {
            callback.onSuccess(true);
        }
    }

    protected void doDisconnect() {
        client.close();
        connected = false;
        if (connectCallback != null) {
            connectCallback.onFailure(ControllerResponseCode.DISCONNECTED);
            connectCallback = null;
        }
    }

    protected FluentCaseInsensitiveStringsMap prepareHeaders(Map<String, String> headers) {
        FluentCaseInsensitiveStringsMap result = new FluentCaseInsensitiveStringsMap();
        if (headers != null) {
            headers.forEach((key, value) -> {
                result.put(key, Collections.singletonList(value));
            });
        }
        return result;
    }
}