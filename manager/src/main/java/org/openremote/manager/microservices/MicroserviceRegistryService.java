/*
 * Copyright 2016, OpenRemote Inc.
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
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.microservices.Microservice;
import org.openremote.model.microservices.MicroserviceStatus;

/**
 * {@link MicroserviceRegistryService} is responsible for registering and
 * managing microservice registrations.
 * 
 * <ul>
 * <li>Callers can create a registration with a given identifier, and the
 * registration will be stored in the registry. The registration will be set to
 * available by default when created or updated.</li>
 * 
 * <li>The registry will handle the expiration of registrations when the TTL has
 * expired, and set the status to unavailable. TTL check can be disabled by
 * setting the ignoreTTL flag to true when registering.</li>
 * </ul>
 */
public class MicroserviceRegistryService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(MicroserviceRegistryService.class.getName());

    protected static final long DEFAULT_TTL_MS = 60000; // 1 minute
    protected static final long CLEANUP_INTERVAL_MS = 30000; // 30 seconds

    protected TimerService timerService;
    protected ScheduledExecutorService scheduledExecutorService;

    // Registration entry record for the in-memory cache
    protected record RegistrationEntry(Microservice service, long expirationTime, boolean ignoreTTL) {
    }

    // Map of registrations, uses a composite key of service id and the given identifier
    protected ConcurrentHashMap<String, RegistrationEntry> registrationMap;

    // Scheduled future for the expiration check task
    protected ScheduledFuture<?> expirationCheckFuture;

    @Override
    public void init(Container container) throws Exception {
        this.timerService = container.getService(TimerService.class);
        this.scheduledExecutorService = container.getScheduledExecutor();
        this.registrationMap = new ConcurrentHashMap<>();
    }

    @Override
    public void start(Container container) throws Exception {
        // Start the expiration check task
        expirationCheckFuture = scheduledExecutorService.scheduleAtFixedRate(this::expirationCheck, 0,
                CLEANUP_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop(Container container) throws Exception {
        // Cancel the expiration check task
        if (expirationCheckFuture != null) {
            expirationCheckFuture.cancel(true);
        }
        registrationMap.clear();
    }

    protected String getRegistrationKey(String serviceId, String identifier) {
        return serviceId + ":" + identifier;
    }

    /**
     * Check for expired registrations and set their status to unavailable if the
     * TTL has expired and the TTL is not ignored
     */
    public void expirationCheck() {
        registrationMap.values().stream()
                .filter(entry -> !entry.ignoreTTL)
                .filter(entry -> entry.expirationTime < timerService.getCurrentTimeMillis())
                .forEach(entry -> {
                    entry.service.setStatus(MicroserviceStatus.UNAVAILABLE);
                });
    }

    /**
     * Register or update a registration for a microservice with a given identifier.
     * 
     * @param identifier   The given identifier (e.g. client remote address)
     * @param microservice The microservice to register
     * @return True if the microservice was registered/updated successfully
     */
    public boolean register(String identifier, Microservice microservice) {
        return register(identifier, microservice, false);
    }

    /**
     * Register or update a registration for a microservice with a given identifier
     * 
     * @param identifier   The given identifier (e.g. client remote address)
     * @param microservice The microservice to register
     * @param ignoreTTL    If true, the TTL will be ignored and the registration will not expire
     * @return True if the microservice was registered/updated successfully
     */
    public boolean register(String identifier, Microservice microservice, boolean ignoreTTL) {
        try {
            String compositeKey = getRegistrationKey(microservice.getServiceId(), identifier);
            LOG.fine("Registering microservice: " + compositeKey + ", ignoreTTL: " + ignoreTTL);
            
            registrationMap.put(compositeKey,
                    new RegistrationEntry(microservice, timerService.getCurrentTimeMillis() + DEFAULT_TTL_MS, ignoreTTL));

            return true;
        } catch (Exception e) {
            LOG.warning("Failed to register microservice: " + e.getMessage());
            return false;
        }
    }

    /**
     * Unregister a microservice with a given identifier
     * 
     * @param identifier The given identifier (e.g. client remote address)
     * @param microservice The microservice to unregister
     * @return True if the microservice was unregistered successfully
     */
    public boolean unregister(String identifier, Microservice microservice) {
        try {
            String compositeKey = getRegistrationKey(microservice.getServiceId(), identifier);
            LOG.fine("Unregistering microservice: " + compositeKey);
            RegistrationEntry removed = registrationMap.remove(compositeKey);
            return removed != null;
        } catch (Exception e) {
            LOG.warning("Failed to unregister microservice: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get all registered services
     * 
     * @return An array of microservices
     */
    public Microservice[] getServices() {
        return registrationMap.values().stream()
                .map(RegistrationEntry::service)
                .toArray(Microservice[]::new);
    }

}
