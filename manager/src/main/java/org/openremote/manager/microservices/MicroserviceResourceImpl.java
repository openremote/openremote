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

import java.util.logging.Logger;

import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.http.RequestParams;
import org.openremote.model.microservices.Microservice;
import org.openremote.model.microservices.MicroserviceResource;

import static org.openremote.model.Constants.MASTER_REALM;

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
                    Response.Status.UNAUTHORIZED);
        }

        if (!isRealmActiveAndAccessible(microservice.getRealm())) {
            throw new WebApplicationException(
                    "Realm '" + microservice.getRealm() + "' is nonexistent, inactive or inaccessible",
                    Response.Status.FORBIDDEN);
        }

        try {
            Microservice registeredMicroservice = microserviceRegistry.registerService(microservice);

            if (registeredMicroservice == null) {
                LOG.severe("Failed to register service: " + microservice.getServiceId() + " with instanceId: "
                        + microservice.getInstanceId() + " due to an unexpected error");
                throw new WebApplicationException("Failed to register service",
                        Response.Status.INTERNAL_SERVER_ERROR);
            }

            return registeredMicroservice;
        } catch (IllegalStateException e) {
            LOG.warning("Failed to register service: " + microservice.getServiceId() + " with instanceId: "
                    + microservice.getInstanceId() + " due to conflict: " + e.getMessage());
            throw new WebApplicationException(e.getMessage(), Response.Status.CONFLICT);
        }

    }

    @Override
    public Microservice registerGlobalService(RequestParams requestParams, Microservice microservice) {
        if (!isServiceAccount()) {
            LOG.warning("Service registration is only available for service users");
            throw new WebApplicationException("Service registration is only available for service users",
                    Response.Status.UNAUTHORIZED);
        }

        if (!isSuperUser()) {
            throw new WebApplicationException("Global services can only be registered by super users",
                    Response.Status.FORBIDDEN);
        }

        if (!microservice.getRealm().equals(MASTER_REALM)) {
            throw new WebApplicationException("Global services must have the realm set to the master realm, got: "
                    + microservice.getRealm(),
                    Response.Status.BAD_REQUEST);
        }

        try {
            Microservice registeredMicroservice = microserviceRegistry.registerService(microservice, true);

            if (registeredMicroservice == null) {
                LOG.severe("Failed to register global service: " + microservice.getServiceId() + " with instanceId: "
                        + microservice.getInstanceId() + " due to an unexpected error");
                throw new WebApplicationException("Failed to register global service",
                        Response.Status.INTERNAL_SERVER_ERROR);
            }

            return registeredMicroservice;
        } catch (IllegalStateException e) {
            LOG.warning("Failed to register global service: " + microservice.getServiceId() + " with instanceId: "
                    + microservice.getInstanceId() + " due to conflict: " + e.getMessage());
            throw new WebApplicationException(e.getMessage(), Response.Status.CONFLICT);
        }
    }

    @Override
    public Microservice[] getServices(RequestParams requestParams, String realm) {
        if (!isRealmActiveAndAccessible(realm)) {
            throw new WebApplicationException("Realm '" + realm + "' is nonexistent, inactive or inaccessible",
                    Response.Status.FORBIDDEN);
        }

        return microserviceRegistry.getServices(realm);
    }

    @Override
    public Microservice getService(RequestParams requestParams, String serviceId, String instanceId) {
        Microservice service = microserviceRegistry.getService(serviceId, instanceId);

        if (service == null) {
            throw new WebApplicationException("Service " + serviceId + " instance " + instanceId + " not found",
                    Response.Status.NOT_FOUND);
        }

        if (!isRealmActiveAndAccessible(service.getRealm())) {
            throw new WebApplicationException(
                    "Realm '" + service.getRealm() + "' is nonexistent, inactive or inaccessible",
                    Response.Status.FORBIDDEN);
        }

        return service;
    }

    @Override
    public Microservice[] getGlobalServices(RequestParams requestParams) {
        return microserviceRegistry.getGlobalServices();
    }

    @Override
    public void heartbeat(RequestParams requestParams, String serviceId, String instanceId) {
        if (!isServiceAccount()) {
            LOG.warning("Service heartbeat is only available for service users");
            throw new WebApplicationException("Service heartbeat is only available for service users",
                    Response.Status.UNAUTHORIZED);
        }

        Microservice service = microserviceRegistry.getService(serviceId, instanceId);

        if (service == null) {
            throw new WebApplicationException("Service " + serviceId + " instance " + instanceId + " not found",
                    Response.Status.NOT_FOUND);
        }

        if (!isRealmActiveAndAccessible(service.getRealm())) {
            throw new WebApplicationException(
                    "Realm '" + service.getRealm() + "' is nonexistent, inactive or inaccessible",
                    Response.Status.FORBIDDEN);
        }

        // Restrict global service mutations to superusers
        if (isServiceGlobalAndUserIsNotSuperUser(service)) {
            throw new WebApplicationException("Heartbeats for global services can only be performed by super users",
                    Response.Status.FORBIDDEN);
        }

        microserviceRegistry.heartbeat(serviceId, instanceId);
    }

    @Override
    public void deregisterService(RequestParams requestParams, String serviceId, String instanceId) {
        if (!isServiceAccount()) {
            LOG.warning("De-registering a service is only available for service users");
            throw new WebApplicationException("De-registering a service is only available for service users",
                    Response.Status.UNAUTHORIZED);
        }

        Microservice service = microserviceRegistry.getService(serviceId, instanceId);

        if (service == null) {
            throw new WebApplicationException("Service " + serviceId + " instance " + instanceId + " not found",
                    Response.Status.NOT_FOUND);
        }

        if (!isRealmActiveAndAccessible(service.getRealm())) {
            throw new WebApplicationException(
                    "Realm '" + service.getRealm() + "' is nonexistent, inactive or inaccessible",
                    Response.Status.FORBIDDEN);
        }

        // Restrict global service mutations to superusers
        if (isServiceGlobalAndUserIsNotSuperUser(service)) {
            throw new WebApplicationException("Global services can only be de-registered by super users",
                    Response.Status.FORBIDDEN);
        }

        microserviceRegistry.deregisterService(serviceId, instanceId);
    }

    protected boolean isServiceGlobalAndUserIsNotSuperUser(Microservice service) {
        return service.getIsGlobal() && !isSuperUser();
    }

}
