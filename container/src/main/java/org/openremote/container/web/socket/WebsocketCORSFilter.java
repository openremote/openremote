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
package org.openremote.container.web.socket;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebsocketCORSFilter implements Filter {

    private static final Logger LOG = Logger.getLogger(WebsocketCORSFilter.class.getName());

    public static final String ALLOWED_ORIGIN = "ALLOWED_ORIGIN";

    protected String allowedOrigin;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        allowedOrigin = filterConfig.getInitParameter(ALLOWED_ORIGIN);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        if (allowedOrigin == null) {
            if (LOG.isLoggable(Level.FINE))
                LOG.fine("No origin restriction, allowing Websocket upgrade request");
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        if (req.getHeader("Upgrade") != null) {
            String origin = req.getHeader("Origin");
            if (origin != null && origin.equals(allowedOrigin)) {
                if (LOG.isLoggable(Level.FINE))
                    LOG.fine("Received origin is allowed origin, allowing Websocket upgrade request: " + origin);
                chain.doFilter(request, response);
                return;
            }
        }

        LOG.info("Illegal origin, dropping Websocket upgrade request");
        resp.sendError(400, "Origin is not allowed");
    }

    @Override
    public void destroy() {
    }

}