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
 * A client can subscribe to {@link SharedEvent}s on the server, providing the
 * type of event it wants to receive as well as filter criteria to restrict the
 * events to an interesting subset.
 * <p>
 * Subscriptions must be refreshed by the client every {@link #RENEWAL_PERIOD_SECONDS}
 * or the server will expire and remove the subscription.
 */
public class EventSubscription<E extends SharedEvent> {

    public static final String MESSAGE_PREFIX = "SUBSCRIBE";
    public static final int RENEWAL_PERIOD_SECONDS = 30;

    protected String eventType;
    protected EventFilter<E> filter;

    protected EventSubscription() {
    }

    public EventSubscription(String eventType) {
        this.eventType = eventType;
    }

    public EventSubscription(Class<E> eventClass) {
        this(Event.getEventType(eventClass));
    }

    public EventSubscription(Class<E> eventClass, EventFilter<E> filter) {
        this(Event.getEventType(eventClass), filter);
    }

    public EventSubscription(String eventType, EventFilter<E> filter) {
        this.eventType = eventType;
        this.filter = filter;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public EventFilter<E> getFilter() {
        return filter;
    }

    public void setFilter(EventFilter<E> filter) {
        this.filter = filter;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "eventType='" + eventType + '\'' +
            ", filter=" + filter +
            '}';
    }
}
