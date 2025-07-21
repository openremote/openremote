/*
 * Copyright 2017, OpenRemote Inc.
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
import io.swagger.v3.oas.annotations.tags.Tag;

import org.openremote.model.Constants;
import org.openremote.model.http.RequestParams;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Tag(name = "Microservice", description = "Registration and management of microservices/external services")
@Path("microservice")
public interface MicroserviceResource {

    /**
     * Creates or updates the active registration for the specified microservice.
     * 
     * @param requestParams The request parameters
     * @return True if the microservice was registered or updated successfully
     */
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "registerService", summary = "Register a new service/microservice instance")
    boolean register(@BeanParam RequestParams requestParams, @NotNull @Valid Microservice service);

    /**
     * Lists all currently registered microservices with their details and status.
     *
     * @param requestParams The request parameters
     * @return An array of microservices objects
     */
    @GET
    @Produces(APPLICATION_JSON)
    @RolesAllowed({ Constants.READ_SERVICES_ROLE })
    @Operation(operationId = "getServices", summary = "List all registered microservices with their details and status")
    Microservice[] getServices(@BeanParam RequestParams requestParams);

    /**
     * Sends a heartbeat to keep the registration alive for a given service/microservice.
     * 
     * @param requestParams The request parameters
     * @param serviceId The ID of the service/microservice to update
     * @return True if the service/microservice was updated successfully
     */
    @PUT
    @Path("/{id}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "sendHeartbeat", summary = "Send a heartbeat to keep the registration alive for a given service/microservice instance")
    boolean heartbeat(@BeanParam RequestParams requestParams, @PathParam("id") String serviceId);

    /**
     * Deregister the active registration for the specified microservice. This
     * causes the service to no longer be listed, when requesting the list of
     * services.
     * 
     * @param requestParams The request parameters
     * @return True if the microservice was unregistered successfully
     */
    @DELETE
    @Path("/{id}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "deregisterService", summary = "Deregister a service/microservice instance from the registry, this causes the service to no longer be listed")
    boolean deregister(@BeanParam RequestParams requestParams, @PathParam("id") String serviceId);


}
