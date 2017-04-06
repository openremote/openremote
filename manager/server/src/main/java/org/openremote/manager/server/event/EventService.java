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
import org.openremote.container.web.socket.WebsocketAuth;
import org.openremote.container.web.socket.WebsocketConstants;
import org.openremote.model.event.shared.CancelEventSubscription;
import org.openremote.model.event.shared.EventSubscription;
import org.openremote.model.event.shared.SharedEvent;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

import static org.apache.camel.builder.PredicateBuilder.or;

public class EventService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(EventService.class.getName());

    public static final String WEBSOCKET_EVENTS = "events";

    // TODO: Some of these options should be configurable depending on expected load etc.
    public static final String INCOMING_EVENT_TOPIC = "seda://IncomingEventTopic?multipleConsumers=true&concurrentConsumers=10&waitForTaskToComplete=NEVER&purgeWhenStopping=true&discardIfNoConsumers=true&limitConcurrentConsumers=false&size=1000";

    public static final String OUTGOING_EVENT_QUEUE = "seda://OutgoingEventQueue?multipleConsumers=false&waitForTaskToComplete=NEVER&purgeWhenStopping=true&discardIfNoConsumers=true&size=1000";

    final protected EventSubscriptions eventSubscriptions = new EventSubscriptions();
    final protected Collection<EventSubscriptionAuthorizer> eventSubscriptionAuthorizers = new CopyOnWriteArraySet<>();

    protected MessageBrokerService messageBrokerService;

    @Override
    public void init(Container container) throws Exception {
        messageBrokerService = container.getService(MessageBrokerService.class);

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
                        boolean isAuthorized = false;
                        for (EventSubscriptionAuthorizer authorizer : eventSubscriptionAuthorizers) {
                            if (authorizer.isAuthorized(auth, subscription)) {
                                isAuthorized = true;
                                break;
                            }
                        }
                        if (!isAuthorized) {
                            LOG.info("Unauthorized subscription from '"
                                + auth.getUsername() + "' in realm '" + auth.getAuthenticatedRealm()
                                + "': " + subscription
                            );
                            return;
                        }
                        eventSubscriptions.update(sessionKey, subscription);
                    })
                    .when(bodyAs(String.class).startsWith(CancelEventSubscription.MESSAGE_PREFIX))
                    .convertBodyTo(CancelEventSubscription.class)
                    .process(exchange -> {
                        String sessionKey = getSessionKey(exchange);
                        eventSubscriptions.cancel(sessionKey, exchange.getIn().getBody(CancelEventSubscription.class));
                    })
                    .when(bodyAs(String.class).startsWith(SharedEvent.MESSAGE_PREFIX))
                    .convertBodyTo(SharedEvent.class)
                    .to(EventService.INCOMING_EVENT_TOPIC)
                    .otherwise()
                    .process(exchange -> {
                        LOG.fine("Unsupported message body: " + exchange.getIn().getBody());
                    })
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
        LOG.fine("Publishing: " + event);
        messageBrokerService.getProducerTemplate().sendBody(
            OUTGOING_EVENT_QUEUE,
            event
        );
    }

    public static String getSessionKey(Exchange exchange) {
        return exchange.getIn().getHeader(WebsocketConstants.SESSION_KEY, null, String.class);
    }

    public static WebsocketAuth getWebsocketAuth(Exchange exchange) {
        WebsocketAuth auth = exchange.getIn().getHeader(WebsocketConstants.AUTH, null, WebsocketAuth.class);
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
