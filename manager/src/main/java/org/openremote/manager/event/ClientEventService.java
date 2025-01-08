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
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.AssetFilter;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.event.Event;
import org.openremote.model.event.RespondableEvent;
import org.openremote.model.event.TriggeredEventSubscription;
import org.openremote.model.event.shared.*;
import org.openremote.model.syslog.SyslogEvent;
import org.openremote.model.util.Pair;

import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static java.lang.System.Logger.Level.*;
import static org.openremote.manager.asset.AssetProcessingService.ATTRIBUTE_EVENT_PROCESSOR;
import static org.openremote.manager.asset.AssetProcessingService.ATTRIBUTE_EVENT_ROUTE_CONFIG_ID;
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
    public static final int PRIORITY = ManagerWebService.PRIORITY - 200;
    public static final String WEBSOCKET_URI = "undertow://ws://0.0.0.0/websocket/events?fireWebSocketChannelEvents=true&sendTimeout=15000&keepAlive=false"; // Host is not used as existing undertow instance is utilised
    protected static final System.Logger LOG = System.getLogger(ClientEventService.class.getName());
    protected static final String PUBLISH_QUEUE = "direct://ClientPublishQueue";

    final protected Collection<EventSubscriptionAuthorizer> eventSubscriptionAuthorizers = new CopyOnWriteArraySet<>();
    final protected Collection<EventAuthorizer> eventAuthorizers = new CopyOnWriteArraySet<>();
    final protected Set<Pair<EventSubscription<? extends Event>, Consumer<? extends Event>>> eventSubscriptions = new CopyOnWriteArraySet<>();
    final protected Map<String, WebSocketChannel> sessionChannels = new ConcurrentHashMap<>();
    final protected Map<String, Map<String, Consumer<? extends Event>>> websocketSessionSubscriptionConsumers = new HashMap<>();
    protected TimerService timerService;
    protected ExecutorService executorService;
    protected MessageBrokerService messageBrokerService;
    protected ManagerIdentityService identityService;
    protected GatewayService gatewayService;
    protected boolean started;
    protected Consumer<Exchange> gatewayInterceptor;

    public static String getSessionKey(Exchange exchange) {
        return exchange.getIn().getHeader(UndertowConstants.CONNECTION_KEY, String.class);
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
        executorService = container.getExecutor();

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
    }

    // TODO: Remove prefix and just use event type then use a subscription wrapper to pass subscription ID around
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void configure() throws Exception {

        // Route for handling inbound websocket messages
        from(WEBSOCKET_URI)
            .routeId("ClientInbound-Websocket")
            .routeConfigurationId(ATTRIBUTE_EVENT_ROUTE_CONFIG_ID)
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

                        // Set an idle timeout value
                        webSocketChannel.setIdleTimeout(30000);

                        // Push auth and realm into channel for future use
                        webSocketChannel.setAttribute(Constants.AUTH_CONTEXT, authContext);
                        webSocketChannel.setAttribute(Constants.REALM_PARAM_NAME, realm);

                        exchange.getIn().setHeader(Constants.AUTH_CONTEXT, authContext);
                        exchange.getIn().setHeader(Constants.REALM_PARAM_NAME, realm);
                        exchange.getIn().setHeader(SESSION_OPEN, true);
                        sessionChannels.put(getSessionKey(exchange), webSocketChannel);
                        LOG.log(DEBUG, "Client connection created: " + webSocketChannel.getSourceAddress());
                    }
                    case ONCLOSE -> {
                        AuthContext authContext = (AuthContext)webSocketChannel.getAttribute(Constants.AUTH_CONTEXT);
                        String realm = (String)webSocketChannel.getAttribute(Constants.REALM_PARAM_NAME);
                        String sessionKey = getSessionKey(exchange);

                        exchange.getIn().setHeader(Constants.AUTH_CONTEXT, authContext);
                        exchange.getIn().setHeader(Constants.REALM_PARAM_NAME, realm);
                        exchange.getIn().setHeader(SESSION_CLOSE, true);
                        sessionChannels.remove(getSessionKey(exchange));
                        LOG.log(DEBUG, "Client connection closed: " + webSocketChannel.getSourceAddress());
                        LOG.log(TRACE, "Removing subscriptions for session: " + sessionKey);
                        synchronized (websocketSessionSubscriptionConsumers) {
                            websocketSessionSubscriptionConsumers.computeIfPresent(sessionKey, (s, subscriptionConsumers) -> {
                                subscriptionConsumers.forEach((subscriptionKey, consumer) -> removeSubscription(consumer));
                                return null;
                            });
                        }
                    }
                    case ONERROR -> {
                        AuthContext authContext = (AuthContext)webSocketChannel.getAttribute(Constants.AUTH_CONTEXT);
                        String realm = (String)webSocketChannel.getAttribute(Constants.REALM_PARAM_NAME);
                        String sessionKey = getSessionKey(exchange);

                        exchange.getIn().setHeader(Constants.AUTH_CONTEXT, authContext);
                        exchange.getIn().setHeader(Constants.REALM_PARAM_NAME, realm);
                        exchange.getIn().setHeader(SESSION_CLOSE_ERROR, true);
                        LOG.log(DEBUG, "Client connection error: " + webSocketChannel.getSourceAddress());
                        try {
                            webSocketChannel.close();
                        } catch (Exception ignored) {}
                        sessionChannels.remove(getSessionKey(exchange));
                        LOG.log(TRACE, "Removing subscriptions for session: " + sessionKey);
                        synchronized (websocketSessionSubscriptionConsumers) {
                            websocketSessionSubscriptionConsumers.computeIfPresent(sessionKey, (s, subscriptionConsumers) -> {
                                subscriptionConsumers.forEach((subscriptionKey, consumer) -> removeSubscription(consumer));
                                return null;
                            });
                        }
                    }
                }

                // Pass to gateway
                if (gatewayInterceptor != null) {
                    gatewayInterceptor.accept(exchange);
                }
            })
            .stop()
            .endChoice()
            .end()
            .process(exchange -> {
                WebSocketChannel webSocketChannel = exchange.getIn().getHeader(UndertowConstants.CHANNEL, WebSocketChannel.class);
                AuthContext authContext = (AuthContext) webSocketChannel.getAttribute(Constants.AUTH_CONTEXT);
                String realm = (String) webSocketChannel.getAttribute(Constants.REALM_PARAM_NAME);

                exchange.getIn().setHeader(Constants.AUTH_CONTEXT, authContext);
                exchange.getIn().setHeader(Constants.REALM_PARAM_NAME, realm);

                // Do basic formatting of exchange
                if (exchange.getIn().getBody() instanceof String bodyStr) {
                    if (bodyStr.startsWith(EventSubscription.SUBSCRIBE_MESSAGE_PREFIX)) {
                        exchange.getIn().setBody(exchange.getIn().getBody(EventSubscription.class));
                    } else if (bodyStr.startsWith(CancelEventSubscription.MESSAGE_PREFIX)) {
                        exchange.getIn().setBody(exchange.getIn().getBody(CancelEventSubscription.class));
                    } else if (bodyStr.startsWith(SharedEvent.MESSAGE_PREFIX)) {
                        exchange.getIn().setBody(exchange.getIn().getBody(SharedEvent.class));
                    }
                }

                if (exchange.getIn().getBody() instanceof RespondableEvent respondableEvent) {
                    // Inject a response consumer
                    respondableEvent.setResponseConsumer(ev -> sendToWebsocketSession(getSessionKey(exchange), ev));
                }

                // Pass to gateway
                if (gatewayInterceptor != null) {
                    gatewayInterceptor.accept(exchange);
                }
            })
            .process(exchange -> {
                AuthContext authContext = exchange.getIn().getHeader(Constants.AUTH_CONTEXT, AuthContext.class);
                String realm = exchange.getIn().getHeader(Constants.REALM_PARAM_NAME, String.class);

                if (exchange.getIn().getBody() instanceof EventSubscription<?> subscription) {
                    String sessionKey = getSessionKey(exchange);
                    LOG.log(TRACE, () -> "Adding subscription for session '" + sessionKey + "': " + subscription);

                    if (!authorizeEventSubscription(realm, authContext, subscription)) {
                        sendToWebsocketSession(sessionKey, new UnauthorizedEventSubscription<>(subscription));
                        exchange.setRouteStop(true);
                        return;
                    }

                    // Force subscription to filter only value changed attribute events
                    if (subscription.getFilter() instanceof AssetFilter assetFilter) {
                        subscription.setFilter(assetFilter.setValueChanged(true));
                    }

                    // Notify the client that the subscription has been created
                    subscription.setSubscribed(true);
                    sendToWebsocketSession(sessionKey, subscription);

                    synchronized (websocketSessionSubscriptionConsumers) {
                        // Create subscription consumer and track it for future removal requests
                        Consumer<? extends SharedEvent> consumer = ev -> onWebsocketSubscriptionTriggered(sessionKey, subscription, ev);
                        Map<String, Consumer<? extends Event>> subscriptionConsumers = websocketSessionSubscriptionConsumers.computeIfAbsent(sessionKey, (s) -> new HashMap<>());
                        String subscriptionKey = subscription.getEventType() + subscription.getSubscriptionId();
                        subscriptionConsumers.put(subscriptionKey, consumer);
                        addSubscription(subscription, consumer);
                    }
                    exchange.setRouteStop(true);
                } else if (exchange.getIn().getBody() instanceof CancelEventSubscription cancelEventSubscription) {
                    String sessionKey = getSessionKey(exchange);
                    LOG.log(TRACE, () -> "Cancelling subscription for session '" + sessionKey + "': " + cancelEventSubscription);

                    synchronized (websocketSessionSubscriptionConsumers) {
                        websocketSessionSubscriptionConsumers.computeIfPresent(sessionKey, (s, subscriptionConsumers) -> {
                            String subscriptionKey = cancelEventSubscription.getEventType() + cancelEventSubscription.getSubscriptionId();
                            Consumer<? extends Event> consumer = subscriptionConsumers.remove(subscriptionKey);
                            if (consumer != null) {
                                removeSubscription(consumer);
                            }
                            if (subscriptionConsumers.isEmpty()) {
                                return null;
                            }
                            return subscriptionConsumers;
                        });
                    }
                    exchange.setRouteStop(true);
                } else if (exchange.getIn().getBody() instanceof SharedEvent event) {
                    if (!authorizeEventWrite(realm, authContext, event)) {
                        exchange.setRouteStop(true);
                        return;
                    }

                    // Special handling for incoming attribute events
                    if (event instanceof AttributeEvent attributeEvent) {
                        // Set timestamp as early as possible if not set
                        if (attributeEvent.getTimestamp() <= 0) {
                            attributeEvent.setTimestamp(timerService.getCurrentTimeMillis());
                        }
                        attributeEvent.setSource("WebsocketClient");

                        messageBrokerService.getFluentProducerTemplate()
                            .withBody(attributeEvent)
                            .to(ATTRIBUTE_EVENT_PROCESSOR)
                            .asyncSend();
                        exchange.setRouteStop(true);
                        return;
                    }
                }
            })
            .to(PUBLISH_QUEUE)
            .end();

        // Send event to each interested subscribers
        from(PUBLISH_QUEUE)
            .routeId("ClientPublishToSubscribers")
            .routeConfigurationId(ATTRIBUTE_EVENT_ROUTE_CONFIG_ID)
            .threads().executorService(executorService)
            .filter(body().isInstanceOf(SharedEvent.class))
            .process(exchange -> {
                SharedEvent event = exchange.getIn().getBody(SharedEvent.class);
                sendToSubscribers(event);
            });
    }

    @SuppressWarnings("unchecked")
    protected <T extends SharedEvent> void sendToSubscribers(T event) {
        eventSubscriptions.forEach(eventSubscriptionConsumerPair -> {
            EventSubscription<?> subscription = eventSubscriptionConsumerPair.getKey();

            if (!subscription.getEventType().equals(event.getEventType())) {
                return;
            }

            T filteredEvent = subscription.getFilter() == null ? event : ((EventSubscription<T>) subscription).getFilter().apply(event);

            if (filteredEvent == null) {
                return;
            }

            Consumer<T> consumer = (Consumer<T>)eventSubscriptionConsumerPair.getValue();
            consumer.accept(filteredEvent);
        });
    }

    /**
     * Authorisation must be done before adding the subscription and is the responsibility of subscription creators.
     */
    public void addSubscription(EventSubscription<? extends Event> eventSubscription, Consumer<? extends Event> consumer) throws IllegalStateException {
        eventSubscriptions.add(new Pair<>(eventSubscription, consumer));
    }
    public <T extends Event> void addSubscription(Class<T> eventClass, Consumer<T> consumer) throws IllegalStateException {
        addSubscription(new EventSubscription<>(eventClass, null), consumer);
    }
    public <T extends Event> void addSubscription(Class<T> eventClass, EventFilter<T> filter, Consumer<T> consumer) throws IllegalStateException {
        addSubscription(new EventSubscription<>(eventClass, filter), consumer);
    }

    public void removeSubscription(Consumer<? extends Event> consumer) {
        eventSubscriptions.removeIf(subscriptionConsumerPair -> subscriptionConsumerPair.value == consumer);
    }

    @Override
    public void start(Container container) {
        started = true;
    }

    @Override
    public void stop(Container container) {
        started = false;
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
     * Publish an event to interested subscribers
     */
    public <T extends Event> void publishEvent(T event) {
        // Only publish if service is started
        if (!started) {
            return;
        }

        if (!(event instanceof SyslogEvent)) {
            LOG.log(System.Logger.Level.TRACE, () -> "Publishing to subscribers: " + event);
        }

        messageBrokerService.getFluentProducerTemplate()
            .withBody(event)
            .to(PUBLISH_QUEUE)
            .asyncSend();
    }

    /**
     * This allows gateway connectors to intercept exchanges from gateway clients; it by-passes the standard processing
     * including authorization so the interceptor provides its own authorization checks.
     */
    public void setGatewayInterceptor(Consumer<Exchange> consumer) {
        this.gatewayInterceptor = consumer;
    }

    protected void onWebsocketSubscriptionTriggered(String sessionKey, EventSubscription<?> subscription, SharedEvent event) {
        // Wrap subscription event in triggered wrapper for client to easily route it
        TriggeredEventSubscription<?> triggeredEventSubscription = new TriggeredEventSubscription<>(Collections.singletonList(event), subscription.getSubscriptionId());

        messageBrokerService.getFluentProducerTemplate()
            .withBody(triggeredEventSubscription)
            .withHeader(UndertowConstants.CONNECTION_KEY, sessionKey)
            .to(WEBSOCKET_URI)
            .asyncSend();
    }

    public void sendToWebsocketSession(String sessionKey, Object data) {
        messageBrokerService.getFluentProducerTemplate()
            .withBody(data)
            .withHeader(UndertowConstants.CONNECTION_KEY, sessionKey)
            .to(WEBSOCKET_URI)
            .asyncSend();
    }

    public void closeWebsocketSession(String sessionKey) {
        WebSocketChannel webSocketChannel = sessionChannels.get(sessionKey);
        if (webSocketChannel != null) {
            LOG.log(INFO, () -> "Force closing websocket session: " + sessionKey);
            try {
                webSocketChannel.close();
            } catch (IOException e) {
                LOG.log(INFO, () -> "Failed to close websocket session: " + sessionKey);
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }
}
