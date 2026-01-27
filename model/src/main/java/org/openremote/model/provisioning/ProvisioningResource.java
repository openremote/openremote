/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.model.provisioning;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import org.openremote.model.http.RequestParams;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;

@Tag(name = "Provisioning", description = "Operations on provisioning configurations")
@Path("provisioning")
public interface ProvisioningResource {

  @GET
  @Produces(APPLICATION_JSON)
  @Operation(
      operationId = "getProvisioningConfigs",
      summary = "Retrieve all provisioning configurations")
  ProvisioningConfig<?, ?>[] getProvisioningConfigs();

  @POST
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @Operation(
      operationId = "createProvisioningConfig",
      summary = "Create a provisioning configuration")
  long createProvisioningConfig(ProvisioningConfig<?, ?> provisioningConfig);

  @PUT
  @Path("{id}")
  @Consumes(APPLICATION_JSON)
  @Operation(
      operationId = "updateProvisioningConfig",
      summary = "Update a provisioning configuration")
  void updateProvisioningConfig(
      @BeanParam RequestParams requestParams,
      @PathParam("id") Long id,
      @Valid ProvisioningConfig<?, ?> provisioningConfig);

  @DELETE
  @Path("{id}")
  @Produces(APPLICATION_JSON)
  @Operation(
      operationId = "deleteProvisioningConfig",
      summary = "Delete a provisioning configuration")
  void deleteProvisioningConfig(@BeanParam RequestParams requestParams, @PathParam("id") Long id);
}
