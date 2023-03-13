package org.openremote.model.alarm;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.Constants;
import org.openremote.model.http.RequestParams;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;

import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
@Tag(name = "Alarm")
@Path("alarm")
public interface AlarmResource {
    /**
     * @param requestParams
     * @return
     */
    @Path("all")
    @GET
    @Produces(APPLICATION_JSON)
    @RolesAllowed(Constants.READ_ADMIN_ROLE)
    SentAlarm[] getAlarms(@BeanParam RequestParams requestParams);

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
    @Consumes(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
    void createAlarm(@BeanParam RequestParams requestParams,
                     Alarm alarm);

    @Path("{alarmId}/setStatus")
    @PUT
    void setAlarmStatus(@BeanParam RequestParams requestParams,
                        @QueryParam("status") String status,
                        @PathParam("alarmId") String alarmId);

    @Path("{alarmId}/setAcknowledged")
    @PUT
    void setAlarmAcknowledged(@BeanParam RequestParams requestParams,
                              @PathParam("alarmId") String alarmId);
}
