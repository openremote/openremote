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
package org.openremote.component.controller2;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.UriEndpointComponent;

import java.net.URI;
import java.util.Map;

public class Controller2Component extends UriEndpointComponent {

    public static final String DISCOVERY = "discovery";
    public static final String HEADER_COMMAND = Controller2Component.class.getCanonicalName() + ".HEADER_COMMAND";

    protected final Controller2Adapter.Manager adapterManager;

    public Controller2Component(Controller2Adapter.Manager adapterManager) {
        super(Controller2Endpoint.class);
        this.adapterManager = adapterManager;
    }

    public Controller2Component() {
        this(Controller2Adapter.DEFAULT_MANAGER);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        if (remaining == null || remaining.length() == 0) {
            throw new IllegalArgumentException("Required '<IP or host name>[:<port>]' in URI");
        }
        try {
            URI endpointUri = URI.create(uri);
            String host = endpointUri.getHost();
            int port = endpointUri.getPort() > 0 ? endpointUri.getPort() : 8080; // TODO default port?
            boolean discoveryOnly = endpointUri.getPath().equals("/" + DISCOVERY);
            return new Controller2Endpoint(uri, this, adapterManager, host, port, discoveryOnly);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid URI: " + uri);
        }
    }

}
