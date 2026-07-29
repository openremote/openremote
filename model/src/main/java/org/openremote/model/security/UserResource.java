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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import org.openremote.model.Constants;
import org.openremote.model.http.OpenApiResponses;
import org.openremote.model.http.RequestParams;
import org.openremote.model.query.UserQuery;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.openremote.model.http.OpenApiDescriptions.*;
import static org.openremote.model.http.OpenApiExamples.USER_CREATE;
import static org.openremote.model.http.OpenApiExamples.USER_QUERY;

/**
 * Manage users in realms and get info of current user.
 */
// TODO Relax permissions to allow regular users to maintain their own realm
@Tag(name = "User", description = "Query and manage identity-provider users, roles, credentials, locale, and live sessions")
@Path("user")
@OpenApiResponses.Authenticated
public interface UserResource {

    @GET
    @Path("{realm}/{clientId}/roles")
    @Produces(APPLICATION_JSON)
    @SuppressWarnings("unusable-by-js")
    @RolesAllowed(Constants.READ_ADMIN_ROLE)
    @Operation(operationId = "getClientRoles", summary = "Retrieve client roles for a realm and client",
        description = "Returns all role definitions exposed by the named identity-provider client. Realm-administrator access is required.")
    @OpenApiResponses.Ok
    @OpenApiResponses.NotFound
    Role[] getClientRoles(
        @BeanParam RequestParams requestParams,
        @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("realm") String realm,
        @Parameter(description = CLIENT_ID, example = EXAMPLE_CLIENT_ID) @PathParam("clientId") String clientId);

    @PUT
    @Path("{realm}/roles")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
    @Operation(operationId = "updateRoles", summary = "Update OpenRemote client roles for a realm",
        description = "Creates, updates, or removes role definitions for the default OpenRemote client in the requested realm.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.BadRequest
    @OpenApiResponses.NotFound
    void updateRoles(
        @BeanParam RequestParams requestParams,
        @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("realm") String realm,
        @RequestBody(required = true, description = "Complete desired set of OpenRemote client role definitions; omitted existing roles are removed.") Role[] roles);

    @PUT
    @Path("{realm}/{clientId}/roles")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
    @Operation(operationId = "updateClientRoles", summary = "Update client roles for a realm and client",
        description = "Creates, updates, or removes role definitions for a named identity-provider client.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.BadRequest
    @OpenApiResponses.NotFound
    void updateClientRoles(
        @BeanParam RequestParams requestParams,
        @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("realm") String realm,
        @RequestBody(required = true, description = "Complete desired set of client role definitions; omitted existing roles are removed.") Role[] roles,
        @Parameter(description = CLIENT_ID, example = EXAMPLE_CLIENT_ID) @PathParam("clientId") String clientId);

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("query")
    @Operation(operationId = "queryUsers", summary = "Query users based on criteria",
        description = "Executes a UserQuery. Non-super users are forced into their authenticated realm and cannot see system accounts; callers with read-users but not read-admin receive basic fields only.")
    @OpenApiResponses.Ok
    @OpenApiResponses.BadRequest
    User[] query(@BeanParam RequestParams requestParams,
                 @RequestBody(required = true, description = "Realm, identity, service-account, asset-link, and field-selection criteria.",
                     content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = UserQuery.class),
                         examples = @ExampleObject(name = "Find human users", summary = "Find human users by username in one realm", value = USER_QUERY))) UserQuery query);

    @GET
    @Path("{realm}/{userId}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getUser", summary = "Retrieve a user in a realm",
        description = "Returns one user when it belongs to the requested accessible realm. Without read-admin permission, callers may retrieve only their own user.")
    @OpenApiResponses.Ok
    @OpenApiResponses.NotFound
    User get(
        @BeanParam RequestParams requestParams,
        @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("realm") String realm,
        @Parameter(description = USER_ID, example = EXAMPLE_USER_ID) @PathParam("userId") String userId);

    @GET
    @Path("user")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getCurrentUser", summary = "Retrieve the currently authenticated user",
        description = "Returns the identity-provider record corresponding to the current access token.")
    @OpenApiResponses.Ok
    @OpenApiResponses.NotFound
    User getCurrent(@BeanParam RequestParams requestParams);

    @PUT
    @Path("{realm}/users")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
    @Operation(operationId = "updateUser", summary = "Update a user in a realm",
        description = "Updates an existing identity-provider user in an administered realm. The master administrator cannot be disabled.")
    @OpenApiResponses.Ok
    @OpenApiResponses.BadRequest
    @OpenApiResponses.NotFound
    @ApiResponse(responseCode = "405", description = "The requested cross-realm or protected master-administrator mutation is not allowed")
    User update(
        @BeanParam RequestParams requestParams,
        @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("realm") String realm,
        @Valid @RequestBody(required = true, description = "User profile, enabled state, attributes, and identity-provider fields to update.") User user);

    @PUT
    @Path("update")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_USER_ROLE)
    @Operation(operationId = "updateSelf", summary = "Update the currently authenticated user",
        description = "Updates the caller's own profile. A supplied user ID must match the access token; role and realm administration are not provided by this endpoint.")
    @OpenApiResponses.Ok
    @OpenApiResponses.BadRequest
    @OpenApiResponses.NotFound
    User updateCurrent(@BeanParam RequestParams requestParams,
                       @Valid @RequestBody(required = true, description = "Editable profile fields for the currently authenticated user.") User user);

    @POST
    @Path("{realm}/users")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
    @Operation(operationId = "createUser", summary = "Create a new user in a realm",
        description = "Creates an identity-provider user in a realm administered by the caller and returns the resulting user record.")
    @OpenApiResponses.Ok
    @OpenApiResponses.BadRequest
    @OpenApiResponses.Conflict
    User create(
        @BeanParam RequestParams requestParams,
        @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("realm") String realm,
        @Valid @RequestBody(required = true, description = "New user profile; omit the id because it is assigned by the identity provider.",
            content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = User.class),
                examples = @ExampleObject(name = "Create a human user", summary = "Create an enabled user with an English locale", value = USER_CREATE))) User user);

    @DELETE
    @Path("{realm}/users/{userId}")
    @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
    @Operation(operationId = "deleteUser", summary = "Delete a user from a realm",
        description = "Permanently deletes a user from an administered realm. The master-realm administrator cannot be deleted.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.NotFound
    @ApiResponse(responseCode = "405", description = "The master-realm administrator cannot be deleted")
    void delete(
        @BeanParam RequestParams requestParams,
        @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("realm") String realm,
        @Parameter(description = USER_ID, example = EXAMPLE_USER_ID) @PathParam("userId") String userId);

    @PUT
    @Path("{realm}/request-password-reset/{userId}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
    @Operation(operationId = "requestUserPasswordReset", summary = "Request a password reset for a user in a realm",
        description = "Asks the identity provider to send or initiate its configured password-reset action for the specified user.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.BadRequest
    @OpenApiResponses.NotFound
    @ApiResponse(responseCode = "405", description = "The caller cannot mutate a user in the requested realm")
    void requestPasswordReset(
        @BeanParam RequestParams requestParams,
        @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("realm") String realm,
        @Parameter(description = USER_ID, example = EXAMPLE_USER_ID) @PathParam("userId") String userId);

    @PUT
    @Path("request-password-reset")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_USER_ROLE)
    @Operation(operationId = "requestPasswordReset", summary = "Request a password reset for the current user",
        description = "Initiates the identity provider's configured password-reset action for the caller's own account.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.BadRequest
    void requestPasswordResetCurrent(@BeanParam RequestParams requestParams);

    @PUT
    @Path("{realm}/reset-password/{userId}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
    @Operation(operationId = "updatePassword", summary = "Update the password for a user in a realm",
        description = "Replaces a user's credential in an administered realm using the supplied credential representation.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.BadRequest
    @OpenApiResponses.NotFound
    @ApiResponse(responseCode = "405", description = "The caller cannot mutate a user in the requested realm")
    void updatePassword(
        @BeanParam RequestParams requestParams,
        @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("realm") String realm,
        @Parameter(description = USER_ID, example = EXAMPLE_USER_ID) @PathParam("userId") String userId,
        @RequestBody(required = true, description = "New credential value and whether it is temporary.") Credential credential);

    @PUT
    @Path("reset-password")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_USER_ROLE)
    @Operation(operationId = "updateOwnPassword", summary = "Update the current user's password",
        description = "Replaces the caller's own identity-provider credential using the supplied credential representation.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.BadRequest
    void updatePasswordCurrent(@BeanParam RequestParams requestParams,
                               @RequestBody(required = true, description = "New credential value and whether it is temporary.") Credential credential);

    @GET
    @Path("{realm}/reset-secret/{userId}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
    @Operation(operationId = "resetSecret", summary = "Reset the secret for a user in a realm",
        description = "Generates and returns a new client secret for a service user in an administered realm.")
    @OpenApiResponses.Ok
    @OpenApiResponses.BadRequest
    @OpenApiResponses.NotFound
    String resetSecret(
        @BeanParam RequestParams requestParams,
        @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("realm") String realm,
        @Parameter(description = "Service-user identifier whose client secret is regenerated.", example = EXAMPLE_USER_ID) @PathParam("userId") String userId);

    @GET
    @Path("{realm}/userRoles/{userId}/{clientId}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getUserClientRoles", summary = "Retrieve a user's client roles",
        description = "Returns role names assigned to one user for the named client. Without read-admin permission, callers may retrieve only their own roles.")
    @OpenApiResponses.Ok
    @OpenApiResponses.NotFound
    String[] getUserClientRoles(
        @BeanParam RequestParams requestParams,
        @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("realm") String realm,
        @Parameter(description = USER_ID, example = EXAMPLE_USER_ID) @PathParam("userId") String userId,
        @Parameter(description = CLIENT_ID, example = EXAMPLE_CLIENT_ID) @PathParam("clientId") String clientId);

    @GET
    @Path("{realm}/userRealmRoles/{userId}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getUserRealmRoles", summary = "Retrieve a user's realm roles",
        description = "Returns realm-level role names assigned to one user. Without read-admin permission, callers may retrieve only their own roles.")
    @OpenApiResponses.Ok
    @OpenApiResponses.NotFound
    String[] getUserRealmRoles(
        @BeanParam RequestParams requestParams,
        @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("realm") String realm,
        @Parameter(description = USER_ID, example = EXAMPLE_USER_ID) @PathParam("userId") String userId);

    @GET
    @Path("userRoles/{clientId}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getCurrentUserClientRoles", summary = "Retrieve the current user's client roles",
        description = "Returns role names assigned to the caller for the named identity-provider client.")
    @OpenApiResponses.Ok
    @OpenApiResponses.NotFound
    String[] getCurrentUserClientRoles(
        @BeanParam RequestParams requestParams,
        @Parameter(description = CLIENT_ID, example = EXAMPLE_CLIENT_ID) @PathParam("clientId") String clientId);

    @GET
    @Path("userRealmRoles")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getCurrentUserRealmRoles", summary = "Retrieve the current user's realm roles",
        description = "Returns realm-level role names assigned to the caller.")
    @OpenApiResponses.Ok
    @OpenApiResponses.NotFound
    String[] getCurrentUserRealmRoles(@BeanParam RequestParams requestParams);

    @PUT
    @Path("{realm}/userRoles/{userId}/{clientId}")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
    @Operation(operationId = "updateUserClientRoles", summary = "Update a user's client roles",
        description = "Replaces the user's assigned role names for the named client in an administered realm.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.BadRequest
    @OpenApiResponses.NotFound
    void updateUserClientRoles(
        @BeanParam RequestParams requestParams,
        @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("realm") String realm,
        @Parameter(description = USER_ID, example = EXAMPLE_USER_ID) @PathParam("userId") String userId,
        @RequestBody(required = true, description = "Complete desired set of client-role names for this user.") String[] roles,
        @Parameter(description = CLIENT_ID, example = EXAMPLE_CLIENT_ID) @PathParam("clientId") String clientId);

    @PUT
    @Path("{realm}/userRealmRoles/{userId}")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
    @Operation(operationId = "updateUserRealmRoles", summary = "Update a user's realm roles",
        description = "Replaces the user's assigned realm-level role names in an administered realm.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.BadRequest
    @OpenApiResponses.NotFound
    void updateUserRealmRoles(
        @BeanParam RequestParams requestParams,
        @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("realm") String realm,
        @Parameter(description = USER_ID, example = EXAMPLE_USER_ID) @PathParam("userId") String userId,
        @RequestBody(required = true, description = "Complete desired set of realm-role names for this user.") String[] roles);

    @PUT
    @Path("locale")
    @Consumes(APPLICATION_JSON)
    @Operation(operationId = "updateCurrentUserLocale", summary = "Update the current user's locale",
        description = "Stores the supplied non-empty locale string as the caller's identity-provider locale attribute.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.BadRequest
    @OpenApiResponses.NotFound
    void updateCurrentUserLocale(
        @BeanParam RequestParams requestParams,
        @RequestBody(required = true, description = "BCP 47 locale identifier for the current user, encoded as a JSON string.") String locale);

    @GET
    @Path("{realm}/userSessions/{userId}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getUserSessions", summary = "Retrieve live sessions for a user",
        description = "Returns active MQTT sessions for one user, including connection ID, creation time, and remote address. Without read-admin permission, callers may inspect only themselves.")
    @OpenApiResponses.Ok
    @OpenApiResponses.NotFound
    UserSession[] getUserSessions(
        @BeanParam RequestParams requestParams,
        @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("realm") String realm,
        @Parameter(description = USER_ID, example = EXAMPLE_USER_ID) @PathParam("userId") String userId);

    @GET
    @Path("{realm}/disconnect/{sessionID}")
    @Operation(operationId = "disconnectUserSession", summary = "Disconnect a user session",
        description = "Terminates the active MQTT connection identified by sessionID.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.NotFound
    void disconnectUserSession(
        @BeanParam RequestParams requestParams,
        @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("realm") String realm,
        @Parameter(description = "Active MQTT connection identifier returned by getUserSessions.", example = "mqtt-connection-7") @PathParam("sessionID") String sessionID);
}
