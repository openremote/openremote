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
/*
 * Copyright 2015, OpenRemote Inc.
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

import org.apache.camel.Converter;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConverters;
import org.openremote.container.Container;
import org.openremote.model.event.shared.CancelEventSubscription;
import org.openremote.model.event.shared.EventSubscription;
import org.openremote.model.event.shared.SharedEvent;

import java.util.logging.Logger;

public class EventTypeConverters implements TypeConverters {

    private static final Logger LOG = Logger.getLogger(EventTypeConverters.class.getName());

    @Converter
    public String writeEvent(SharedEvent event, Exchange exchange) throws Exception {
        return SharedEvent.MESSAGE_PREFIX + Container.JSON.writeValueAsString(event);
    }

    @Converter
    public SharedEvent readEvent(String string, Exchange exchange) throws Exception {
        if (!string.startsWith(SharedEvent.MESSAGE_PREFIX))
            return null;
        string = string.substring(SharedEvent.MESSAGE_PREFIX.length());
        return Container.JSON.readValue(string, SharedEvent.class);
    }

    @Converter
    public EventSubscription readEventSubscription(String string, Exchange exchange) throws Exception {
        if (!string.startsWith(EventSubscription.MESSAGE_PREFIX))
            return null;
        string = string.substring(EventSubscription.MESSAGE_PREFIX.length());
        return Container.JSON.readValue(string, EventSubscription.class);
    }

    @Converter
    public CancelEventSubscription readCancelEventSubscription(String string, Exchange exchange) throws Exception {
        if (!string.startsWith(CancelEventSubscription.MESSAGE_PREFIX))
            return null;
        string = string.substring(CancelEventSubscription.MESSAGE_PREFIX.length());
        return Container.JSON.readValue(string, CancelEventSubscription.class);
    }
}