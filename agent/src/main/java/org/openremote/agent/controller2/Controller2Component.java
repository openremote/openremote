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
import org.apache.camel.impl.UriEndpointComponent;

import java.net.URI;
import java.net.URL;
import java.util.Map;

public class Controller2Component extends UriEndpointComponent {

    public static final String URI_SYNTAX = "'controller2://<IP or host name>:<port>/([<device URI>/<resource URI>]|[discovery|inventory])[?authUsername=username&authPassword=secret]";

    public static final String HEADER_DEVICE_URI = Controller2Component.class.getCanonicalName() + ".HEADER_DEVICE_URI";
    public static final String HEADER_RESOURCE_URI = Controller2Component.class.getCanonicalName() + ".HEADER_RESOURCE_URI";
    public static final String HEADER_COMMAND_VALUE = Controller2Component.class.getCanonicalName() + ".HEADER_COMMAND_VALUE";

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
        try {
            URI endpointUri = URI.create(remaining);
            if (endpointUri.getPort() <= 0) {
                throw new Exception("Invalid port number");
            }

            URL controllerUrl = new URL(endpointUri.getScheme(), endpointUri.getHost(), endpointUri.getPort(), "/controller");
            Endpoint ep = new Controller2Endpoint(uri, this, adapterManager, controllerUrl, endpointUri.getPath());
            setProperties(ep, parameters);
            return ep;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Required URL in format of " + URI_SYNTAX, ex);
        }
    }
}
