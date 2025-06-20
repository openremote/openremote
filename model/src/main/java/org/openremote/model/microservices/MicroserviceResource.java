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

/**
 * Microservice resource, this resource is used by external microservices/services to
 * register themselves with the {@link MicroserviceRegistryService}.
 * 
 * <li>The client is responsible for periodically sending the
 * {@link Microservice} object to the {@link MicroserviceRegistryService}.</li>
 * 
 * <li>The {@link MicroserviceRegistryService} will store the registration in
 * memory, and will set the status to available. The client should update the
 * registration with the latest status and details.</li>
 * 
 * <li>The {@link MicroserviceRegistryService} will also handle the expiration of
 * registrations.
 */
@Tag(name = "Service", description = "Registration and management of services/microservices")
@Path("service")
public interface MicroserviceResource {

    /**
     * Creates or updates the active registration for the specified microservice.
     * 
     * @param requestParams The request parameters
     * @param microservice The microservice to register and/or update
     * @return True if the microservice was registered or updated successfully
     */
    @POST
    @Path("register")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "register", summary = "Create/update the registration for a service/microservice")
    boolean register(@BeanParam RequestParams requestParams, @NotNull @Valid Microservice microservice);

    /**
     * Unregisters the active registration for the specified microservice. This
     * causes the service to no longer be listed, when requesting the list of
     * services.
     * 
     * @param requestParams The request parameters
     * @param microservice The microservice to unregister
     * @return True if the microservice was unregistered successfully
     */
    @POST
    @Path("unregister")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "unregister", summary = "Directly remove the registration for a service/microservice, this causes the service to no longer be listed")
    boolean unregister(@BeanParam RequestParams requestParams, @NotNull @Valid Microservice microservice);

    /**
     * Lists all currently registered microservices with their details and status.
     * 
     * @param requestParams The request parameters
     * @return An array of microservices objects
     */
    @GET
    @Path("")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({ Constants.READ_SERVICES_ROLE })
    @Operation(operationId = "list", summary = "List all registered microservices with their details and status")
    Microservice[] list(@BeanParam RequestParams requestParams);

}
