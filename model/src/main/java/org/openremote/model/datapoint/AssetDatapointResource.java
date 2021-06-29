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
import org.openremote.model.Constants;
import org.openremote.model.http.RequestParams;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Tag(name = "Asset Datapoint")
@Path("asset/datapoint")
public interface AssetDatapointResource {

    /**
     * Retrieve the historical datapoints of an asset attribute. Regular users can only access assets in their
     * authenticated realm, the superuser can access assets in other (all) realms. A 403 status is returned if a
     * regular user tries to access an asset in a realm different than its authenticated realm, or if the user is
     * restricted and the asset is not linked to the user. A 400 status is returned if the asset attribute does
     * not have datapoint storage enabled.
     */
    @GET
    @Path("{assetId}/attribute/{attributeName}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ASSETS_ROLE})
    ValueDatapoint<?>[] getDatapoints(@BeanParam RequestParams requestParams,
                                   @PathParam("assetId") String assetId,
                                   @PathParam("attributeName") String attributeName,
                                   @QueryParam("interval") DatapointInterval datapointInterval,
                                   @QueryParam("step") Integer stepSize,
                                   @QueryParam("fromTimestamp") long fromTimestamp,
                                   @QueryParam("toTimestamp") long toTimestamp);

    @GET
    @Path("periods")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ASSETS_ROLE})
    DatapointPeriod getDatapointPeriod(@BeanParam RequestParams requestParams,
                                          @QueryParam("assetId") String assetId,
                                          @QueryParam("attributeName") String attributeName);

    @GET
    @Path("export")
    @Produces("application/zip")
    @RolesAllowed({Constants.READ_ASSETS_ROLE})
    void getDatapointExport(@Suspended AsyncResponse asyncResponse,
                            @QueryParam("attributeRefs") String attributeRefsString,
                            @QueryParam("fromTimestamp") long fromTimestamp,
                            @QueryParam("toTimestamp") long toTimestamp);
}
