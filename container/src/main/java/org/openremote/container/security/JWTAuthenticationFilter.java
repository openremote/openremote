package org.openremote.container.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.openremote.model.Constants;

import javax.ws.rs.core.HttpHeaders;
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

        } catch (Exception e) {
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        }
    }
}
