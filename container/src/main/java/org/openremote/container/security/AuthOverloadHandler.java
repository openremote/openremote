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
package org.openremote.container.security;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

import java.util.Deque;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.openremote.model.Constants.REALM_PARAM_NAME;

/**
 * If a client can't set Authorization header (e.g. Javascript websocket API), use a request
 * parameter. This handler grabs the parameter and sets it as a regular header. This handler
 * will also grab {@link org.openremote.model.Constants#REALM_PARAM_NAME} as
 * a request parameter and set it as a regular header.
 */
public class AuthOverloadHandler implements HttpHandler {

    final protected HttpHandler next;

    public AuthOverloadHandler(HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Deque<String> authParameter = exchange.getQueryParameters().get(AUTHORIZATION);
        if (authParameter != null && authParameter.peekFirst() != null) {
            exchange.getRequestHeaders().put(HttpString.tryFromString(AUTHORIZATION), authParameter.pollFirst());
        }

        Deque<String> authRealmParameter = exchange.getQueryParameters().get(REALM_PARAM_NAME);
        if (authRealmParameter != null && authRealmParameter.peekFirst() != null) {
            exchange.getRequestHeaders().put(HttpString.tryFromString(REALM_PARAM_NAME), authRealmParameter.pollFirst());
        }

        next.handleRequest(exchange);
    }
}
