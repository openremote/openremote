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
package org.openremote.manager.shared.connector;

import jsinterop.annotations.JsType;
import org.openremote.manager.shared.asset.AssetInfo;
import org.openremote.manager.shared.attribute.Attributes;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.http.SuccessStatusCode;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("connector")
@JsType(isNative = true)
public interface ConnectorResource {
    @GET
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    // TODO Implement admin roles on server
    //@RolesAllowed({"read:admin"})
    AssetInfo[] getConnectors(@BeanParam RequestParams requestParams);

    @GET
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @Path("{connectorId}")
    // TODO Implement admin roles on server
    //@RolesAllowed({"read:admin"})
    AssetInfo getConnector(@BeanParam RequestParams requestParams, @PathParam("connectorId")String connectorId);

    @GET
    @Path("{assetId}/children")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    // TODO Implement admin roles on server
    //@RolesAllowed({"read:assets"})
    AssetInfo[] getChildren(@BeanParam RequestParams requestParams, @PathParam("assetId") String parentId);

    @GET
    @Path("settings/new/{parentAssetId}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    Attributes getAssetSettings(@BeanParam RequestParams requestParams, @PathParam("parentAssetId") String parentId);

    @GET
    @Path("settings/discovery/{parentAssetId}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    Attributes getAssetDiscoverySettings(@BeanParam RequestParams requestParams, @PathParam("parentAssetId") String parentId);
}
