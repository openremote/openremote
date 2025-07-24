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
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;


@Tag(name = "Microservice", description = "Registration and management of microservices")
@Path("microservice")
public interface MicroserviceResource {

    /**
     * Creates or updates the active registration for the specified microservice.
     * 
     * @param requestParams The request parameters
     * @param microservice The microservice to register and/or update
     * @return a response containing the serviceId and instanceId of the registered microservice
     */
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({ Constants.WRITE_SERVICES_ROLE })
    @Operation(operationId = "registerService", summary = "Register a external service/microservice")
    MicroserviceRegistrationResponse registerService(@BeanParam RequestParams requestParams, @NotNull @Valid Microservice microservice);

    /**
     * Lists all currently registered microservices with their details and status.
     * 
     * @param requestParams The request parameters
     * @return An array of microservices objects
     */
    @GET
    @Produces(APPLICATION_JSON)
    @RolesAllowed({ Constants.READ_SERVICES_ROLE })
    @Operation(operationId = "getServices", summary = "List all registered external services/microservices with their details and current status")
    MicroserviceInfo[] getServices(@BeanParam RequestParams requestParams);


    /**
     * Send a heartbeat to update the active registration TTL for the specified microservice.
     * This is used to indicate that the microservice is still running and available.
     * 
     * @param requestParams The request parameters
     * @param serviceId The serviceId of the microservice to send the heartbeat to
     * @param instanceId The instanceId of the microservice to send the heartbeat to
     * @return HTTP 200 OK if the heartbeat was sent successfully, otherwise an error response
     */
    @PUT
    @Path("{serviceId}/{instanceId}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({ Constants.WRITE_SERVICES_ROLE })
    @Operation(operationId = "sendHeartbeat", summary = "Update the active registration TTL for the specified microservice")
    Response sendHeartbeat(@BeanParam RequestParams requestParams, @PathParam("serviceId") String serviceId, @PathParam("instanceId") String instanceId);

    /**
     * Deregisters the active registration for the specified microservice. This
     * causes the service to no longer be listed, when requesting the list of
     * services.
     * 
     * @param requestParams The request parameters
     * @param serviceId The serviceId of the microservice to deregister
     * @param instanceId The instanceId of the microservice to deregister
     * @return HTTP 200 OK if the microservice was deregistered successfully, otherwise an error response
     */
    @DELETE
    @Path("{serviceId}/{instanceId}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({ Constants.WRITE_SERVICES_ROLE })
    @Operation(operationId = "deregisterService", summary = "Deregister a external service/microservice")
    Response deregisterService(@BeanParam RequestParams requestParams, @PathParam("serviceId") String serviceId, @PathParam("instanceId") String instanceId);


}
