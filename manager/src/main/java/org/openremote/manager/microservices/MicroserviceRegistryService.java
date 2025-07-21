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
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
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

    // Default TTL for a registration, 90 seconds
    protected static final long DEFAULT_TTL_MS = 90000;

    protected TimerService timerService;
    protected ScheduledExecutorService scheduledExecutorService;
    protected ManagerIdentityService identityService;

    // Registration wrapper record for the in-memory cache
    protected record RegistrationEntry(Microservice service, long expirationTime, boolean ignoreTTL) {
    }

    // Map of registrations, uses the serviceId as the key
    // E.g. "energy-service"
    protected ConcurrentHashMap<String, RegistrationEntry> registrationMap;

    // Scheduled future for the expiration check task
    protected ScheduledFuture<?> expirationCheckFuture;

    @Override
    public void init(Container container) throws Exception {
        this.timerService = container.getService(TimerService.class);
        this.scheduledExecutorService = container.getScheduledExecutor();
        this.identityService = container.getService(ManagerIdentityService.class);

        // Register the microservice resource
        container.getService(ManagerWebService.class).addApiSingleton(
                new MicroserviceResourceImpl(timerService, identityService, this));

        this.registrationMap = new ConcurrentHashMap<>();
    }

    @Override
    public void start(Container container) throws Exception {
        expirationCheckFuture = scheduledExecutorService.scheduleAtFixedRate(this::expirationCheck, 0,
                DEFAULT_TTL_MS, TimeUnit.MILLISECONDS);
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
    public void expirationCheck() {
        long now = timerService.getCurrentTimeMillis();
        registrationMap.entrySet().stream()
                .filter(entry -> !entry.getValue().ignoreTTL())
                .filter(entry -> entry.getValue().expirationTime() < now)
                .forEach(entry -> registrationMap.computeIfPresent(entry.getKey(), (k, registration) -> {
                    // Copy with new status to avoid side effects
                    Microservice service = new Microservice(registration.service().getLabel(),
                            registration.service().getServiceId(),
                            registration.service().getWebUiUrl(),
                            MicroserviceStatus.UNAVAILABLE,
                            registration.service().getMultiTenancy());

                    service.setStatus(MicroserviceStatus.UNAVAILABLE);
                    return new RegistrationEntry(service, registration.expirationTime(), registration.ignoreTTL());
                }));
    }

    /**
     * Register a microservice
     *
     * @param microservice The microservice to register
     * @return True if the microservice was registered successfully
     */
    public boolean register(Microservice microservice) {
        return register(microservice, false);
    }

    /**
     * Refresh the TTL for a microservice
     *
     * @param serviceId  The ID of the microservice to refresh the TTL for
     * @return True if the TTL was refreshed successfully
     */
    public boolean heartbeat(String serviceId) {
        try {
            RegistrationEntry entry = registrationMap.get(serviceId);
            if (entry != null) {
                registrationMap.put(serviceId, new RegistrationEntry(
                        entry.service(),
                        timerService.getCurrentTimeMillis() + DEFAULT_TTL_MS,
                        entry.ignoreTTL()));
                return true;
            }
            return false;
        } catch (Exception e) {
            LOG.warning("Failed to refresh TTL for serviceId: " + serviceId + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Register a microservice
     *
     * @param microservice The microservice to register
     * @param ignoreTTL    If true, the TTL will be ignored and the registration
     *                     will not expire
     * @return True if the microservice was registered/updated successfully
     */
    public boolean register(Microservice microservice, boolean ignoreTTL) {
        try {
            registrationMap.put(microservice.getServiceId(),
                    new RegistrationEntry(microservice, timerService.getCurrentTimeMillis() + DEFAULT_TTL_MS,
                            ignoreTTL));

            return true;
        } catch (Exception e) {
            LOG.warning("Failed to register serviceId: " + microservice.getServiceId() + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Deregister a microservice
     *
     * @param serviceId  The ID of the microservice to unregister
     * @return True if the microservice was unregistered successfully
     */
    public boolean deregister(String serviceId) {
        try {
            RegistrationEntry removed = registrationMap.remove(serviceId);
            return removed != null;
        } catch (Exception e) {
            LOG.warning("Failed to deregister serviceId: " + serviceId + " - " + e.getMessage());
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
