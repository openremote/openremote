/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.model.provisioning;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.http.OpenApiResponses;
import org.openremote.model.http.RequestParams;

import jakarta.validation.Valid;
import jakarta.ws.rs.*;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Tag(name = "Provisioning", description = "Super-administrator operations for device provisioning configurations")
@Path("provisioning")
public interface ProvisioningResource {

    @GET
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getProvisioningConfigs", summary = "Retrieve all provisioning configurations",
        description = "Returns every provisioning configuration. This operation is restricted to a super administrator.")
    @OpenApiResponses.Ok
    @OpenApiResponses.Authenticated
    ProvisioningConfig<?, ?>[] getProvisioningConfigs();

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "createProvisioningConfig", summary = "Create a provisioning configuration",
        description = "Persists a new provisioning configuration and returns its generated numeric identifier. This operation is restricted to a super administrator.")
    @OpenApiResponses.Ok
    @OpenApiResponses.Authenticated
    @OpenApiResponses.BadRequest
    long createProvisioningConfig(
        @RequestBody(required = true, description = "Complete typed provisioning configuration to create.") ProvisioningConfig<?, ?> provisioningConfig);

    @PUT
    @Path("{id}")
    @Consumes(APPLICATION_JSON)
    @Operation(operationId = "updateProvisioningConfig", summary = "Update a provisioning configuration",
        description = "Replaces the provisioning configuration identified by the path. This operation is restricted to a super administrator.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.Authenticated
    @OpenApiResponses.BadRequest
    void updateProvisioningConfig(
        @BeanParam RequestParams requestParams,
        @Parameter(description = "Numeric provisioning configuration identifier.", example = "12") @PathParam("id") Long id,
        @Valid @RequestBody(required = true, description = "Complete typed provisioning configuration to replace the existing configuration.") ProvisioningConfig<?, ?> provisioningConfig);

    @DELETE
    @Path("{id}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "deleteProvisioningConfig", summary = "Delete a provisioning configuration",
        description = "Permanently removes the provisioning configuration identified by the path. This operation is restricted to a super administrator.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.Authenticated
    void deleteProvisioningConfig(
        @BeanParam RequestParams requestParams,
        @Parameter(description = "Numeric provisioning configuration identifier.", example = "12") @PathParam("id") Long id);
}
