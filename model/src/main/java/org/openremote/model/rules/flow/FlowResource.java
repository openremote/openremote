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
package org.openremote.model.rules.flow;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import org.openremote.model.http.RequestParams;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;

@Tag(name = "Flow", description = "Operations on flows")
@Path("flow")
public interface FlowResource {
  @GET
  @Produces(APPLICATION_JSON)
  @Operation(operationId = "getAllNodeDefinitions", summary = "Retrieve all node definitions")
  Node[] getAllNodeDefinitions(@BeanParam RequestParams requestParams);

  @GET
  @Path("{type}")
  @Produces(APPLICATION_JSON)
  @Operation(
      operationId = "getAllNodeDefinitionsByType",
      summary = "Retrieve all node definitions by type")
  Node[] getAllNodeDefinitionsByType(
      @BeanParam RequestParams requestParams, @PathParam("type") NodeType type);

  @GET
  @Path("{name}")
  @Produces(APPLICATION_JSON)
  @Operation(operationId = "getNodeDefinition", summary = "Retrieve a node definition by name")
  Node getNodeDefinition(@BeanParam RequestParams requestParams, @PathParam("name") String name);
}
