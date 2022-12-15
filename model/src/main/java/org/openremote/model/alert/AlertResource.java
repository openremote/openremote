package org.openremote.model.alert;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.Constants;
import org.openremote.model.http.RequestParams;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Tag(name = "Alert")
@Path("alert")
public interface AlertResource {

    /**
     * @param requestParams
     * @param id
     * @param severity
     * @return
     */
    @GET
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ADMIN_ROLE})
    SentAlert[] getAlerts(@BeanParam RequestParams requestParams,
                          @QueryParam("id") Long id,
                          @QueryParam("severity") String severity);

    /**
     *
     * @param requestParams
     * @param id
     * @param severity
     */
    @DELETE
    @RolesAllowed({Constants.WRITE_ADMIN_ROLE})
    void removeAlerts(@BeanParam RequestParams requestParams,
                      @QueryParam("id") Long id,
                      @QueryParam("severity") String severity);

    /**
     *
     * @param requestParams
     * @param alertId
     */
    @DELETE
    @Path("{alertId}")
    @RolesAllowed({Constants.WRITE_ADMIN_ROLE})
    void removeAlert(@BeanParam RequestParams requestParams,
                     @PathParam("alertId") Long alertId);

    /**
     * @param requestParams
     * @param alert
     */
    @POST
    @Path("alert")
    @Consumes(APPLICATION_JSON)
    void createAlert(@BeanParam RequestParams requestParams,
                   Alert alert);
}
