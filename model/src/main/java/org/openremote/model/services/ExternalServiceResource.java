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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.openremote.model.Constants;
import org.openremote.model.http.OpenApiResponses;
import org.openremote.model.http.RequestParams;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.*;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.openremote.model.http.OpenApiDescriptions.EXAMPLE_REALM;
import static org.openremote.model.http.OpenApiDescriptions.REALM;

/**
 * REST resource for managing external services.
 * <p>
 * This resource provides endpoints for service discovery, registration, and
 * management of external services through the manager.
 * <p>
 * Registered services are made available via the OpenRemote manager's Web UI
 * and API, enabling centralized service management and monitoring.
 */
@Tag(name = "Services", description = "Register, discover, renew, and deregister leased external-service instances")
@Path("service")
@OpenApiResponses.Authenticated
public interface ExternalServiceResource {

        /**
         * Register a new external service with the OpenRemote manager.
         * <p>
         * Creates a new registration entry and returns the registered external service
         * with its generated instanceId and initial status.
         * <p>
         * This service will be made available only to the realm it is registered for.
         * 
         * @param service The external service to register
         * @return The registered external service with its instanceId and status
         */
        @POST
        @Consumes(APPLICATION_JSON)
        @Produces(APPLICATION_JSON)
        @RolesAllowed({ Constants.WRITE_SERVICES_ROLE })
        @Operation(operationId = "registerService", summary = "Register an external service with the OpenRemote Manager",
            description = "Registers a realm-scoped service instance for the request realm. The caller must be a service account; the returned object contains the assigned instance ID and initial lease state.", responses = {
                        @ApiResponse(responseCode = "200", description = "Service registered successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExternalService.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid external service object"),
                        @ApiResponse(responseCode = "409", description = "ExternalService instance already registered"),
        })
        ExternalService registerService(@BeanParam RequestParams requestParams,
                        @NotNull @Valid @RequestBody(required = true,
                            description = "Realm-scoped service metadata and lease settings; instanceId is assigned by the Manager.") ExternalService service);

        /**
         * Register a new global external service with the OpenRemote
         * manager. This service will be made available to all realms.
         * <p>
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
        @Operation(operationId = "registerGlobalService", summary = "Register a global external service with the OpenRemote Manager",
            description = "Registers a service visible in every realm. The caller must be a super-user service account and the request must use the master realm.", responses = {
                        @ApiResponse(responseCode = "200", description = "Service registered successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExternalService.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid external service object"),
                        @ApiResponse(responseCode = "409", description = "ExternalService instance already registered"),
        })
        ExternalService registerGlobalService(@BeanParam RequestParams requestParams,
                        @NotNull @Valid @RequestBody(required = true,
                            description = "Globally visible service metadata and lease settings; instanceId is assigned by the Manager.") ExternalService service);

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
        @Operation(operationId = "getServices", summary = "List registered external services for a realm",
            description = "Returns all active realm-scoped service registrations for an active realm accessible to the caller.", responses = {
                        @ApiResponse(responseCode = "200", description = "List of registered external services", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExternalService[].class))),
        })
        @OpenApiResponses.BadRequest
        ExternalService[] getServices(@BeanParam RequestParams requestParams,
                        @Parameter(description = REALM, example = EXAMPLE_REALM, required = true) @QueryParam("realm") @NotNull String realm);

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
        @Operation(operationId = "getService", summary = "Retrieve an external service instance",
            description = "Returns one registered instance by stable service ID and numeric instance ID, after verifying access to the service's realm.", responses = {
                        @ApiResponse(responseCode = "200", description = "ExternalService retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExternalService.class))),
                        @ApiResponse(responseCode = "404", description = "ExternalService not found"),
        })
        @OpenApiResponses.BadRequest
        ExternalService getService(@BeanParam RequestParams requestParams,
                        @Parameter(description = "Stable service type identifier chosen by the service.", example = "weather") @PathParam("serviceId") @NotNull @Size(min = 1) String serviceId,
                        @Parameter(description = "Numeric instance identifier assigned during registration.", example = "1") @PathParam("instanceId") int instanceId);

        /**
         * Retrieve all external services that are globally registered
         * 
         * @return Array of globally accessible external services
         */
        @GET
        @Path("global")
        @Produces(APPLICATION_JSON)
        @RolesAllowed({ Constants.READ_SERVICES_ROLE })
        @Operation(operationId = "getGlobalServices", summary = "List globally available external services",
            description = "Returns active registrations marked as globally available across realms.", responses = {
                        @ApiResponse(responseCode = "200", description = "List of registered external services", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExternalService[].class))),
        })
        ExternalService[] getGlobalServices(@BeanParam RequestParams requestParams);

        /**
         * Send a heartbeat to refresh the active registration lease for an external service.
         * <p>
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
        @Operation(operationId = "heartbeat", summary = "Renew an external service registration lease",
            description = "Extends the lease of one service instance. The caller must be the service account that registered it; global-service heartbeats additionally require a super user.", responses = {
                        @ApiResponse(responseCode = "204", description = "Heartbeat sent successfully"),
                        @ApiResponse(responseCode = "404", description = "Service instance not found"),
        })
        @OpenApiResponses.BadRequest
        void heartbeat(@BeanParam RequestParams requestParams,
                        @Parameter(description = "Stable service type identifier chosen by the service.", example = "weather") @PathParam("serviceId") @NotNull @Size(min = 1) String serviceId,
                        @Parameter(description = "Numeric instance identifier assigned during registration.", example = "1") @PathParam("instanceId") int instanceId);

        /**
         * Deregister an external service from the registry.
         * <p>
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
        @Operation(operationId = "deregisterService", summary = "Deregister an external service",
            description = "Removes an active service registration. The caller must be the service account that registered it; global-service removal additionally requires a super user.", responses = {
                        @ApiResponse(responseCode = "204", description = "Service deregistered successfully"),
                        @ApiResponse(responseCode = "404", description = "Service instance not found"),
        })
        void deregisterService(@BeanParam RequestParams requestParams,
                        @Parameter(description = "Stable service type identifier chosen by the service.", example = "weather") @PathParam("serviceId") String serviceId,
                        @Parameter(description = "Numeric instance identifier assigned during registration.", example = "1") @PathParam("instanceId") int instanceId);

}
