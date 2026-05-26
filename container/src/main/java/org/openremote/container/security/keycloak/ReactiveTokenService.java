/*
 * Copyright 2026, OpenRemote Inc.
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
package org.openremote.container.security.keycloak;

import java.util.concurrent.CompletionStage;

import org.keycloak.representations.AccessTokenResponse;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

/**
 * This is a reactive async version of the keycloak {@link
 * org.keycloak.admin.client.token.TokenService}
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public interface ReactiveTokenService {

  @POST
  @Path("/realms/{realm}/protocol/openid-connect/token")
  CompletionStage<AccessTokenResponse> grantToken(
      @PathParam("realm") String realm, MultivaluedMap<String, String> map);

  @POST
  @Path("/realms/{realm}/protocol/openid-connect/token")
  CompletionStage<AccessTokenResponse> refreshToken(
      @PathParam("realm") String realm, MultivaluedMap<String, String> map);

  @POST
  @Path("/realms/{realm}/protocol/openid-connect/logout")
  CompletionStage<Void> logout(
      @PathParam("realm") String realm, MultivaluedMap<String, String> map);
}
