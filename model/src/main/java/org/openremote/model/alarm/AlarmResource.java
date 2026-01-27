/*
 * Copyright 2024, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.model.alarm;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.List;

import org.openremote.model.Constants;
import org.openremote.model.http.RequestParams;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;

@Tag(name = "Alarm", description = "Operations on alarms")
@Path("alarm")
public interface AlarmResource {

  @GET
  @Produces(APPLICATION_JSON)
  @RolesAllowed({Constants.READ_ALARMS_ROLE})
  @Operation(
      operationId = "getAlarms",
      summary = "Retrieve all alarms or a subset using filter criteria")
  SentAlarm[] getAlarms(
      @BeanParam RequestParams requestParams,
      @QueryParam("realm") String realm,
      @QueryParam("status") Alarm.Status status,
      @QueryParam("assetId") String assetId,
      @QueryParam("assigneeId") String assigneeId);

  @POST
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @RolesAllowed({Constants.WRITE_ALARMS_ROLE})
  @Operation(operationId = "createAlarm", summary = "Create an alarm")
  SentAlarm createAlarm(
      @BeanParam RequestParams requestParams,
      @RequestBody Alarm alarm,
      @QueryParam("assetIds") List<String> assetIds);

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
  void updateAlarm(
      @BeanParam RequestParams requestParams,
      @PathParam("alarmId") Long alarmId,
      @RequestBody SentAlarm alarm);

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
  List<AlarmAssetLink> getAssetLinks(
      @BeanParam RequestParams requestParams,
      @PathParam("alarmId") Long alarmId,
      @QueryParam("realm") String realm);

  @PUT
  @Path("assets")
  @Consumes(APPLICATION_JSON)
  @RolesAllowed({Constants.WRITE_ALARMS_ROLE})
  @Operation(operationId = "setAssetLinks", summary = "Set the asset links of an alarm")
  void setAssetLinks(
      @BeanParam RequestParams requestParams, @RequestBody List<AlarmAssetLink> links);
}
