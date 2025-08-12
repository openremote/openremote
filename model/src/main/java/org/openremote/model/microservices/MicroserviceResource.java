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

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * REST resource for managing microservices and external services.
 * 
 * This resource provides endpoints for service discovery, registration, and management
 * of external services and microservices through the OpenRemote manager.
 * 
 * Registered services are made available via the OpenRemote manager's Web UI and API,
 * enabling centralized service management and monitoring.
 */
@Tag(name = "Services", description = "Registration and management of microservices/external services")
@Path("service")
public interface MicroserviceResource {

    /**
     * Register a new microservice or external service with the OpenRemote manager.
     * 
     * Creates a new registration entry and returns the registered microservice
     * with its generated instanceId and initial status.
     * 
     * @param microservice The microservice to register
     * @return The registered microservice with its instanceId and status
     */
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({ Constants.WRITE_SERVICES_ROLE })
    @Operation(operationId = "registerService", summary = "Register an external service/microservice with the OpenRemote manager", responses = {
            @ApiResponse(responseCode = "200", description = "Service registered successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Microservice.class))),
            @ApiResponse(responseCode = "400", description = "Invalid microservice object"),
            @ApiResponse(responseCode = "409", description = "Microservice instance already registered"),
    })
    Microservice registerService(@BeanParam RequestParams requestParams,
            @NotNull @Valid Microservice microservice);

    /**
     * Retrieve all registered microservices and external services for a specific realm.
     * 
     * Returns a list of all currently registered services within the specified realm,
     * including their details and current status.
     * 
     * @param realm The realm to filter services by
     * @return Array of registered microservices for the specified realm
     */
    @GET
    @Path("realm")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({ Constants.READ_SERVICES_ROLE })
    @Operation(operationId = "getServices", summary = "List all registered external services/microservices for the given realm within the OpenRemote manager", responses = {
            @ApiResponse(responseCode = "200", description = "List of registered microservices", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Microservice[].class))),
    })
    Microservice[] getServices(@BeanParam RequestParams requestParams, @QueryParam("realm") String realm);


    /**
     * Retrieve all globally available microservices and external services.
     * 
     * Returns a list of all registered services that are accessible across all realms,
     * typically used for system-wide services with global access permissions.
     * 
     * @return Array of globally accessible microservices
     */
     @GET
     @Path("global")
     @Produces(APPLICATION_JSON)
     @RolesAllowed({ Constants.READ_SERVICES_ROLE })
     @Operation(operationId = "getGlobalServices", summary = "List all registered external services/microservices that are globally accessible within the OpenRemote manager", responses = {
             @ApiResponse(responseCode = "200", description = "List of registered microservices", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Microservice[].class))),
     })
     Microservice[] getGlobalServices(@BeanParam RequestParams requestParams);

    /**
     * Send a heartbeat to refresh the active registration lease for a microservice.
     * 
     * This endpoint is used by microservices to indicate they are still running
     * and available. It extends the service's lease duration and maintains its
     * active status in the registry.
     * 
     * @param serviceId  The serviceId of the microservice to send the heartbeat to
     * @param instanceId The instanceId of the microservice to send the heartbeat to
     */
    @PUT
    @Path("{serviceId}/{instanceId}")
    @RolesAllowed({ Constants.WRITE_SERVICES_ROLE })
    @Operation(operationId = "heartbeat", summary = "Update the active registration lease for the specified microservice", responses = {
            @ApiResponse(responseCode = "204", description = "Heartbeat sent successfully"),
            @ApiResponse(responseCode = "404", description = "Service instance not found"),
    })
    void heartbeat(@BeanParam RequestParams requestParams,
            @PathParam("serviceId") @NotNull @Size(min = 1) String serviceId,
            @PathParam("instanceId") @NotNull @Size(min = 1) String instanceId);

    /**
     * Deregister a microservice or external service from the registry.
     * 
     * Removes the active registration for the specified service, causing it to
     * no longer be available through the microservice registry. This is typically
     * called when a service shuts down or needs to be removed from the system.
     * 
     * @param serviceId  The serviceId of the microservice to deregister
     * @param instanceId The instanceId of the microservice to deregister
     */
    @DELETE
    @Path("{serviceId}/{instanceId}")
    @RolesAllowed({ Constants.WRITE_SERVICES_ROLE })
    @Operation(operationId = "deregisterService", summary = "Deregister an external service/microservice", responses = {
            @ApiResponse(responseCode = "204", description = "Service deregistered successfully"),
            @ApiResponse(responseCode = "404", description = "Service instance not found"),
    })
    void deregisterService(@BeanParam RequestParams requestParams, @PathParam("serviceId") String serviceId,
            @PathParam("instanceId") String instanceId);

}
