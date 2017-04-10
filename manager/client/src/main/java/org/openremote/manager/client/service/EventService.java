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
package org.openremote.manager.client.service;

import org.openremote.model.event.shared.EventFilter;
import org.openremote.model.event.shared.SharedEvent;

/**
 * Communicate with the server through {@link SharedEvent}s.
 * <p>
 * Call {@link #subscribe} and {@link #unsubscribe} to tell the server which
 * events you would like to receive. Register on the
 * {@link org.openremote.model.event.bus.EventBus} to actually receive the events.
 * <p>
 * If your subscription fails, a {@link org.openremote.manager.client.event.SubscriptionFailureEvent}
 * will be dispatched on the event bus.
 * <p>
 * Events can be send to the server with {@link #dispatch}. This is how the client
 * sends events to the server, because dispatching events on the local event bus
 * will not send them to the server.
 */
public interface EventService {

    void connect();

    <E extends SharedEvent> void subscribe(Class<E> eventClass);

    <E extends SharedEvent> void subscribe(Class<E> eventClass, EventFilter<E> filter);

    <E extends SharedEvent> void unsubscribe(Class<E> eventClass);

    void dispatch(SharedEvent sharedEvent);
}
