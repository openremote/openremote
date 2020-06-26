/*
 * Copyright 2017, OpenRemote Inc.
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
 * but WITHOUT ANY WARRANTY without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.test

import com.fasterxml.jackson.databind.ObjectMapper
import org.openremote.app.client.event.SubscriptionFailureEvent
import org.openremote.app.client.event.EventService
import org.openremote.model.event.Event
import org.openremote.model.event.TriggeredEventSubscription
import org.openremote.model.event.bus.EventBus
import org.openremote.model.event.shared.*

import javax.websocket.*

/**
 * Does the same job as {@link org.openremote.app.client.event.EventServiceImpl}.
 */
class ClientEventService implements EventService {

    final protected EventBus eventBus
    final protected ObjectMapper objectMapper
    final protected EventBusWebsocketEndpoint endpoint

    class EventBusWebsocketEndpoint extends Endpoint {

        Session session

        @Override
        void onOpen(Session session, EndpointConfig config) {
            this.session = session
            session.addMessageHandler new MessageHandler.Whole<String>() {
                @Override
                void onMessage(String data) {
                    if (data == null || data.length() == 0)
                        return
                    try {
                        if (data.startsWith(UnauthorizedEventSubscription.MESSAGE_PREFIX)) {
                            data = data.substring(UnauthorizedEventSubscription.MESSAGE_PREFIX.length())
                            UnauthorizedEventSubscription failure = objectMapper.readValue(data, UnauthorizedEventSubscription.class)
                            eventBus.dispatch(new SubscriptionFailureEvent(failure.subscription.getEventType()))
                        } else if (data.startsWith(TriggeredEventSubscription.MESSAGE_PREFIX)) {
                            data = data.substring(TriggeredEventSubscription.MESSAGE_PREFIX.length())

                            if (data.startsWith("{")) {
                                TriggeredEventSubscription event = objectMapper.readValue(data, TriggeredEventSubscription.class)
                                if (event.events != null) {
                                    event.events.forEach{ Event evt -> eventBus.dispatch(evt)}
                                }
                            }
                        } else if (data.startsWith(SharedEvent.MESSAGE_PREFIX)) {
                            data = data.substring(SharedEvent.MESSAGE_PREFIX.length())

                            if (data.startsWith("[")) {
                                // Handle array of events
                                SharedEvent[] events = objectMapper.readValue(data, SharedEvent[].class)
                                for (SharedEvent event : events) {
                                    eventBus.dispatch(event)
                                }
                            }
                        }
                    } catch (Throwable t) {
                        // We don't see problems in tests if we don't log it here
                        System.err.println t
                        t.printStackTrace(System.err)
                    }
                }
            }
        }

        @Override
        void onClose(Session session, CloseReason closeReason) {
            this.session = null
        }

        void send(SharedEvent sharedEvent) {
            if (session == null || !session.isOpen())
                throw new IllegalStateException("Session not open")
            session.basicRemote.sendText(SharedEvent.MESSAGE_PREFIX + objectMapper.writeValueAsString(sharedEvent))
        }

        void sendSubscription(EventSubscription subscription) {
            if (session == null || !session.isOpen())
                throw new IllegalStateException("Session not open")
            final String data = EventSubscription.SUBSCRIBE_MESSAGE_PREFIX + objectMapper.writeValueAsString(subscription)
            session.basicRemote.sendText(data)
        }

        void sendCancelSubscription(CancelEventSubscription cancelSubscription) {
            if (session == null || !session.isOpen())
                throw new IllegalStateException("Session not open")
            final String data = CancelEventSubscription.MESSAGE_PREFIX + objectMapper.writeValueAsString(cancelSubscription)
            session.basicRemote.sendText(data)
        }

        void close() {
            if (session && session.isOpen()) {
                session.close()
                Thread.sleep 250 // Give the server a chance to end the session before we move on to the next test
            }
        }
    }

    ClientEventService(EventBus eventBus, ObjectMapper objectMapper) {
        this.eventBus = eventBus
        this.objectMapper = objectMapper
        this.endpoint = new EventBusWebsocketEndpoint()
    }

    EventBusWebsocketEndpoint getEndpoint() {
        return endpoint
    }

    @Override
    <E extends SharedEvent> void subscribe(Class<E> eventClass) {
        endpoint.sendSubscription(new EventSubscription(eventClass))
    }

    @Override
    <E extends SharedEvent> void subscribe(Class<E> eventClass, EventFilter<E> filter) {
        endpoint.sendSubscription(new EventSubscription(eventClass, filter))
    }

    @Override
    <E extends SharedEvent> void unsubscribe(Class<E> eventClass) {
        endpoint.sendCancelSubscription(new CancelEventSubscription(eventClass))
    }

    @Override
    void dispatch(SharedEvent sharedEvent) {
        endpoint.send(sharedEvent)
    }

    @Override
    void stop() {
        close()
    }

    void close() {
        endpoint.close()
    }
}
