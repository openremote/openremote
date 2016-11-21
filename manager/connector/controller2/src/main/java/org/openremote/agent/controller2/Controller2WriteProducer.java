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
package org.openremote.agent.controller2;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.apache.camel.util.URISupport;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.manager.shared.connector.ConnectorComponent.HEADER_DEVICE_KEY;
import static org.openremote.manager.shared.connector.ConnectorComponent.HEADER_DEVICE_RESOURCE_KEY;

public class Controller2WriteProducer extends DefaultProducer {

    private static final Logger LOG = Logger.getLogger(Controller2WriteProducer.class.getName());

    public Controller2WriteProducer(Controller2Endpoint endpoint) {
        super(endpoint);
    }

    @Override
    public Controller2Endpoint getEndpoint() {
        return (Controller2Endpoint) super.getEndpoint();
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String deviceKey = exchange.getIn().getHeader(HEADER_DEVICE_KEY, null, String.class);
        String resourceKey = exchange.getIn().getHeader(HEADER_DEVICE_RESOURCE_KEY, null, String.class);
        if (deviceKey == null || "".equals(deviceKey) || resourceKey == null || "".equals(resourceKey)) {
            throw new IllegalArgumentException(
                "Both device and resource key message headers must be defined for write producer"
            );
        }

        Object commandValue = exchange.getIn().getBody();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Writing to '" + deviceKey + " : " + resourceKey+ "' with value: " + commandValue);
        }
        getEndpoint().getAdapter().getControllerState().writeResource(deviceKey, resourceKey, commandValue);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + URISupport.sanitizeUri(getEndpoint().getEndpointUri()) + "]";
    }
}
