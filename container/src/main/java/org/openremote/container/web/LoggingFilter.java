/*
 * Copyright 2026, OpenRemote Inc.
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

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import jakarta.ws.rs.ext.Provider;
import org.openremote.model.Constants;
import org.openremote.model.syslog.SyslogCategory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Principal;

/**
 * A servlet {@link jakarta.servlet.Filter} that logs requests and responses to the {@link #LOG}.
 */
@Provider
public class LoggingFilter implements Filter {

    protected static final int MAX_BODY_BYTES = 1024;
    protected static final String ORIGIN_HEADER = "Origin";
    protected static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    public static final String FILTER_APPLIED_ATTR = LoggingFilter.class.getName() + ".FILTER_APPLIED";
    protected static final System.Logger LOG = System.getLogger(LoggingFilter.class.getName() + "." + SyslogCategory.API.name());
    protected final boolean devMode;

    public LoggingFilter() {
        this(false);
    }

    public LoggingFilter(boolean devMode) {
        this.devMode = devMode;
    }

    public static void logException(boolean devMode, String source, System.Logger.Level level, Throwable t) {
        logException(devMode, source, level, t, null, null, null);
    }

    public static void logException(
            boolean devMode,
            String source,
            System.Logger.Level level,
            Throwable t,
            HttpServletRequest req,
            Integer status,
            String contentType
    ) {
        if (t == null) {
            return;
        }

        if (LOG.isLoggable(level)) {
            String message = buildHttpLogMessage(req, status, contentType, null, null, t, source);
            if (devMode || level.getSeverity() >= System.Logger.Level.INFO.getSeverity()) {
                LOG.log(level, message, t);
            } else {
                LOG.log(level, message);
            }
        }
    }

    protected static String buildHttpLogMessage(
            HttpServletRequest req,
            Integer status,
            String contentType,
            String body,
            Long tookMillis,
            Throwable thrown,
            String source
    ) {
        String method = req != null ? req.getMethod() : null;
        String requestPathAndQuery = req != null
                ? req.getRequestURI() + (req.getQueryString() != null ? "?" + req.getQueryString() : "")
                : null;
        String requestRealm = req != null ? req.getHeader(Constants.REALM_PARAM_NAME) : null;
        Principal principal = req != null ? req.getUserPrincipal() : null;
        String username = principal != null ? principal.getName() : null;
        String origin = req != null ? req.getHeader(ORIGIN_HEADER) : null;
        String forwardedFor = req != null ? req.getHeader(FORWARDED_FOR_HEADER) : null;

        return "HTTP request={"
                + "method=" + method
                + ", path=" + requestPathAndQuery
                + ", requestRealm=" + requestRealm
                + ", username=" + username
                + ", origin=" + origin
                + ", forwarded-for=" + forwardedFor
                + "}, response={"
                + (source != null ? "source=" + source + ", " : "")
                + (tookMillis != null ? "tookMillis=" + tookMillis + ", " : "")
                + "status=" + status
                + ", contentType=" + contentType
                + (body != null ? ", body=" + body : "")
                + (thrown != null ? ", exception=" + thrown.getClass().getName() + ": " + thrown.getMessage() : "")
                + "}";
    }

    /**
     * Minimal response wrapper that tees response body into a buffer while still writing to the real response.
     */
    static final class BufferingHttpServletResponse extends HttpServletResponseWrapper {

        private final ByteArrayOutputStream capture;
        private final int maxBytes;
        private int httpStatus = HttpServletResponse.SC_OK;
        private PrintWriter teeWriter;

        BufferingHttpServletResponse(HttpServletResponse response, int maxBytes) {
            super(response);
            this.capture = new ByteArrayOutputStream(Math.min(maxBytes, 8 * 1024));
            this.maxBytes = maxBytes;
        }

        @Override
        public void setStatus(int sc) {
            this.httpStatus = sc;
            super.setStatus(sc);
        }

        @Override
        public void sendError(int sc) throws IOException {
            this.httpStatus = sc;
            super.sendError(sc);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            this.httpStatus = sc;
            super.sendError(sc, msg);
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (teeWriter != null) {
                return teeWriter;
            }

            Charset cs = getCharacterEncoding() != null ? Charset.forName(getCharacterEncoding()) : StandardCharsets.UTF_8;

            PrintWriter real = super.getWriter();
            teeWriter = new PrintWriter(new TeeWriter(real, capture, cs, maxBytes), true);
            return teeWriter;
        }

        @Override
        public int getStatus() {
            return httpStatus;
        }

        String getCapturedBodyAsString() {
            Charset cs = getCharacterEncoding() != null ? Charset.forName(getCharacterEncoding()) : StandardCharsets.UTF_8;
            return capture.toString(cs);
        }
    }

    static final class TeeWriter extends java.io.Writer {
        private final PrintWriter downstream;
        private final ByteArrayOutputStream capture;
        private final Charset charset;
        private final int maxBytes;

        TeeWriter(PrintWriter downstream, ByteArrayOutputStream capture, Charset charset, int maxBytes) {
            this.downstream = downstream;
            this.capture = capture;
            this.charset = charset;
            this.maxBytes = maxBytes;
        }

        @Override
        public void write(char[] cbuf, int off, int len) {
            downstream.write(cbuf, off, len);

            if (capture.size() < maxBytes) {
                byte[] bytes = new String(cbuf, off, len).getBytes(charset);
                int remaining = maxBytes - capture.size();
                capture.write(bytes, 0, Math.min(remaining, bytes.length));
            }
        }

        @Override
        public void flush() {
            downstream.flush();
        }

        @Override
        public void close() {
            downstream.close();
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest req) || !(response instanceof HttpServletResponse res)) {
            chain.doFilter(request, response);
            return;
        }

        req.setAttribute(FILTER_APPLIED_ATTR, Boolean.TRUE);

        final long startNanos = System.nanoTime();
        final boolean traceEnabled = LOG.isLoggable(System.Logger.Level.TRACE);

        HttpServletResponse effectiveResponse = res;
        BufferingHttpServletResponse wrapped = null;

        if (traceEnabled) {
            wrapped = new BufferingHttpServletResponse(res, MAX_BODY_BYTES);
            effectiveResponse = wrapped;
        }

        Throwable thrown = null;

        try {
            chain.doFilter(request, effectiveResponse);
        } catch (Throwable t) {
            thrown = t;
            // Ensure status is at least 500 if nothing was set
            if (effectiveResponse.getStatus() < 400) {
                effectiveResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            throw t;
        } finally {
            long tookMillis = (System.nanoTime() - startNanos) / 1_000_000;
            int status = effectiveResponse.getStatus();

            if (traceEnabled || status >= 400 || thrown != null) {

                System.Logger.Level level = (status >= 500 || thrown != null)
                        ? System.Logger.Level.WARNING
                        : status >= 400 ? System.Logger.Level.TRACE : System.Logger.Level.DEBUG;

                if (LOG.isLoggable(level)) {
                    String contentType = effectiveResponse.getContentType();
                    String body = null;

                    if (traceEnabled) {
                        boolean isTextual = contentType == null
                                || contentType.startsWith("text/")
                                || contentType.contains("json")
                                || contentType.contains("xml")
                                || contentType.contains("problem+json");
                        if (isTextual) {
                            body = wrapped.getCapturedBodyAsString();
                        }
                    }

                    String message = buildHttpLogMessage(
                            req,
                            status,
                            contentType,
                            traceEnabled ? body : null,
                            tookMillis,
                            thrown,
                            null
                    );

                    if (thrown != null && (devMode || level.getSeverity() >= System.Logger.Level.INFO.getSeverity())) {
                        LOG.log(level, message, thrown);
                    } else {
                        LOG.log(level, message);
                    }
                }
            }
        }
    }
}
