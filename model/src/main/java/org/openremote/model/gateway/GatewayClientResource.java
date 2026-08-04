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
package org.openremote.model.gateway;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.Constants;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.http.OpenApiResponses;
import org.openremote.model.http.RequestParams;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.openremote.model.http.OpenApiDescriptions.EXAMPLE_REALM;
import static org.openremote.model.http.OpenApiDescriptions.REALM;

/**
 * Resource for managing the connection to a central manager
 */
@Tag(name = "Gateway", description = "Configure this Manager's connections to central OpenRemote gateway services")
@Path("gateway")
@OpenApiResponses.Authenticated
public interface GatewayClientResource {

    /**
     * Get the {@link GatewayConnection} for the specified realm, user must be a realm admin
     */
    @GET
    @Path("connection/{realm}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ADMIN_ROLE})
    @Operation(operationId = "getConnection", summary = "Retrieve the gateway connection of a realm",
        description = "Returns the central-manager connection configured for a realm, or null when none exists. Realm administrators can read their own realm; super users can read any realm.")
    @OpenApiResponses.Ok
    @OpenApiResponses.BadRequest
    @ApiResponse(responseCode = "204", description = "No gateway connection is configured for the realm")
    GatewayConnection getConnection(@BeanParam RequestParams requestParams,
                                    @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("realm") String realm);

    @GET
    @Path("status/{realm}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ADMIN_ROLE})
    @Operation(operationId = "getConnectionStatus", summary = "Retrieve the gateway connection status of a realm",
        description = "Returns the live connection state for the requested local realm.")
    @OpenApiResponses.Ok
    ConnectionStatus getConnectionStatus(@BeanParam RequestParams requestParams,
                                         @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("realm") String realm);

    /**
     * Get the {@link GatewayConnection}s for all realms, user must be a super user
     */
    @GET
    @Path("connection")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ADMIN_ROLE})
    @Operation(operationId = "getConnections", summary = "Retrieve the gateway connections of all realms",
        description = "Returns every configured central-manager connection. Unlike the single-realm endpoint, this operation is restricted to super users.")
    @OpenApiResponses.Ok
    @OpenApiResponses.BadRequest
    List<GatewayConnection> getConnections(@BeanParam RequestParams requestParams);

    /**
     * Update a {@link GatewayConnection} for the specified realm
     */
    @PUT
    @Path("connection/{realm}")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ADMIN_ROLE})
    @Operation(operationId = "setConnection", summary = "Update the gateway connection of a realm",
        description = "Creates or replaces the connection for the path realm. The server forces the local realm, including realm predicates in attribute filters, to match the path.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.BadRequest
    void setConnection(@BeanParam RequestParams requestParams,
                       @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("realm") String realm,
                       @RequestBody(required = true, description = "Central-manager connection settings. localRealm and realms in attribute filters are forced to match the path realm.") @Valid GatewayConnection connection);

    @DELETE
    @Path("connection/{realm}")
    @RolesAllowed({Constants.WRITE_ADMIN_ROLE})
    @Operation(operationId = "deleteConnection", summary = "Delete the gateway connection of a realm",
        description = "Removes the central-manager connection for one realm. Realm administrators can remove their own connection; super users can remove any connection.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.BadRequest
    void deleteConnection(@BeanParam RequestParams requestParams,
                          @Parameter(description = REALM, example = EXAMPLE_REALM) @PathParam("realm") String realm);

    @DELETE
    @Path("connection")
    @RolesAllowed({Constants.WRITE_ADMIN_ROLE})
    @Operation(operationId = "deleteConnections", summary = "Delete the gateway connections of multiple realms",
        description = "Removes connections for all repeated realm query parameters. Deleting multiple realms is restricted to super users.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.BadRequest
    void deleteConnections(@BeanParam RequestParams requestParams,
                           @Parameter(description = "Realm names whose gateway connections should be removed. Repeat the query parameter for multiple realms.", example = EXAMPLE_REALM,
                               required = true, style = ParameterStyle.FORM, explode = Explode.TRUE) @QueryParam("realm") List<String> realms);
}
