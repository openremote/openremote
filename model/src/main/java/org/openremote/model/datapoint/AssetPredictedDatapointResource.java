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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import org.openremote.model.datapoint.query.AssetDatapointQuery;
import org.openremote.model.http.OpenApiResponses;
import org.openremote.model.http.RequestParams;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.openremote.model.http.OpenApiDescriptions.*;

@Tag(name = "Asset Predicted Datapoint", description = "Read and replace predicted future values for asset attributes")
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
    @Operation(operationId = "getPredictedDatapoints", summary = "Retrieve the predicted datapoints of an asset attribute",
        description = "Queries predicted values for one attribute using the supplied time-range and aggregation criteria. Anonymous access requires public-read access on both asset and attribute.")
    @OpenApiResponses.Ok
    @OpenApiResponses.BadRequest
    @OpenApiResponses.Forbidden
    @OpenApiResponses.NotFound
    ValueDatapoint<?>[] getPredictedDatapoints(@BeanParam RequestParams requestParams,
                                               @Parameter(description = ASSET_ID, example = EXAMPLE_ASSET_ID) @PathParam("assetId") String assetId,
                                               @Parameter(description = ATTRIBUTE_NAME, example = EXAMPLE_ATTRIBUTE_NAME) @PathParam("attributeName") String attributeName,
                                               @RequestBody(description = "Optional time range, ordering, limit, and aggregation strategy for predicted values.") AssetDatapointQuery query);

    @PUT
    @Path("{assetId}/{attributeName}")
    @Consumes(APPLICATION_JSON)
    @Operation(operationId = "writePredictedDatapoints", summary = "Write the predicted datapoints of an asset attribute",
        description = "Replaces or adds predicted values for one attribute. Anonymous access is allowed only when the asset and attribute are public-write; authenticated callers require attribute-write access.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.BadRequest
    @OpenApiResponses.Forbidden
    @OpenApiResponses.NotFound
    void writePredictedDatapoints(@BeanParam RequestParams requestParams,
                                  @Parameter(description = ASSET_ID, example = EXAMPLE_ASSET_ID) @PathParam("assetId") String assetId,
                                  @Parameter(description = ATTRIBUTE_NAME, example = EXAMPLE_ATTRIBUTE_NAME) @PathParam("attributeName") String attributeName,
                                  @RequestBody(required = true, description = "Timestamped predicted values to store for the attribute.") ValueDatapoint<?>[] predictedDatapoints);
}
