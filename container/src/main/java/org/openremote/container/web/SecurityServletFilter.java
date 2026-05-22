/*
 * Copyright 2025, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.container.web;

import java.io.IOException;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A security filter that will check if the request is authenticated and that the user has any of
 * the specified roles by calling {@link HttpServletRequest#isUserInRole} for each {@link
 * #rolesAllowed} until one of them is true. To be used by non JAX-RS servlet deployments to mimic
 * {@link jakarta.annotation.security.RolesAllowed}
 */
public class SecurityServletFilter implements Filter {

  protected final String[] rolesAllowed;

  public SecurityServletFilter(String[] rolesAllowed) {
    this.rolesAllowed = rolesAllowed;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    boolean userHasRole = false;
    for (String allowedRole : rolesAllowed) {
      if (req.isUserInRole(allowedRole)) {
        userHasRole = true;
        break;
      }
    }

    if (userHasRole) {
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
