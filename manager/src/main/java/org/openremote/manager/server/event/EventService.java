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

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.bean.AmbiguousMethodCallException;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerContext;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.web.socket.IsUserInRole;
import org.openremote.container.web.socket.WebsocketAuth;
import org.openremote.container.web.socket.WebsocketConstants;
import org.openremote.manager.shared.event.Event;
import org.openremote.manager.shared.event.Message;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.camel.builder.PredicateBuilder.not;

public class EventService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(EventService.class.getName());
    public static final String WEBSOCKET_EVENTS = "events";

    public static final String INCOMING_EVENT_QUEUE = "seda://incomingEvent?multipleConsumers=true&waitForTaskToComplete=NEVER";
    public static final String OUTGOING_EVENT_QUEUE = "seda://outgoingEvent?multipleConsumers=true&waitForTaskToComplete=NEVER";

    protected ProducerTemplate producerTemplate;

    @Override
    public void init(Container container) throws Exception {

    }

    @Override
    public void configure(Container container) throws Exception {
        MessageBrokerService messageBrokerService = container.getService(MessageBrokerService.class);
        MessageBrokerContext context = messageBrokerService.getContext();

        context.getTypeConverterRegistry().addTypeConverters(new EventTypeConverters());

        producerTemplate = context.createProducerTemplate();

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                // TODO This is only a simple example for role checking in routes
                interceptFrom("websocket://" + WEBSOCKET_EVENTS)
                    .when(not(new IsUserInRole("read")))
                    .to("log:org.openremote.event.forbidden?level=INFO&showAll=true&multiline=true")
                    .stop();

                from("websocket://" + WEBSOCKET_EVENTS)
                    .routeId("Receive incoming events on WebSocket")
                    .convertBodyTo(Event.class)
                    .to(EventService.INCOMING_EVENT_QUEUE);

                from(EventService.INCOMING_EVENT_QUEUE)
                    .routeId("Handle incoming events")
                    .doTry()
                    .bean(EventService.this, "onEvent")
                    .doCatch(AmbiguousMethodCallException.class) // No overloaded method for given event subtype
                    .log("log:org.openremote.event.unhandled?level=DEBUG&showCaughtException=false&showBodyType=false&showExchangePattern=false&showStackTrace=false")
                    .stop()
                    .endDoTry();

                from(EventService.OUTGOING_EVENT_QUEUE)
                    .routeId("Send outgoing events to all WebSocket sessions")
                    .to("websocket://" + WEBSOCKET_EVENTS + "?sendToAll=true");
            }
        });
    }

    @Override
    public void start(Container container) {

    }

    @Override
    public void stop(Container container) {

    }

    public void sendEvent(Event event) {
        if (LOG.isLoggable(Level.FINE))
            LOG.fine("Sending: " + event);
        producerTemplate.sendBody(OUTGOING_EVENT_QUEUE, event);
    }

    public void onEvent(Message message) {
        if (LOG.isLoggable(Level.FINE))
            LOG.fine("### On incoming message event: " + message);

        sendEvent(new Message("Hello from server, the time is: " + System.currentTimeMillis()));
    }

}
