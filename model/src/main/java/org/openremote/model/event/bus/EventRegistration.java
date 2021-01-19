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
package org.openremote.model.event.bus;

import org.openremote.model.event.Event;

import javax.validation.constraints.NotNull;

/**
 * Handle for receiving events from {@link EventBus}.
 */
public class EventRegistration<E extends Event> {

    final public boolean prepare;
    final public Class<E> eventClass;
    final public EventListener<E> listener;

    public EventRegistration(EventListener<E> listener) {
        this(false, listener);
    }

    public EventRegistration(Class<E> eventClass, EventListener<E> listener) {
        this(false, eventClass, listener);
    }

    @SuppressWarnings("unchecked")
    public EventRegistration(boolean prepare, EventListener<E> listener) {
        this(prepare, (Class<E>)Event.class, listener);
    }

    public EventRegistration(boolean prepare, @NotNull Class<E> eventClass, EventListener<E> listener) {
        this.prepare = prepare;
        this.eventClass = eventClass;
        this.listener = listener;
    }

    public boolean isPrepare() {
        return prepare;
    }

    public Class<E> getEventClass() {
        return eventClass;
    }

    public String getEventType() {
        return Event.getEventType(getEventClass());
    }

    public EventListener<E> getListener() {
        return listener;
    }

    public boolean isMatching(@NotNull Event event) {
        return getEventClass().isAssignableFrom(event.getClass());
    }
}
