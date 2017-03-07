/*
 * Copyright 2017, OpenRemote Inc.
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

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;

/**
 * Unified exception handling for all web services (JAX-RS, Undertow, WebSockets)
 * <p/>
 * In production we want to be INFOrmed of exceptions but only log a stacktrace if FINE
 * debug logging is enabled. We never want to send a stacktrace to a client in production.
 *
 * TODO: The 401 (wrong roles or invalid access token) and 403 (no access token) errors are still the default
 * Servlet handling and bypass both JAX-RS and Undertow handling
 */
@Provider
public class WebServiceExceptions implements ExceptionMapper<Exception>, HttpHandler {

    private static final Logger LOG = Logger.getLogger(WebServiceExceptions.class.getName());

    final protected boolean devMode;

    @Context
    protected Request request;

    @Context
    protected UriInfo uriInfo;

    protected HttpHandler nextHandler;

    public WebServiceExceptions(boolean devMode) {
        this.devMode = devMode;
    }

    public WebServiceExceptions(boolean devMode, HttpHandler nextHandler) {
        this.devMode = devMode;
        this.nextHandler = nextHandler;
    }

    // JAX-RS
    @Override
    public Response toResponse(Exception exception) {

        logException(exception, request.getMethod() + " " + uriInfo.getRequestUri());

        int statusCode = 500;
        if (exception instanceof WebApplicationException) {
            Response response = ((WebApplicationException) exception).getResponse();
            switch (response.getStatusInfo().getFamily()) {
                case INFORMATIONAL:
                case SUCCESSFUL:
                case REDIRECTION:
                case OTHER:
                    return response;
                default:
                    statusCode = response.getStatus();
            }
        }

        try {
            if (devMode) {
                return Response.status(statusCode).entity(renderDevModeError(statusCode, exception)).type(TEXT_PLAIN_TYPE).build();
            } else {
                return Response.status(statusCode).entity(renderProductionError(statusCode, exception)).type(TEXT_PLAIN_TYPE).build();
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Couldn't render server error trace response (in developer mode)", ex);
            return Response.serverError().build();
        }
    }

    // Undertow
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        try {
            if (nextHandler == null)
                throw new IllegalStateException("This Undertow handler must wrap another handler");
            nextHandler.handleRequest(exchange);
        } catch (Throwable t) {

            logException(t, exchange.getRequestMethod() + " " + exchange.getRequestPath());

            if (exchange.isResponseChannelAvailable()) {
                try {
                    if (devMode) {
                        exchange.getResponseSender().send(renderDevModeError(500, t));
                    } else {
                        exchange.getResponseSender().send(renderProductionError(500, t));
                    }
                } catch (Exception ex2) {
                    LOG.log(Level.SEVERE, "Couldn't render server error response (in developer mode)", ex2);
                }
            }
        }
    }

    // WebSockets
    /* TODO Not sure how to do this without coupling everything
    public void handleError(@Observes SessionError sessionError) {
        logException(sessionError.getThrowable(), "WebSocket Session " + sessionError.getSession().getId());
    }
    */

    public String renderDevModeError(int statusCode, Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return "Request failed with HTTP error status: " + statusCode + "\n\n" + sw.toString();
    }

    public String renderProductionError(int statusCode, Throwable t) {
        return "Request failed with HTTP error status: " + statusCode + "\n\nPlease contact the help desk.";
    }

    public void logException(Throwable throwable, String info) {
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Web service exception ('" + info + "')", throwable);
        } else {
            LOG.log(Level.INFO, "Web service exception ('" + info + "'), root cause: " + getRootCause(throwable));
        }
    }

    public static Throwable getRootCause(Throwable throwable) {
        Throwable last;
        do {
            last = throwable;
            throwable = getCause(throwable);
        }
        while (throwable != null);
        return last;
    }

    protected static Throwable getCause(Throwable throwable) {
        // Prevent endless loop when t == t.getCause()
        if (throwable != null && throwable.getCause() != null && throwable != throwable.getCause())
            return throwable.getCause();
        return null;
    }
}
