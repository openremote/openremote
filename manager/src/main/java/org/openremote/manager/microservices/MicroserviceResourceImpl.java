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
package org.openremote.manager.microservices;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.http.RequestParams;
import org.openremote.model.microservices.Microservice;
import org.openremote.model.microservices.MicroserviceResource;
import org.openremote.model.util.UniqueIdentifierGenerator;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class MicroserviceResourceImpl extends ManagerWebResource implements MicroserviceResource {

    protected MicroserviceRegistryService microserviceRegistry;
    private static final Logger LOG = Logger.getLogger(MicroserviceResourceImpl.class.getName());

    public MicroserviceResourceImpl(TimerService timerService, ManagerIdentityService identityService,
            MicroserviceRegistryService serviceRegistry) {
        super(timerService, identityService);
        this.microserviceRegistry = serviceRegistry;
    }

    @Override
    public Microservice registerService(RequestParams requestParams, Microservice microservice) {
        if (!isServiceAccount()) {
            LOG.warning("Service registration not allowed for non-service users");
            throw new WebApplicationException("Service registration not allowed for non-service users",
                    Response.Status.FORBIDDEN);
        }

        // Generate a instanceId if not provided
        if (microservice.getInstanceId() == null) {
            String instanceId = UniqueIdentifierGenerator.generateId();
            microservice.setInstanceId(instanceId);
        }

        try {
            Microservice registeredMicroservice = microserviceRegistry.registerService(microservice);

            if (registeredMicroservice == null) {
                LOG.warning("Failed to register microservice: " + microservice.getServiceId() + " with instanceId: "
                        + microservice.getInstanceId());
                throw new WebApplicationException("Failed to register microservice",
                        Response.Status.INTERNAL_SERVER_ERROR);
            }

            return registeredMicroservice;
        } catch (IllegalStateException e) {
            LOG.warning("Failed to register microservice: " + microservice.getServiceId() + " with instanceId: "
                    + microservice.getInstanceId() + ": " + e.getMessage());
            throw new WebApplicationException(e.getMessage(), Response.Status.CONFLICT);
        }

    }

    @Override
    public Microservice[] getServices(RequestParams requestParams) {
        Microservice[] services = microserviceRegistry.getServices();

        // Map services to the representation model
        return Arrays.stream(services)
                .toArray(Microservice[]::new);
    }

    @Override
    public void heartbeat(RequestParams requestParams, String serviceId, String instanceId) {
        if (!isServiceAccount()) {
            LOG.warning("Service heartbeat not allowed for non-service users");
            throw new WebApplicationException("Service heartbeat not allowed for non-service users",
                    Response.Status.FORBIDDEN);
        }

        try {
            microserviceRegistry.heartbeat(serviceId, instanceId);
        } catch (NoSuchElementException e) {
            LOG.warning("Failed heartbeat for microservice " + serviceId + " instance " + instanceId + ": "
                    + e.getMessage());
            throw new WebApplicationException(e.getMessage(), Response.Status.NOT_FOUND);
        }
    }

    @Override
    public void deregisterService(RequestParams requestParams, String serviceId, String instanceId) {
        if (!isServiceAccount()) {
            LOG.warning("Service deregistration not allowed for non-service users");
            throw new WebApplicationException("Service deregistration not allowed for non-service users",
                    Response.Status.FORBIDDEN);
        }

        try {
            microserviceRegistry.deregisterService(serviceId, instanceId);
            LOG.fine("Successfully deregistered microservice: " + serviceId + " instance: " + instanceId);
        } catch (NoSuchElementException e) {
            LOG.warning("Failed to deregister microservice " + serviceId + " instance " + instanceId + ": "
                    + e.getMessage());
            throw new WebApplicationException(e.getMessage(), Response.Status.NOT_FOUND);
        }
    }

}
