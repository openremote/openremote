/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.model.event.shared;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A wrapper for events that require a response that provides a message ID to allow the request and response to be
 * re-associated on the client; the message ID can be any string but should be unique enough to prevent collisions with
 * other pending request-response events from the same client.
 * <p>
 * It is up to the backend to then use this message ID on the reply and to also determine what the reply message should be.
 */
public class EventRequestResponseWrapper<T extends SharedEvent> {

    public static final String MESSAGE_PREFIX = "REQUESTRESPONSE:";

    protected String messageId;
    protected T event;

    @JsonCreator
    public EventRequestResponseWrapper(@JsonProperty("messageId") String messageId, @JsonProperty("event") T event) {
        this.messageId = messageId;
        this.event = event;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public T getEvent() {
        return event;
    }

    public void setEvent(T event) {
        this.event = event;
    }
}
