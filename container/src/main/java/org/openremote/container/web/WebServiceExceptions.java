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
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import java.io.PrintWriter;
import java.io.StringWriter;

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
 */
public class WebServiceExceptions {

    private static final System.Logger LOG = System.getLogger(WebServiceExceptions.class.getName());

    /**
     * The mapper handles JAX-RS exceptions
     */
    public static class JAXRSExceptionMapper implements ExceptionMapper<Throwable> {
        final protected boolean devMode;

        public JAXRSExceptionMapper(boolean devMode) {
            this.devMode = devMode;
        }

        @Override
        public Response toResponse(Throwable exception) {
            return handleResteasyException(devMode, exception);
        }
    }

    /**
     * An exception handler for any exception that bubbles out of a servlet or occurs within Undertow itself
     */
    public static class RootUndertowExceptionHandler extends io.undertow.server.handlers.ExceptionHandler {
        final protected boolean devMode;

        public RootUndertowExceptionHandler(boolean devMode, HttpHandler next) {
            super(next);
            this.devMode = devMode;
            addExceptionHandler(Throwable.class, (HttpServerExchange exchange) -> {
                // Get the exception that was thrown
                Throwable throwable = exchange.getAttachment(io.undertow.server.handlers.ExceptionHandler.THROWABLE);
                LoggingFilter.logException(
                        devMode,
                        "Undertow",
                        System.Logger.Level.ERROR,
                        throwable,
                        null,
                        500,
                        TEXT_PLAIN_TYPE.toString()
                );

                // Send a clean response to the client
                if (!exchange.isResponseStarted()) {
                    exchange.setStatusCode(500);
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, TEXT_PLAIN_TYPE.toString());
                    exchange.getResponseSender().send(devMode ? renderDevModeError(500, throwable) : renderProductionError(500, throwable));
                }
            });
        }
    }

    /**
     * Exception handler for servlet deployments to prevent HTML error pages being returned
     */
    public static class ServletExceptionHandler implements ExceptionHandler {
        final protected boolean devMode;

        public ServletExceptionHandler(boolean devMode) {
            this.devMode = devMode;
        }

        @Override
        public boolean handleThrowable(HttpServerExchange exchange, ServletRequest request, ServletResponse response, Throwable throwable) {
            Throwable effectiveException = throwable;
            int status = 500;

            // Unpack ServletException to see if the root cause is a WebApplicationException
            if (throwable instanceof jakarta.servlet.ServletException && throwable.getCause() != null) {
                effectiveException = throwable.getCause();
            }

            if (effectiveException instanceof OptimisticLockException) {
                // We get here if JPA entity id is set on merge but it doesn't already exist (unless ID generator is configured to allow ID to be assigned)
                // Or we get here is JPA entity version doesn't match what's in the DB
                effectiveException = new NotAllowedException(effectiveException);
            }

            if (effectiveException instanceof WebApplicationException webApplicationException) {
                status = webApplicationException.getResponse().getStatus();
            }

            boolean filterApplied = Boolean.TRUE.equals(request.getAttribute(LoggingFilter.FILTER_APPLIED_ATTR));
            if (!filterApplied) {
                System.Logger.Level level = status >= 500 ? System.Logger.Level.ERROR : status >= 400 ? System.Logger.Level.DEBUG : System.Logger.Level.TRACE;
                LoggingFilter.logException(
                        devMode,
                        "Servlet",
                        level,
                        effectiveException,
                        request instanceof HttpServletRequest httpRequest ? httpRequest : null,
                        status,
                        response.getContentType()
                );
            }

            if (!exchange.isResponseStarted()) {
                exchange.setStatusCode(status);
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, TEXT_PLAIN_TYPE.toString());
                exchange.getResponseSender().send(devMode ?
                        renderDevModeError(status, effectiveException) :
                        renderProductionError(status, effectiveException));
            }

            return true;
        }
    }

    public static Response handleResteasyException(boolean devMode, Throwable throwable) {
        Throwable effectiveException = throwable;
        int status = 500;
        Response webApplicationResponse = null;

        // Unpack ServletException to see if the root cause is a WebApplicationException
        if (throwable instanceof jakarta.servlet.ServletException && throwable.getCause() != null) {
            effectiveException = throwable.getCause();
        }

        if (effectiveException instanceof OptimisticLockException) {
            // We get here if JPA entity id is set on merge but it doesn't already exist (unless ID generator is
            // configured to allow ID to be assigned). Or we get here is JPA entity version doesn't match what's in the DB
            effectiveException = new NotAllowedException(effectiveException);
        }

        if (effectiveException instanceof WebApplicationException webApplicationException) {
            webApplicationResponse = webApplicationException.getResponse();

            switch (webApplicationResponse.getStatusInfo().getFamily()) {
                case CLIENT_ERROR:
                    status = webApplicationResponse.getStatus();
                    break;
                case SERVER_ERROR:
                    status = webApplicationResponse.getStatus();
                    break;
                default:
                    LoggingFilter.logException(
                            devMode,
                            "JAX-RS",
                            System.Logger.Level.TRACE,
                            effectiveException,
                            null,
                            webApplicationResponse.getStatus(),
                            webApplicationResponse.getMediaType() != null ? webApplicationResponse.getMediaType().toString() : null
                    );
                    // If it's not a client or server error, it's not really an "exception" to
                    // be handled but a status that should be returned to the client
                    return webApplicationResponse;
            }
        }

        try {
            System.Logger.Level level = status >= 500 ? System.Logger.Level.ERROR : status >= 400 ? System.Logger.Level.DEBUG : System.Logger.Level.TRACE;
            LoggingFilter.logException(
                    devMode,
                    "JAX-RS",
                    level,
                    effectiveException,
                    null,
                    status,
                    webApplicationResponse != null && webApplicationResponse.getMediaType() != null
                            ? webApplicationResponse.getMediaType().toString()
                            : TEXT_PLAIN_TYPE.toString()
            );

            if (webApplicationResponse != null && webApplicationResponse.hasEntity()) {
                return Response.fromResponse(webApplicationResponse).build();
            }

            Response.ResponseBuilder responseBuilder = webApplicationResponse != null
                ? Response.fromResponse(webApplicationResponse)
                : Response.status(status);
            String errorBody = devMode
                ? renderDevModeError(status, effectiveException)
                : renderProductionError(status, effectiveException);
            return responseBuilder.entity(errorBody).type(TEXT_PLAIN_TYPE).build();
        } catch (Exception ex) {
            LOG.log(System.Logger.Level.ERROR, "Couldn't render server error trace response", ex);
            return Response.serverError().build();
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
            + sw;
    }

    public static String renderProductionError(int statusCode, Throwable t) {
        Response.Status status = Response.Status.fromStatusCode(statusCode);
        return "Request failed with HTTP error status: "
            + statusCode
            + (status != null ? " " + status.getReasonPhrase() : "");
    }
}
