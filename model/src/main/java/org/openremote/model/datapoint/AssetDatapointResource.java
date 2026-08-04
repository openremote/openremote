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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.Constants;
import org.openremote.model.datapoint.query.AssetDatapointAllQuery;
import org.openremote.model.datapoint.query.AssetDatapointIntervalQuery;
import org.openremote.model.datapoint.query.AssetDatapointLTTBQuery;
import org.openremote.model.datapoint.query.AssetDatapointNearestQuery;
import org.openremote.model.datapoint.query.AssetDatapointQuery;
import org.openremote.model.http.OpenApiResponses;
import org.openremote.model.http.RequestParams;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.openremote.model.http.OpenApiDescriptions.*;
import static org.openremote.model.http.OpenApiExamples.*;

@Tag(name = "Asset Datapoint", description = "Query and export historical values stored for asset attributes")
@Path("asset/datapoint")
public interface AssetDatapointResource {

    /**
     * Retrieve the historical datapoints of an asset attribute. Regular users can only access assets in their
     * authenticated realm, the superuser can access assets in other (all) realms. A 403 status is returned if a
     * regular user tries to access an asset in a realm different than its authenticated realm, or if the user is
     * restricted and the asset is not linked to the user. A 400 status is returned if the asset attribute does
     * not have datapoint storage enabled.
     */
    @POST
    @Path("{assetId}/{attributeName}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getDatapoints", summary = "Retrieve the historical datapoints of an asset attribute",
        description = "Returns stored historical values for one attribute. A null body returns the available series; a polymorphic query body controls the time range and retrieval or aggregation strategy. Anonymous access requires public-read access on both asset and attribute.")
    @OpenApiResponses.Ok
    @OpenApiResponses.BadRequest
    @OpenApiResponses.Forbidden
    @OpenApiResponses.NotFound
    @OpenApiResponses.PayloadTooLarge
    ValueDatapoint<?>[] getDatapoints(@BeanParam RequestParams requestParams,
                                      @Parameter(description = ASSET_ID, example = EXAMPLE_ASSET_ID) @PathParam("assetId") String assetId,
                                      @Parameter(description = ATTRIBUTE_NAME, example = EXAMPLE_ATTRIBUTE_NAME) @PathParam("attributeName") String attributeName,
                                      @RequestBody(description = "Optional polymorphic datapoint query. Set type to all, interval, lttb, or nearest. Omit the body to return the available stored series.",
                                          content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(
                                              oneOf = {
                                                  AssetDatapointAllQuery.class,
                                                  AssetDatapointIntervalQuery.class,
                                                  AssetDatapointLTTBQuery.class,
                                                  AssetDatapointNearestQuery.class
                                              }),
                                              examples = {
                                                  @ExampleObject(name = "All values", summary = "Return every datapoint in a one-day inclusive range", value = DATAPOINT_ALL_QUERY),
                                                  @ExampleObject(name = "Hourly average", summary = "Average one day of values into hourly buckets and fill gaps", value = DATAPOINT_INTERVAL_QUERY),
                                                  @ExampleObject(name = "LTTB downsampling", summary = "Downsample one day to at most 500 representative points", value = DATAPOINT_LTTB_QUERY),
                                                  @ExampleObject(name = "Nearest value", summary = "Return the closest datapoint at or before a Unix-seconds timestamp", value = DATAPOINT_NEAREST_QUERY)
                                              })) AssetDatapointQuery query);

    @GET
    @Path("periods")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ASSETS_ROLE})
    @Operation(operationId = "getDatapointPeriod", summary = "Retrieve a datapoint period of an asset attribute",
        description = "Returns the oldest and newest stored timestamps for one attribute. The asset and attribute must be readable by the authenticated user and configured for datapoint storage.")
    @OpenApiResponses.Ok
    @OpenApiResponses.Authenticated
    @OpenApiResponses.BadRequest
    @OpenApiResponses.NotFound
    DatapointPeriod getDatapointPeriod(@BeanParam RequestParams requestParams,
                                          @Parameter(description = ASSET_ID, example = EXAMPLE_ASSET_ID, required = true) @QueryParam("assetId") String assetId,
                                          @Parameter(description = ATTRIBUTE_NAME, example = EXAMPLE_ATTRIBUTE_NAME, required = true) @QueryParam("attributeName") String attributeName);

    @GET
    @Path("export")
    @Produces("application/zip")
    @RolesAllowed({Constants.READ_ASSETS_ROLE})
    @Operation(operationId = "getDatapointExport", summary = "Export historical datapoints",
        description = "Streams a ZIP archive containing exported datapoints for one or more attribute references over the requested inclusive time range. attributeRefs is a JSON-encoded array of objects with id and name fields.")
    @ApiResponse(responseCode = "200", description = "A ZIP archive containing the exported data",
        headers = @Header(name = "Content-Disposition", description = "Attachment filename for the ZIP archive", schema = @Schema(type = "string", example = "attachment; filename=\"dataexport.zip\"")),
        content = @Content(mediaType = "application/zip", schema = @Schema(type = "string", format = "binary")))
    @OpenApiResponses.Authenticated
    @OpenApiResponses.BadRequest
    @OpenApiResponses.NotFound
    @OpenApiResponses.PayloadTooLarge
    @OpenApiResponses.ServerError
    void getDatapointExport(@Suspended AsyncResponse asyncResponse,
                            @Parameter(description = "JSON-encoded array of objects with id and name fields.", example = "[{\"id\":\"7A6p4AnLTkKxJUCQAAABAA\",\"name\":\"temperature\"}]", required = true) @QueryParam("attributeRefs") String attributeRefsString,
                            @Parameter(description = "Start of the export range as Unix time in milliseconds.", example = EXAMPLE_TIMESTAMP, required = true) @QueryParam("fromTimestamp") long fromTimestamp,
                            @Parameter(description = "End of the export range as Unix time in milliseconds.", example = "1767312000000", required = true) @QueryParam("toTimestamp") long toTimestamp,
                            @Parameter(description = "Export file format.", example = "CSV") @QueryParam("format") @DefaultValue("CSV") DatapointExportFormat format);

}
