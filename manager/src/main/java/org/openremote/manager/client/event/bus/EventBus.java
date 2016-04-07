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
package org.openremote.manager.client.event.bus;

import org.openremote.manager.shared.event.Event;

import java.util.*;

/**
 * Simple observer/observable implementation.
 */
public class EventBus {

    final protected List<EventRegistration> registrations = new ArrayList<>();

    synchronized public List<EventRegistration> getRegistrations() {
        return Collections.unmodifiableList(registrations);
    }

    synchronized public <E extends Event> EventRegistration<E> register(Class<E> eventClass, EventListener<E> listener) {
        return register(false, eventClass, listener);
    }

    synchronized public <E extends Event> EventRegistration<E> register(boolean prepare, Class<E> eventClass, EventListener<E> listener) {
        EventRegistration<E> registration = new EventRegistration<E>(prepare, eventClass, listener);
        add(registration);
        return registration;
    }

    synchronized public void addAll(List<EventRegistration> registrations) {
        this.registrations.addAll(registrations);
    }

    synchronized public void add(EventRegistration registration) {
        this.registrations.add(registration);
    }

    synchronized public void removeAll(Collection<EventRegistration> registrations) {
        this.registrations.removeAll(registrations);
    }

    synchronized public void remove(EventRegistration registration) {
        this.registrations.remove(registration);
    }

    @SuppressWarnings("unchecked")
    synchronized public void dispatch(Event event, EventRegistration... skipRegistrations) {
        boolean vetoed = false;
        List<EventRegistration> skips = skipRegistrations != null ? Arrays.asList(skipRegistrations) : Collections.EMPTY_LIST;

        // Call all "prepare" phase listeners, find out if any event is vetoed
        for (EventRegistration registration : registrations) {
            if (skips.contains(registration))
                continue;

            if (!registration.isMatching(event) || !registration.isPrepare())
                continue;
            try {
                registration.getListener().on(event);
            } catch (VetoEventException ex) {
                vetoed = true;
                break;
            }
        }
        // If event is not vetoed, call all "non-prepare" listeners
        if (!vetoed) {
            for (EventRegistration registration : registrations) {
                if (skips.contains(registration))
                    continue;
                if (!registration.isMatching(event) || registration.isPrepare())
                    continue;
                registration.getListener().on(event);
            }
        }
    }

}
