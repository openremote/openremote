package org.openremote.container.security;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

import java.util.Deque;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;

/**
 * If a client can't set Authorization header (e.g. Javascript websocket API), use a request
 * parameter. This handler grabs the parameter and sets it as a regular header.
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
        next.handleRequest(exchange);
    }
}
