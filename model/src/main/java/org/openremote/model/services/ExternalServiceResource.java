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
package org.openremote.model.services;

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
 * REST resource for managing external services.
 * 
 * This resource provides endpoints for service discovery, registration, and
 * management of external services through the manager.
 * 
 * Registered services are made available via the OpenRemote manager's Web UI
 * and API, enabling centralized service management and monitoring.
 */
@Tag(name = "Services", description = "Registration and management of external services")
@Path("service")
public interface ExternalServiceResource {

        /**
         * Register a new external service with the OpenRemote manager.
         * 
         * Creates a new registration entry and returns the registered external service
         * with its generated instanceId and initial status.
         * 
         * This service will be made available only to the realm it is registered for.
         * 
         * @param service The external service to register
         * @return The registered external service with its instanceId and status
         */
        @POST
        @Consumes(APPLICATION_JSON)
        @Produces(APPLICATION_JSON)
        @RolesAllowed({ Constants.WRITE_SERVICES_ROLE })
        @Operation(operationId = "registerService", summary = "Register an external service with the OpenRemote manager", responses = {
                        @ApiResponse(responseCode = "200", description = "Service registered successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExternalService.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid external service object"),
                        @ApiResponse(responseCode = "409", description = "ExternalService instance already registered"),
        })
        ExternalService registerService(@BeanParam RequestParams requestParams,
                        @NotNull @Valid ExternalService service);

        /**
         * Register a new global external service with the OpenRemote
         * manager. This service will be made available to all realms.
         * 
         * Creates a new registration entry and returns the registered external service
         * with its generated instanceId and initial status.
         * 
         * @param service The external service to register
         * @return The registered external service with its instanceId and status
         */
        @POST
        @Path("global")
        @Consumes(APPLICATION_JSON)
        @Produces(APPLICATION_JSON)
        @RolesAllowed({ Constants.WRITE_SERVICES_ROLE })
        @Operation(operationId = "registerGlobalService", summary = "Register a global external service with the OpenRemote manager", responses = {
                        @ApiResponse(responseCode = "200", description = "Service registered successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExternalService.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid external service object"),
                        @ApiResponse(responseCode = "409", description = "ExternalService instance already registered"),
        })
        ExternalService registerGlobalService(@BeanParam RequestParams requestParams,
                        @NotNull @Valid ExternalService service);

        /**
         * Retrieve all registered external services for a specific
         * realm.
         * 
         * @param realm The realm to filter services by
         * @return Array of registered external services for the specified realm
         */
        @GET
        @Produces(APPLICATION_JSON)
        @RolesAllowed({ Constants.READ_SERVICES_ROLE })
        @Operation(operationId = "getServices", summary = "List all registered external services for the given realm within the OpenRemote manager", responses = {
                        @ApiResponse(responseCode = "200", description = "List of registered external services", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExternalService[].class))),
        })
        ExternalService[] getServices(@BeanParam RequestParams requestParams, @QueryParam("realm") @NotNull String realm);

        /**
         * Retrieve a specific external service by its serviceId and
         * instanceId.
         * 
         * @param serviceId  The serviceId of the external service to retrieve
         * @param instanceId The instanceId of the external service to retrieve
         * @return The external service with the specified serviceId and instanceId
         */
        @GET
        @Path("{serviceId}/{instanceId}")
        @Produces(APPLICATION_JSON)
        @RolesAllowed({ Constants.READ_SERVICES_ROLE })
        @Operation(operationId = "getService", summary = "Retrieve a specific external service by its serviceId and instanceId", responses = {
                        @ApiResponse(responseCode = "200", description = "ExternalService retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExternalService.class))),
                        @ApiResponse(responseCode = "404", description = "ExternalService not found"),
        })
        ExternalService getService(@BeanParam RequestParams requestParams,
                        @PathParam("serviceId") @NotNull @Size(min = 1) String serviceId,
                        @PathParam("instanceId") @NotNull @Size(min = 1) String instanceId);

        /**
         * Retrieve all external services that are globally registered
         * 
         * @return Array of globally accessible external services
         */
        @GET
        @Path("global")
        @Produces(APPLICATION_JSON)
        @RolesAllowed({ Constants.READ_SERVICES_ROLE })
        @Operation(operationId = "getGlobalServices", summary = "List all registered external services that are globally accessible within the OpenRemote manager", responses = {
                        @ApiResponse(responseCode = "200", description = "List of registered external services", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExternalService[].class))),
        })
        ExternalService[] getGlobalServices(@BeanParam RequestParams requestParams);

        /**
         * Send a heartbeat to refresh the active registration lease for an external service.
         * 
         * This endpoint is used by external services to indicate they are still running
         * and available. It extends the service's lease duration and maintains its
         * active status in the registry.
         * 
         * @param serviceId  The serviceId of the external service to send the heartbeat to
         * @param instanceId The instanceId of the external service to send the heartbeat to
         */
        @PUT
        @Path("{serviceId}/{instanceId}")
        @RolesAllowed({ Constants.WRITE_SERVICES_ROLE })
        @Operation(operationId = "heartbeat", summary = "Update the active registration lease for the specified external service", responses = {
                        @ApiResponse(responseCode = "204", description = "Heartbeat sent successfully"),
                        @ApiResponse(responseCode = "404", description = "Service instance not found"),
        })
        void heartbeat(@BeanParam RequestParams requestParams,
                        @PathParam("serviceId") @NotNull @Size(min = 1) String serviceId,
                        @PathParam("instanceId") @NotNull @Size(min = 1) String instanceId);

        /**
         * Deregister an external service from the registry.
         * 
         * Removes the active registration for the specified service, causing it to
         * no longer be available through the external service registry. This is typically
         * called when a service shuts down or needs to be removed from the system.
         * 
         * @param serviceId  The serviceId of the external service to deregister
         * @param instanceId The instanceId of the external service to deregister
         */
        @DELETE
        @Path("{serviceId}/{instanceId}")
        @RolesAllowed({ Constants.WRITE_SERVICES_ROLE })
        @Operation(operationId = "deregisterService", summary = "Deregister an external service", responses = {
                        @ApiResponse(responseCode = "204", description = "Service deregistered successfully"),
                        @ApiResponse(responseCode = "404", description = "Service instance not found"),
        })
        void deregisterService(@BeanParam RequestParams requestParams, @PathParam("serviceId") String serviceId,
                        @PathParam("instanceId") String instanceId);

}
