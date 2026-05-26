/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.model.security;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import org.openremote.model.Constants;
import org.openremote.model.http.RequestParams;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;

/**
 * Manage realms.
 *
 * <p>All operations can only be called by the superuser.
 *
 * <p>TODO Relax permissions to allow regular users to maintain their own realm
 */
@Tag(name = "Realm", description = "Operations on realms")
@Path("realm")
public interface RealmResource {

  @GET
  @Produces(APPLICATION_JSON)
  @Operation(operationId = "getAllRealms", summary = "Retrieve all realms")
  Realm[] getAll(@BeanParam RequestParams requestParams);

  /** Will return realm and display names for accessible realms by authenticated user */
  @GET
  @Path("accessible")
  @Produces(APPLICATION_JSON)
  @Operation(
      operationId = "getAccessibleRealms",
      summary = "Retrieve accessible realms for the authenticated user")
  Realm[] getAccessible(@BeanParam RequestParams requestParams);

  /**
   * Regular users can call this, but only to obtain details about their currently authenticated and
   * active realm.
   */
  @GET
  @Path("{name}")
  @Produces(APPLICATION_JSON)
  @Operation(
      operationId = "getRealm",
      summary = "Retrieve details about the currently authenticated and active realm")
  Realm get(@BeanParam RequestParams requestParams, @PathParam("name") String realm);

  @PUT
  @Path("{name}")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
  @Operation(operationId = "updateRealm", summary = "Update a realm")
  void update(
      @BeanParam RequestParams requestParams,
      @PathParam("name") String realmName,
      @Valid Realm realm);

  @POST
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
  @Operation(operationId = "createRealm", summary = "Create a new realm")
  void create(@BeanParam RequestParams requestParams, @Valid Realm realm);

  @DELETE
  @Path("{name}")
  @Produces(APPLICATION_JSON)
  @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
  @Operation(operationId = "deleteRealm", summary = "Delete a realm")
  void delete(@BeanParam RequestParams requestParams, @PathParam("name") String realm);
}
