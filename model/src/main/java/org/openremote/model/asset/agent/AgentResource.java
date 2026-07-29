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
package org.openremote.model.asset.agent;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.Constants;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetResource;
import org.openremote.model.asset.AssetTreeNode;
import org.openremote.model.file.FileInfo;
import org.openremote.model.http.OpenApiResponses;
import org.openremote.model.http.RequestParams;
import org.openremote.model.protocol.ProtocolInstanceDiscovery;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.openremote.model.http.OpenApiDescriptions.*;

/**
 * This resource is for Agent specific tasks like import and discovery; normal asset/attribute CRUD operations should
 * still use {@link AssetResource}.
 */
@Tag(name = "Agent", description = "Discover protocol instances and import or discover assets through Agent assets")
@Path("agent")
@OpenApiResponses.Authenticated
public interface AgentResource {

    /**
     * Do protocol instance ({@link Agent}) discovery for the specified agent type {@link AgentDescriptor}; the
     * associated {@link Protocol} must implement {@link ProtocolInstanceDiscovery} otherwise an empty set of results
     * will be returned. The {@link Asset} parent where the {@link Agent} will be added should be specified so the
     * backend can determine if the {@link Agent} is being created on an Edge gateway instance or on this local
     * instance.
     *
     * @return A list of {@link Agent}s that can be created to create a connection to the discovered instance(s).
     */
    @GET
    @Path("instanceDiscovery/{agentType}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ASSETS_ROLE})
    @Operation(operationId = "doProtocolInstanceDiscovery", summary = "Discover protocol instances",
        description = "Runs instance discovery for an agent type and returns candidate Agent assets. parentId selects local versus edge-gateway discovery; non-super users are always constrained to their authenticated realm.")
    @OpenApiResponses.Ok
    @OpenApiResponses.BadRequest
    @OpenApiResponses.NotFound
    @ApiResponse(responseCode = "415", description = "The selected agent protocol does not support instance discovery")
    Agent<?, ?, ?>[] doProtocolInstanceDiscovery(
        @BeanParam RequestParams requestParams,
        @Parameter(description = "Optional parent asset used to choose local or edge-gateway discovery.", example = EXAMPLE_ASSET_ID) @QueryParam("parentId") String parentId,
        @Parameter(description = "Registered agent asset type whose protocol supports instance discovery.", example = "HTTPAgent") @PathParam("agentType") String agentType,
        @Parameter(description = REALM + " Non-super users are always restricted to their authenticated realm.", example = EXAMPLE_REALM) @QueryParam("realm") String realm
    );

    /**
     * Do {@link Asset} discovery for the specified {@link Agent}; the associated {@link Protocol} must implement {@link
     * org.openremote.model.protocol.ProtocolAssetDiscovery} otherwise an empty set of results will be returned.
     * <p>
     * Currently this request will automatically add the found {@link Asset}s to the system as children of the specified
     * {@link Agent} as well as returning them in the response.
     */
    @GET
    @Path("assetDiscovery/{agentId}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ASSETS_ROLE})
    @Operation(operationId = "doProtocolAssetDiscovery", summary = "Discover assets through an agent",
        description = "Runs the agent protocol's asset discovery, waits up to ten seconds for results, returns the discovered tree, and asynchronously persists discovered assets below the agent.")
    @OpenApiResponses.Ok
    @OpenApiResponses.BadRequest
    @OpenApiResponses.NotFound
    @OpenApiResponses.ServerError
    AssetTreeNode[] doProtocolAssetDiscovery(
        @BeanParam RequestParams requestParams,
        @Parameter(description = "Agent asset that performs discovery.", example = EXAMPLE_ASSET_ID) @PathParam("agentId") String agentId,
        @Parameter(description = REALM + " Non-super users are always restricted to their authenticated realm.", example = EXAMPLE_REALM) @QueryParam("realm") String realm
    );

    /**
     * Do {@link Asset} import for the specified {@link Agent} using the supplied {@link FileInfo}; the associated
     * {@link Protocol} must implement {@link org.openremote.model.protocol.ProtocolAssetImport} otherwise an empty set
     * of results will be returned.
     * <p>
     * Currently this request will automatically add the found {@link Asset}s to the system as children of the specified
     * {@link Agent} as well as returning them in the response.
     * <b>NOTE:</b> The {@link FileInfo} must be a file that the protocol understands.
     */
    @POST
    @Path("assetImport/{agentId}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ASSETS_ROLE})
    @Operation(operationId = "doProtocolAssetImport", summary = "Import assets through an agent",
        description = "Decodes the supplied FileInfo, asks the agent protocol to import it, persists the returned asset tree below the agent, and returns that tree.")
    @OpenApiResponses.Ok
    @OpenApiResponses.BadRequest
    @OpenApiResponses.NotFound
    @ApiResponse(responseCode = "405", description = "The selected agent protocol does not support asset import")
    @OpenApiResponses.ServerError
    // TODO: File upload should use standard multipart mechanism
    AssetTreeNode[] doProtocolAssetImport(
        @BeanParam RequestParams requestParams,
        @Parameter(description = "Agent asset that performs the import.", example = EXAMPLE_ASSET_ID) @PathParam("agentId") String agentId,
        @Parameter(description = REALM + " Non-super users are always restricted to their authenticated realm.", example = EXAMPLE_REALM) @QueryParam("realm") String realm,
        @RequestBody(required = true, description = "File metadata and Base64 or text contents in the format understood by the selected agent protocol.") FileInfo fileInfo
    );
}
