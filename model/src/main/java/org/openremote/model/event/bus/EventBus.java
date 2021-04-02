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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple observer/observable implementation, thread-safe.
 * <p>
 * Listeners must match the event type (as in {@link Event#getEventType}) exactly, a listener for an
 * event supertype will not receive events of a subtype.
 * <p>
 * Listeners can be registered to execute in the <em>prepare</em> phase, and cancel dispatch by
 * throwing a {@link VetoEventException}.
 */
public class EventBus {

    private static final Logger LOG = Logger.getLogger(EventBus.class.getName());

    final protected List<EventRegistration<?>> registrations = new ArrayList<>();

    public List<EventRegistration<?>> getRegistrations() {
        return Collections.unmodifiableList(registrations);
    }

    public <E extends Event> EventRegistration<E> register(Class<E> eventClass, EventListener<E> listener) {
        return register(false, eventClass, listener);
    }

    public <E extends Event> EventRegistration<E> register(boolean prepare, Class<E> eventClass, EventListener<E> listener) {
        EventRegistration<E> registration = new EventRegistration<>(prepare, eventClass, listener);
        add(registration);
        return registration;
    }

    public void addAll(List<EventRegistration<?>> registrations) {
        synchronized (this.registrations) {
            this.registrations.addAll(registrations);
        }
    }

    public void add(EventRegistration<?> registration) {
        synchronized (this.registrations) {
            this.registrations.add(registration);
        }
    }

    public void removeAll(Collection<EventRegistration<?>> registrations) {
        synchronized (this.registrations) {
            this.registrations.removeAll(registrations);
        }
    }

    public void remove(EventRegistration<?> registration) {
        synchronized (this.registrations) {
            this.registrations.remove(registration);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Event> void dispatch(T event) {
        synchronized (this.registrations) {
            boolean vetoed = false;

            // Call all "prepare" phase listeners, find out if any event is vetoed
            List<EventRegistration<?>> activePrepareRegistrations = new ArrayList<>();
            for (EventRegistration<?> registration : registrations) {
                if (!registration.isMatching(event) || !registration.isPrepare())
                    continue;
                activePrepareRegistrations.add(registration);
            }
            // Only notify listeners after iterating registrations, some listeners might modify registrations!
            for (EventRegistration<?> activePrepareRegistration : activePrepareRegistrations) {
                if (activePrepareRegistration.isMatching(event)) {
                    try {
                        ((EventRegistration<T>)activePrepareRegistration).getListener().on(event);
                    } catch (VetoEventException ex) {
                        vetoed = true;
                        break;
                    }
                }
            }

            // If event is not vetoed, call all "non-prepare" listeners
            if (!vetoed) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Dispatching event: " + event);
                }
                List<EventRegistration<?>> activeRegistrations = new ArrayList<>();
                for (EventRegistration<?> registration : registrations) {
                    if (!registration.isMatching(event) || registration.isPrepare())
                        continue;
                    activeRegistrations.add(registration);
                }
                // Only notify listeners after iterating registrations, some listeners might modify registrations!
                for (EventRegistration<?> activeRegistration : activeRegistrations) {
                    if (activeRegistration.isMatching(event)) {
                        ((EventRegistration<T>)activeRegistration).getListener().on(event);
                    }
                }
            } else if (LOG.isLoggable(Level.FINE)) {
                LOG.finer("Dropping vetoed event: " + event);
            }
        }
    }

}
