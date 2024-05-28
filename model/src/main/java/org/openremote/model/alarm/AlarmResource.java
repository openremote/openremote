package org.openremote.model.alarm;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.Constants;
import org.openremote.model.http.RequestParams;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;

import java.util.ArrayList;
import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Tag(name = "Alarm")
@Path("alarm")
@Consumes(APPLICATION_JSON)
public interface AlarmResource {

    @Path("all")
    @GET
    @Produces(APPLICATION_JSON)
    @RolesAllowed({ Constants.READ_ALARMS_ROLE })
    SentAlarm[] getAlarms(@BeanParam RequestParams requestParams);


    @DELETE
    @Path("alarms")
    @RolesAllowed({Constants.WRITE_ALARMS_ROLE})
    void removeAlarms(@BeanParam RequestParams requestParams, @RequestBody List<Long> ids);


     @DELETE
     @Path("{alarmId}")
     @Consumes(APPLICATION_JSON)
     @RolesAllowed({Constants.WRITE_ALARMS_ROLE})
     void removeAlarm(@BeanParam RequestParams requestParams,
                      @PathParam("alarmId") Long alarmId);


    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({ Constants.WRITE_ALARMS_ROLE })
    SentAlarm createAlarm(@BeanParam RequestParams requestParams,
            Alarm alarm);

    @Path("{source}/{sourceId}")
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ALARMS_ROLE})
    SentAlarm createAlarmWithSource(@BeanParam RequestParams requestParams, @RequestBody Alarm alarm, @PathParam("source") Alarm.Source source, @PathParam("sourceId") String sourceId);

    @Path("{alarmId}")
    @PUT
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({ Constants.WRITE_ALARMS_ROLE })
    void updateAlarm(@BeanParam RequestParams requestParams,
            @PathParam("alarmId") Long alarmId,
            SentAlarm alarm);

    @Path("assets")
    @PUT
    void setAssetLinks(@BeanParam RequestParams requestParams,
                      @RequestBody ArrayList<AlarmAssetLink> links);

    @Path("{alarmId}/assets")
    @GET
    @Produces(APPLICATION_JSON)
    List<AlarmAssetLink> getAssetLinks(@BeanParam RequestParams requestParams,
            @PathParam("alarmId") Long alarmId,
            String realm);

    @Path("{assetId}/alarms")
    @GET
    @Produces(APPLICATION_JSON)
    List<SentAlarm> getAlarmsByAssetId(@BeanParam RequestParams requestParams,
                                       @PathParam("assetId") String assetId);

    @Path("open")
    @GET
    @Produces(APPLICATION_JSON)
    List<SentAlarm> getOpenAlarms(@BeanParam RequestParams requestParams);
}
