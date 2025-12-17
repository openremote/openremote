/*
 * Copyright 2017, OpenRemote Inc.
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
import org.openremote.model.query.UserQuery;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;

/** Manage users in realms and get info of current user. */
// TODO Relax permissions to allow regular users to maintain their own realm
@Tag(name = "User", description = "Operations on users")
@Path("user")
public interface UserResource {

  @GET
  @Path("{realm}/{clientId}/roles")
  @Produces(APPLICATION_JSON)
  @SuppressWarnings("unusable-by-js")
  @RolesAllowed(Constants.READ_ADMIN_ROLE)
  @Operation(
      operationId = "getClientRoles",
      summary = "Retrieve client roles for a realm and client")
  Role[] getClientRoles(
      @BeanParam RequestParams requestParams,
      @PathParam("realm") String realm,
      @PathParam("clientId") String clientId);

  @PUT
  @Path("{realm}/roles")
  @Consumes(APPLICATION_JSON)
  @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
  @Operation(operationId = "updateRoles", summary = "Update roles for a realm")
  void updateRoles(
      @BeanParam RequestParams requestParams, @PathParam("realm") String realm, Role[] roles);

  @PUT
  @Path("{realm}/{clientId}/roles")
  @Consumes(APPLICATION_JSON)
  @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
  @Operation(
      operationId = "updateClientRoles",
      summary = "Update client roles for a realm and client")
  void updateClientRoles(
      @BeanParam RequestParams requestParams,
      @PathParam("realm") String realm,
      Role[] roles,
      @PathParam("clientId") String clientId);

  @POST
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @Path("query")
  @Operation(operationId = "queryUsers", summary = "Query users based on criteria")
  User[] query(@BeanParam RequestParams requestParams, UserQuery query);

  @GET
  @Path("{realm}/{userId}")
  @Produces(APPLICATION_JSON)
  @Operation(operationId = "getUser", summary = "Retrieve a user in a realm")
  User get(
      @BeanParam RequestParams requestParams,
      @PathParam("realm") String realm,
      @PathParam("userId") String userId);

  @GET
  @Path("user")
  @Produces(APPLICATION_JSON)
  @Operation(operationId = "getCurrentUser", summary = "Retrieve the currently authenticated user")
  User getCurrent(@BeanParam RequestParams requestParams);

  @PUT
  @Path("{realm}/users")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
  @Operation(operationId = "updateUser", summary = "Update a user in a realm")
  User update(
      @BeanParam RequestParams requestParams, @PathParam("realm") String realm, @Valid User user);

  @PUT
  @Path("update")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @RolesAllowed(Constants.WRITE_USER_ROLE)
  @Operation(operationId = "updateSelf", summary = "Update the currently authenticated user")
  User updateCurrent(@BeanParam RequestParams requestParams, @Valid User user);

  @POST
  @Path("{realm}/users")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
  @Operation(operationId = "createUser", summary = "Create a new user in a realm")
  User create(
      @BeanParam RequestParams requestParams, @PathParam("realm") String realm, @Valid User user);

  @DELETE
  @Path("{realm}/users/{userId}")
  @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
  @Operation(operationId = "deleteUser", summary = "Delete a user from a realm")
  void delete(
      @BeanParam RequestParams requestParams,
      @PathParam("realm") String realm,
      @PathParam("userId") String userId);

  @PUT
  @Path("{realm}/request-password-reset/{userId}")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
  @Operation(
      operationId = "requestUserPasswordReset",
      summary = "Request a password reset for a user in a realm")
  void requestPasswordReset(
      @BeanParam RequestParams requestParams,
      @PathParam("realm") String realm,
      @PathParam("userId") String userId);

  @PUT
  @Path("request-password-reset")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @RolesAllowed(Constants.WRITE_USER_ROLE)
  @Operation(
      operationId = "requestPasswordReset",
      summary = "Request a password reset for the currently authenticated user")
  void requestPasswordResetCurrent(@BeanParam RequestParams requestParams);

  @PUT
  @Path("{realm}/reset-password/{userId}")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
  @Operation(operationId = "updatePassword", summary = "Update the password for a user in a realm")
  void updatePassword(
      @BeanParam RequestParams requestParams,
      @PathParam("realm") String realm,
      @PathParam("userId") String userId,
      Credential credential);

  @PUT
  @Path("reset-password")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @RolesAllowed(Constants.WRITE_USER_ROLE)
  @Operation(
      operationId = "updateOwnPassword",
      summary = "Update the password for the currently authenticated user")
  void updatePasswordCurrent(@BeanParam RequestParams requestParams, Credential credential);

  @GET
  @Path("{realm}/reset-secret/{userId}")
  @Produces(APPLICATION_JSON)
  @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
  @Operation(operationId = "resetSecret", summary = "Reset the secret for a user in a realm")
  String resetSecret(
      @BeanParam RequestParams requestParams,
      @PathParam("realm") String realm,
      @PathParam("userId") String userId);

  @GET
  @Path("{realm}/userRoles/{userId}/{clientId}")
  @Produces(APPLICATION_JSON)
  @Operation(
      operationId = "getUserClientRoles",
      summary = "Retrieve client roles for a user using a client ID in a realm")
  String[] getUserClientRoles(
      @BeanParam RequestParams requestParams,
      @PathParam("realm") String realm,
      @PathParam("userId") String userId,
      @PathParam("clientId") String clientId);

  @GET
  @Path("{realm}/userRealmRoles/{userId}")
  @Produces(APPLICATION_JSON)
  @Operation(
      operationId = "getUserRealmRoles",
      summary = "Retrieve realm roles for a user in a realm")
  String[] getUserRealmRoles(
      @BeanParam RequestParams requestParams,
      @PathParam("realm") String realm,
      @PathParam("userId") String userId);

  @GET
  @Path("userRoles/{clientId}")
  @Produces(APPLICATION_JSON)
  @Operation(
      operationId = "getCurrentUserClientRoles",
      summary = "Retrieve client roles for the currently authenticated user using a client ID")
  String[] getCurrentUserClientRoles(
      @BeanParam RequestParams requestParams, @PathParam("clientId") String clientId);

  @GET
  @Path("userRealmRoles")
  @Produces(APPLICATION_JSON)
  @Operation(
      operationId = "getCurrentUserRealmRoles",
      summary = "Retrieve realm roles for the currently authenticated user")
  String[] getCurrentUserRealmRoles(@BeanParam RequestParams requestParams);

  @PUT
  @Path("{realm}/userRoles/{userId}/{clientId}")
  @Consumes(APPLICATION_JSON)
  @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
  @Operation(
      operationId = "updateUserClientRoles",
      summary = "Update client roles for a user in a realm using a client ID")
  void updateUserClientRoles(
      @BeanParam RequestParams requestParams,
      @PathParam("realm") String realm,
      @PathParam("userId") String userId,
      String[] roles,
      @PathParam("clientId") String clientId);

  @PUT
  @Path("{realm}/userRealmRoles/{userId}")
  @Consumes(APPLICATION_JSON)
  @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
  @Operation(
      operationId = "updateUserRealmRoles",
      summary = "Update realm roles for a user in a realm")
  void updateUserRealmRoles(
      @BeanParam RequestParams requestParams,
      @PathParam("realm") String realm,
      @PathParam("userId") String userId,
      String[] roles);

  @PUT
  @Path("locale")
  @Consumes(APPLICATION_JSON)
  @Operation(
      operationId = "updateCurrentUserLocale",
      summary = "Update locale for the current user in a realm")
  void updateCurrentUserLocale(@BeanParam RequestParams requestParams, String locale);

  @GET
  @Path("{realm}/userSessions/{userId}")
  @Produces(APPLICATION_JSON)
  @Operation(operationId = "getUserSessions", summary = "Retrieve sessions for a user in a realm")
  UserSession[] getUserSessions(
      @BeanParam RequestParams requestParams,
      @PathParam("realm") String realm,
      @PathParam("userId") String userId);

  @GET
  @Path("{realm}/disconnect/{sessionID}")
  @Operation(
      operationId = "disconnectUserSession",
      summary = "Disconnect a user session using a session ID")
  void disconnectUserSession(
      @BeanParam RequestParams requestParams,
      @PathParam("realm") String realm,
      @PathParam("sessionID") String sessionID);
}
