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
import java.util.logging.Logger;

import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.http.RequestParams;
import org.openremote.model.microservices.Microservice;
import org.openremote.model.microservices.MicroserviceInfo;
import org.openremote.model.microservices.MicroserviceNotFoundException;
import org.openremote.model.microservices.MicroserviceRegisterResponse;
import org.openremote.model.microservices.MicroserviceResource;
import org.openremote.model.util.UniqueIdentifierGenerator;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
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
    public MicroserviceRegisterResponse registerService(RequestParams requestParams, Microservice microservice) {
        if (!isServiceAccount()) {
            LOG.warning("Only service users can register services");
            throw new ForbiddenException("Only service users can register services");
        }

        // Generate a unique instanceId
        String instanceId = UniqueIdentifierGenerator.generateId();

        try {
            microserviceRegistry.registerService(microservice, instanceId);
            return new MicroserviceRegisterResponse(instanceId);

        } catch (Exception e) {
            LOG.warning("Failed to register microservice: " + e.getMessage());
            throw new InternalServerErrorException("Failed to register microservice");
        }
    }

    @Override
    public MicroserviceInfo[] getServices(RequestParams requestParams) {
        Microservice[] services = microserviceRegistry.getServices();
        return Arrays.stream(services)
                .map(MicroserviceInfo::fromMicroservice)
                .toArray(MicroserviceInfo[]::new);
    }

    @Override
    public Response heartbeat(RequestParams requestParams, String serviceId, String instanceId) {
        if (!isServiceAccount()) {
            LOG.warning("Only service users can send heartbeats");
            throw new ForbiddenException("Only service users can send heartbeats");
        }

        try {
            microserviceRegistry.heartbeat(serviceId, instanceId);
            return Response.noContent().build();
        } catch (MicroserviceNotFoundException e) {
            LOG.warning("Failed heartbeat for microservice " + serviceId + " instance " + instanceId + ": "
                    + e.getMessage());
            throw new NotFoundException(e.getMessage());
        } catch (Exception e) {
            LOG.warning("Failed heartbeat for microservice " + serviceId + " instance " + instanceId + ": "
                    + e.getMessage());
            throw new InternalServerErrorException("Failed to send heartbeat for microservice");
        }
    }

    @Override
    public Response deregisterService(RequestParams requestParams, String serviceId, String instanceId) {
        if (!isServiceAccount()) {
            LOG.warning("Only service users can deregister services");
            throw new ForbiddenException("Only service users can deregister services");
        }
        try {
            microserviceRegistry.deregisterService(serviceId, instanceId);
            return Response.noContent().build();
        } catch (MicroserviceNotFoundException e) {
            LOG.warning("Failed to deregister microservice " + serviceId + " instance " + instanceId + ": "
                    + e.getMessage());
            throw new NotFoundException(e.getMessage());
        } catch (Exception e) {
            LOG.warning("Failed to deregister microservice " + serviceId + " instance " + instanceId + ": "
                    + e.getMessage());
            throw new InternalServerErrorException("Failed to deregister microservice");
        }
    }

}
