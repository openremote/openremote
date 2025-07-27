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

import java.util.ArrayList;
import java.util.List;
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
import org.openremote.model.microservices.MicroserviceNotFoundException;
import org.openremote.model.microservices.MicroserviceStatus;
import org.openremote.model.microservices.MicroserviceRegistryEntry;

/**
 * Service for registering and managing microservice lifecycle with TTL-based
 * expiration.
 *
 * <p>
 * Provides centralized registry functionality including registration
 * management, heartbeat
 * management, and status tracking. Services are marked unavailable when TTL
 * expires.
 * </p>
 *
 * <ul>
 * <li>TTL-based registration with automatic expiration (default: 90s)</li>
 * <li>Heartbeat mechanism for TTL renewal</li>
 * </ul>
 */
public class MicroserviceRegistryService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(MicroserviceRegistryService.class.getName());

    protected static final long DEFAULT_TTL_MS = 90000; // 90 seconds till a service is marked as unavailable

    protected TimerService timerService;
    protected ScheduledExecutorService scheduledExecutorService;
    protected ManagerIdentityService identityService;

    // unique serviceId -> list of registered instances
    protected ConcurrentHashMap<String, List<MicroserviceRegistryEntry>> registry;

    // Scheduled future for the TTL check task
    protected ScheduledFuture<?> ttlCheckFuture;

    @Override
    public void init(Container container) throws Exception {
        this.timerService = container.getService(TimerService.class);
        this.scheduledExecutorService = container.getScheduledExecutor();
        this.identityService = container.getService(ManagerIdentityService.class);

        // Register the microservice REST resource
        container.getService(ManagerWebService.class).addApiSingleton(
                new MicroserviceResourceImpl(timerService, identityService, this));

        this.registry = new ConcurrentHashMap<>();
    }

    @Override
    public void start(Container container) throws Exception {
        ttlCheckFuture = scheduledExecutorService.scheduleAtFixedRate(this::runTTLCheck, 0,
                DEFAULT_TTL_MS / 2, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop(Container container) throws Exception {
        // ensure the TTL check task is cancelled
        if (ttlCheckFuture != null) {
            ttlCheckFuture.cancel(true);
        }
        registry.clear();
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
            registry.computeIfAbsent(microservice.getServiceId(), k -> new ArrayList<>())
                    .add(new MicroserviceRegistryEntry(microservice, instanceId,
                            timerService.getCurrentTimeMillis() + DEFAULT_TTL_MS,
                            ignoreTTL));

        } catch (Exception e) {
            LOG.warning("Failed to register microservice: " + e.getMessage());
        }
    }

    /**
     * Update the active registration TTL for the specified microservice.
     * This is used to indicate that the microservice is still running and
     * available.
     * 
     * If the microservice is not found, a {@link MicroserviceNotFoundException}
     * is thrown.
     *
     * @param serviceId  The serviceId of the microservice to send the heartbeat to
     * @param instanceId The instanceId of the microservice to send the heartbeat to
     */
    public void heartbeat(String serviceId, String instanceId) {
        List<MicroserviceRegistryEntry> instances = registry.get(serviceId);

        if (instances == null) {
            LOG.warning("Failed to refresh TTL for microservice: " + serviceId + ", instanceId: " + instanceId
                    + " - service not found");
            throw new MicroserviceNotFoundException("Specified service could not be found");
        }

        MicroserviceRegistryEntry entry = instances.stream()
                .filter(e -> e.getMicroservice().getServiceId().equals(serviceId)
                        && e.getInstanceId().equals(instanceId))
                .findFirst()
                .orElse(null);

        if (entry != null) {
            // Refresh TTL
            entry.setExpirationTime(timerService.getCurrentTimeMillis() + DEFAULT_TTL_MS);
            entry.getMicroservice().setStatus(MicroserviceStatus.AVAILABLE);

        } else {
            LOG.warning("Failed to refresh TTL for microservice: " + serviceId + ", instanceId: " + instanceId
                    + " - instance not found");
            throw new MicroserviceNotFoundException("Specified instance of service could not be found");
        }
    }

    /**
     * Deregister a microservice instance
     * 
     * If the microservice is not found, a {@link MicroserviceNotFoundException}
     * is thrown.
     *
     * @param serviceId  The serviceId of the microservice to deregister
     * @param instanceId The instanceId of the microservice to deregister
     */
    public void deregisterService(String serviceId, String instanceId) {

        List<MicroserviceRegistryEntry> instances = registry.get(serviceId);
        if (instances == null) {
            throw new MicroserviceNotFoundException("Specified service could not be found");
        }

        MicroserviceRegistryEntry entry = instances.stream()
                .filter(e -> e.getMicroservice().getServiceId().equals(serviceId)
                        && e.getInstanceId().equals(instanceId))
                .findFirst()
                .orElse(null);

        if (entry == null) {
            throw new MicroserviceNotFoundException("Specified instance of service could not be found");
        }

        instances.remove(entry);

        // If no instances are left, remove the service from the registry
        if (instances.size() == 0) {
            registry.remove(serviceId);
        }
    }

    /**
     * Get all registered services/microservices
     *
     * @return An array of all registered microservices
     */
    public Microservice[] getServices() {
        runTTLCheck();

        return registry.values().stream()
                .flatMap(List::stream)
                .map(MicroserviceRegistryEntry::getMicroservice)
                .toArray(Microservice[]::new);
    }

    /**
     * Check for expired registrations and set their status to unavailable if the
     * TTL has expired and the TTL is not ignored
     */
    protected void runTTLCheck() {
        registry.values().stream()
                .flatMap(List::stream)
                .filter(entry -> !entry.isIgnoreTTL())
                .filter(entry -> entry.isExpired(timerService.getCurrentTimeMillis()))
                .forEach(entry -> entry.getMicroservice().setStatus(MicroserviceStatus.UNAVAILABLE));
    }

}
