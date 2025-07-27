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
import java.util.stream.Collectors;

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
 * <li>Purge unavailable instances after 24 hours</li>
 * </ul>
 */
public class MicroserviceRegistryService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(MicroserviceRegistryService.class.getName());

    // 90 seconds till a service is marked as unavailable
    protected static final long DEFAULT_TTL_MS = 90000;

    // 24 hours after an instance is marked as unavailable, it is purged
    protected static final long PURGE_UNAVAILABLE_MS = 1000 * 60 * 60 * 24;

    protected TimerService timerService;
    protected ScheduledExecutorService scheduledExecutorService;
    protected ManagerIdentityService identityService;

    // serviceId -> list of registered instances
    protected ConcurrentHashMap<String, List<MicroserviceRegistryEntry>> registry;

    // Scheduled future for the TTL check task
    protected ScheduledFuture<?> markExpiredInstancesAsUnavailableFuture;

    // Scheduled future for the purge task
    protected ScheduledFuture<?> cleanupExpiredUnavailableInstancesFuture;

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

        // Periodically check for expired instances
        markExpiredInstancesAsUnavailableFuture = scheduledExecutorService.scheduleAtFixedRate(
                this::markExpiredInstancesAsUnavailable, 0,
                DEFAULT_TTL_MS / 2, TimeUnit.MILLISECONDS);

        // Periodically cleanup expired unavailable instances
        cleanupExpiredUnavailableInstancesFuture = scheduledExecutorService.scheduleAtFixedRate(
                this::cleanupExpiredUnavailableInstances, 0,
                PURGE_UNAVAILABLE_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop(Container container) throws Exception {
        // ensure the TTL check task is cancelled
        if (markExpiredInstancesAsUnavailableFuture != null) {
            markExpiredInstancesAsUnavailableFuture.cancel(true);
        }
        if (cleanupExpiredUnavailableInstancesFuture != null) {
            cleanupExpiredUnavailableInstancesFuture.cancel(true);
        }
        registry.clear();
    }

    /**
     * Register a microservice instance
     *
     * @param microservice The microservice to register
     */
    public void registerService(Microservice microservice) {
        try {
            LOG.fine("Registering microservice: " + microservice.getServiceId() + ", instanceId: "
                    + microservice.getInstanceId());

            List<MicroserviceRegistryEntry> instances = registry.computeIfAbsent(microservice.getServiceId(),
                    k -> new ArrayList<>());

            // Check if the given instance already exists
            MicroserviceRegistryEntry existingEntry = instances.stream()
                    .filter(e -> e.getMicroservice().getInstanceId().equals(microservice.getInstanceId()))
                    .findFirst()
                    .orElse(null);

            if (existingEntry != null) {
                LOG.warning("Microservice instance already registered: " + microservice.getServiceId()
                        + ", instanceId: " + microservice.getInstanceId());
                throw new IllegalStateException("Microservice instance already registered");
            }

            // Add new entry
            instances.add(
                    new MicroserviceRegistryEntry(microservice, timerService.getCurrentTimeMillis() + DEFAULT_TTL_MS));

        } catch (Exception e) {
            LOG.warning("Failed to register microservice: " + e.getMessage());
            throw new RuntimeException("Failed to register microservice", e);
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
                .filter(e -> e.getMicroservice().getInstanceId().equals(instanceId))
                .findFirst()
                .orElse(null);

        if (entry != null) {
            // Update the expiration time and set the status to available
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
                .filter(e -> e.getMicroservice().getInstanceId().equals(instanceId))
                .findFirst()
                .orElse(null);

        if (entry == null) {
            throw new MicroserviceNotFoundException("Specified instance of service could not be found");
        }

        instances.remove(entry);

        // If no instances are left, remove the service from the registry
        if (instances.isEmpty()) {
            registry.remove(serviceId);
        }
    }

    /**
     * Get all registered services/microservices and their instances
     *
     * @return An array of all registered microservices and their instances
     */
    public Microservice[] getServices() {
        markExpiredInstancesAsUnavailable();

        return registry.values().stream()
                .flatMap(List::stream)
                .map(MicroserviceRegistryEntry::getMicroservice)
                .toArray(Microservice[]::new);
    }

    /**
     * Check for expired registrations and mark them as unavailable if the
     * TTL has expired.
     */
    protected void markExpiredInstancesAsUnavailable() {
        long currentTime = timerService.getCurrentTimeMillis();

        registry.values().stream()
                .flatMap(List::stream)
                .filter(entry -> entry.getExpirationTime() < currentTime
                        && entry.getMicroservice().getStatus() == MicroserviceStatus.AVAILABLE)
                .forEach(entry -> entry.getMicroservice().setStatus(MicroserviceStatus.UNAVAILABLE));
    }

    protected void cleanupExpiredUnavailableInstances() {
        long currentTime = timerService.getCurrentTimeMillis();
        long purgeThreshold = currentTime + PURGE_UNAVAILABLE_MS;

        List<MicroserviceRegistryEntry> toRemove = registry.values().stream()
                .flatMap(List::stream)
                .filter(entry -> entry.getMicroservice().getStatus() == MicroserviceStatus.UNAVAILABLE
                        && entry.getExpirationTime() < purgeThreshold)
                .collect(Collectors.toList());

        // Remove the collected entries
        toRemove.forEach(entry -> {
            deregisterService(entry.getMicroservice().getServiceId(), entry.getMicroservice().getInstanceId());
        });

    }

}
