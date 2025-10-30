package org.openremote.container.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;

/**
 * A security filter that will check if the request is authenticated and that the user has all the specified roles
 * by calling {@link HttpServletRequest#isUserInRole} for each {@link #requiredRoles}
 */
public class SecurityFilter implements Filter {

    final protected String[] requiredRoles;

    public SecurityFilter(String[] requiredRoles) {
        this.requiredRoles = requiredRoles;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        boolean userHasAllRoles = true;
        for (String requiredRole : requiredRoles) {
            if (!req.isUserInRole(requiredRole)) {
                userHasAllRoles = false;
                break;
            }
        }

        if (userHasAllRoles) {
            chain.doFilter(request, response);
        } else {
            if (req.getUserPrincipal() == null) {
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            } else {
                res.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
            }
        }
    }
}
