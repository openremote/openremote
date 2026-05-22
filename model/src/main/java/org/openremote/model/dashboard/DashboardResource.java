/*
 * Copyright 2026, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.model.dashboard;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import org.openremote.model.Constants;
import org.openremote.model.http.RequestParams;
import org.openremote.model.query.DashboardQuery;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;

@Tag(name = "Dashboard", description = "Operations on dashboards")
@Path("dashboard")
public interface DashboardResource {

  /**
   * Retrieve all dashboards from a realm, where the user has access to.
   *
   * @return An array of {@link Dashboard} from the realm
   */
  @GET
  @Path("all/{realm}")
  @Produces(APPLICATION_JSON)
  @Operation(operationId = "getAllRealmDashboards", summary = "Retrieve all accessible dashboards")
  Dashboard[] getAllRealmDashboards(
      @BeanParam RequestParams requestParams, @PathParam("realm") String realm);

  /**
   * Queries a specific {@link Dashboard} by its ID and realm, if a user has access to it.
   *
   * @return {@link Dashboard}
   */
  @GET
  @Path("{realm}/{dashboardId}")
  @Produces(APPLICATION_JSON)
  @Operation(operationId = "getDashboard", summary = "Retrieve a dashboard")
  Dashboard get(
      @BeanParam RequestParams requestParams,
      @PathParam("realm") String realm,
      @PathParam("dashboardId") String dashboardId);

  /**
   * Advanced query endpoint for retrieving {@link Dashboard} from the database. Based on the {@link
   * DashboardQuery} given, it will specifically filter the entries that are returned. For example
   * filtering by displayName, ID, or dashboard access.
   *
   * @return An array of {@link Dashboard}
   */
  @POST
  @Path("query")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @Operation(operationId = "queryDashboards", summary = "Retrieve dashboards using a query")
  Dashboard[] query(@BeanParam RequestParams requestParams, @Valid DashboardQuery dashboardQuery);

  @POST
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @RolesAllowed({Constants.WRITE_INSIGHTS_ROLE})
  @Operation(operationId = "createDashboard", summary = "Create a dashboard")
  Dashboard create(@BeanParam RequestParams requestParams, @Valid Dashboard dashboard);

  @PUT
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @RolesAllowed({Constants.WRITE_INSIGHTS_ROLE})
  @Operation(operationId = "updateDashboard", summary = "Update a dashboard")
  Dashboard update(@BeanParam RequestParams requestParams, @Valid Dashboard dashboard);

  @DELETE
  @Path("{realm}/{dashboardId}")
  @Produces(APPLICATION_JSON)
  @RolesAllowed({Constants.WRITE_INSIGHTS_ROLE})
  @Operation(operationId = "deleteDashboard", summary = "Delete a dashboard")
  void delete(
      @BeanParam RequestParams requestParams,
      @PathParam("realm") String realm,
      @PathParam("dashboardId") String dashboardId);
}
