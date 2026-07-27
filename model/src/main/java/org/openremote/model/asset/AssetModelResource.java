/*
 * Copyright 2019, OpenRemote Inc.
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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Request;
import org.jboss.resteasy.annotations.cache.Cache;
import org.openremote.model.http.OpenApiResponses;
import org.openremote.model.http.RequestParams;
import org.openremote.model.value.MetaItemDescriptor;
import org.openremote.model.value.ValueDescriptor;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.Map;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.openremote.model.http.OpenApiDescriptions.EXAMPLE_ASSET_ID;

/**
 * Resource for handling model requests and also providing server side validation of {@link Asset}s
 */
// TODO: Implement generic Asset<?> validation for assets and agents
@Tag(name = "Asset Model", description = "Discover asset, attribute-value, and metadata types supported by this Manager or a gateway")
@Path("model")
public interface AssetModelResource {

    /**
     * Retrieve the {@link AssetTypeInfo} of each {@link Asset} type available
     * in this system or from a {@link org.openremote.model.asset.impl.GatewayAsset} depending on whether or not a
     * parentId is supplied, if it isn't then this instance is used, if it is and the {@link Asset} or one of its'
     * ancestors resides on a {@link org.openremote.model.asset.impl.GatewayAsset} then that gateway instance is used.
     */
    @GET
    @Path("assetInfos")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getAssetInfos", summary = "Retrieve the asset type information of each available asset type",
        description = "Returns type metadata from this Manager by default. When parentId identifies an asset below a gateway, the gateway's model is used instead; parentType can provide type context for a new parent that does not yet exist.")
    @OpenApiResponses.Ok
    AssetTypeInfo[] getAssetInfos(@BeanParam RequestParams requestParams,
                                   @Parameter(description = "Optional parent asset used to select the local or gateway model", example = EXAMPLE_ASSET_ID) @QueryParam("parentId") String parentId,
                                   @Parameter(description = "Optional parent asset type, primarily for assets that have not been persisted yet", example = "GroupAsset") @QueryParam("parentType") String parentType);

    /**
     * Retrieve the specific {@link AssetTypeInfo} of the specified} {@link
     * Asset} type available in this system or from a {@link org.openremote.model.asset.impl.GatewayAsset} depending on
     * whether or not a parentId * is supplied, if it isn't then this instance is used, if it is and the {@link Asset}
     * or one of its' ancestors resides * on a {@link org.openremote.model.asset.impl.GatewayAsset} then that gateway
     * instance is used.
     */
    @GET
    @Path("assetInfo/{assetType}")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getAssetInfo", summary = "Retrieve the asset type information of an asset type",
        description = "Returns metadata for one asset type from this Manager or, when parentId is below a gateway, from that gateway's model. An unknown or gateway-provided type that is not locally available returns no content.")
    @OpenApiResponses.Ok
    @ApiResponse(responseCode = "204", description = "No model information is available for the requested asset type")
    AssetTypeInfo getAssetInfo(@BeanParam RequestParams requestParams,
                               @Parameter(description = "Optional parent asset used to select the local or gateway model", example = EXAMPLE_ASSET_ID) @QueryParam("parentId") String parentId,
                               @Parameter(description = "Asset type name", example = "ThingAsset", required = true) @PathParam("assetType") String assetType);

    /**
     * Retrieve the asset descriptors {@link AssetDescriptor} available in this system or from a {@link
     * org.openremote.model.asset.impl.GatewayAsset} depending on whether or not a * parentId is supplied, if it isn't
     * then this instance is used, if it is and the {@link Asset} or one of its' * ancestors resides on a {@link
     * org.openremote.model.asset.impl.GatewayAsset} then that gateway instance is used.
     */
    @GET
    @Path("assetDescriptors")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getAssetDescriptors", summary = "Retrieve the available asset descriptors",
        description = "Returns the descriptors used to construct supported asset types, using a gateway model when the optional parent context resolves below a gateway.")
    @OpenApiResponses.Ok
    AssetDescriptor<?>[] getAssetDescriptors(@BeanParam RequestParams requestParams,
                                              @Parameter(description = "Optional parent asset used to select the local or gateway model", example = EXAMPLE_ASSET_ID) @QueryParam("parentId") String parentId,
                                              @Parameter(description = "Optional parent asset type, primarily for assets that have not been persisted yet", example = "GroupAsset") @QueryParam("parentType") String parentType);

    /**
     * Retrieve value descriptors {@link ValueDescriptor} available in this system or from a {@link
     * org.openremote.model.asset.impl.GatewayAsset} depending on whether or not a  parentId is supplied, if it isn't
     * then this instance is used, if it is and the {@link Asset} or one of its' ancestors resides on a {@link
     * org.openremote.model.asset.impl.GatewayAsset} then that gateway instance is used.
     */
    @GET
    @Path("valueDescriptors")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getValueDescriptors", summary = "Retrieve the available value descriptors",
        description = "Returns attribute value descriptors keyed by descriptor name, using a gateway model when parentId resolves below a gateway.")
    @OpenApiResponses.Ok
    Map<String, ValueDescriptor<?>> getValueDescriptors(@BeanParam RequestParams requestParams,
                                                         @Parameter(description = "Optional parent asset used to select the local or gateway model", example = EXAMPLE_ASSET_ID) @QueryParam("parentId") String parentId);

    /**
     * Retrieve meta descriptors {@link MetaItemDescriptor} available in this system or from a {@link
     * org.openremote.model.asset.impl.GatewayAsset} depending on whether or not a parentId is supplied, if it isn't
     * then this instance is used, if it is and the {@link Asset} or one of its' ancestors resides on a {@link
     * org.openremote.model.asset.impl.GatewayAsset} then that gateway instance is used.
     */
    @GET
    @Path("metaItemDescriptors")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getMetaItemDescriptors", summary = "Retrieve the available meta item descriptors",
        description = "Returns attribute metadata descriptors keyed by descriptor name, using a gateway model when parentId resolves below a gateway.")
    @OpenApiResponses.Ok
    Map<String, MetaItemDescriptor<?>> getMetaItemDescriptors(@BeanParam RequestParams requestParams,
                                                               @Parameter(description = "Optional parent asset used to select the local or gateway model", example = EXAMPLE_ASSET_ID) @QueryParam("parentId") String parentId);

    /**
     * Retrieve the JSON Schema for a {@link ValueDescriptor} available in this system. A value descriptor schema is only meant to be retrieved
     * once per client. Either when a new {@code name} is requested or the "If-None-Match" header does not match the current ETag. The HTTP client should
     * use the provided ETag to cache the response.
     */
    @GET
    @Path("getValueDescriptorSchema")
    @Produces(APPLICATION_JSON)
    @Cache(noCache = true)
    @Operation(operationId = "getValueDescriptorSchema", summary = "Retrieve the JSON Schema of the specified value descriptor",
        description = "Returns the JSON Schema and a weak ETag for the named value descriptor. Send If-None-Match on later requests to receive 304 when the schema has not changed.")
    @ApiResponse(responseCode = "200", description = "The current JSON Schema",
        headers = @Header(name = "ETag", description = "Weak entity tag identifying this schema version", schema = @Schema(type = "string", example = "W/\"17af8b28\"")),
        content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(type = "object")))
    @ApiResponse(responseCode = "304", description = "The supplied ETag still identifies the current schema",
        headers = @Header(name = "ETag", description = "Entity tag supplied by the client", schema = @Schema(type = "string")))
    @OpenApiResponses.NotFound
    Response getValueDescriptorSchema(
        @Parameter(description = "Value descriptor name", example = "number", required = true) @QueryParam("name") String name,
        @Context Request request);
}
