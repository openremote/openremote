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
import io.undertow.servlet.api.ExceptionHandler;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;

/**
 * Unified exception handling for all web services (Resteasy, Undertow, Servlets, WebSockets).
 * <p>
 * This is naturally quite messy, our goal is to disable all the other default exception
 * handling and do it here in a "simple" way.
 * <p>
 * In production we want to be INFOrmed of exceptions but only log a stacktrace if FINE
 * debug logging is enabled. We never want to send a stacktrace or some crappy HTML to a
 * client in production.
 * <p>
 * TODO: Fine-grained logging for exception, filter which should be logged at INFO or WARN in production
 * TODO: Websocket session errors
 */
public class WebServiceExceptions {

    private static final Logger LOG = Logger.getLogger(WebServiceExceptions.class.getName());

    @Provider
    public static class DefaultResteasyExceptionMapper implements ExceptionMapper<Exception> {
        @Context
        protected Request request;

        @Context
        protected UriInfo uriInfo;

        final protected boolean devMode;

        public DefaultResteasyExceptionMapper(boolean devMode) {
            this.devMode = devMode;
        }

        @Override
        public Response toResponse(Exception exception) {
            return handleResteasyException(devMode, "RESTEasy Dispatch", request, uriInfo, exception);
        }
    }

    // bburke hardcoded a response entity in "new ForbiddenException", which means we can't handle it anymore,
    // according to JAX-RS rules. however, he then checks if there is an exact matching handler, so it's
    // still possible...
    @Provider
    public static class ForbiddenResteasyExceptionMapper implements ExceptionMapper<ForbiddenException> {

        @Context
        protected Request request;

        @Context
        protected UriInfo uriInfo;

        final protected boolean devMode;

        public ForbiddenResteasyExceptionMapper(boolean devMode) {
            this.devMode = devMode;
        }

        @Override
        public Response toResponse(ForbiddenException exception) {
            return handleResteasyException(devMode, "RESTEasy Role Security", request, uriInfo, exception);
        }
    }

    public static class RootUndertowExceptionHandler extends io.undertow.server.handlers.ExceptionHandler {
        final protected boolean devMode;

        public RootUndertowExceptionHandler(boolean devMode, HttpHandler next) {
            super(next);
            this.devMode = devMode;
            addExceptionHandler(Throwable.class, (HttpServerExchange exchange) -> {
                // Get the exception that was thrown
                Throwable throwable = exchange.getAttachment(io.undertow.server.handlers.ExceptionHandler.THROWABLE);

                LOG.log(Level.SEVERE, "Captured uncaught exception in Undertow chain:", throwable);

                // 3. Send a clean response to the client
                if (!exchange.isResponseStarted()) {
                    exchange.setStatusCode(500);
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    exchange.getResponseSender().send("Internal Server Error");
                }
            });
        }
    }

    public static class ServletUndertowExceptionHandler implements ExceptionHandler {
        final protected boolean devMode;

        public ServletUndertowExceptionHandler(boolean devMode) {
            this.devMode = devMode;
        }

        @Override
        public boolean handleThrowable(HttpServerExchange exchange, ServletRequest request, ServletResponse response, Throwable throwable) {
            LOG.log(Level.SEVERE, "Servlet exception captured deployment=" +
                    request.getServletContext().getServletContextName() throwable);

            // 3. Send a clean response to the client
            if (!exchange.isResponseStarted()) {
                exchange.setStatusCode(500);
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                exchange.getResponseSender().send("Internal Server Error");
            }

            return true;
        }
    }

    public static Response handleResteasyException(boolean devMode, String origin, Request request, UriInfo uriInfo, Throwable throwable) {

        int statusCode = 500;

        if (throwable instanceof OptimisticLockException) {
            // We get here if JPA entity id is set on merge but it doesn't already exist (unless ID generator is configured to allow ID to be assigned)
            // Or we get here is JPA entity version doesn't match what's in the DB
            throwable = new NotAllowedException(throwable);
        }

        if (throwable instanceof WebApplicationException) {
            Response response = ((WebApplicationException) throwable).getResponse();
            switch (response.getStatusInfo().getFamily()) {
                case CLIENT_ERROR:
                case SERVER_ERROR:
                    statusCode = response.getStatus();
                    break;
                default:
                    // If it's not a client or server error, it's not really an "exception" to
                    // be handled but a status that should be returned to the client
                    return response;
            }
        }
        try {
            if (devMode) {
                return Response.status(statusCode).entity(renderDevModeError(statusCode, throwable)).type(TEXT_PLAIN_TYPE).build();
            } else {
                return Response.status(statusCode).entity(renderProductionError(statusCode, throwable)).type(TEXT_PLAIN_TYPE).build();
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Couldn't render server error trace response", ex);
            return Response.serverError().build();
        }
    }

    public static void handleUndertowException(boolean devMode, String origin, boolean logOnly, HttpServerExchange exchange, Throwable throwable) {

        if (!logOnly && exchange.isResponseChannelAvailable()) {
            exchange.getResponseHeaders().put(
                HttpString.tryFromString(HttpHeaders.CONTENT_TYPE), "text/plain"
            );
            try {
                if (devMode) {
                    exchange.getResponseSender().send(renderDevModeError(exchange.getStatusCode(), throwable));
                } else {
                    exchange.getResponseSender().send(renderProductionError(exchange.getStatusCode(), throwable));
                }
            } catch (Exception ex2) {
                LOG.log(Level.SEVERE, "Couldn't render server error response", ex2);
            }
        }
    }

    public static String renderDevModeError(int statusCode, Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        Response.Status status = Response.Status.fromStatusCode(statusCode);
        return "Request failed with HTTP error status: "
            + statusCode
            + (status != null ? " " + status.getReasonPhrase() : "")
            + "\n\n"
            + sw.toString();
    }

    public static String renderProductionError(int statusCode, Throwable t) {
        Response.Status status = Response.Status.fromStatusCode(statusCode);
        return "Request failed with HTTP error status: "
            + statusCode
            + (status != null ? " " + status.getReasonPhrase() : "");
    }
}
