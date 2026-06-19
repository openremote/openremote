/*
 * Copyright 2026 OpenRemote Inc.
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
package org.openremote.model.asset;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import org.openremote.model.http.RequestParams;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Tag(name = "Custom asset types", description = "Operations on user-defined asset type definitions")
@Path("custom-asset-types")
public interface CustomAssetTypeResource {

    @GET
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getCustomAssetTypes", summary = "Retrieve all custom asset type definitions")
    CustomAssetTypeDefinition[] getAll(@BeanParam RequestParams requestParams);

    @GET
    @Path("{name}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getCustomAssetType", summary = "Retrieve a custom asset type definition")
    CustomAssetTypeDefinition get(@BeanParam RequestParams requestParams, @PathParam("name") String name);

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "createCustomAssetType", summary = "Create a custom asset type definition")
    CustomAssetTypeDefinition create(
        @BeanParam RequestParams requestParams,
        @NotNull @Valid CustomAssetTypeDefinition definition
    );

    @PUT
    @Path("{name}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "updateCustomAssetType", summary = "Update a custom asset type definition")
    CustomAssetTypeDefinition update(
        @BeanParam RequestParams requestParams,
        @PathParam("name") String name,
        @NotNull @Valid CustomAssetTypeDefinition definition
    );

    @DELETE
    @Path("{name}")
    @Operation(operationId = "deleteCustomAssetType", summary = "Delete a custom asset type definition")
    void delete(@BeanParam RequestParams requestParams, @PathParam("name") String name);

    @POST
    @Path("{name}/validate")
    @Consumes(APPLICATION_JSON)
    @Operation(operationId = "validateCustomAssetType", summary = "Validate a custom asset type definition")
    void validate(
        @BeanParam RequestParams requestParams,
        @PathParam("name") String name,
        @NotNull @Valid CustomAssetTypeDefinition definition
    );

    @GET
    @Path("{name}/usage")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getCustomAssetTypeUsage", summary = "Retrieve custom asset type usage count")
    long getUsage(@BeanParam RequestParams requestParams, @PathParam("name") String name);
}
