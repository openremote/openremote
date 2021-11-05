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
package org.openremote.manager.event;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.security.AuthContext;
import org.openremote.container.timer.TimerService;
import org.openremote.container.web.ConnectionConstants;
import org.openremote.manager.gateway.GatewayService;
import org.openremote.manager.mqtt.MqttBrokerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.event.shared.*;
import org.openremote.model.syslog.SyslogEvent;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.camel.builder.Builder.header;
import static org.apache.camel.builder.PredicateBuilder.or;
import static org.openremote.container.web.ConnectionConstants.SESSION;

/**
 * Receives and publishes messages, handles the client/server event bus.
 * <p>
 * Messages always start with a message discriminator in all uppercase letters, followed
 * by an optional JSON payload.
 * <p>
 * The following messages can be sent by a client:
 * <dl>
 * <dt><code>SUBSCRIBE:{...}</code><dt>
 * <dd><p>
 * The payload is a serialized representation of {@link EventSubscription} with an optional
 * {@link org.openremote.model.event.shared.EventFilter}. Clients can subscribe to receive {@link SharedEvent}s
 * when they are published on the server. Subscriptions are handled by {@link SharedEvent#getEventType}.
 * </p></dd>
 * <dt><code>UNSUBSCRIBE:{...}</code></dt>
 * <dd><p>
 * The payload is a serialized representation of {@link CancelEventSubscription}. If a client
 * does not want to wait for expiration of its subscriptions, it can cancel a subscription.
 * </p></dd>
 * <dt><code>EVENT:{...}</code></dt>
 * <dd><p>
 * The payload is a serialized representation of a subtype of {@link SharedEvent}. If the server
 * does not recognize the event, it is silently ignored.
 * </p></dd>
 * </dl>
 * <p>
 * The following messages can be published/returned by the server:
 * <dl>
 * <dt><code>UNAUTHORIZED:{...}</code></dt>
 * <dd><p>
 * The payload is a serialized representation of {@link UnauthorizedEventSubscription}.
 * </p></dd>
 * <dt><code>EVENT:{...}</code></dt>
 * <dd><p>
 * The payload is a serialized representation of a subtype of {@link SharedEvent}.
 * </p></dd>
 * <dt><code>EVENT:[...]</code></dt>
 * <dd><p>
 * The payload is an array of {@link SharedEvent}s.
 * </p></dd>
 * </dl>
 */
public class ClientEventService implements ContainerService {

    protected static class SessionInfo {
        String connectionType;
        Runnable closeRunnable;

        public SessionInfo(String connectionType, Runnable closeRunnable) {
            this.connectionType = connectionType;
            this.closeRunnable = closeRunnable;
        }
    }

    public static final int PRIORITY = ManagerWebService.PRIORITY - 200;
    public static final String HEADER_ACCESS_RESTRICTED = ClientEventService.class.getName() + ".HEADER_ACCESS_RESTRICTED";
    public static final String HEADER_CONNECTION_TYPE = ClientEventService.class.getName() + ".HEADER_CONNECTION_TYPE";
    public static final String HEADER_CONNECTION_TYPE_WEBSOCKET = ClientEventService.class.getName() + ".HEADER_CONNECTION_TYPE_WEBSOCKET";
    public static final String HEADER_CONNECTION_TYPE_MQTT = ClientEventService.class.getName() + ".HEADER_CONNECTION_TYPE_MQTT";
    public static final String HEADER_REQUEST_RESPONSE_MESSAGE_ID = ClientEventService.class.getName() + ".HEADER_REQUEST_RESPONSE_MESSAGE_ID";
    private static final Logger LOG = Logger.getLogger(ClientEventService.class.getName());
    public static final String WEBSOCKET_EVENTS = "events";
    protected static final String INTERNAL_SESSION_KEY = "ClientEventServiceInternal";

    // TODO: Some of these options should be configurable depending on expected load etc.
    public static final String CLIENT_EVENT_TOPIC = "seda://ClientEventTopic?multipleConsumers=true&concurrentConsumers=1&waitForTaskToComplete=NEVER&purgeWhenStopping=true&discardIfNoConsumers=true&limitConcurrentConsumers=false&size=1000";

    public static final String CLIENT_EVENT_QUEUE = "seda://ClientEventQueue?multipleConsumers=false&waitForTaskToComplete=NEVER&purgeWhenStopping=true&discardIfNoConsumers=true&size=25000";

    final protected Collection<EventSubscriptionAuthorizer> eventSubscriptionAuthorizers = new CopyOnWriteArraySet<>();
    final protected Collection<Consumer<Exchange>> exchangeInterceptors = new CopyOnWriteArraySet<>();
    protected Map<String, SessionInfo> sessionKeyInfoMap = new HashMap<>();
    protected TimerService timerService;
    protected MessageBrokerService messageBrokerService;
    protected ManagerIdentityService identityService;
    protected EventSubscriptions eventSubscriptions;
    protected GatewayService gatewayService;
    protected Set<EventSubscription<?>> pendingInternalSubscriptions;
    protected boolean stopped;

    /**
     * Method to stop further processing of the exchange
     */
    public static void stopMessage(Exchange exchange) {
        exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);
    }

    public static boolean isInbound(Exchange exchange) {
        return header(HEADER_CONNECTION_TYPE).isNotNull().matches(exchange);
    }

    public static String getSessionKey(Exchange exchange) {
        return exchange.getIn().getHeader(ConnectionConstants.SESSION_KEY, String.class);
    }

    public static String getClientId(Exchange exchange) {
        AuthContext authContext = exchange.getIn().getHeader(Constants.AUTH_CONTEXT, AuthContext.class);
        if(authContext != null) {
            return authContext.getClientId();
        }
        return null;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        timerService = container.getService(TimerService.class);
        messageBrokerService = container.getService(MessageBrokerService.class);
        identityService = container.getService(ManagerIdentityService.class);
        gatewayService = container.getService(GatewayService.class);

        eventSubscriptions = new EventSubscriptions(
            container.getService(TimerService.class)
        );

        messageBrokerService.getContext().getTypeConverterRegistry().addTypeConverters(
            new EventTypeConverters()
        );

        // TODO: Remove prefix and just use event type then use a subscription wrapper to pass subscription ID around
        messageBrokerService.getContext().addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                from("websocket://" + WEBSOCKET_EVENTS)
                    .routeId("FromClientWebsocketEvents")
                    .process(exchange -> exchange.getIn().setHeader(HEADER_CONNECTION_TYPE, HEADER_CONNECTION_TYPE_WEBSOCKET))
                    .to(ClientEventService.CLIENT_EVENT_QUEUE)
                    .end();

                from(ClientEventService.CLIENT_EVENT_QUEUE)
                    .routeId("ClientEvents")
                    .choice()
                    .when(header(ConnectionConstants.SESSION_OPEN))
                        .process(exchange -> {
                            String sessionKey = getSessionKey(exchange);
                            sessionKeyInfoMap.put(sessionKey, createSessionInfo(sessionKey, exchange));
                            passToInterceptors(exchange);
                        })
                        .stop()
                    .when(or(
                        header(ConnectionConstants.SESSION_CLOSE),
                        header(ConnectionConstants.SESSION_CLOSE_ERROR)
                    ))
                        .process(exchange -> {
                            String sessionKey = getSessionKey(exchange);
                            sessionKeyInfoMap.remove(sessionKey);
                            eventSubscriptions.cancelAll(sessionKey);
                            passToInterceptors(exchange);
                        })
                        .stop()
                    .end()
                    .process(exchange -> {

                        // Do basic formatting of exchange
                        EventRequestResponseWrapper<?> requestResponse = null;
                        if (exchange.getIn().getBody() instanceof EventRequestResponseWrapper) {
                            requestResponse = exchange.getIn().getBody(EventRequestResponseWrapper.class);
                        } else if (exchange.getIn().getBody() instanceof String && exchange.getIn().getBody(String.class).startsWith(EventRequestResponseWrapper.MESSAGE_PREFIX)) {
                            requestResponse = exchange.getIn().getBody(EventRequestResponseWrapper.class);
                        }
                        if (requestResponse != null) {
                            SharedEvent event = requestResponse.getEvent();
                            exchange.getIn().setHeader(HEADER_REQUEST_RESPONSE_MESSAGE_ID, requestResponse.getMessageId());
                            exchange.getIn().setBody(event);
                        }

                        if (exchange.getIn().getBody() instanceof String) {
                            String bodyStr = exchange.getIn().getBody(String.class);
                            if (bodyStr.startsWith(EventSubscription.SUBSCRIBE_MESSAGE_PREFIX)) {
                                exchange.getIn().setBody(exchange.getIn().getBody(EventSubscription.class));
                            } else if (bodyStr.startsWith(CancelEventSubscription.MESSAGE_PREFIX)) {
                                exchange.getIn().setBody(exchange.getIn().getBody(CancelEventSubscription.class));
                            } else if (bodyStr.startsWith(SharedEvent.MESSAGE_PREFIX)) {
                                exchange.getIn().setBody(exchange.getIn().getBody(SharedEvent.class));
                            }
                        }

                        if (exchange.getIn().getBody() instanceof SharedEvent) {
                            SharedEvent event = exchange.getIn().getBody(SharedEvent.class);
                            // If there is no timestamp in event, set to system time
                            if (event.getTimestamp() <= 0) {
                                event.setTimestamp(timerService.getCurrentTimeMillis());
                            }
                        }
                    })
                    .process(exchange -> passToInterceptors(exchange))
                    .choice()
                    .when(body().isInstanceOf(EventSubscription.class))
                        .process(exchange -> {
                            String sessionKey = getSessionKey(exchange);
                            EventSubscription<?> subscription = exchange.getIn().getBody(EventSubscription.class);
                            AuthContext authContext = exchange.getIn().getHeader(Constants.AUTH_CONTEXT, AuthContext.class);
                            if (authorizeEventSubscription(authContext, subscription)) {
                                boolean restrictedUser = identityService.getIdentityProvider().isRestrictedUser(authContext);
                                eventSubscriptions.createOrUpdate(sessionKey, restrictedUser, subscription);
                                subscription.setSubscribed(true);
                                sendToSession(sessionKey, subscription);
                            } else {
                                LOG.warning("Unauthorized subscription from '"
                                        + authContext.getUsername() + "' in realm '" + authContext.getAuthenticatedRealm()
                                        + "': " + subscription
                                );
                                sendToSession(sessionKey, new UnauthorizedEventSubscription<>(subscription));
                            }
                        })
                        .stop()
                    .when(body().isInstanceOf(CancelEventSubscription.class))
                        .process(exchange -> {
                            String sessionKey = getSessionKey(exchange);
                            eventSubscriptions.cancel(sessionKey, exchange.getIn().getBody(CancelEventSubscription.class));
                        })
                        .stop()
                    .when(body().isInstanceOf(SharedEvent.class))
                        .choice()
                            .when(header(HEADER_CONNECTION_TYPE).isNotNull()) // Inbound messages from clients
                                .to(ClientEventService.CLIENT_EVENT_TOPIC)
                                .stop()
                            .when(header(HEADER_CONNECTION_TYPE).isNull()) // Outbound message to clients
                                .split(method(eventSubscriptions, "splitForSubscribers"))
                                .process(exchange -> {
                                    String sessionKey = getSessionKey(exchange);
                                    sendToSession(sessionKey, exchange.getIn().getBody());
                                })
                                .stop()
                        .endChoice()
                    .otherwise()
                        .process(exchange -> LOG.info("Unsupported message body: " + exchange.getIn().getBody()))
                    .end();
            }
        });

        // Add pending internal subscriptions
        if (!pendingInternalSubscriptions.isEmpty()) {
            pendingInternalSubscriptions.forEach(subscription ->
                eventSubscriptions.createOrUpdate(INTERNAL_SESSION_KEY, false, subscription));
        }

        pendingInternalSubscriptions = null;
    }

    /**
     * Make an internal subscription to {@link SharedEvent}s sent on the client event bus
     */
    public <T extends SharedEvent> String addInternalSubscription(Class<T> eventClass, EventFilter<T> filter, Consumer<T> eventConsumer) {
        return addInternalSubscription(Integer.toString(Objects.hash(eventClass, filter, eventConsumer)), eventClass, filter, eventConsumer);
    }
    public <T extends SharedEvent> String addInternalSubscription(String subscriptionId, Class<T> eventClass, EventFilter<T> filter, Consumer<T> eventConsumer) {
        EventSubscription<T> subscription = new EventSubscription<T>(eventClass, filter, subscriptionId, eventConsumer);
        if (eventSubscriptions == null) {
            // Not initialised yet
            if (pendingInternalSubscriptions == null) {
                pendingInternalSubscriptions = new HashSet<>();
            }
            pendingInternalSubscriptions.add(subscription);
        } else {
            eventSubscriptions.createOrUpdate(INTERNAL_SESSION_KEY, false, subscription);
        }
        return subscriptionId;
    }

    public void cancelInternalSubscription(String subscriptionId) {
        eventSubscriptions.cancel(INTERNAL_SESSION_KEY, new CancelEventSubscription(subscriptionId));
    }

    @Override
    public void start(Container container) {
        stopped = false;
    }

    @Override
    public void stop(Container container) {
        stopped = true;
    }

    public void addExchangeInterceptor(Consumer<Exchange> exchangeInterceptor) throws RuntimeException {
        exchangeInterceptors.add(exchangeInterceptor);
    }

    public void removeExchangeInterceptor(Consumer<Exchange> exchangeInterceptor) {
        exchangeInterceptors.remove(exchangeInterceptor);
    }

    public void addSubscriptionAuthorizer(EventSubscriptionAuthorizer authorizer) {
        this.eventSubscriptionAuthorizers.add(authorizer);
    }

    public boolean authorizeEventSubscription(AuthContext authContext, EventSubscription<?> subscription) {
        return eventSubscriptionAuthorizers.stream()
                .anyMatch(authorizer -> authorizer.apply(authContext, subscription));
    }

    public <T extends SharedEvent> void publishEvent(T event) {
        publishEvent(true, event);
    }

    /**
     * @param accessRestricted <code>true</code> if this event can be received by restricted user sessions.
     */
    public <T extends SharedEvent> void publishEvent(boolean accessRestricted, T event) {
        // Only publish if service is not stopped
        if (stopped) {
            return;
        }

        if (messageBrokerService != null && messageBrokerService.getProducerTemplate() != null) {
            // Don't log that we are publishing a syslog event,
            if (!(event instanceof SyslogEvent)) {
                LOG.finer("Publishing: " + event);
            }
            messageBrokerService.getProducerTemplate()
                .sendBodyAndHeader(CLIENT_EVENT_QUEUE, event, HEADER_ACCESS_RESTRICTED, accessRestricted);
        }
    }

    public void sendToSession(String sessionKey, Object data) {
        if (messageBrokerService != null && messageBrokerService.getProducerTemplate() != null) {
            LOG.finer("Sending to session '" + sessionKey + "': " + data);
            SessionInfo sessionInfo = sessionKeyInfoMap.get(sessionKey);
            if (sessionInfo == null) {
                LOG.info("Cannot send to requested session it doesn't exist or is disconnected");
                return;
            }
            if (sessionInfo.connectionType.equals(HEADER_CONNECTION_TYPE_WEBSOCKET)) {
                messageBrokerService.getProducerTemplate().sendBodyAndHeader(
                        "websocket://" + WEBSOCKET_EVENTS,
                        data,
                        ConnectionConstants.SESSION_KEY, sessionKey
                );
            } else if (sessionInfo.connectionType.equals(HEADER_CONNECTION_TYPE_MQTT)) {
                messageBrokerService.getProducerTemplate().sendBodyAndHeader(
                        MqttBrokerService.MQTT_CLIENT_QUEUE,
                        data,
                        ConnectionConstants.SESSION_KEY, sessionKey
                );
            }
        }
    }

    public void closeSession(String sessionKey) {
        SessionInfo sessionInfo = sessionKeyInfoMap.get(sessionKey);

        if (sessionInfo == null || sessionInfo.closeRunnable == null) {
            return;
        }

        LOG.fine("Closing session: " + sessionKey);
        sessionInfo.closeRunnable.run();
    }

    protected void passToInterceptors(Exchange exchange) {
        // Pass to each interceptor and stop if any interceptor marks the exchange as stop routing
        if (exchangeInterceptors.stream().anyMatch(interceptor -> {
            interceptor.accept(exchange);
            boolean stop = exchange.getProperty(Exchange.ROUTE_STOP, false, Boolean.class);
            if (stop) {
                LOG.finer("Client event interceptor marked exchange as `stop routing`");
            }
            return stop;
        }));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }

    protected static SessionInfo createSessionInfo(String sessionKey, Exchange exchange) {

        String connectionType = (String) exchange.getIn().getHeader(HEADER_CONNECTION_TYPE);
        Runnable closeRunnable = null;

        if (HEADER_CONNECTION_TYPE_WEBSOCKET.equals(connectionType)) {
            Session session = exchange.getIn().getHeader(SESSION, Session.class);
            closeRunnable = () -> {
                try {
                    session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, ""));
                } catch (Exception e) {
                    LOG.log(Level.INFO, "Failed to close client session: " + sessionKey);
                }
            };
        } else if (HEADER_CONNECTION_TYPE_MQTT.equals(connectionType)) {
            closeRunnable = exchange.getIn().getHeader(SESSION, Runnable.class);
        }

        return new SessionInfo(connectionType, closeRunnable);
    }
}
