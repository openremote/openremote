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

import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.http.RequestParams;
import org.openremote.model.value.MetaItemDescriptor;
import org.openremote.model.value.ValueDescriptor;

import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Resource for handling model requests and also providing server side validation of {@link Asset}s
 */
// TODO: Implement generic Asset<?> validation for assets and agents
@Tag(name = "Asset Model")
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
    AssetTypeInfo[] getAssetInfos(@BeanParam RequestParams requestParams, @QueryParam("parentId") String parentId, @QueryParam("parentType") String parentType);

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
    AssetTypeInfo getAssetInfo(@BeanParam RequestParams requestParams, @QueryParam("parentId") String parentId, @PathParam("assetType") String assetType);

    /**
     * Retrieve the asset descriptors {@link AssetDescriptor} available in this system or from a {@link
     * org.openremote.model.asset.impl.GatewayAsset} depending on whether or not a * parentId is supplied, if it isn't
     * then this instance is used, if it is and the {@link Asset} or one of its' * ancestors resides on a {@link
     * org.openremote.model.asset.impl.GatewayAsset} then that gateway instance is used.
     */
    @GET
    @Path("assetDescriptors")
    @Produces(APPLICATION_JSON)
    AssetDescriptor<?>[] getAssetDescriptors(@BeanParam RequestParams requestParams, @QueryParam("parentId") String parentId, @QueryParam("parentType") String parentType);

    /**
     * Retrieve value descriptors {@link ValueDescriptor} available in this system or from a {@link
     * org.openremote.model.asset.impl.GatewayAsset} depending on whether or not a * parentId is supplied, if it isn't
     * then this instance is used, if it is and the {@link Asset} or one of its' * ancestors resides on a {@link
     * org.openremote.model.asset.impl.GatewayAsset} then that gateway instance is used.
     */
    @GET
    @Path("valueDescriptors")
    @Produces(APPLICATION_JSON)
    ValueDescriptor<?>[] getValueDescriptors(@BeanParam RequestParams requestParams, @QueryParam("parentId") String parentId);

    /**
     * Retrieve meta descriptors {@link MetaItemDescriptor} available in this system or from a {@link
     * org.openremote.model.asset.impl.GatewayAsset} depending on whether or not a * parentId is supplied, if it isn't
     * then this instance is used, if it is and the {@link Asset} or one of its' * ancestors resides on a {@link
     * org.openremote.model.asset.impl.GatewayAsset} then that gateway instance is used.
     */
    @GET
    @Path("metaItemDescriptors")
    @Produces(APPLICATION_JSON)
    MetaItemDescriptor<?>[] getMetaItemDescriptors(@BeanParam RequestParams requestParams, @QueryParam("parentId") String parentId);
}
