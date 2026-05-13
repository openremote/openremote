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
package org.openremote.container.security;

import jakarta.security.enterprise.AuthenticationException;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.HttpHeaders;
import org.openremote.model.Constants;

import java.io.IOException;
import java.security.Principal;

import static org.openremote.model.http.RequestParams.BEARER_AUTH_PREFIX;
import static org.openremote.model.http.RequestParams.getBearerAuth;

public class JWTAuthenticationFilter implements Filter {

    public static final String NAME = "JWTAuthFilter";
    public static final String AUTH_TYPE = "JWT";
    protected final TokenVerifier tokenVerifier;

    public JWTAuthenticationFilter(TokenVerifier tokenVerifier) {
        this.tokenVerifier = tokenVerifier;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest) || !(response instanceof HttpServletResponse httpResponse)) {
            chain.doFilter(request, response);
            return;
        }

        String realm = httpRequest.getHeader(Constants.REALM_PARAM_NAME);
        if (realm == null) {
            httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, Constants.REALM_PARAM_NAME + " header is missing");
            return;
        }

        String authHeader = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_AUTH_PREFIX)) {
            // Anonymous - If the resource is protected, the container or application logic will handle the 401/403.
            chain.doFilter(request, response);
            return;
        }

        String token = getBearerAuth(authHeader);

        try {
            final TokenPrincipal principal = tokenVerifier.verify(realm, token);

            // Wrap the request to provide security context
            HttpServletRequestWrapper authenticatedRequest = new HttpServletRequestWrapper(httpRequest) {
                @Override
                public Principal getUserPrincipal() {
                    return principal;
                }

                @Override
                public boolean isUserInRole(String role) {
                    return principal.isUserInRole(role);
                }

                @Override
                public String getAuthType() {
                    return AUTH_TYPE;
                }
            };

            chain.doFilter(authenticatedRequest, response);

        } catch (AuthenticationException e) {
            throw new NotAuthorizedException("Authentication has failed: " + e.getMessage(), Response.status(Response.Status.UNAUTHORIZED).build(), e);
        }
    }
}
