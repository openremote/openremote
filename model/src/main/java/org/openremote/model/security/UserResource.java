/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.model.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.Constants;
import org.openremote.model.http.RequestParams;
import org.openremote.model.query.UserQuery;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Manage users in realms and get info of current user.
 */
// TODO Relax permissions to allow regular users to maintain their own realm
@Tag(name = "User", description = "Operations on users")
@Path("user")
public interface UserResource {

    @GET
    @Path("{realm}/roles")
    @Produces(APPLICATION_JSON)
    @SuppressWarnings("unusable-by-js")
    @RolesAllowed(Constants.READ_ADMIN_ROLE)
    @Operation(operationId = "getRoles", summary = "Retrieve roles for a realm")
    Role[] getRoles(@BeanParam RequestParams requestParams, @PathParam("realm") String realm);

    @GET
    @Path("{realm}/{clientId}/roles")
    @Produces(APPLICATION_JSON)
    @SuppressWarnings("unusable-by-js")
    @RolesAllowed(Constants.READ_ADMIN_ROLE)
    @Operation(operationId = "getClientRoles", summary = "Retrieve client roles for a realm and client")
    Role[] getClientRoles(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @PathParam("clientId") String clientId);

    @PUT
    @Path("{realm}/roles")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
    @Operation(operationId = "updateRoles", summary = "Update roles for a realm")
    void updateRoles(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, Role[] roles);

    @PUT
    @Path("{realm}/{clientId}/roles")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
    @Operation(operationId = "updateClientRoles", summary = "Update client roles for a realm and client")
    void updateClientRoles(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, Role[] roles, @PathParam("clientId") String clientId);

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
    User get(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @PathParam("userId") String userId);

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
    User update(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @Valid User user);

    @POST
    @Path("{realm}/users")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
    @Operation(operationId = "createUser", summary = "Create a new user in a realm")
    User create(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @Valid User user);

    @DELETE
    @Path("{realm}/users/{userId}")
    @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
    @Operation(operationId = "deleteUser", summary = "Delete a user from a realm")
    void delete(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @PathParam("userId") String userId);

    @PUT
    @Path("{realm}/reset-password/{userId}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
    @Operation(operationId = "resetPassword", summary = "Reset the password for a user in a realm")
    void resetPassword(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @PathParam("userId") String userId, Credential credential);

    @GET
    @Path("{realm}/reset-secret/{userId}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
    @Operation(operationId = "resetSecret", summary = "Reset the secret for a user in a realm")
    String resetSecret(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @PathParam("userId") String userId);

    @GET
    @Path("{realm}/userRoles/{userId}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getUserRoles", summary = "Retrieve client roles for a user in a realm")
    Role[] getUserRoles(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @PathParam("userId") String userId);

    @GET
    @Path("{realm}/userRoles/{userId}/{clientId}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getUserClientRoles", summary = "Retrieve client roles for a user using a client ID in a realm")
    Role[] getUserClientRoles(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @PathParam("userId") String userId, @PathParam("clientId") String clientId);

    @GET
    @Path("{realm}/userRealmRoles/{userId}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getUserRealmRoles", summary = "Retrieve realm roles for a user in a realm")
    Role[] getUserRealmRoles(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @PathParam("userId") String userId);

    @GET
    @Path("userRoles")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getCurrentUserRoles", summary = "Retrieve client roles for the currently authenticated user")
    Role[] getCurrentUserRoles(@BeanParam RequestParams requestParams);

    @GET
    @Path("userRoles/{clientId}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getCurrentUserClientRoles", summary = "Retrieve client roles for the currently authenticated user using a client ID")
    Role[] getCurrentUserClientRoles(@BeanParam RequestParams requestParams, @PathParam("clientId") String clientId);

    @GET
    @Path("userRealmRoles")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getCurrentUserRealmRoles", summary = "Retrieve realm roles for the currently authenticated user")
    Role[] getCurrentUserRealmRoles(@BeanParam RequestParams requestParams);

    @PUT
    @Path("{realm}/userRoles/{userId}")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
    @Operation(operationId = "updateUserRoles", summary = "Update client roles for a user in a realm")
    void updateUserRoles(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @PathParam("userId") String userId, Role[] roles);

    @PUT
    @Path("{realm}/userRoles/{userId}/{clientId}")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
    @Operation(operationId = "updateUserClientRoles", summary = "Update client roles for a user in a realm using a client ID")
    void updateUserClientRoles(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @PathParam("userId") String userId, Role[] roles, @PathParam("clientId") String clientId);

    @PUT
    @Path("{realm}/userRealmRoles/{userId}")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
    @Operation(operationId = "updateUserRealmRoles", summary = "Update realm roles for a user in a realm")
    void updateUserRealmRoles(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @PathParam("userId") String userId, Role[] roles);

    @GET
    @Path("{realm}/userSessions/{userId}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getUserSessions", summary = "Retrieve sessions for a user in a realm")
    UserSession[] getUserSessions(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @PathParam("userId") String userId);

    @GET
    @Path("{realm}/disconnect/{sessionID}")
    @Operation(operationId = "disconnectUserSession", summary = "Disconnect a user session using a session ID")
    void disconnectUserSession(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @PathParam("sessionID") String sessionID);
}
