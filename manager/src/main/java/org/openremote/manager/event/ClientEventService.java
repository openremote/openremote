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
import org.keycloak.representations.idm.ClientRepresentation;
import org.openremote.agent.protocol.ProtocolClientEventService;
import org.openremote.container.Container;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.security.AuthContext;
import org.openremote.container.timer.TimerService;
import org.openremote.container.web.ConnectionConstants;
import org.openremote.manager.concurrent.ManagerExecutorService;
import org.openremote.manager.gateway.GatewayService;
import org.openremote.manager.mqtt.MqttBrokerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Constants;
import org.openremote.model.event.shared.*;
import org.openremote.model.security.User;
import org.openremote.model.syslog.SyslogEvent;
import org.openremote.model.util.TextUtil;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.camel.builder.PredicateBuilder.or;
import static org.openremote.container.web.ConnectionConstants.SESSION;
import static org.openremote.agent.protocol.ProtocolClientEventService.getSessionKey;
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
 * when they are published on the server. Subscriptions are handled by {@link SharedEvent#getEventType}, there
 * can only be one active subscription for a particular event type and any new subscription for the same event
 * type will replace any currently active subscription. The <code>SUBSCRIBE</code> message must be send
 * repeatedly to renew the subscription,or the server will expire the subscription. The default expiration
 * time is {@link EventSubscription#RENEWAL_PERIOD_SECONDS}; it is recommended clients renew the subscription
 * in shorter periods to allow for processing time of the renewal.
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
public class ClientEventService implements ProtocolClientEventService {

    protected static class SessionInfo {
        String connectionType;
        Runnable closeRunnable;

        public SessionInfo(String connectionType, Runnable closeRunnable) {
            this.connectionType = connectionType;
            this.closeRunnable = closeRunnable;
        }
    }

    public static final int PRIORITY = ManagerWebService.PRIORITY - 200;
    private static final Logger LOG = Logger.getLogger(ClientEventService.class.getName());
    public static final String WEBSOCKET_EVENTS = "events";

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
    protected boolean stopped;

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
            container.getService(TimerService.class),
            container.getService(ManagerExecutorService.class)
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
                            } else if (bodyStr.startsWith(RenewEventSubscriptions.MESSAGE_PREFIX)) {
                                exchange.getIn().setBody(exchange.getIn().getBody(RenewEventSubscriptions.class));
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
                                boolean restrictedUser = identityService.getIdentityProvider().isRestrictedUser(authContext.getUserId());
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
                    .when(body().isInstanceOf(RenewEventSubscriptions.class))
                        .process(exchange -> {
                            String sessionKey = getSessionKey(exchange);
                            AuthContext authContext = exchange.getIn().getHeader(Constants.AUTH_CONTEXT, AuthContext.class);
                            boolean restrictedUser = identityService.getIdentityProvider().isRestrictedUser(authContext.getUserId());
                            eventSubscriptions.update(sessionKey, restrictedUser,exchange.getIn().getBody(RenewEventSubscriptions.class).getSubscriptionIds());
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
                        .process(exchange -> LOG.fine("Unsupported message body: " + exchange.getIn().getBody()))
                    .end();
            }
        });
    }

    @Override
    public void start(Container container) {
        stopped = false;
    }

    @Override
    public void stop(Container container) {
        stopped = true;
    }

    @Override
    public void addExchangeInterceptor(Consumer<Exchange> exchangeInterceptor) throws RuntimeException {
        exchangeInterceptors.add(exchangeInterceptor);
    }

    @Override
    public void removeExchangeInterceptor(Consumer<Exchange> exchangeInterceptor) {
        exchangeInterceptors.remove(exchangeInterceptor);
    }

    @Override
    public void addClientCredentials(ClientCredentials clientCredentials) throws RuntimeException {
        if (!(identityService.getIdentityProvider() instanceof ManagerKeycloakIdentityProvider)) {
            throw new IllegalStateException("Cannot add client credentials if not using Keycloak identity provider");
        }

        if (TextUtil.isNullOrEmpty(clientCredentials.getRealm())
            || TextUtil.isNullOrEmpty(clientCredentials.getClientId())
            || TextUtil.isNullOrEmpty(clientCredentials.getSecret())) {
            throw new IllegalArgumentException("Invalid client credentials realm, client ID and secret must be specified");
        }

        ManagerKeycloakIdentityProvider keycloakIdentityProvider = (ManagerKeycloakIdentityProvider)identityService.getIdentityProvider();

        // Check if exists already
        ClientRepresentation clientRepresentation = keycloakIdentityProvider.getClient(clientCredentials.getRealm(), clientCredentials.getClientId());

        if (clientRepresentation == null) {
            clientRepresentation = new ClientRepresentation();
            clientRepresentation.setId(UUID.nameUUIDFromBytes(clientCredentials.getClientId().getBytes()).toString());
            clientRepresentation.setStandardFlowEnabled(false);
            clientRepresentation.setImplicitFlowEnabled(false);
            clientRepresentation.setDirectAccessGrantsEnabled(false);
            clientRepresentation.setServiceAccountsEnabled(true);
            clientRepresentation.setClientAuthenticatorType("client-secret");
            clientRepresentation.setClientId(clientCredentials.getClientId());
        }

        clientRepresentation.setSecret(clientCredentials.getSecret());
        LOG.info("Creating/updating client event service client credentials: " + clientCredentials);
        keycloakIdentityProvider.createClient(clientCredentials.getRealm(), clientRepresentation);

        User serviceUser = keycloakIdentityProvider.getClientServiceUser(clientCredentials.getRealm(), clientCredentials.getClientId());
        keycloakIdentityProvider.updateRoles(clientCredentials.getRealm(), serviceUser.getId(), clientCredentials.getRoles());
    }

    @Override
    public void removeClientCredentials(String realm, String clientId) throws RuntimeException {
        if (!(identityService.getIdentityProvider() instanceof ManagerKeycloakIdentityProvider)) {
            throw new IllegalStateException("Cannot remove client credentials if not using Keycloak identity provider");
        }

        if (TextUtil.isNullOrEmpty(realm)
            || TextUtil.isNullOrEmpty(clientId)) {
            throw new IllegalArgumentException("Invalid client credentials realm and client ID must be specified");
        }

        ManagerKeycloakIdentityProvider keycloakIdentityProvider = (ManagerKeycloakIdentityProvider)identityService.getIdentityProvider();
        LOG.info("Deleting client event service client: realm=" + realm + ", client ID=" + clientId);
        keycloakIdentityProvider.deleteClient(realm, clientId);
    }

    public void addSubscriptionAuthorizer(EventSubscriptionAuthorizer authorizer) {
        this.eventSubscriptionAuthorizers.add(authorizer);
    }

    public boolean authorizeEventSubscription(AuthContext authContext, EventSubscription<?> subscription) {
        return eventSubscriptionAuthorizers.stream()
                .anyMatch(authorizer -> authorizer.apply(authContext, subscription));
    }

    public void publishEvent(SharedEvent event) {
        publishEvent(true, event);
    }

    /**
     * @param accessRestricted <code>true</code> if this event can be received by restricted user sessions.
     */
    public void publishEvent(boolean accessRestricted, SharedEvent event) {
        // Only publish if service is not stopped
        if (stopped) {
            return;
        }

        if (messageBrokerService != null && messageBrokerService.getProducerTemplate() != null) {
            // Don't log that we are publishing a syslog event,
            if (!(event instanceof SyslogEvent)) {
                LOG.fine("Publishing: " + event);
            }
            messageBrokerService.getProducerTemplate()
                .sendBodyAndHeader(CLIENT_EVENT_QUEUE, event, HEADER_ACCESS_RESTRICTED, accessRestricted);
        }
    }

    public void sendToSession(String sessionKey, Object data) {
        if (messageBrokerService != null && messageBrokerService.getProducerTemplate() != null) {
            LOG.fine("Sending to session '" + sessionKey + "': " + data);
            SessionInfo sessionInfo = sessionKeyInfoMap.get(sessionKey);
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

    public EventSubscriptions getEventSubscriptions() {
        return eventSubscriptions;
    }

    protected void passToInterceptors(Exchange exchange) {
        // Pass to each interceptor and stop if any interceptor marks the exchange as stop routing
        if (exchangeInterceptors.stream().anyMatch(interceptor -> {
            interceptor.accept(exchange);
            boolean stop = exchange.getProperty(Exchange.ROUTE_STOP, false, Boolean.class);
            if (stop) {
                LOG.fine("Client event interceptor marked exchange as `stop routing`");
            }
            return stop;
        }));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }

    // TODO: Implement MQTT close once supported by Moquette (https://github.com/moquette-io/moquette/issues/469)
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
        }

        return new SessionInfo(connectionType, closeRunnable);
    }
}
