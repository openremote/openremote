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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.microservices.Microservice;
import org.openremote.model.microservices.MicroserviceStatus;

/**
 * Service for registering and managing microservice lifecycle with TTL-based
 * expiration.
 * 
 * <p>
 * Provides centralized registry functionality including registration, heartbeat
 * management,
 * deregistration, and status tracking. Services are marked unavailable when TTL
 * expires.
 * </p>
 * 
 * <ul>
 * <li>TTL-based registration with automatic expiration (default: 90s)</li>
 * <li>Heartbeat mechanism for TTL renewal</li>
 * <li>Thread-safe concurrent operations</li>
 * <li>REST API via {@link MicroserviceResource}</li>
 * </ul>
 * 
 * @see MicroserviceResource
 */
public class MicroserviceRegistryService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(MicroserviceRegistryService.class.getName());

    protected static final long DEFAULT_TTL_MS = 90000; // 90 seconds till a service is marked as unavailable

    protected TimerService timerService;
    protected ScheduledExecutorService scheduledExecutorService;
    protected ManagerIdentityService identityService;

    // Registration record
    protected record RegistrationEntry(Microservice service, long expirationTime, boolean ignoreTTL) {
    }

    // <serviceId, <instanceId, RegistrationEntry>>
    protected ConcurrentHashMap<String, ConcurrentHashMap<String, RegistrationEntry>> registrationMap;

    // Scheduled future for the expiration check task
    protected ScheduledFuture<?> expirationCheckFuture;

    @Override
    public void init(Container container) throws Exception {
        this.timerService = container.getService(TimerService.class);
        this.scheduledExecutorService = container.getScheduledExecutor();
        this.identityService = container.getService(ManagerIdentityService.class);

        // Register the microservice REST resource
        container.getService(ManagerWebService.class).addApiSingleton(
                new MicroserviceResourceImpl(timerService, identityService, this));

        this.registrationMap = new ConcurrentHashMap<>();
    }

    @Override
    public void start(Container container) throws Exception {
        expirationCheckFuture = scheduledExecutorService.scheduleAtFixedRate(this::runExpirationCheck, 0,
                DEFAULT_TTL_MS / 2, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop(Container container) throws Exception {
        if (expirationCheckFuture != null) {
            expirationCheckFuture.cancel(true);
        }
        registrationMap.clear();
    }

    /**
     * Check for expired registrations and set their status to unavailable if the
     * TTL has expired and the TTL is not ignored
     */
    public void runExpirationCheck() {
        registrationMap.values().stream()
                .flatMap(map -> map.values().stream())
                .filter(entry -> !entry.ignoreTTL)
                .filter(entry -> entry.expirationTime < timerService.getCurrentTimeMillis())
                .forEach(entry -> {
                    entry.service.setStatus(MicroserviceStatus.UNAVAILABLE);
                });
    }

    /**
     * Register or update a registration for a microservice with a given identifier.
     * 
     * @param microservice The microservice to register
     * @param instanceId   The instanceId of the microservice to register
     */
    public void registerService(Microservice microservice, String instanceId) {
        registerService(microservice, instanceId, false);
    }

    /**
     * Register or update a registration for a microservice with a given identifier
     * 
     * @param microservice The microservice to register
     * @param instanceId   The instanceId of the microservice to register
     * @param ignoreTTL    If true, the TTL will be ignored and the registration
     *                     will not expire
     */
    public void registerService(Microservice microservice, String instanceId, boolean ignoreTTL) {
        try {
            LOG.fine("Registering microservice: " + microservice.getServiceId() + ", instanceId: " + instanceId
                    + ", ignoreTTL: " + ignoreTTL);
            registrationMap.computeIfAbsent(microservice.getServiceId(), k -> new ConcurrentHashMap<>())
                    .put(instanceId,
                            new RegistrationEntry(microservice, timerService.getCurrentTimeMillis() + DEFAULT_TTL_MS,
                                    ignoreTTL));

        } catch (Exception e) {
            LOG.warning("Failed to register microservice: " + e.getMessage());
        }
    }

    /**
     * Send a heartbeat to update the active registration TTL for the specified
     * microservice.
     * This is used to indicate that the microservice is still running and
     * available.
     * 
     * @param serviceId  The serviceId of the microservice to send the heartbeat to
     * @param instanceId The instanceId of the microservice to send the heartbeat to
     */
    public void sendHeartbeat(String serviceId, String instanceId) {
        ConcurrentHashMap<String, RegistrationEntry> instances = registrationMap.get(serviceId);
        if (instances == null) {
            LOG.warning("Failed to send heartbeat to microservice: " + serviceId + ", instanceId: " + instanceId
                    + " - service not found");
            return;
        }
        RegistrationEntry entry = instances.get(instanceId);
        if (entry != null) {
            // Refresh the TTL expiration time
            instances.put(instanceId,
                    new RegistrationEntry(entry.service, timerService.getCurrentTimeMillis() + DEFAULT_TTL_MS,
                            entry.ignoreTTL));

            // Set the status to available
            entry.service.setStatus(MicroserviceStatus.AVAILABLE);

        } else {
            LOG.warning("Failed to send heartbeat to microservice: " + serviceId + ", instanceId: " + instanceId
                    + " - instance not found");
        }
    }

    /**
     * Deregister a microservice
     * 
     * @param serviceId  The serviceId of the microservice to deregister
     * @param instanceId The instanceId of the microservice to deregister
     */
    public void deregisterService(String serviceId, String instanceId) {
        try {
            ConcurrentHashMap<String, RegistrationEntry> instances = registrationMap.get(serviceId);
            if (instances == null) {
                return;
            }
            instances.remove(instanceId);
        } catch (Exception e) {
            LOG.warning("Failed to deregister microservice: " + e.getMessage());
        }
    }

    /**
     * Get all registered services
     * 
     * @return An array of microservices
     */
    public Microservice[] getServices() {
        // Ensure that the expiration check is run before returning the services
        // This is to ensure that the services are up to date and that the TTL is
        // respected
        runExpirationCheck();

        return registrationMap.values().stream()
                .flatMap(map -> map.values().stream())
                .map(RegistrationEntry::service)
                .toArray(Microservice[]::new);
    }

}
