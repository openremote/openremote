package org.openremote.model.dashboard;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import org.openremote.model.Constants;
import org.openremote.model.http.RequestParams;
import org.openremote.model.query.DashboardQuery;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Tag(name = "Dashboard")
@Path("dashboard")
public interface DashboardResource {

    /**
     * Retrieve all dashboards from a realm, where the user has access to.
     * @return An array of {@link Dashboard} from the realm
     */
    @GET
    @Path("all/{realm}")
    @Produces(APPLICATION_JSON)
    Dashboard[] getAllRealmDashboards(@BeanParam RequestParams requestParams, @PathParam("realm") String realm);

    /**
     * Queries a specific {@link Dashboard} by its ID and realm, if a user has access to it.
     * @return {@link Dashboard}
     */
    @GET
    @Path("{realm}/{dashboardId}")
    @Produces(APPLICATION_JSON)
    Dashboard get(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @PathParam("dashboardId") String dashboardId);

    /**
     * Advanced query endpoint for retrieving {@link Dashboard} from the database.
     * Based on the {@link DashboardQuery} given, it will specifically filter the entries that are returned.
     * For example filtering by displayName, ID, or dashboard access.
     *
     * @return An array of {@link Dashboard}
     */
    @POST
    @Path("query")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    Dashboard[] query(@BeanParam RequestParams requestParams, @Valid DashboardQuery dashboardQuery);

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
