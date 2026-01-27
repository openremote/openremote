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
package org.openremote.model.gateway;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import org.openremote.model.Constants;
import org.openremote.model.http.RequestParams;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;

@Tag(name = "Gateway", description = "Operations on gateways")
@Path("gateway")
public interface GatewayServiceResource {

  /** TODO: write docs */
  @GET
  @Path("tunnel/{realm}")
  @Produces(APPLICATION_JSON)
  @RolesAllowed({Constants.READ_ADMIN_ROLE})
  @Operation(
      operationId = "getAllActiveTunnelInfos",
      summary = "Retrieve all active gateway tunnel information of a realm")
  GatewayTunnelInfo[] getAllActiveTunnelInfos(
      @BeanParam RequestParams requestParams, @PathParam("realm") String realm);

  /** TODO: write docs */
  @GET
  @Path("tunnel/{realm}/{id}")
  @Produces(APPLICATION_JSON)
  @Operation(
      operationId = "getGatewayActiveTunnelInfos",
      summary = "Retrieve the active gateway tunnel information of gateway in a realm")
  GatewayTunnelInfo[] getGatewayActiveTunnelInfos(
      @BeanParam RequestParams requestParams,
      @PathParam("realm") String realm,
      @PathParam("id") String gatewayId);

  @GET
  @Path("tunnel/{realm}/{id}/{target}/{targetPort}")
  @Produces(APPLICATION_JSON)
  @Operation(
      operationId = "getActiveTunnelInfo",
      summary = "Retrieve the gateway tunnel information of tunnel for a gateway in a realm")
  GatewayTunnelInfo getActiveTunnelInfo(
      @BeanParam RequestParams requestParams,
      @PathParam("realm") String realm,
      @PathParam("id") String gatewayId,
      @PathParam("target") String target,
      @PathParam("targetPort") int targetPort);

  /** TODO: write docs */
  @POST
  @Path("tunnel")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @Operation(operationId = "startTunnel", summary = "Start a tunnel for a gateway")
  GatewayTunnelInfo startTunnel(GatewayTunnelInfo tunnelInfo);

  /** TODO: write docs */
  @DELETE
  @Path("tunnel")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @Operation(operationId = "stopTunnel", summary = "Stop a tunnel for a gateway")
  void stopTunnel(GatewayTunnelInfo tunnelInfo);
}
