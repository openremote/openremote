package org.openremote.model.dashboard;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.Constants;
import org.openremote.model.http.RequestParams;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Tag(name = "Dashboard")
@Path("dashboard")
public interface DashboardResource {

    @GET
    @Path("all/{realm}")
    @Produces(APPLICATION_JSON)
    Dashboard[] getAllRealmDashboards(@BeanParam RequestParams requestParams, @PathParam("realm") String realm);

    @GET
    @Path("{realm}/{dashboardId}")
    @Produces(APPLICATION_JSON)
    Dashboard get(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @PathParam("dashboardId") String dashboardId);

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_INSIGHTS_ROLE})
    Dashboard create(@BeanParam RequestParams requestParams, @Valid Dashboard dashboard);

    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_INSIGHTS_ROLE})
    Dashboard update(@BeanParam RequestParams requestParams, @Valid Dashboard dashboard);

    @DELETE
    @Path("{realm}/{dashboardId}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_INSIGHTS_ROLE})
    void delete(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @PathParam("dashboardId") String dashboardId);
}
