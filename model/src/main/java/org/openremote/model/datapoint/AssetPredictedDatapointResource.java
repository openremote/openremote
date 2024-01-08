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
package org.openremote.model.datapoint;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import org.openremote.model.datapoint.query.AssetDatapointQuery;
import org.openremote.model.http.RequestParams;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Tag(name = "Asset Predicted Datapoint")
@Path("asset/predicted")
public interface AssetPredictedDatapointResource {
    /**
     * Retrieve the predicted datapoints of an asset attribute. Regular users can only access assets in their
     * authenticated realm, the superuser can access assets in other (all) realms. A 403 status is returned if a
     * regular user tries to access an asset in a realm different than its authenticated realm, or if the user is
     * restricted and the asset is not linked to the user. A 400 status is returned if the asset attribute does
     * not have datapoint storage enabled.
     */
    @POST
    @Path("{assetId}/{attributeName}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    ValueDatapoint<?>[] getPredictedDatapoints(@BeanParam RequestParams requestParams,
                                               @PathParam("assetId") String assetId,
                                               @PathParam("attributeName") String attributeName,
                                               AssetDatapointQuery query);

    @PUT
    @Path("{assetId}/{attributeName}")
    @Consumes(APPLICATION_JSON)
    void writePredictedDatapoints(@BeanParam RequestParams requestParams,
                                  @PathParam("assetId") String assetId,
                                  @PathParam("attributeName") String attributeName,
                                  ValueDatapoint<?>[] predictedDatapoints);
}
