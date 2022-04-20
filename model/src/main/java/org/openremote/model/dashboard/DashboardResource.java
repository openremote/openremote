package org.openremote.model.dashboard;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.Constants;
import org.openremote.model.http.RequestParams;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.*;

import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Tag(name = "Dashboard")
@Path("dashboard")
public interface DashboardResource {

    @GET
    @Path("user/all")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ASSETS_ROLE})
    Dashboard[] getAllUserDashboards(@BeanParam RequestParams requestParams);

    @GET
    @Path("{dashboardId}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ASSETS_ROLE})
    Dashboard get(@BeanParam RequestParams requestParams, @PathParam("dashboardId") String dashboardId);

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ASSETS_ROLE})
    Dashboard create(@BeanParam RequestParams requestParams, @Valid Dashboard dashboard);

    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ASSETS_ROLE})
    void update(@BeanParam RequestParams requestParams, @Valid Dashboard dashboard);

    @DELETE
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ASSETS_ROLE})
    void delete(@BeanParam RequestParams requestParams, @QueryParam("dashboardId") List<String> fields);
}
