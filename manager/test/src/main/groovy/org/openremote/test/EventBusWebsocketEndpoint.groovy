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
package org.openremote.test

import com.fasterxml.jackson.databind.ObjectMapper
import org.openremote.model.event.bus.EventBus
import org.openremote.model.event.shared.CancelEventSubscription
import org.openremote.model.event.shared.EventSubscription
import org.openremote.model.event.shared.SharedEvent

import javax.websocket.*

class EventBusWebsocketEndpoint extends Endpoint {

    final EventBus eventBus;
    final ObjectMapper objectMapper;
    Session session;

    EventBusWebsocketEndpoint(EventBus eventBus, ObjectMapper objectMapper) {
        this.eventBus = eventBus
        this.objectMapper = objectMapper;
    }

    @Override
    void onOpen(Session session, EndpointConfig config) {
        this.session = session;
        session.addMessageHandler new MessageHandler.Whole<String>() {
            @Override
            void onMessage(String data) {
                if (data == null || !data.startsWith(SharedEvent.MESSAGE_PREFIX))
                    return;
                data = data.substring(SharedEvent.MESSAGE_PREFIX.length());
                SharedEvent event = objectMapper.readValue(data, SharedEvent.class);
                try {
                    eventBus.dispatch(event);
                } catch (Throwable t) {
                    System.err.println t
                    t.printStackTrace(System.err)
                }
            }
        }
    }

    @Override
    void onClose(Session session, CloseReason closeReason) {
        this.session = null;
    }

    void send(SharedEvent sharedEvent) {
        if (session == null || !session.isOpen())
            throw new IllegalStateException("Session not open")
        session.basicRemote.sendText(SharedEvent.MESSAGE_PREFIX + objectMapper.writeValueAsString(sharedEvent));
    }

    void sendSubscription(EventSubscription subscription) {
        if (session == null || !session.isOpen())
            throw new IllegalStateException("Session not open")
        final String data = EventSubscription.MESSAGE_PREFIX + objectMapper.writeValueAsString(subscription);
        session.basicRemote.sendText(data);
    }

    void sendCancelSubscription(CancelEventSubscription cancelSubscription) {
        if (session == null || !session.isOpen())
            throw new IllegalStateException("Session not open")
        final String data = CancelEventSubscription.MESSAGE_PREFIX + objectMapper.writeValueAsString(cancelSubscription);
        session.basicRemote.sendText(data);
    }

    void close() {
        if (session && session.isOpen()) {
            session.close()
            Thread.sleep 250 // Give the server a chance to end the session before we move on to the next test
        }
    }
}
