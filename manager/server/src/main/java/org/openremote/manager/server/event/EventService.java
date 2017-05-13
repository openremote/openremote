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
package org.openremote.manager.server.event;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.container.timer.TimerService;
import org.openremote.container.web.socket.WebsocketAuth;
import org.openremote.container.web.socket.WebsocketConstants;
import org.openremote.manager.server.asset.AssetProcessingService;
import org.openremote.manager.server.asset.AssetStorageService;
import org.openremote.manager.server.concurrent.ManagerExecutorService;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.manager.shared.security.User;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.event.shared.CancelEventSubscription;
import org.openremote.model.event.shared.EventSubscription;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.event.shared.UnauthorizedEventSubscription;
import org.openremote.model.syslog.SyslogEvent;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

import static org.apache.camel.builder.PredicateBuilder.or;
import static org.openremote.manager.server.asset.AssetProcessingService.ASSET_QUEUE;

/**
 * Receives and publishes messages, handles the client/server event bus.
 * <p>
 * Messages always start with a message discriminator in all uppercase letters, followed
 * by an optional JSON payload.
 * <p>
 * The following messages can be send by a client:
 * <dl>
 * <dt><code>SUBSCRIBE{...}</code><dt>
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
 * <dt><code>UNSUBSCRIBE{...}</code></dt>
 * <dd><p>
 * The payload is a serialized representation of {@link CancelEventSubscription}. If a client
 * does not want to wait for expiration of its subscriptions, it can cancel a subscription.
 * </p></dd>
 * <dt><code>EVENT{...}</code></dt>
 * <dd><p>
 * The payload is a serialized representation of a subtype of {@link SharedEvent}. If the server
 * does not recognize the event, it is silently ignored.
 * </p></dd>
 * </dl>
 * <p>
 * The following messages can be published/returned by the server:
 * <dl>
 * <dt><code>UNAUTHORIZED{...}</code></dt>
 * <dd><p>
 * The payload is a serialized representation of {@link UnauthorizedEventSubscription}.
 * </p></dd>
 * <dt><code>EVENT{...}</code></dt>
 * <dd><p>
 * The payload is a serialized representation of a subtype of {@link SharedEvent}.
 * </p></dd>
 * <dt><code>EVENT[...]</code></dt>
 * <dd><p>
 * The payload is an array of {@link SharedEvent}s.
 * </p></dd>
 * </dl>
 */
public class EventService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(EventService.class.getName());

    public static final String WEBSOCKET_EVENTS = "events";

    // TODO: Some of these options should be configurable depending on expected load etc.
    public static final String INCOMING_EVENT_TOPIC = "seda://IncomingEventTopic?multipleConsumers=true&concurrentConsumers=1&waitForTaskToComplete=NEVER&purgeWhenStopping=true&discardIfNoConsumers=true&limitConcurrentConsumers=false&size=1000";

    public static final String OUTGOING_EVENT_QUEUE = "seda://OutgoingEventQueue?multipleConsumers=false&waitForTaskToComplete=NEVER&purgeWhenStopping=true&discardIfNoConsumers=true&size=1000";

    final protected Collection<EventSubscriptionAuthorizer> eventSubscriptionAuthorizers = new CopyOnWriteArraySet<>();
    protected MessageBrokerService messageBrokerService;
    protected ManagerIdentityService managerIdentityService;
    protected AssetStorageService assetStorageService;
    protected AssetProcessingService assetProcessingService;
    protected EventSubscriptions eventSubscriptions;

    @Override
    public void init(Container container) throws Exception {
        messageBrokerService = container.getService(MessageBrokerService.class);
        managerIdentityService = container.getService(ManagerIdentityService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        assetProcessingService = container.getService(AssetProcessingService.class);

        eventSubscriptions = new EventSubscriptions(
            container.getService(TimerService.class),
            container.getService(ManagerExecutorService.class)
        );


        MessageBrokerSetupService messageBrokerSetupService = container.getService(MessageBrokerSetupService.class);
        messageBrokerSetupService.getContext().getTypeConverterRegistry().addTypeConverters(
            new EventTypeConverters()
        );

        messageBrokerSetupService.getContext().addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                from("websocket://" + WEBSOCKET_EVENTS)
                    .routeId("Receive incoming message on WebSocket session(s)")
                    .choice()
                    .when(header(WebsocketConstants.SESSION_OPEN))
                    .process(exchange -> {
                        // Do nothing except stop the exchanges
                    })
                    .stop()
                    .when(or(
                        header(WebsocketConstants.SESSION_CLOSE),
                        header(WebsocketConstants.SESSION_CLOSE_ERROR)
                    ))
                    .process(exchange -> {
                        String sessionKey = getSessionKey(exchange);
                        eventSubscriptions.cancelAll(sessionKey);
                    })
                    .stop()
                    .end()
                    .choice()
                    .when(bodyAs(String.class).startsWith(EventSubscription.MESSAGE_PREFIX))
                    .convertBodyTo(EventSubscription.class)
                    .process(exchange -> {
                        String sessionKey = getSessionKey(exchange);
                        EventSubscription subscription = exchange.getIn().getBody(EventSubscription.class);
                        WebsocketAuth auth = getWebsocketAuth(exchange);
                        if (eventSubscriptionAuthorizers.stream()
                            .anyMatch(authorizer -> authorizer.apply(auth, subscription))) {
                            eventSubscriptions.update(sessionKey, subscription);
                        } else {
                            LOG.warning("Unauthorized subscription from '"
                                + auth.getUsername() + "' in realm '" + auth.getAuthenticatedRealm()
                                + "': " + subscription
                            );
                            sendToSession(sessionKey, new UnauthorizedEventSubscription(subscription.getEventType()));
                        }
                    })
                    .when(bodyAs(String.class).startsWith(CancelEventSubscription.MESSAGE_PREFIX))
                    .convertBodyTo(CancelEventSubscription.class)
                    .process(exchange -> {
                        String sessionKey = getSessionKey(exchange);
                        eventSubscriptions.cancel(sessionKey, exchange.getIn().getBody(CancelEventSubscription.class));
                    })
                    .when(bodyAs(String.class).startsWith(SharedEvent.MESSAGE_PREFIX))
                        .convertBodyTo(SharedEvent.class)
                        .process(exchange -> {
                            WebsocketAuth auth = getWebsocketAuth(exchange);

                            // Extract user object to use as the sender
                            User user = managerIdentityService.getUser(auth.getAccessToken());
                            exchange.getIn().setHeader(SharedEvent.HEADER_SENDER, user);
                        }).end()
                    .choice()
                    .when(body().isInstanceOf(AttributeEvent.class))
                        .to(ASSET_QUEUE).stop()
                    .when(body().isInstanceOf(SharedEvent.class))
                        .to(INCOMING_EVENT_TOPIC)
                    .otherwise()
                    .process(exchange -> LOG.fine("Unsupported message body: " + exchange.getIn().getBody()))
                    .end();

                from(EventService.OUTGOING_EVENT_QUEUE)
                    .routeId("Send outgoing events to WebSocket session(s)")
                    .choice()
                    .when(body().isInstanceOf(SharedEvent.class))
                    .split(method(eventSubscriptions, "splitForSubscribers"))
                    .to("websocket://" + WEBSOCKET_EVENTS)
                    .end();
            }
        });
    }

    @Override
    public void start(Container container) {
    }

    @Override
    public void stop(Container container) {

    }

    public void addSubscriptionAuthorizer(EventSubscriptionAuthorizer authorizer) {
        this.eventSubscriptionAuthorizers.add(authorizer);
    }

    public void publishEvent(SharedEvent event) {
        if (messageBrokerService != null && messageBrokerService.getProducerTemplate() != null) {
            // Don't log that we are publishing a syslog event,
            if (!(event instanceof SyslogEvent)) {
                LOG.fine("Publishing: " + event);
            }
            messageBrokerService.getProducerTemplate().sendBody(
                OUTGOING_EVENT_QUEUE,
                event
            );
        }
    }

    public void sendToSession(String sessionKey, Object data) {
        if (messageBrokerService != null && messageBrokerService.getProducerTemplate() != null) {
            LOG.fine("Sending to session '" + sessionKey + "': " + data);
            messageBrokerService.getProducerTemplate().sendBodyAndHeader(
                "websocket://" + WEBSOCKET_EVENTS,
                data,
                WebsocketConstants.SESSION_KEY, sessionKey
            );
        }
    }

    public static String getSessionKey(Exchange exchange) {
        return exchange.getIn().getHeader(WebsocketConstants.SESSION_KEY, String.class);
    }

    public static WebsocketAuth getWebsocketAuth(Exchange exchange) {
        WebsocketAuth auth = exchange.getIn().getHeader(WebsocketConstants.AUTH, WebsocketAuth.class);
        if (auth == null)
            throw new IllegalStateException("No authorization details in websocket exchange");
        return auth;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }

}
