package org.openremote.model.alarm;
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
    /**
     * @param requestParams
     * @return
     */
    @Path("all")
    @GET
    @Produces(APPLICATION_JSON)
    @RolesAllowed({ Constants.READ_ALARMS_ROLE })
    SentAlarm[] getAlarms(@BeanParam RequestParams requestParams);

     /**
     *
     * @param requestParams
     * @param alarmId
     */
     @DELETE
     @Path("{alarmId}/delete")
     @RolesAllowed({Constants.WRITE_ALARMS_ROLE})
     void removeAlarm(@BeanParam RequestParams requestParams,
                      @PathParam("alarmId") Long alarmId);

    /**
     * @param requestParams
     * @param alarm
     */
    @POST
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({ Constants.WRITE_ALARMS_ROLE })
    void createAlarm(@BeanParam RequestParams requestParams,
            Alarm alarm);

    @Path("{alarmId}/update")
    @PUT
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({ Constants.WRITE_ALARMS_ROLE })
    void updateAlarm(@BeanParam RequestParams requestParams,
            @PathParam("alarmId") Long alarmId,
            Alarm alarm);

    @Path("{alarmId}/update")
    @PUT
    void updateAlarm(@BeanParam RequestParams requestParams,
                     @PathParam("alarmId") Long alarmId,
                     Alarm alarm);

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


    @Path("{alarmId}/assetLink")
    @PUT
    void setAssetLink(@BeanParam RequestParams requestParams,
                      @PathParam("alarmId") String alarmId,
                      String assetId,
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
}
