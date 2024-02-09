package org.openremote.model.alarm;

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
    SentAlarm[] getAlarms(@BeanParam RequestParams requestParams);


    @DELETE
    @Path("{ids}/delete")
    @RolesAllowed({Constants.WRITE_ALARMS_ROLE})
    void removeAlarms(@BeanParam RequestParams requestParams, @PathParam("ids") List<Long> ids);


     @DELETE
     @Path("{alarmId}/delete")
     @RolesAllowed({Constants.WRITE_ALARMS_ROLE})
     void removeAlarm(@BeanParam RequestParams requestParams,
     @PathParam("alarmId") Long alarmId);


    @POST
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ALARMS_ROLE})
    SentAlarm createAlarm(@BeanParam RequestParams requestParams,
                     Alarm alarm);

    @Path("create/{source}")
    @POST
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ALARMS_ROLE})
    SentAlarm createAlarmWithSource(@BeanParam RequestParams requestParams, Alarm alarm, @PathParam("source") Alarm.Source source, String sourceId);

    @Path("{alarmId}/update")
    @PUT
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ALARMS_ROLE})
    void updateAlarm(@BeanParam RequestParams requestParams,
                     @PathParam("alarmId") Long alarmId,
                     SentAlarm alarm);

    @Path("{alarmId}/setStatus")
    @PUT
    @Consumes(APPLICATION_JSON)
    void setAlarmStatus(@BeanParam RequestParams requestParams,
                        @QueryParam("status") String status,
                        @PathParam("alarmId") String alarmId);

    @Path("{alarmId}/setAcknowledged")
    @PUT
    void setAlarmAcknowledged(@BeanParam RequestParams requestParams,
                              @PathParam("alarmId") String alarmId);

    @Path("{alarmId}/assign")
    @PUT
    void assignUser(@BeanParam RequestParams requestParams,
                    @PathParam("alarmId") Long alarmId,
                    String userId,
                    String realm);


    @Path("{alarmId}/assetLinks/get")
    @GET
    @Produces(APPLICATION_JSON)
    List<AlarmAssetLink> getAssetLinks(@BeanParam RequestParams requestParams,
            @PathParam("alarmId") Long alarmId,
            String realm);

    @Path("{alarmId}/userLinks/get")
    @GET
    @Produces(APPLICATION_JSON)
    List<AlarmUserLink> getUserLinks(@BeanParam RequestParams requestParams,
            @PathParam("alarmId") Long alarmId,
            String realm);

    @Path("{assetId}/alarmLinks/get")
    @GET
    @Produces(APPLICATION_JSON)
    List<SentAlarm> getAlarmsByAssetId(@BeanParam RequestParams requestParams,
                                       @PathParam("assetId") String assetId);

    @Path("/alarms/open/get")
    @GET
    @Produces(APPLICATION_JSON)
    List<SentAlarm> getOpenAlarms(@BeanParam RequestParams requestParams);

    @Path("/assetLinks/set")
    @PUT
    void setAssetLinks(@BeanParam RequestParams requestParams,
                       ArrayList<AlarmAssetLink> links);
}
