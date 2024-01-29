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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.undertow.UndertowComponent;
import org.apache.camel.component.undertow.UndertowConstants;
import org.apache.camel.component.undertow.UndertowHostKey;
import org.keycloak.KeycloakPrincipal;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.security.AuthContext;
import org.openremote.container.security.basic.BasicAuthContext;
import org.openremote.container.security.keycloak.AccessTokenAuthContext;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.gateway.GatewayService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.event.Event;
import org.openremote.model.event.shared.*;
import org.openremote.model.syslog.SyslogEvent;

import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static java.lang.System.Logger.Level.*;
import static org.apache.camel.builder.PredicateBuilder.or;
import static org.openremote.manager.asset.AssetProcessingService.ATTRIBUTE_EVENT_ROUTER_QUEUE;
import static org.openremote.manager.asset.AssetProcessingService.ATTRIBUTE_EVENT_ROUTE_CONFIG_ID;
import static org.openremote.manager.system.HealthService.OR_CAMEL_ROUTE_METRIC_PREFIX;
import static org.openremote.model.Constants.*;

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
public class ClientEventService extends RouteBuilder implements ContainerService {

    protected static class SessionInfo {
        String connectionType;
        Runnable closeRunnable;

        public SessionInfo(String connectionType, Runnable closeRunnable) {
            this.connectionType = connectionType;
            this.closeRunnable = closeRunnable;
        }
    }

    public static final int PRIORITY = ManagerWebService.PRIORITY - 200;
    public static final String HEADER_CONNECTION_TYPE = ClientEventService.class.getName() + ".HEADER_CONNECTION_TYPE";
    public static final String HEADER_CONNECTION_TYPE_WEBSOCKET = "websocket";
    public static final String HEADER_CONNECTION_TYPE_MQTT = "mqtt";
    public static final String HEADER_REQUEST_RESPONSE_MESSAGE_ID = ClientEventService.class.getName() + ".HEADER_REQUEST_RESPONSE_MESSAGE_ID";
    public static final String WEBSOCKET_URI = "undertow://ws://0.0.0.0/websocket/events?fireWebSocketChannelEvents=true&sendTimeout=15000"; // Host is not used as existing undertow instance is utilised
    public static final String CLIENT_INBOUND_QUEUE = "seda://ClientInboundQueue?multipleConsumers=true&concurrentConsumers=2&waitForTaskToComplete=IfReplyExpected&purgeWhenStopping=true&discardIfNoConsumers=true&limitConcurrentConsumers=false&size=1000";
    public static final String CLIENT_OUTBOUND_QUEUE = "seda://ClientOutboundQueue?multipleConsumers=true&concurrentConsumers=2&purgeWhenStopping=true&discardIfNoConsumers=true&limitConcurrentConsumers=false&size=1000";
    protected static final System.Logger LOG = System.getLogger(ClientEventService.class.getName());
    protected static final String INTERNAL_SESSION_KEY = "ClientEventServiceInternal";
    protected static final String PUBLISH_QUEUE = "seda://ClientPublishQueue?multipleConsumers=false&purgeWhenStopping=true&discardIfNoConsumers=true&size=1000";

    final protected Collection<EventSubscriptionAuthorizer> eventSubscriptionAuthorizers = new CopyOnWriteArraySet<>();
    final protected Collection<EventAuthorizer> eventAuthorizers = new CopyOnWriteArraySet<>();
    final protected Collection<Consumer<Exchange>> exchangeInterceptors = new CopyOnWriteArraySet<>();
    protected ConcurrentMap<String, SessionInfo> sessionKeyInfoMap = new ConcurrentHashMap<>();
    protected TimerService timerService;
    protected ExecutorService executorService;
    protected MessageBrokerService messageBrokerService;
    protected ManagerIdentityService identityService;
    protected EventSubscriptions eventSubscriptions;
    protected GatewayService gatewayService;
    protected Set<EventSubscription<?>> pendingInternalSubscriptions;
    protected boolean started;
    protected Counter queueFullCounter;

    public static String getSessionKey(Exchange exchange) {
        return exchange.getIn().getHeader(SESSION_KEY, String.class);
    }

    public static String getClientId(Exchange exchange) {
        AuthContext authContext = exchange.getIn().getHeader(AUTH_CONTEXT, AuthContext.class);
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
        executorService = container.getExecutorService();
        MeterRegistry meterRegistry = container.getMeterRegistry();

        if (meterRegistry != null) {
            queueFullCounter = meterRegistry.counter(OR_CAMEL_ROUTE_METRIC_PREFIX + "_failed_queue_full", Tags.empty());
        }

        eventSubscriptions = new EventSubscriptions(
            container.getService(TimerService.class)
        );

        UndertowComponent undertowWebsocketComponent = new UndertowComponent(messageBrokerService.getContext()) {
            @Override
            protected org.apache.camel.component.undertow.UndertowHost createUndertowHost(UndertowHostKey key) {
                return new UndertowHost(container, key, getHostOptions());
            }
        };
        messageBrokerService.getContext().addComponent("undertow", undertowWebsocketComponent);


        messageBrokerService.getContext().getTypeConverterRegistry().addTypeConverters(
            new EventTypeConverters()
        );

        messageBrokerService.getContext().addRoutes(this);

        // Add pending internal subscriptions
        if (pendingInternalSubscriptions != null && !pendingInternalSubscriptions.isEmpty()) {
            pendingInternalSubscriptions.forEach(subscription ->
                doInternalSubscription(INTERNAL_SESSION_KEY, subscription));
        }

        pendingInternalSubscriptions = null;
    }

    // TODO: Remove prefix and just use event type then use a subscription wrapper to pass subscription ID around
    @Override
    public void configure() throws Exception {

        // Route for deserializing incoming websocket messages and normalising them for the INBOUND_CLIENT_QUEUE
        from(WEBSOCKET_URI)
            .routeId("ClientInbound-Websocket")
            .routeConfigurationId(ATTRIBUTE_EVENT_ROUTE_CONFIG_ID)
            .process(exchange -> {
                String connectionKey = exchange.getIn().getHeader(UndertowConstants.CONNECTION_KEY, String.class);
                exchange.getIn().setHeader(HEADER_CONNECTION_TYPE, HEADER_CONNECTION_TYPE_WEBSOCKET);
                exchange.getIn().setHeader(SESSION_KEY, connectionKey);
            })
            .choice()
            .when(header(UndertowConstants.EVENT_TYPE))
            .process(exchange -> {
                UndertowConstants.EventType eventType = exchange.getIn().getHeader(UndertowConstants.EVENT_TYPE_ENUM, UndertowConstants.EventType.class);
                WebSocketChannel webSocketChannel = exchange.getIn().getHeader(UndertowConstants.CHANNEL, WebSocketChannel.class);

                switch (eventType) {
                    case ONOPEN -> {
                        WebSocketHttpExchange httpExchange = exchange.getIn().getHeader(UndertowConstants.EXCHANGE, WebSocketHttpExchange.class);
                        String realm = httpExchange.getRequestHeader(Constants.REALM_PARAM_NAME);
                        Principal principal = httpExchange.getUserPrincipal();
                        AuthContext authContext = null;

                        if (principal instanceof KeycloakPrincipal<?> keycloakPrincipal) {
                            authContext = new AccessTokenAuthContext(
                                keycloakPrincipal.getKeycloakSecurityContext().getRealm(),
                                keycloakPrincipal.getKeycloakSecurityContext().getToken()
                            );
                        } else if (principal instanceof BasicAuthContext) {
                            authContext = (BasicAuthContext) principal;
                        } else if (principal != null) {
                            LOG.log(INFO, "Unsupported user principal type: " + principal);
                        }

                        // Push auth and realm into channel for future use
                        webSocketChannel.setAttribute(Constants.AUTH_CONTEXT, authContext);
                        webSocketChannel.setAttribute(Constants.REALM_PARAM_NAME, realm);

                        exchange.getIn().setHeader(Constants.AUTH_CONTEXT, authContext);
                        exchange.getIn().setHeader(Constants.REALM_PARAM_NAME, realm);
                        exchange.getIn().setHeader(SESSION_TERMINATOR, getWebsocketSessionTerminator(webSocketChannel));
                        exchange.getIn().setHeader(SESSION_OPEN, true);
                        LOG.log(DEBUG, "Client connection created: " + webSocketChannel.getSourceAddress());
                    }
                    case ONCLOSE -> {
                        AuthContext authContext = (AuthContext)webSocketChannel.getAttribute(Constants.AUTH_CONTEXT);
                        String realm = (String)webSocketChannel.getAttribute(Constants.REALM_PARAM_NAME);

                        exchange.getIn().setHeader(Constants.AUTH_CONTEXT, authContext);
                        exchange.getIn().setHeader(Constants.REALM_PARAM_NAME, realm);

                        // Use protocol agnostic session open header
                        exchange.getIn().setHeader(SESSION_CLOSE, true);
                        LOG.log(DEBUG, "Client connection closed: " + webSocketChannel.getSourceAddress());
                    }
                    case ONERROR -> {
                        AuthContext authContext = (AuthContext)webSocketChannel.getAttribute(Constants.AUTH_CONTEXT);
                        String realm = (String)webSocketChannel.getAttribute(Constants.REALM_PARAM_NAME);

                        exchange.getIn().setHeader(Constants.AUTH_CONTEXT, authContext);
                        exchange.getIn().setHeader(Constants.REALM_PARAM_NAME, realm);

                        // Use protocol agnostic session open header
                        exchange.getIn().setHeader(SESSION_CLOSE_ERROR, true);
                        LOG.log(DEBUG, "Client connection error: " + webSocketChannel.getSourceAddress());
                        try {
                            webSocketChannel.close();
                        } catch (Exception ignored) {}
                    }
                }
            })
            .to(CLIENT_INBOUND_QUEUE)
            .stop()
            .endChoice()
            .end()
            .process(exchange -> {

                WebSocketChannel webSocketChannel = exchange.getIn().getHeader(UndertowConstants.CHANNEL, WebSocketChannel.class);
                AuthContext authContext = (AuthContext)webSocketChannel.getAttribute(Constants.AUTH_CONTEXT);
                String realm = (String)webSocketChannel.getAttribute(Constants.REALM_PARAM_NAME);

                exchange.getIn().setHeader(Constants.AUTH_CONTEXT, authContext);
                exchange.getIn().setHeader(Constants.REALM_PARAM_NAME, realm);

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

                // Perform authorisation
                if (exchange.getIn().getBody() instanceof SharedEvent) {
                    SharedEvent event = exchange.getIn().getBody(SharedEvent.class);

                    if (!authorizeEventWrite(realm, authContext, event)) {
                        exchange.setRouteStop(true);
                    }
                } else if (exchange.getIn().getBody() instanceof EventSubscription<?>) {
                    EventSubscription<?> subscription = exchange.getIn().getBody(EventSubscription.class);
                    String sessionKey = getSessionKey(exchange);

                    if (!authorizeEventSubscription(realm, authContext, subscription)) {
                        sendToSession(sessionKey, new UnauthorizedEventSubscription<>(subscription));
                        exchange.setRouteStop(true);
                    }
                }
            })
            .to(CLIENT_INBOUND_QUEUE)
            .end();

        from(CLIENT_INBOUND_QUEUE)
            .routeId("ClientInbound-EventProcessor")
            .routeConfigurationId(ATTRIBUTE_EVENT_ROUTE_CONFIG_ID)
            .process(exchange -> {
                // Process session open before passing to any interceptors
                Boolean isSessionOpen = exchange.getIn().getHeader(SESSION_OPEN, Boolean.class);
                if (isSessionOpen != null) {
                    String sessionKey = getSessionKey(exchange);
                    LOG.log(TRACE, "Adding session: " + sessionKey);
                    sessionKeyInfoMap.put(sessionKey, createSessionInfo(sessionKey, exchange));
                }
                passToInterceptors(exchange);
            })
            .choice()
            .when(body().isInstanceOf(AttributeEvent.class))
            .process(exchange -> {
                AttributeEvent attributeEvent = exchange.getIn().getBody(AttributeEvent.class);
                // Set timestamp as early as possible if not set
                if (attributeEvent.getTimestamp() <= 0) {
                    attributeEvent.setTimestamp(timerService.getCurrentTimeMillis());
                }
            })
            .to(ATTRIBUTE_EVENT_ROUTER_QUEUE)
            .stop()
            .when(or(
                header(SESSION_CLOSE),
                header(SESSION_CLOSE_ERROR)
            ))
            .process(exchange -> {
                String sessionKey = getSessionKey(exchange);
                LOG.log(TRACE, "Removing session: " + sessionKey);
                sessionKeyInfoMap.remove(sessionKey);
                eventSubscriptions.cancelAll(sessionKey);
            })
            .stop()
            .end()
            .choice()
            .when(body().isInstanceOf(EventSubscription.class))
            .process(exchange -> {
                String sessionKey = getSessionKey(exchange);
                EventSubscription<?> subscription = exchange.getIn().getBody(EventSubscription.class);
                LOG.log(TRACE, () -> "Adding subscription for session '" + sessionKey + "': " + subscription);
                eventSubscriptions.createOrUpdate(sessionKey, subscription);
                subscription.setSubscribed(true);
                sendToSession(sessionKey, subscription);
            })
            .stop()
            .when(body().isInstanceOf(CancelEventSubscription.class))
            .process(exchange -> {
                String sessionKey = getSessionKey(exchange);
                CancelEventSubscription cancelEventSubscription = exchange.getIn().getBody(CancelEventSubscription.class);
                eventSubscriptions.cancel(sessionKey, cancelEventSubscription);
                LOG.log(TRACE, () -> "Cancelling subscription for session '" + sessionKey + "': " + cancelEventSubscription);
            })
            .stop()
            .end();

        // Split publish messages for individual subscribers
        from(PUBLISH_QUEUE)
            .routeId("ClientOutbound-Splitter")
            .split(method(eventSubscriptions, "splitForSubscribers"))
            .process(exchange -> {
                String sessionKey = getSessionKey(exchange);
                SessionInfo sessionInfo = sessionKeyInfoMap.get(sessionKey);
                if (sessionInfo == null) {
                    LOG.log(INFO, "Cannot send to requested session it doesn't exist or is disconnected: " + sessionKey);
                    return;
                }
                exchange.getIn().setHeader(HEADER_CONNECTION_TYPE, sessionInfo.connectionType);
            })
            .to(CLIENT_OUTBOUND_QUEUE);

        // Route messages destined for websocket clients
        from(CLIENT_OUTBOUND_QUEUE)
            .routeId("ClientOutbound-Websocket")
            .filter(header(HEADER_CONNECTION_TYPE).isEqualTo(HEADER_CONNECTION_TYPE_WEBSOCKET))
            .process(exchange -> {
                String sessionKey = exchange.getIn().getHeader(SESSION_KEY, String.class);
                messageBrokerService.getFluentProducerTemplate()
                    .withBody(exchange.getIn().getBody())
                    .withHeader(UndertowConstants.CONNECTION_KEY, sessionKey)
                    .to(WEBSOCKET_URI)
                    .asyncSend();
            });
    }

    /**
     * Make an internal subscription to {@link SharedEvent}s sent on the client event bus
     */
    public <T extends SharedEvent> String addInternalSubscription(Class<T> eventClass, EventFilter<T> filter, Consumer<T> eventConsumer) throws IllegalStateException {
        return addInternalSubscription(Integer.toString(Objects.hash(eventClass, filter, eventConsumer)), eventClass, filter, eventConsumer);
    }
    public <T extends SharedEvent> String addInternalSubscription(String subscriptionId, Class<T> eventClass, EventFilter<T> filter, Consumer<T> eventConsumer) throws IllegalStateException {

        EventSubscription<T> subscription = new EventSubscription<T>(eventClass, filter, subscriptionId, eventConsumer);
        if (eventSubscriptions == null) {
            // Not initialised yet
            if (pendingInternalSubscriptions == null) {
                pendingInternalSubscriptions = new HashSet<>();
            }
            pendingInternalSubscriptions.add(subscription);
        } else {
            doInternalSubscription(INTERNAL_SESSION_KEY, subscription);
        }
        return subscriptionId;
    }

    protected void doInternalSubscription(String sessionKey, EventSubscription<?> eventSubscription) throws IllegalStateException {
        if (!authorizeEventSubscription(null, null, eventSubscription)) {
            LOG.log(WARNING, () -> "Internal subscription failed: " + eventSubscription);
            throw new IllegalStateException("Internal subscription failed");
        }
        eventSubscriptions.createOrUpdate(sessionKey, eventSubscription);
    }

    public void cancelInternalSubscription(String sessionId) {
        eventSubscriptions.cancel(INTERNAL_SESSION_KEY, new CancelEventSubscription(sessionId));
    }

    public void cancelSubscriptions(String sessionId) {
        eventSubscriptions.cancelAll(sessionId);
    }

    @Override
    public void start(Container container) {
        started = true;
    }

    @Override
    public void stop(Container container) {
        started = false;
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

    public void addEventAuthorizer(EventAuthorizer authorizer) {
        this.eventAuthorizers.add(authorizer);
    }

    /**
     * This handles basic authorisation checks for clients that want to subscribe to events in the system
     */
    public boolean authorizeEventSubscription(String realm, AuthContext authContext, EventSubscription<?> subscription) {
        boolean authorized = eventSubscriptionAuthorizers.stream()
                .anyMatch(authorizer -> authorizer.authorise(realm, authContext, subscription));

        if (!authorized) {
            if (authContext != null) {
                LOG.log(DEBUG, "Client not authorised to subscribe: subscription=" + subscription + ", requestRealm=" + realm + ", username=" + authContext.getUsername() + ", userRealm=" + authContext.getAuthenticatedRealmName());
            } else {
                LOG.log(DEBUG, "Client not authorised to subscribe: subscription=" + subscription + ", requestRealm=" + realm + ", user=null");
            }
        }

        return authorized;
    }

    /**
     * This handles basic authorisation checks for clients that want to write an event to the system; this gets hit a lot
     * so should be as performant as possible
     */
    // TODO: Implement auth cache in OR that covers HTTP, WS and MQTT (ActiveMQ Authorization cache currently covers MQTT which is the main use case)
    public <T extends SharedEvent> boolean authorizeEventWrite(String realm, AuthContext authContext, T event) {
        boolean authorized = eventAuthorizers.stream()
            .anyMatch(authorizer -> authorizer.authorise(realm, authContext, event));

        if (!authorized) {
            if (authContext != null) {
                LOG.log(DEBUG, () -> "Client not authorised to send event: type=" + event.getEventType() + ", requestRealm=" + realm + ", user=" + authContext.getUsername() + ", userRealm=" + authContext.getAuthenticatedRealmName());
            } else {
                LOG.log(DEBUG, () -> "Client not authorised to send event: type=" + event.getEventType() + ", requestRealm=" + realm + ", user=null");
            }
        }

        return authorized;
    }

    /**
     * Publish an event to interested clients
     */
    public <T extends Event> void publishEvent(T event) {
        // Only publish if service is started
        if (!started) {
            return;
        }

        if (messageBrokerService != null && messageBrokerService.getFluentProducerTemplate() != null) {
            // Don't log that we are publishing a syslog event,
            if (!(event instanceof SyslogEvent)) {
                LOG.log(System.Logger.Level.TRACE, () -> "Publishing to clients: " + event);
            }
            messageBrokerService.getFluentProducerTemplate()
                .withBody(event)
                .to(PUBLISH_QUEUE)
                .asyncSend();
        }
    }

    public void sendToSession(String sessionKey, Object data) {
        if (messageBrokerService != null && messageBrokerService.getFluentProducerTemplate() != null) {
            LOG.log(TRACE, () -> "Sending to session '" + sessionKey + "': " + data);
            SessionInfo sessionInfo = sessionKeyInfoMap.get(sessionKey);
            if (sessionInfo == null) {
                LOG.log(DEBUG, () -> "Send to session '" + sessionKey + "' failed, it doesn't exist or is disconnected: " + data);
                return;
            }
            messageBrokerService.getFluentProducerTemplate()
                .withBody(data)
                .withHeader(SESSION_KEY, sessionKey)
                .withHeader(HEADER_CONNECTION_TYPE, sessionInfo.connectionType)
                .to(CLIENT_OUTBOUND_QUEUE)
                .asyncSend();
        }
    }

    public void closeSession(String sessionKey) {
        SessionInfo sessionInfo = sessionKeyInfoMap.get(sessionKey);

        if (sessionInfo == null || sessionInfo.closeRunnable == null) {
            return;
        }

        LOG.log(DEBUG, "Closing session: " + sessionKey);
        sessionInfo.closeRunnable.run();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    protected void passToInterceptors(Exchange exchange) {
        // Pass to each interceptor and stop if any interceptor marks the exchange as stop routing
       exchangeInterceptors.stream().anyMatch(interceptor -> {
            interceptor.accept(exchange);
            boolean stop = exchange.isRouteStop();
            if (stop) {
                LOG.log(TRACE, "Client event interceptor marked exchange as `stop routing`: " + interceptor);
            }
            return stop;
        });
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }

    protected static SessionInfo createSessionInfo(String sessionKey, Exchange exchange) {
        String connectionType = (String) exchange.getIn().getHeader(HEADER_CONNECTION_TYPE);
        Runnable closeRunnable = exchange.getIn().getHeader(SESSION_TERMINATOR, Runnable.class);
        return new SessionInfo(connectionType, closeRunnable);
    }

    /**
     * Provides a protocol agnostic way to close a client session
     */
    protected Runnable getWebsocketSessionTerminator(WebSocketChannel webSocketChannel) {
        return () -> {
            try {
                webSocketChannel.close();
            } catch (IOException ignored) {}
        };
    }
}
