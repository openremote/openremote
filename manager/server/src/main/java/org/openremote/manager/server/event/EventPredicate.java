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
package org.openremote.manager.server.event;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.openremote.manager.shared.event.Event;

public class EventPredicate<T extends Event> implements Predicate {

    final protected Class<T> eventType;

    public EventPredicate(Class<T> eventType) {
        this.eventType = eventType;
    }

    @Override
    public boolean matches(Exchange exchange) {
        return exchange.getIn().getBody() != null
            && eventType.isAssignableFrom(exchange.getIn().getBody().getClass());
    }
}
