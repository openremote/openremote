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
import org.openremote.manager.mqtt.DefaultMQTTHandler;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.event.shared.*;
import org.openremote.model.syslog.SyslogEvent;

import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.TRACE;
import static org.apache.camel.builder.PredicateBuilder.or;
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
// TODO: Implement session expiry based on security principal
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
    private static final System.Logger LOG = System.getLogger(ClientEventService.class.getName());
    public static final String WEBSOCKET_URI = "undertow://ws://0.0.0.0/websocket/events?fireWebSocketChannelEvents=true&sendTimeout=15000"; // Host is not used as existing undertow instance is utilised
    protected static final String INTERNAL_SESSION_KEY = "ClientEventServiceInternal";

    // TODO: Some of these options should be configurable depending on expected load etc.
    public static final String CLIENT_EVENT_TOPIC = "seda://ClientEventTopic?multipleConsumers=true&concurrentConsumers=1&waitForTaskToComplete=NEVER&purgeWhenStopping=true&discardIfNoConsumers=true&limitConcurrentConsumers=false&size=1000";

//    public static final String CLIENT_EVENT_QUEUE = "seda://ClientEventQueue?multipleConsumers=false&waitForTaskToComplete=NEVER&purgeWhenStopping=true&discardIfNoConsumers=true&size=25000";
    // A direct endpoint with threading which then makes it async
    public static final String CLIENT_EVENT_QUEUE = "direct://ClientEventQueue";

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
    protected boolean stopped;

    /**
     * Method to stop further processing of the exchange
     */
    public static void stopMessage(Exchange exchange) {
        exchange.setRouteStop(true);
    }

    public static boolean isInbound(Exchange exchange) {
        return org.apache.camel.builder.Builder.header(HEADER_CONNECTION_TYPE).isNotNull().matches(exchange);
    }

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

        ManagerWebService webService = container.getService(ManagerWebService.class);

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
        if (!pendingInternalSubscriptions.isEmpty()) {
            pendingInternalSubscriptions.forEach(subscription ->
                eventSubscriptions.createOrUpdate(INTERNAL_SESSION_KEY, subscription));
        }

        pendingInternalSubscriptions = null;
    }

    // TODO: Remove prefix and just use event type then use a subscription wrapper to pass subscription ID around
    @Override
    public void configure() throws Exception {
        from(WEBSOCKET_URI)
            .routeId("FromWebsocket")
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
                            LOG.log(System.Logger.Level.INFO, "Unsupported user principal type: " + principal);
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
            .to(CLIENT_EVENT_QUEUE)
            .stop()
            .when(body().isInstanceOf(SharedEvent.class))
            .process(exchange -> {
                // Set timestamp if not set
                if (exchange.getIn().getBody() instanceof SharedEvent) {
                    SharedEvent event = exchange.getIn().getBody(SharedEvent.class);
                    // If there is no timestamp in event, set to system time
                    if (event.getTimestamp() <= 0) {
                        event.setTimestamp(timerService.getCurrentTimeMillis());
                    }
                }
            })
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
                        stopMessage(exchange);
                    }
                } else if (exchange.getIn().getBody() instanceof EventSubscription<?>) {
                    EventSubscription<?> subscription = exchange.getIn().getBody(EventSubscription.class);
                    String sessionKey = getSessionKey(exchange);

                    if (!authorizeEventSubscription(realm, authContext, subscription)) {
                        sendToSession(sessionKey, new UnauthorizedEventSubscription<>(subscription));
                        stopMessage(exchange);
                    }
                }
            })
            .to(CLIENT_EVENT_QUEUE)
            .end();

        // TODO: Priority fix threads here means injected timestamps for same attribute can produce EVENT_OUTDATED errors in processing chain
        from(CLIENT_EVENT_QUEUE)
            .routeId("ClientEvents")
            .threads().executorService(executorService)
            .choice()
            .when(header(SESSION_OPEN))
            .process(exchange -> {
                String sessionKey = getSessionKey(exchange);
                LOG.log(TRACE, "Adding session: " + sessionKey);
                sessionKeyInfoMap.put(sessionKey, createSessionInfo(sessionKey, exchange));
                passToInterceptors(exchange);
            })
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
                passToInterceptors(exchange);
            })
            .stop()
            .end()
            .process(this::passToInterceptors)
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
            .when(body().isInstanceOf(SharedEvent.class))
            .choice()
            .when(header(HEADER_CONNECTION_TYPE).isNotNull()) // Inbound messages from client
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
            .process(exchange -> LOG.log(TRACE, () -> "Unsupported message body: " + exchange.getIn().getBody()))
            .end();
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
            eventSubscriptions.createOrUpdate(INTERNAL_SESSION_KEY, subscription);
        }
        return subscriptionId;
    }

    public void cancelInternalSubscription(String sessionId) {
        eventSubscriptions.cancel(INTERNAL_SESSION_KEY, new CancelEventSubscription(sessionId));
    }

    public void cancelSubscriptions(String sessionId) {
        eventSubscriptions.cancelAll(sessionId);
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

    // TODO: Priority this should be done once in a consistent way for events coming from any source
    /**
     * This handles basic authorisation checks for clients that want to write an event to the system
     */
    public <T extends SharedEvent> boolean authorizeEventWrite(String realm, AuthContext authContext, T event) {
        boolean authorized = eventAuthorizers.stream()
            .anyMatch(authorizer -> authorizer.authorise(realm, authContext, event));

        if (!authorized) {
            if (authContext != null) {
                LOG.log(DEBUG, "Client not authorised to send event: type=" + event.getEventType() + ", requestRealm=" + realm + ", username=" + authContext.getUsername() + ", userRealm=" + authContext.getAuthenticatedRealmName());
            } else {
                LOG.log(DEBUG,"Client not authorised to send event: type=" + event.getEventType() + ", requestRealm=" + realm + ", user=null");
            }
        }

        return authorized;
    }

    public <T extends SharedEvent> void publishEvent(T event) {
        // Only publish if service is not stopped
        if (stopped) {
            return;
        }

        if (messageBrokerService != null && messageBrokerService.getFluentProducerTemplate() != null) {
            // Don't log that we are publishing a syslog event,
            if (!(event instanceof SyslogEvent)) {
                LOG.log(System.Logger.Level.TRACE, () -> "Publishing to clients: " + event);
            }
            messageBrokerService.getFluentProducerTemplate()
                .withBody(event)
                .to(CLIENT_EVENT_QUEUE)
                .asyncSend();
        }
    }

    public void sendToSession(String sessionKey, Object data) {
        if (messageBrokerService != null && messageBrokerService.getFluentProducerTemplate() != null) {
            LOG.log(TRACE, () -> "Sending to session '" + sessionKey + "': " + data);
            SessionInfo sessionInfo = sessionKeyInfoMap.get(sessionKey);
            if (sessionInfo == null) {
                LOG.log(INFO, "Cannot send to requested session it doesn't exist or is disconnected:" + sessionKey);
                return;
            }
            if (sessionInfo.connectionType.equals(HEADER_CONNECTION_TYPE_WEBSOCKET)) {
                messageBrokerService.getFluentProducerTemplate()
                    .withBody(data)
                    .withHeader(UndertowConstants.CONNECTION_KEY, sessionKey)
                    .to(WEBSOCKET_URI)
                    .asyncSend();
            } else if (sessionInfo.connectionType.equals(HEADER_CONNECTION_TYPE_MQTT)) {
                messageBrokerService.getFluentProducerTemplate()
                    .withBody(data)
                    .withHeader(SESSION_KEY, sessionKey)
                    .to(DefaultMQTTHandler.CLIENT_QUEUE)
                    .asyncSend();
            }
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

    protected void passToInterceptors(Exchange exchange) {
        // Pass to each interceptor and stop if any interceptor marks the exchange as stop routing
        if (exchangeInterceptors.stream().anyMatch(interceptor -> {
            interceptor.accept(exchange);
            boolean stop = exchange.isRouteStop();
            if (stop) {
                LOG.log(TRACE, "Client event interceptor marked exchange as `stop routing`: " + interceptor);
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
        Runnable closeRunnable = exchange.getIn().getHeader(SESSION_TERMINATOR, Runnable.class);
        return new SessionInfo(connectionType, closeRunnable);
    }

    protected Runnable getWebsocketSessionTerminator(WebSocketChannel webSocketChannel) {
        return () -> {
            try {
                webSocketChannel.close();
            } catch (IOException ignored) {}
        };
    }
}
