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

import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.Constants;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetResource;
import org.openremote.model.asset.AssetTreeNode;
import org.openremote.model.file.FileInfo;
import org.openremote.model.http.RequestParams;
import org.openremote.model.protocol.ProtocolInstanceDiscovery;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * This resource is for Agent specific tasks like import and discovery; normal asset/attribute CRUD operations should
 * still use {@link AssetResource}.
 */
@Tag(name = "Agent")
@Path("agent")
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
    Agent<?, ?, ?>[] doProtocolInstanceDiscovery(
        @BeanParam RequestParams requestParams,
        @QueryParam("parentId") String parentId,
        @PathParam("agentType") String agentType,
        @QueryParam("realm") String realm
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
    AssetTreeNode[] doProtocolAssetDiscovery(
        @BeanParam RequestParams requestParams,
        @PathParam("agentId") String agentId,
        @QueryParam("realm") String realm
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
    // TODO: File upload should use standard multipart mechanism
    AssetTreeNode[] doProtocolAssetImport(
        @BeanParam RequestParams requestParams,
        @PathParam("agentId") String agentId,
        @QueryParam("realm") String realm,
        FileInfo fileInfo
    );
}
