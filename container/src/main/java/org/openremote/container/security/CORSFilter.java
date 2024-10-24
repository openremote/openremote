/*
 * Copyright 2024, OpenRemote Inc.
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
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.container.security;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.jboss.resteasy.spi.CorsHeaders;
import org.openremote.container.web.file.HttpFilter;
import org.openremote.model.util.TextUtil;

import io.undertow.util.StatusCodes;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.HttpMethod;

public class CORSFilter extends HttpFilter {
    protected boolean allowCredentials = true;
    protected String allowedMethods;
    protected String allowedHeaders;
    protected String exposedHeaders;
    protected int corsMaxAge = -1;
    protected Set<String> allowedOrigins = new HashSet<>();

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public String getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(String allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public String getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(String allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public String getExposedHeaders() {
        return exposedHeaders;
    }

    public void setExposedHeaders(String exposedHeaders) {
        this.exposedHeaders = exposedHeaders;
    }

    public int getCorsMaxAge() {
        return corsMaxAge;
    }

    public void setCorsMaxAge(int corsMaxAge) {
        this.corsMaxAge = corsMaxAge;
    }

    public Set<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(Set<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(HttpServletRequest request, HttpServletResponse response, HttpSession session, FilterChain chain) throws ServletException, IOException {
        String origin = request.getHeader(CorsHeaders.ORIGIN);
        boolean isOptions = request.getMethod().equals(HttpMethod.OPTIONS);

        if (origin == null) {
            chain.doFilter(request, response);
            return;
        }

        if (!originOk(origin)) {
            response.sendError(StatusCodes.FORBIDDEN, "Origin not allowed");
            return;
        }

        if (isOptions) {
            response.setStatus(StatusCodes.OK);
            response.setHeader(CorsHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            if (allowCredentials) {
                response.setHeader(CorsHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            }

            String requestMethods = request.getHeader(CorsHeaders.ACCESS_CONTROL_REQUEST_METHOD);
            if (!TextUtil.isNullOrEmpty(requestMethods)) {
                if (allowedMethods != null)
                {
                    requestMethods = this.allowedMethods;
                }
                response.setHeader(CorsHeaders.ACCESS_CONTROL_ALLOW_METHODS, requestMethods);
            }

            String requestHeaders = request.getHeader(CorsHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
            if (!TextUtil.isNullOrEmpty(requestHeaders)) {
                if (allowedHeaders != null)
                {
                    requestHeaders = this.allowedHeaders;
                }
                response.setHeader(CorsHeaders.ACCESS_CONTROL_ALLOW_HEADERS, requestHeaders);
            }

            if (corsMaxAge > -1)
            {
                response.setHeader(CorsHeaders.ACCESS_CONTROL_MAX_AGE, Integer.toString(corsMaxAge));
            }
        } else {
            response.setHeader(CorsHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);

            if (allowCredentials) {
                response.setHeader(CorsHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            }

            if (exposedHeaders != null) {
                response.setHeader(CorsHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, exposedHeaders);
            }

            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {

    }

    protected boolean originOk(String origin) {
        // startsWith test allows for host matching without explicit port mapping
        return allowedOrigins.contains("*") || allowedOrigins.contains(origin) || allowedOrigins.stream().anyMatch(origin::startsWith);
    }

}
