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
package org.openremote.container.security;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.openremote.model.Constants.REALM_PARAM_NAME;

import java.util.Deque;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

/**
 * For use by websocket requests where Authorization header cannot be set; grabs the {@link
 * jakarta.ws.rs.core.HttpHeaders#AUTHORIZATION} query parameter and sets it as a regular header.
 * This handler will also grab {@link org.openremote.model.Constants#REALM_PARAM_NAME} as a request
 * parameter and set it as a regular header.
 */
public class WebsocketAuthParamHandler implements HttpHandler {

  protected final HttpHandler next;

  public WebsocketAuthParamHandler(HttpHandler next) {
    this.next = next;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    Deque<String> authParameter = exchange.getQueryParameters().get(AUTHORIZATION);
    if (authParameter != null && authParameter.peekFirst() != null) {
      exchange
          .getRequestHeaders()
          .put(HttpString.tryFromString(AUTHORIZATION), authParameter.pollFirst());
    }

    Deque<String> authRealmParameter = exchange.getQueryParameters().get(REALM_PARAM_NAME);
    if (authRealmParameter != null && authRealmParameter.peekFirst() != null) {
      exchange
          .getRequestHeaders()
          .put(HttpString.tryFromString(REALM_PARAM_NAME), authRealmParameter.pollFirst());
    }

    next.handleRequest(exchange);
  }
}
