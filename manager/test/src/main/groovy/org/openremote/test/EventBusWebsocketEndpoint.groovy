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
import org.openremote.model.event.Event
import org.openremote.model.event.bus.EventBus

import javax.websocket.*

class EventBusWebsocketEndpoint extends Endpoint {

    final EventBus eventBus;
    final ObjectMapper eventMapper;
    Session session;

    EventBusWebsocketEndpoint(EventBus eventBus, ObjectMapper eventMapper) {
        this.eventBus = eventBus
        this.eventMapper = eventMapper;
    }

    @Override
    void onOpen(Session session, EndpointConfig config) {
        this.session = session;
        session.addMessageHandler new MessageHandler.Whole<String>() {
            @Override
            void onMessage(String data) {
                Event event = eventMapper.readValue(data, Event.class);
                eventBus.dispatch(event);
            }
        }
    }

    @Override
    void onClose(Session session, CloseReason closeReason) {
        this.session = null;
    }

    public void close() {
        if (session) {
            session.close()
            Thread.sleep 250 // Give the server a chance to end the session before we move on to the next test
        }
    }
}
