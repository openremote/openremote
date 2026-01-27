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
 * A security filter that will check if the request is authenticated and that the user has all the
 * specified roles by calling {@link HttpServletRequest#isUserInRole} for each {@link
 * #requiredRoles}
 */
public class SecurityFilter implements Filter {

  protected final String[] requiredRoles;

  public SecurityFilter(String[] requiredRoles) {
    this.requiredRoles = requiredRoles;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

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
