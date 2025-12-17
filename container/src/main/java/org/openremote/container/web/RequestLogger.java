/*
 * Copyright 2023, OpenRemote Inc.
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

import org.openremote.container.security.keycloak.KeycloakIdentityProvider;
import org.openremote.model.Constants;
import org.openremote.model.syslog.SyslogCategory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS {@link ContainerRequestFilter} that logs incoming requests to the {@link #REQUEST_LOG}.
 */
@Provider
public class RequestLogger implements ContainerRequestFilter {

  protected static final System.Logger REQUEST_LOG =
      System.getLogger(RequestLogger.class.getName() + "." + SyslogCategory.API.name());

  @Context private HttpServletRequest request;

  @Context protected SecurityContext securityContext;

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    REQUEST_LOG.log(
        System.Logger.Level.DEBUG,
        () -> {
          String requestPath = request.getRequestURI();
          String address = request.getRemoteAddr();
          String port = String.valueOf(request.getRemotePort());
          String forwardedAddress = requestContext.getHeaderString("X-Forwarded-For");
          String method = requestContext.getMethod();
          String realm = requestContext.getHeaderString(Constants.REALM_PARAM_NAME);
          String responseType = requestContext.getHeaderString(HttpHeaders.ACCEPT);
          String usernameAndRealm =
              securityContext != null
                  ? KeycloakIdentityProvider.getSubjectNameAndRealm(
                      securityContext.getUserPrincipal())
                  : null;

          return "Client request '"
              + method
              + " "
              + requestPath
              + " (responseType="
              + responseType
              + ")', requestRealm="
              + realm
              + ", username="
              + usernameAndRealm
              + ", origin="
              + address
              + ":"
              + port
              + ", forwarded-for="
              + forwardedAddress;
        });
  }
}
