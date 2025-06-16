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
package org.openremote.model.services;

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
 * Service resource, this resource is used by external services to register
 * themselves with the {@link ServiceRegistryService}.
 * 
 * <li>The client is responsible for periodically sending the
 * {@link ServiceRegistration} to the {@link ServiceRegistryService}.</li>
 * 
 * <li>The {@link ServiceRegistryService} will store the service registration in
 * memory, and will set the status to available. The client should update the
 * registration with the status of the service.</li>
 * 
 * <li>The {@link ServiceRegistryService} will also handle the expiration of
 * service registrations, and set the status to unavailable. Registrations
 * should be updated within the TTL of the registration cache.</li>
 */
@Tag(name = "Service", description = "Operations on external services")
@Path("service")
public interface ServiceResource {

    /**
     * Creates or updates the active registration for the specified service.
     * This is a protected endpoint requires the token to be associated with a
     * service user.
     */
    @POST
    @Path("register")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "register", summary = "Create/update the registration for an external service")
    boolean register(@BeanParam RequestParams requestParams, @NotNull @Valid ServiceDescriptor serviceDescriptor);

    @GET
    @Path("")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({ Constants.READ_SERVICES_ROLE })
    @Operation(operationId = "list", summary = "List all registered services and their status")
    ServiceDescriptor[] list(@BeanParam RequestParams requestParams);

}
