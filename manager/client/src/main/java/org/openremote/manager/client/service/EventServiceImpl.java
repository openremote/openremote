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
import org.openremote.manager.client.event.*;
import org.openremote.manager.client.event.session.*;
import org.openremote.manager.client.util.Timeout;
import org.openremote.model.Constants;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.shared.*;

import java.util.*;
import java.util.logging.Logger;

public class EventServiceImpl implements EventService {

    private static final Logger LOG = Logger.getLogger(EventServiceImpl.class.getName());

    public static EventService create(SecurityService securityService,
                                      EventBus eventBus,
                                      SharedEventMapper sharedEventMapper,
                                      SharedEventArrayMapper sharedEventArrayMapper,
                                      EventSubscriptionMapper eventSubscriptionMapper,
                                      CancelEventSubscriptionMapper cancelEventSubscriptionMapper,
                                      UnauthorizedEventSubscriptionMapper unauthorizedEventSubscriptionMapper) {
        Location location = Browser.getWindow().getLocation();
        return new EventServiceImpl(
            securityService,
            eventBus,
            ("https:".equals(location.getProtocol()) ? "wss" : "ws")
                + "://" + location.getHostname() + ":" + location.getPort() + "/websocket/events",
            sharedEventMapper,
            sharedEventArrayMapper,
            eventSubscriptionMapper,
            cancelEventSubscriptionMapper,
            unauthorizedEventSubscriptionMapper
        );
    }

    // Give up after 10 minutes of reconnection attempts, waiting 5 seconds between each
    public static final int MAX_ATTEMPTS = 120;
    public static final int DELAY_MILLIS = 5000;

    // While not connected, buffer sent messages and finally flush the queue when connected again
    public static final int MAX_QUEUE_SIZE = 1000;

    final protected SecurityService securityService;
    final protected EventBus eventBus;
    final protected String serviceUrl;
    final protected SharedEventMapper sharedEventMapper;
    final protected SharedEventArrayMapper sharedEventArrayMapper;
    final protected EventSubscriptionMapper eventSubscriptionMapper;
    final protected CancelEventSubscriptionMapper cancelEventSubscriptionMapper;
    final protected UnauthorizedEventSubscriptionMapper unauthorizedEventSubscriptionMapper;

    final protected List<String> queue = new ArrayList<>();
    final protected Map<String, Integer> activeSubscriptions = new HashMap<>();

    protected WebSocket webSocket;
    protected int failureCount;

    public EventServiceImpl(SecurityService securityService,
                            EventBus eventBus,
                            String serviceUrl,
                            SharedEventMapper sharedEventMapper,
                            SharedEventArrayMapper sharedEventArrayMapper,
                            EventSubscriptionMapper eventSubscriptionMapper,
                            CancelEventSubscriptionMapper cancelEventSubscriptionMapper,
                            UnauthorizedEventSubscriptionMapper unauthorizedEventSubscriptionMapper) {
        this.securityService = securityService;
        this.eventBus = eventBus;
        this.serviceUrl = serviceUrl;
        this.sharedEventMapper = sharedEventMapper;
        this.sharedEventArrayMapper = sharedEventArrayMapper;
        this.eventSubscriptionMapper = eventSubscriptionMapper;
        this.cancelEventSubscriptionMapper = cancelEventSubscriptionMapper;
        this.unauthorizedEventSubscriptionMapper = unauthorizedEventSubscriptionMapper;

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
                LOG.fine("Flushing send data queue: " + queue.size());
                Iterator<String> it = queue.iterator();
                while (it.hasNext()) {
                    String next = it.next();
                    sendData(next);
                    it.remove();
                }
            }
        );

        this.eventBus.register(
            SessionClosedErrorEvent.class,
            event -> {
                if (event.getReason().code == 1001) {
                    LOG.fine("Session closed (probably idle timeout), reconnecting without delay...");
                    reconnect(0);
                } else {
                    LOG.fine("Session closed with error, incrementing failure count: " + failureCount);
                    failureCount++;
                    eventBus.dispatch(new ShowFailureEvent("Dropped server connection, will try a few more times to reach: " + serviceUrl, 3000));
                    if (failureCount < MAX_ATTEMPTS) {
                        reconnect();
                    } else {
                        String failureMessage = "Giving up connecting to service after " + failureCount + " failures: " + serviceUrl;
                        eventBus.dispatch(new ShowFailureEvent(failureMessage, ShowFailureEvent.DURABLE));
                        LOG.severe(failureMessage);
                    }
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
            Constants.ACCESS_TOKEN_LIFESPAN_SECONDS / 2,
            refreshed -> {
                // If it wasn't refreshed, it was still valid, in both cases we can continue
                LOG.fine("Connecting to event websocket: " + serviceUrl);

                String authenticatedServiceUrl = serviceUrl
                    + "?Auth-Realm=" + securityService.getAuthenticatedRealm()
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

    @Override
    public <E extends SharedEvent> void subscribe(Class<E> eventClass) {
        subscribe(eventClass, null);
    }

    @Override
    public <E extends SharedEvent> void subscribe(Class<E> eventClass, EventFilter<E> filter) {
        EventSubscription<E> subscription = new EventSubscription<>(eventClass, filter);
        final String key = subscription.getEventType();
        final String data = EventSubscription.MESSAGE_PREFIX + eventSubscriptionMapper.write(subscription);
        if (activeSubscriptions.containsKey(key)) {
            Browser.getWindow().clearInterval(activeSubscriptions.get(key));
        }
        LOG.fine("Subscribing on server: " + subscription);
        sendData(data);
        int interval = Browser.getWindow().setInterval(
            () -> {
                LOG.fine("Updating subscription on server: " + subscription);
                sendData(data);
            }, EventSubscription.RENEWAL_PERIOD_SECONDS / 2 * 1000
        );
        activeSubscriptions.put(key, interval);
    }

    @Override
    public <E extends SharedEvent> void unsubscribe(Class<E> eventClass) {
        CancelEventSubscription<E> cancellation = new CancelEventSubscription<>(eventClass);
        final String key = cancellation.getEventType();
        if (activeSubscriptions.containsKey(key)) {
            Browser.getWindow().clearInterval(activeSubscriptions.get(key));
            LOG.fine("Unsubscribing on server: " + cancellation);
            sendData(CancelEventSubscription.MESSAGE_PREFIX + cancelEventSubscriptionMapper.write(cancellation));
        }
    }

    @Override
    public void dispatch(SharedEvent sharedEvent) {
        LOG.fine("Dispatching: " + sharedEvent);
        sendData(SharedEvent.MESSAGE_PREFIX + sharedEventMapper.write(sharedEvent));
    }

    protected void sendData(String data) {
        if (webSocket != null && webSocket.getReadyState() == WebSocket.OPEN) {
            LOG.fine("Sending data on WebSocket: " + data);
            webSocket.send(data);
        } else {
            if (failureCount >= MAX_ATTEMPTS) {
                LOG.warning("WebSocket connection failed, discarding: " + data);
            } else if (queue.size() <= MAX_QUEUE_SIZE) {
                LOG.fine("WebSocket not connected, queuing: " + data);
                queue.add(data);
            } else {
                LOG.warning("WebSocket not connected, send queue full, discarding: " + data);
            }
        }
    }

    protected void onDataReceived(String data) {
        if (data == null || data.length() == 0)
            return;
        if (data.startsWith(UnauthorizedEventSubscription.MESSAGE_PREFIX)) {
            data = data.substring(UnauthorizedEventSubscription.MESSAGE_PREFIX.length());
            UnauthorizedEventSubscription failure = unauthorizedEventSubscriptionMapper.read(data);
            eventBus.dispatch(new SubscriptionFailureEvent(failure.getEventType()));
        } else if (data.startsWith(SharedEvent.MESSAGE_PREFIX)) {
            data = data.substring(SharedEvent.MESSAGE_PREFIX.length());
            if (data.startsWith("[")) {
                // Handle array of events
                SharedEvent[] events = sharedEventArrayMapper.read(data);
                for (SharedEvent event : events) {
                    eventBus.dispatch(event);
                }
            } else {
                SharedEvent event = sharedEventMapper.read(data);
                eventBus.dispatch(event);
            }
        }
    }

    protected void reconnect() {
        reconnect(DELAY_MILLIS);
    }

    protected void reconnect(int delayMillis) {
        LOG.fine("Session reconnection attempt '" + serviceUrl + "' with delay milliseconds: " + delayMillis);
        SessionConnectEvent event = new SessionConnectEvent();
        if (delayMillis <= 0) {
            eventBus.dispatch(event);
        } else {
            Timeout.debounce(event.getEventType(), () -> eventBus.dispatch(event), delayMillis);
        }
    }
}
