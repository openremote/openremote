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
package org.openremote.manager.client.service;

import elemental.client.Browser;
import elemental.events.CloseEvent;
import elemental.html.Location;
import elemental.html.WebSocket;
import org.openremote.manager.client.event.EventMapper;
import org.openremote.manager.client.event.MessageReceivedEvent;
import org.openremote.manager.client.event.ServerSendEvent;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.util.Timeout;
import org.openremote.manager.shared.event.Event;
import org.openremote.manager.shared.event.Message;
import org.openremote.manager.shared.event.session.*;
import org.openremote.manager.shared.event.ui.ShowFailureEvent;

import java.util.logging.Logger;

public class EventServiceImpl implements EventService {

    private static final Logger LOG = Logger.getLogger(EventServiceImpl.class.getName());

    public static EventService create(SecurityService securityService, EventBus eventBus, EventMapper eventMapper) {
        Location location = Browser.getWindow().getLocation();
        return new EventServiceImpl(
            securityService,
            eventBus,
            "ws://" + location.getHostname() + ":" + location.getPort() + "/websocket/events",
            eventMapper
        );
    }

    public static final int MAX_ATTEMPTS = 12;
    public static final int DELAY_MILLIS = 5000;

    final protected SecurityService securityService;
    final protected EventBus eventBus;
    final protected String serviceUrl;
    final protected EventMapper eventMapper;

    protected WebSocket webSocket;
    protected int failureCount;

    public EventServiceImpl(SecurityService securityService, EventBus eventBus, String serviceUrl, EventMapper eventMapper) {
        this.securityService = securityService;
        this.eventBus = eventBus;
        this.serviceUrl = serviceUrl;
        this.eventMapper = eventMapper;

        this.eventBus.register(
            ServerSendEvent.class,
            serverSendEvent -> sendData(this.eventMapper.write(serverSendEvent.getEvent()))
        );

        this.eventBus.register(
            SessionConnectEvent.class,
            event -> connect()
        );

        this.eventBus.register(
            SessionCloseEvent.class,
            event -> {
                if (webSocket != null) {
                    webSocket.close(1000, "SessionCloseEvent");
                }
            }
        );

        this.eventBus.register(
            SessionOpenedEvent.class,
            event -> {
                LOG.fine("Session opened successfully, resetting failure count...");
                failureCount = 0;
            }
        );

        this.eventBus.register(
            SessionClosedErrorEvent.class,
            event -> {
                eventBus.dispatch(new ShowFailureEvent("Dropped server connection, will try a few more times to reach: " + serviceUrl, 3000));
                LOG.fine("Session closed with error, incrementing failure count: " + failureCount);
                failureCount++;
                if (failureCount < MAX_ATTEMPTS) {
                    reconnect();
                } else {
                    String failureMessage = "Giving up connecting to service after " + failureCount + " failures: " + serviceUrl;
                    eventBus.dispatch(new ShowFailureEvent(failureMessage, ShowFailureEvent.DURABLE));
                    LOG.severe(failureMessage);
                }
            }
        );

    }

    @Override
    public void connect() {
        if (webSocket != null) {
            if (webSocket.getReadyState() != WebSocket.CLOSED) {
                // Close silently
                LOG.fine(
                    "New connection attempt to '" + serviceUrl + "', closing " +
                        "stale existing connection silently: " + webSocket.getUrl()
                );
                webSocket.setOnclose(null);
                webSocket.close();
            }
            webSocket = null;
        }

        securityService.updateToken(
            SecurityService.MIN_VALIDITY_SECONDS,
            refreshed -> {
                // If it wasn't refreshed, it was still valid, in both cases we can continue
                LOG.fine("Connecting to event websocket: " + serviceUrl);

                String authenticatedServiceUrl = serviceUrl
                    + "?realm=" + securityService.getRealm()
                    + "&Authorization=Bearer " + securityService.getToken();

                webSocket = Browser.getWindow().newWebSocket(authenticatedServiceUrl);
                webSocket.setOnopen(evt -> {
                    if (webSocket.getReadyState() == WebSocket.OPEN) {
                        LOG.fine("WebSocket open: " + webSocket.getUrl());
                        eventBus.dispatch(new SessionOpenedEvent());
                    }
                });
                webSocket.setOnclose(evt -> {
                    CloseEvent closeEvent = (CloseEvent) evt;
                    if (closeEvent.isWasClean() && closeEvent.getCode() == 1000) {
                        LOG.fine("WebSocket closed: " + webSocket.getUrl());
                        eventBus.dispatch(new SessionClosedCleanEvent());
                    } else {
                        LOG.fine("WebSocket '" + webSocket.getUrl() + "' closed with error: " + closeEvent.getCode());
                        eventBus.dispatch(new SessionClosedErrorEvent(
                            new SessionClosedErrorEvent.Error(closeEvent.getCode(), closeEvent.getReason())
                        ));
                    }
                });
                webSocket.setOnmessage(evt -> {
                    elemental.events.MessageEvent messageEvent = (elemental.events.MessageEvent) evt;
                    String data = messageEvent.getData().toString();
                    LOG.fine("Received data on WebSocket: " + data);
                    onDataReceived(data);
                });
            },
            () -> {
                LOG.fine("Failed to update access token for WebSocket");
                eventBus.dispatch(new SessionClosedErrorEvent(
                    new SessionClosedErrorEvent.Error(4000, "Failed to update access token")
                ));
            }
        );

    }

    protected void sendData(String data) {
        if (webSocket != null && webSocket.getReadyState() == WebSocket.OPEN) {
            LOG.fine("Sending data on WebSocket: " + data);
            webSocket.send(data);
        } else {
            LOG.fine("WebSocket not connected, discarding: " + data);
        }
    }

    protected void onDataReceived(String data) {
        Event event = eventMapper.read(data);
        if (event.getType().equals(Event.getType(Message.class))) {
            Message message = (Message) event;
            eventBus.dispatch(new MessageReceivedEvent(message));
        } else {
            eventBus.dispatch(event);
        }
    }

    protected void reconnect() {
        LOG.fine("Session reconnection attempt '" + serviceUrl + "' with delay milliseconds: " + DELAY_MILLIS);
        SessionConnectEvent event = new SessionConnectEvent();
        Timeout.debounce(event.getType(), () -> eventBus.dispatch(event), DELAY_MILLIS);
    }
}
