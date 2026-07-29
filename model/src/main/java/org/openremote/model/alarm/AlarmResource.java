/*
 * Copyright 2024, OpenRemote Inc.
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
package org.openremote.model.alarm;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.Constants;
import org.openremote.model.http.OpenApiResponses;
import org.openremote.model.http.RequestParams;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.openremote.model.http.OpenApiDescriptions.*;
import static org.openremote.model.http.OpenApiExamples.ALARM_ASSET_LINK_INVALID_MULTIPLE_ALARMS;
import static org.openremote.model.http.OpenApiExamples.ALARM_ASSET_LINK_MULTIPLE;
import static org.openremote.model.http.OpenApiExamples.ALARM_ASSET_LINK_SINGLE;
import static org.openremote.model.http.OpenApiExamples.ALARM_CREATE;

@Tag(name = "Alarm", description = "Create, query, maintain, and associate alarms with assets")
@Path("alarm")
@OpenApiResponses.Authenticated
public interface AlarmResource {

    @GET
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ALARMS_ROLE})
    @Operation(operationId = "getAlarms", summary = "Retrieve all alarms or a subset using filter criteria",
        description = "Returns alarms in the requested accessible realm, optionally filtered by status, linked asset, or assignee. When realm is omitted, the authenticated realm is used.")
    @OpenApiResponses.Ok
    @OpenApiResponses.BadRequest
    SentAlarm[] getAlarms(@BeanParam RequestParams requestParams,
                          @Parameter(description = REALM + " Defaults to the authenticated realm.", example = EXAMPLE_REALM) @QueryParam("realm") String realm,
                          @Parameter(description = "Alarm lifecycle status to match.", example = "OPEN") @QueryParam("status") Alarm.Status status,
                          @Parameter(description = "Return alarms linked to this asset.", example = EXAMPLE_ASSET_ID) @QueryParam("assetId") String assetId,
                          @Parameter(description = "Return alarms assigned to this identity-provider user.", example = EXAMPLE_USER_ID) @QueryParam("assigneeId") String assigneeId);

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ALARMS_ROLE})
    @Operation(operationId = "createAlarm", summary = "Create an alarm",
        description = "Creates a manual alarm in an accessible realm and optionally links it to the supplied asset IDs. The authenticated user or client becomes the alarm source.")
    @OpenApiResponses.Ok
    @OpenApiResponses.BadRequest
    @OpenApiResponses.NotFound
    SentAlarm createAlarm(@BeanParam RequestParams requestParams,
                          @RequestBody(required = true, description = "Alarm to create. The source and sourceId are assigned from the authenticated caller.",
                              content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = Alarm.class),
                                  examples = @ExampleObject(name = "High temperature alarm", summary = "Create an open high-severity alarm", value = ALARM_CREATE))) Alarm alarm,
                          @Parameter(description = "Asset IDs to link to the new alarm. Repeat the query parameter for multiple assets.", example = EXAMPLE_ASSET_ID,
                              style = ParameterStyle.FORM, explode = Explode.TRUE) @QueryParam("assetIds") List<String> assetIds);

    @DELETE
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ALARMS_ROLE})
    @Operation(operationId = "removeAlarms", summary = "Remove alarms",
        description = "Permanently removes all alarms identified by the request body after verifying access to every alarm's realm.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.BadRequest
    @OpenApiResponses.NotFound
    void removeAlarms(@BeanParam RequestParams requestParams,
                      @RequestBody(required = true, description = "Numeric IDs of the alarms to remove.") List<Long> ids);

    @GET
    @Path("{alarmId}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ALARMS_ROLE})
    @Operation(operationId = "getAlarm", summary = "Retrieve an alarm",
        description = "Returns one alarm when its realm is active and accessible to the authenticated user.")
    @OpenApiResponses.Ok
    @OpenApiResponses.NotFound
    SentAlarm getAlarm(@BeanParam RequestParams requestParams,
                       @Parameter(description = "Numeric alarm identifier.", example = "1234") @PathParam("alarmId") Long alarmId);

    @PUT
    @Path("{alarmId}")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ALARMS_ROLE})
    @Operation(operationId = "updateAlarm", summary = "Update an alarm",
        description = "Updates the mutable state of an existing alarm after verifying access to its current realm.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.NotFound
    void updateAlarm(@BeanParam RequestParams requestParams,
                     @Parameter(description = "Numeric alarm identifier.", example = "1234") @PathParam("alarmId") Long alarmId,
                     @RequestBody(required = true, description = "Complete updated alarm state.") SentAlarm alarm);

    @DELETE
    @Path("{alarmId}")
    @RolesAllowed({Constants.WRITE_ALARMS_ROLE})
    @Operation(operationId = "removeAlarm", summary = "Remove an alarm",
        description = "Permanently removes one alarm after verifying access to its realm.")
    @OpenApiResponses.NoContent
    @OpenApiResponses.BadRequest
    @OpenApiResponses.NotFound
    void removeAlarm(@BeanParam RequestParams requestParams,
                     @Parameter(description = "Numeric alarm identifier.", example = "1234") @PathParam("alarmId") Long alarmId);

    @GET
    @Path("{alarmId}/assets")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ALARMS_ROLE})
    @Operation(operationId = "getAssetLinks", summary = "Retrieve the asset links of an alarm",
        description = "Returns links from the alarm to assets in the explicitly supplied accessible realm.")
    @OpenApiResponses.Ok
    @OpenApiResponses.NotFound
    List<AlarmAssetLink> getAssetLinks(@BeanParam RequestParams requestParams,
                                       @Parameter(description = "Numeric alarm identifier.", example = "1234") @PathParam("alarmId") Long alarmId,
                                       @Parameter(description = REALM, example = EXAMPLE_REALM, required = true) @QueryParam("realm") String realm);

    @PUT
    @Path("assets")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ALARMS_ROLE})
    @Operation(
            operationId = "setAssetLinks",
            summary = "Add asset links to a single alarm",
            description = """
                    Adds links between assets and one existing alarm.

                    The request body is a list for backward compatibility, but this operation now has single-alarm semantics:
                    every item must contain the same `id.realm` and the same `id.alarmId`. The alarm must exist in that realm,
                    every asset must exist in the same realm, and the authenticated user must have access to that realm. Requests
                    containing mixed alarm IDs or realms are rejected as bad requests.

                    Existing links are left unchanged; duplicate links in the request or links that already exist are ignored.
                    This operation does not remove existing links from the alarm.
                    """
    )
    @OpenApiResponses.NoContent
    @OpenApiResponses.BadRequest
    @OpenApiResponses.NotFound
    void setAssetLinks(
            @BeanParam RequestParams requestParams,
            @RequestBody(
                    required = true,
                    description = "Alarm asset links to add. All items must use the same `id.realm` and `id.alarmId`.",
                    content = @Content(
                            mediaType = APPLICATION_JSON,
                            array = @ArraySchema(schema = @Schema(implementation = AlarmAssetLink.class)),
                            examples = {
                                    @ExampleObject(
                                            name = "Single asset link",
                                            summary = "Link one asset to one alarm",
                                            value = ALARM_ASSET_LINK_SINGLE
                                    ),
                                    @ExampleObject(
                                            name = "Multiple asset links",
                                            summary = "Link multiple assets to the same alarm",
                                            value = ALARM_ASSET_LINK_MULTIPLE
                                    ),
                                    @ExampleObject(
                                            name = "Invalid multiple alarms",
                                            summary = "Rejected because the request targets more than one alarm",
                                            value = ALARM_ASSET_LINK_INVALID_MULTIPLE_ALARMS
                                    )
                            }
                    )
            ) List<AlarmAssetLink> links);
}
