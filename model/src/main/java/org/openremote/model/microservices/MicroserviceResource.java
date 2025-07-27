/*
 * Copyright 2025, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.model.microservices;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.openremote.model.Constants;
import org.openremote.model.http.RequestParams;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * The microservice resource is used to manage microservice/external service
 * registrations within the OpenRemote manager
 */
@Tag(name = "Microservice", description = "Registration and management of microservices")
@Path("microservice")
public interface MicroserviceResource {

    /**
     * Create a new registration for the specified microservice and return the
     * instanceId of the registered microservice.
     * 
     * @param microservice The microservice to register
     */
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({ Constants.WRITE_SERVICES_ROLE })
    @Operation(operationId = "registerService", summary = "Register an external service/microservice", responses = {
            @ApiResponse(responseCode = "201", description = "Service registered successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MicroserviceRegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid microservice object"),
    })
    MicroserviceRegisterResponse registerService(@BeanParam RequestParams requestParams,
            @NotNull @Valid Microservice microservice);

    /**
     * Lists all currently registered microservices with their details and status.
     */
    @GET
    @Produces(APPLICATION_JSON)
    @RolesAllowed({ Constants.READ_SERVICES_ROLE })
    @Operation(operationId = "getServices", summary = "List all registered external services/microservices with their details and current status", responses = {
            @ApiResponse(responseCode = "200", description = "List of registered microservices", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MicroserviceInfo[].class))),
    })
    MicroserviceInfo[] getServices(@BeanParam RequestParams requestParams);

    /**
     * Send a heartbeat to update the active registration TTL for the specified
     * microservice.
     * This is used to indicate that the microservice is still running and
     * available.
     * 
     * @param serviceId  The serviceId of the microservice to send the heartbeat to
     * @param instanceId The instanceId of the microservice to send the heartbeat to
     */
    @PUT
    @Path("{serviceId}/{instanceId}")
    @RolesAllowed({ Constants.WRITE_SERVICES_ROLE })
    @Operation(operationId = "heartbeat", summary = "Update the active registration TTL for the specified microservice", responses = {
            @ApiResponse(responseCode = "200", description = "Heartbeat sent successfully"),
            @ApiResponse(responseCode = "404", description = "Service not found"),
    })
    Response heartbeat(@BeanParam RequestParams requestParams,
            @PathParam("serviceId") @NotNull @Size(min = 1) String serviceId,
            @PathParam("instanceId") @NotNull @Size(min = 1) String instanceId);

    /**
     * Deregister the active registration for the specified microservice. This
     * causes the service to no longer be registered with the microservice registry.
     * 
     * @param serviceId  The serviceId of the microservice to deregister
     * @param instanceId The instanceId of the microservice to deregister
     */
    @DELETE
    @Path("{serviceId}/{instanceId}")
    @RolesAllowed({ Constants.WRITE_SERVICES_ROLE })
    @Operation(operationId = "deregisterService", summary = "Deregister an external service/microservice", responses = {
            @ApiResponse(responseCode = "200", description = "Service deregistered successfully"),
            @ApiResponse(responseCode = "404", description = "Service not found"),
    })
    Response deregisterService(@BeanParam RequestParams requestParams, @PathParam("serviceId") String serviceId,
            @PathParam("instanceId") String instanceId);

}
