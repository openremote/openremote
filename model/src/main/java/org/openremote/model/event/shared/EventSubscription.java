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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.openremote.model.event.Event;

/**
 * A consumer can subscribe to {@link Event}s on the server, providing the
 * type of event it wants to receive as well as filter criteria to restrict the
 * events to an interesting subset.
 * <p>
 * A subscription can optionally contain a {@link #subscriptionId} which allows a client
 * to have multiple subscriptions for the same event type.
 */
public class EventSubscription<E extends Event> {

    public static final String SUBSCRIBE_MESSAGE_PREFIX = "SUBSCRIBE:";
    public static final String SUBSCRIBED_MESSAGE_PREFIX = "SUBSCRIBED:";

    protected String eventType;
    protected EventFilter<E> filter;
    protected String subscriptionId;
    @JsonIgnore
    protected boolean subscribed;

    protected EventSubscription() {
    }

    public EventSubscription(String eventType) {
        this.eventType = eventType;
    }

    public EventSubscription(Class<E> eventClass) {
        this(Event.getEventType(eventClass));
    }

    public EventSubscription(String eventType, EventFilter<E> filter) {
        this.eventType = eventType;
        this.filter = filter;
    }

    public EventSubscription(Class<E> eventClass, EventFilter<E> filter) {
        this.eventType = Event.getEventType(eventClass);
        this.filter = filter;
    }

    public EventSubscription(Class<E> eventClass, EventFilter<E> filter, String subscriptionId) {
        this.eventType = Event.getEventType(eventClass);
        this.filter = filter;
        this.subscriptionId = subscriptionId;
    }

    public String getEventType() {
        return eventType == null ? "" : eventType;
    }

    public EventFilter<E> getFilter() {
        return filter;
    }

    public void setFilter(EventFilter<E> filter) {
        this.filter = filter;
    }

    public boolean isEventType(Class<? extends Event> eventClass) {
        return Event.getEventType(eventClass).equals(getEventType());
    }

    public String getSubscriptionId() {
        return subscriptionId == null ? "" : subscriptionId;
    }

    public void setSubscribed(boolean subscribed) {
        this.subscribed = subscribed;
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "eventType='" + eventType + '\'' +
            ", filter=" + filter +
            ", subscriptionId='" + subscriptionId + '\'' +
            '}';
    }
}
