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
package org.openremote.container.web;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.openremote.container.Container;
import org.openremote.container.json.ElementalMessageBodyConverter;
import org.openremote.container.json.JacksonConfig;
import org.openremote.container.security.BearerAuthClientRequestFilter;
import org.openremote.container.security.ClientSecretRequestFilter;

import javax.ws.rs.client.Client;
import java.net.URI;

public interface WebClient {

    static ResteasyClientBuilder registerDefaults(Container container, ResteasyClientBuilder builder) {
        return builder
            .register(new JacksonConfig(container))
            .register(ElementalMessageBodyConverter.class)
            .register(new BearerAuthClientRequestFilter())
            .register(new ClientSecretRequestFilter());
    }

    static ResteasyWebTarget getTarget(Client client, URI uri) {
        return getTarget(client, uri, null);
    }

    static ResteasyWebTarget getTarget(Client client, URI uri, String accessToken) {
        ResteasyWebTarget target = ((ResteasyWebTarget) client.target(uri));
        if (accessToken != null) {
            target.property("accessToken", accessToken);
        }
        return target;
    }

    static ResteasyWebTarget getTarget(Client client, URI uri, String clientId, String clientSecret) {
        ResteasyWebTarget target = getTarget(client, uri, null);
        if (clientId != null) {
            target.property("clientId", clientId);
        }
        if (clientSecret != null) {
            target.property("clientSecret", clientSecret);
        }
        return target;
    }

    static <T> T getTargetResource(ResteasyWebTarget target, Class<T> resourceType) {
        return target.proxy(resourceType);
    }
}
