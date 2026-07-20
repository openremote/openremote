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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.Constants;
import org.openremote.model.http.RequestParams;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Tag(name = "Alarm", description = "Operations on alarms")
@Path("alarm")
public interface AlarmResource {

    @GET
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ALARMS_ROLE})
    @Operation(operationId = "getAlarms", summary = "Retrieve all alarms or a subset using filter criteria")
    SentAlarm[] getAlarms(@BeanParam RequestParams requestParams, @QueryParam("realm") String realm,
                          @QueryParam("status") Alarm.Status status, @QueryParam("assetId") String assetId,
                          @QueryParam("assigneeId") String assigneeId);

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ALARMS_ROLE})
    @Operation(operationId = "createAlarm", summary = "Create an alarm")
    SentAlarm createAlarm(@BeanParam RequestParams requestParams, @RequestBody Alarm alarm, @QueryParam("assetIds") List<String> assetIds);

    @DELETE
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ALARMS_ROLE})
    @Operation(operationId = "removeAlarms", summary = "Remove alarms")
    void removeAlarms(@BeanParam RequestParams requestParams, @RequestBody List<Long> ids);

    @GET
    @Path("{alarmId}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ALARMS_ROLE})
    @Operation(operationId = "getAlarm", summary = "Retrieve an alarm")
    SentAlarm getAlarm(@BeanParam RequestParams requestParams, @PathParam("alarmId") Long alarmId);

    @PUT
    @Path("{alarmId}")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ALARMS_ROLE})
    @Operation(operationId = "updateAlarm", summary = "Update an alarm")
    void updateAlarm(@BeanParam RequestParams requestParams, @PathParam("alarmId") Long alarmId, @RequestBody SentAlarm alarm);

    @DELETE
    @Path("{alarmId}")
    @RolesAllowed({Constants.WRITE_ALARMS_ROLE})
    @Operation(operationId = "removeAlarm", summary = "Remove an alarm")
    void removeAlarm(@BeanParam RequestParams requestParams, @PathParam("alarmId") Long alarmId);

    @GET
    @Path("{alarmId}/assets")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ALARMS_ROLE})
    @Operation(operationId = "getAssetLinks", summary = "Retrieve the asset links of an alarm")
    List<AlarmAssetLink> getAssetLinks(@BeanParam RequestParams requestParams, @PathParam("alarmId") Long alarmId,
                                       @QueryParam("realm") String realm);

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
                    every asset must exist in the same realm, and the authenticated user must have access to that realm.

                    Existing links are left unchanged; duplicate links in the request or links that already exist are ignored.
                    This operation does not remove existing links from the alarm.
                    """,
            responses = {
                    @ApiResponse(responseCode = "204", description = "Asset links were added, or already existed"),
                    @ApiResponse(responseCode = "400", description = "Invalid request body, multiple alarm IDs or realms were provided, or the alarm/assets are not all in the same realm"),
                    @ApiResponse(responseCode = "401", description = "Authentication is required"),
                    @ApiResponse(responseCode = "403", description = "The authenticated user is not allowed to write alarms in the requested realm"),
                    @ApiResponse(responseCode = "404", description = "The alarm or one or more assets do not exist")
            }
    )
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
                                            value = """
                                                    [
                                                      {
                                                        "id": {
                                                          "realm": "building",
                                                          "alarmId": 1234,
                                                          "assetId": "7A6p4AnLTkKxJUCQAAABAA"
                                                        }
                                                      }
                                                    ]
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Multiple asset links",
                                            summary = "Link multiple assets to the same alarm",
                                            value = """
                                                    [
                                                      {
                                                        "id": {
                                                          "realm": "building",
                                                          "alarmId": 1234,
                                                          "assetId": "7A6p4AnLTkKxJUCQAAABAA"
                                                        }
                                                      },
                                                      {
                                                        "id": {
                                                          "realm": "building",
                                                          "alarmId": 1234,
                                                          "assetId": "2Qjr4AnLTkKxJUCQAAACAA"
                                                        }
                                                      }
                                                    ]
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Invalid multiple alarms",
                                            summary = "Rejected because the request targets more than one alarm",
                                            value = """
                                                    [
                                                      {
                                                        "id": {
                                                          "realm": "building",
                                                          "alarmId": 1234,
                                                          "assetId": "7A6p4AnLTkKxJUCQAAABAA"
                                                        }
                                                      },
                                                      {
                                                        "id": {
                                                          "realm": "building",
                                                          "alarmId": 5678,
                                                          "assetId": "2Qjr4AnLTkKxJUCQAAACAA"
                                                        }
                                                      }
                                                    ]
                                                    """
                                    )
                            }
                    )
            ) List<AlarmAssetLink> links);
}
