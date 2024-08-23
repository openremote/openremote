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

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.Constants;
import org.openremote.model.http.RequestParams;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Tag(name = "Alarm")
@Path("alarm")
@Consumes(APPLICATION_JSON)
public interface AlarmResource {

    @Path("all/{realm}")
    @GET
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ALARMS_ROLE})
    SentAlarm[] getAlarms(@BeanParam RequestParams requestParams, @PathParam("realm") String realm);

    @DELETE
    @Path("alarms")
    @RolesAllowed({Constants.WRITE_ALARMS_ROLE})
    void removeAlarms(@BeanParam RequestParams requestParams, @RequestBody List<Long> ids);

    @DELETE
    @Path("{alarmId}")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ALARMS_ROLE})
    void removeAlarm(@BeanParam RequestParams requestParams, @PathParam("alarmId") Long alarmId);

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ALARMS_ROLE})
    SentAlarm createAlarm(@BeanParam RequestParams requestParams, Alarm alarm);

    @Path("{alarmId}")
    @PUT
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ALARMS_ROLE})
    void updateAlarm(@BeanParam RequestParams requestParams, @PathParam("alarmId") Long alarmId, SentAlarm alarm);

    @Path("assets")
    @PUT
    @RolesAllowed({Constants.WRITE_ALARMS_ROLE})
    void setAssetLinks(@BeanParam RequestParams requestParams, @RequestBody List<AlarmAssetLink> links);

    @Path("{alarmId}/assets/{realm}")
    @GET
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ALARMS_ROLE})
    List<AlarmAssetLink> getAssetLinks(@BeanParam RequestParams requestParams, @PathParam("alarmId") Long alarmId, @PathParam("realm") String realm);

    @Path("{assetId}/alarms")
    @GET
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ALARMS_ROLE})
    List<SentAlarm> getAlarmsByAssetId(@BeanParam RequestParams requestParams, @PathParam("assetId") String assetId);

    @Path("open")
    @GET
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ALARMS_ROLE})
    List<SentAlarm> getOpenAlarms(@BeanParam RequestParams requestParams);
}
