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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Converter;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConverters;
import org.openremote.container.Container;
import org.openremote.manager.shared.event.Event;
import org.slf4j.LoggerFactory;

public class EventTypeConverters implements TypeConverters {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(EventTypeConverters.class);


    @Converter
    public String writeEvent(Event event, Exchange exchange) throws Exception {
        LOG.trace("Writing event JSON: " + event);
        return getObjectMapper(exchange).writeValueAsString(event);
    }

    @Converter
    public Event readEvent(String string, Exchange exchange) throws Exception {
        LOG.trace("Reading event JSON: " + string);
        return getObjectMapper(exchange).readValue(string, Event.class);
    }

    protected static ObjectMapper getObjectMapper(Exchange exchange) {
        Container container = (Container) exchange.getContext().getRegistry().lookupByName(Container.class.getName());
        return container.JSON;
    }}