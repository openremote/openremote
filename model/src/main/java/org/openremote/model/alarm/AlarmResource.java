package org.openremote.model.alarm;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.Constants;
import org.openremote.model.http.RequestParams;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;

import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
@Tag(name = "Alarm")
@Path("/AlarmService")
public interface AlarmResource {
    /**
     * @param requestParams
     * @param id
     * @param severity
     * @return
     */
    @GET
    @Produces(APPLICATION_JSON)
    @Path("/alarms")
    SentAlarm[] getAlarms(@BeanParam RequestParams requestParams,
                          @QueryParam("id") String id,
                          @QueryParam("severity") String severity);

//    /**
//     *
//     * @param requestParams
//     * @param id
//     * @param severity
//     */
//    @DELETE
//    @RolesAllowed({Constants.WRITE_ADMIN_ROLE})
//    void removeAlerts(@BeanParam RequestParams requestParams,
//                      @QueryParam("id") Long id,
//                      @QueryParam("severity") String severity,
//                      @QueryParam("status") String status);
//
//    /**
//     *
//     * @param requestParams
//     * @param alertId
//     */
//    @DELETE
//    @Path("{alertId}")
//    @RolesAllowed({Constants.WRITE_ADMIN_ROLE})
//    void removeAlert(@BeanParam RequestParams requestParams,
//                     @PathParam("alertId") Long alertId);

    /**
     * @param requestParams
     * @param alarm
     */
    @POST
    @Path("/alarms")
    @Consumes(APPLICATION_JSON)
    void createAlarm(@BeanParam RequestParams requestParams,
                     Alarm alarm);

    @PUT
    @Path("{alarmId}/setStatus")
    void setAlarmStatus(@BeanParam RequestParams requestParams,
                        @QueryParam("status") String status,
                        @PathParam("alarmId") String alarmId);

    @PUT
    @Path("{alarmId}/setAcknowledged")
    void setAlarmAcknowledged(@BeanParam RequestParams requestParams,
                              @PathParam("alarmId") String alarmId);
}
