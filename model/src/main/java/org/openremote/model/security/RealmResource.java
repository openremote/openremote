/*
 * Copyright 2016, OpenRemote Inc.
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
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.Constants;
import org.openremote.model.http.OpenApiResponses;
import org.openremote.model.http.RequestParams;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.openremote.model.http.OpenApiDescriptions.EXAMPLE_REALM;
import static org.openremote.model.http.OpenApiDescriptions.REALM;

/**
 * Manage realms.
 * <p>
 * All operations can only be called by the superuser.
 * <p>
 * TODO Relax permissions to allow regular users to maintain their own realm
 */
@Tag(name = "Realm", description = "Discover accessible realms and administer identity-provider realms")
@Path("realm")
public interface RealmResource {

    @GET
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getAllRealms", summary = "Retrieve all realms",
        description = "Returns complete realm records from the identity provider. This operation is restricted to a super user.")
    @OpenApiResponses.Ok
    @OpenApiResponses.Authenticated
    @OpenApiResponses.ServerError
    Realm[] getAll(@BeanParam RequestParams requestParams);

    /**
     * Will return realm and display names for accessible realms by authenticated user
     */
    @GET
    @Path("accessible")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getAccessibleRealms", summary = "Retrieve realms accessible to the caller",
        description = "Returns only realm name and display name. Super users receive all realms; authenticated users receive their realm; anonymous callers receive the request realm.")
    @OpenApiResponses.Ok
    @OpenApiResponses.ServerError
    Realm[] getAccessible(@BeanParam RequestParams requestParams);

    /**
     * Regular users can call this, but only to obtain details about their currently authenticated and active realm.
     */
    @GET
    @Path("{name}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getRealm", summary = "Retrieve an accessible realm",
        description = "Returns complete details for a named active realm when it is accessible to the caller. Regular users cannot use this endpoint to inspect other realms.")
    @OpenApiResponses.Ok
    @OpenApiResponses.Forbidden
    @OpenApiResponses.NotFound
    Realm get(@BeanParam RequestParams requestParams,
              @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("name") String realm);

    @PUT
    @Path("{name}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
    @Operation(operationId = "updateRealm", summary = "Update a realm",
        description = "Updates a realm through the identity provider. Super-user access is required; the master realm cannot be renamed or disabled.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.Authenticated
    @OpenApiResponses.BadRequest
    @OpenApiResponses.Conflict
    @ApiResponse(responseCode = "405", description = "The requested mutation is not allowed for the master realm")
    void update(
        @BeanParam RequestParams requestParams,
        @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("name") String realmName,
        @Valid @RequestBody(required = true, description = "Complete realm settings to apply; the path name identifies the existing realm.") Realm realm);

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
    @Operation(operationId = "createRealm", summary = "Create a new realm",
        description = "Creates and configures a realm in the identity provider. This operation is restricted to a super user.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.Authenticated
    @OpenApiResponses.BadRequest
    @OpenApiResponses.Conflict
    void create(@BeanParam RequestParams requestParams,
                @Valid @RequestBody(required = true, description = "Realm name, display name, activation state, and optional identity-provider settings.") Realm realm);

    @DELETE
    @Path("{name}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
    @Operation(operationId = "deleteRealm", summary = "Delete a realm",
        description = "Permanently deletes a realm from the identity provider. The master realm cannot be deleted; related Manager data is not automatically removed.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.Authenticated
    @OpenApiResponses.NotFound
    @ApiResponse(responseCode = "405", description = "The master realm cannot be deleted")
    void delete(@BeanParam RequestParams requestParams,
                @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("name") String realm);
}
