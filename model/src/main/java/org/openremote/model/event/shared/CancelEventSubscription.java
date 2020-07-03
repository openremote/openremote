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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.model.event.shared;

import org.openremote.model.event.Event;

/**
 * A client can unsubscribe from {@link SharedEvent}s on the server, providing the
 * type of event it doesn't want to receive anymore or the subscription ID to remove
 * a specific subscriber.
 */
public class CancelEventSubscription {

    public static final String MESSAGE_PREFIX = "UNSUBSCRIBE:";

    protected String eventType;

    protected String subscriptionId;

    protected CancelEventSubscription() {
    }

    public <E extends SharedEvent> CancelEventSubscription(Class<E> eventClass) {
        this(Event.getEventType(eventClass), null);
    }

    public <E extends SharedEvent> CancelEventSubscription(Class<E> eventClass, String subscriptionId) {
        this(Event.getEventType(eventClass), subscriptionId);
    }

    public CancelEventSubscription(String eventType, String subscriptionId) {
        this.eventType = eventType;
        this.subscriptionId = subscriptionId;
    }

    public CancelEventSubscription(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "eventType='" + eventType + '\'' +
            "subscriptionId=" + subscriptionId +
            '}';
    }
}
