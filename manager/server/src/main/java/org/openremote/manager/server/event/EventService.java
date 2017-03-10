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
import org.openremote.container.message.MessageBrokerContext;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.container.web.socket.WebsocketConstants;
import org.openremote.manager.shared.event.Event;

import java.util.logging.Logger;

public class EventService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(EventService.class.getName());
    public static final String WEBSOCKET_EVENTS = "events";

    // TODO: Unbounded queue
    public static final String INCOMING_EVENT_QUEUE = "seda://IncomingEvent?multipleConsumers=true&waitForTaskToComplete=NEVER";
    // TODO: Unbounded queue
    public static final String OUTGOING_EVENT_QUEUE = "seda://OutgoingEvent?multipleConsumers=true&waitForTaskToComplete=NEVER";

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
                    .routeId("Receive incoming events on WebSocket session(s)")
                    .convertBodyTo(Event.class)
                    .to(EventService.INCOMING_EVENT_QUEUE);

                from(EventService.OUTGOING_EVENT_QUEUE)
                    .routeId("Send outgoing events to WebSocket session(s)")
                    .to("websocket://" + WEBSOCKET_EVENTS);
            }
        });
    }

    @Override
    public void start(Container container) {

    }

    @Override
    public void stop(Container container) {

    }

    public static String getSessionKey(Exchange exchange) {
        return exchange.getIn().getHeader(WebsocketConstants.SESSION_KEY, null, String.class);
    }

    public void sendEvent(String session, Event event) {
        messageBrokerService.getProducerTemplate().sendBodyAndHeader(
            OUTGOING_EVENT_QUEUE,
            event,
            session != null ? WebsocketConstants.SESSION_KEY : WebsocketConstants.SEND_TO_ALL,
            session != null ? session : true
        );
    }

    public void sendEvent(Event event) {
        sendEvent(null, event);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }

}
