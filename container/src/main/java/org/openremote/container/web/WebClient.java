/*
 * Copyright 2016, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.container.web;

import java.net.URI;

import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.jboss.resteasy.plugins.interceptors.GZIPDecodingInterceptor;
import org.openremote.container.json.JacksonConfig;
import org.openremote.container.security.BearerAuthClientRequestFilter;
import org.openremote.container.security.ClientSecretRequestFilter;

import jakarta.ws.rs.client.Client;

public interface WebClient {

  String REQUEST_PROPERTY_ACCESS_TOKEN = WebClient.class.getName() + ".accessToken";
  String REQUEST_PROPERTY_CLIENT_ID = WebClient.class.getName() + ".clientId";
  String REQUEST_PROPERTY_CLIENT_SECRET = WebClient.class.getName() + ".clientSecret";
  String REQUEST_PROPERTY_X_FORWARDED_FOR = WebClient.class.getName() + ".xForwardedFor";
  String REQUEST_PROPERTY_X_FORWARDED_HOST = WebClient.class.getName() + ".xForwardedHost";
  String REQUEST_PROPERTY_X_FORWARDED_PROTO = WebClient.class.getName() + ".xForwardedProto";
  String REQUEST_PROPERTY_X_FORWARDED_PORT = WebClient.class.getName() + ".xForwardedPort";

  static ResteasyClientBuilderImpl registerDefaults(ResteasyClientBuilderImpl builder) {
    return builder
        .register(new JacksonConfig())
        .register(new ProxyClientRequestFilter())
        .register(new BearerAuthClientRequestFilter())
        .register(new ClientSecretRequestFilter())
        .register(new GZIPDecodingInterceptor());
  }

  static ResteasyWebTarget getTarget(
      Client client, URI uri, String accessToken, String forwardFor, URI forwardUri) {
    return getTarget(
        client,
        uri,
        accessToken,
        forwardFor,
        forwardUri != null ? forwardUri.getHost() : null,
        forwardUri != null ? forwardUri.getScheme() : null,
        forwardUri != null ? forwardUri.getPort() : null);
  }

  static ResteasyWebTarget getTarget(
      Client client,
      URI uri,
      String accessToken,
      String forwardFor,
      String forwardHost,
      String forwardProto,
      Integer forwardPort) {
    ResteasyWebTarget target = ((ResteasyWebTarget) client.target(uri));
    if (accessToken != null) {
      target.property(REQUEST_PROPERTY_ACCESS_TOKEN, accessToken);
    }
    if (forwardFor != null) {
      target.property(REQUEST_PROPERTY_X_FORWARDED_FOR, forwardFor);
    }
    if (forwardHost != null) {
      target.property(REQUEST_PROPERTY_X_FORWARDED_HOST, forwardHost);
    }
    if (forwardProto != null) {
      target.property(REQUEST_PROPERTY_X_FORWARDED_PROTO, forwardProto);
    }
    if (forwardPort != null) {
      target.property(REQUEST_PROPERTY_X_FORWARDED_PORT, forwardPort);
    }
    return target;
  }

  static ResteasyWebTarget getTarget(Client client, URI uri, String clientId, String clientSecret) {
    ResteasyWebTarget target = getTarget(client, uri, null, null, null, null, null);
    if (clientId != null) {
      target.property(REQUEST_PROPERTY_CLIENT_ID, clientId);
    }
    if (clientSecret != null) {
      target.property(REQUEST_PROPERTY_CLIENT_SECRET, clientSecret);
    }
    return target;
  }

  static <T> T getTargetResource(ResteasyWebTarget target, Class<T> resourceType) {
    return target.proxy(resourceType);
  }
}
