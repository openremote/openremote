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

import static org.openremote.model.Constants.MASTER_REALM;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.openremote.container.timer.TimerService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.event.shared.EventFilter;
import org.openremote.model.event.shared.EventSubscription;
import org.openremote.model.event.shared.RealmFilter;
import org.openremote.model.microservices.Microservice;
import org.openremote.model.microservices.MicroserviceEvent;
import org.openremote.model.microservices.MicroserviceStatus;
import org.openremote.model.security.ClientRole;
import org.openremote.model.microservices.MicroserviceLeaseInfo;

/**
 * Service discovery and registration for microservices/external services.
 *
 * <p>
 * Provides centralized registry functionality including registration
 * management,
 * heartbeat management, and status tracking. Services are marked unavailable
 * when their lease expires.
 * </p>
 *
 * <ul>
 * <li>Lease-based registration with automatic expiration (default: 60s)</li>
 * <li>Heartbeat mechanism for lease renewal</li>
 * <li>Deregister unavailable instances after 24 hours</li>
 * </ul>
 */
public class MicroserviceRegistryService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(MicroserviceRegistryService.class.getName());

    // Lease duration is 60 seconds until expiration
    protected static final long DEFAULT_LEASE_DURATION_MS = 60000;
    // Mark expired service instances every 30 seconds
    protected static final long MARK_EXPIRED_INSTANCES_INTERVAL_MS = 30000;

    // Deregister threshold is 24 hours since a service has gone unavailable
    protected static final long DEFAULT_DEREGISTER_UNAVAILABLE_MS = 86400000;
    // Check for deregister-able services every hour
    protected static final long DEREGISTER_UNAVAILABLE_INTERVAL_MS = 3600000;

    protected TimerService timerService;
    protected ScheduledExecutorService scheduledExecutorService;
    protected ManagerIdentityService identityService;
    protected ClientEventService clientEventService;

    // serviceId -> list of registered microservices/services
    protected ConcurrentHashMap<String, List<Microservice>> registry;

    // Scheduled future for the lease check task
    protected ScheduledFuture<?> markExpiredInstancesAsUnavailableFuture;

    // Scheduled future for the deregister task
    protected ScheduledFuture<?> deregisterExpiredUnavailableInstancesFuture;

    @Override
    public void init(Container container) throws Exception {
        this.timerService = container.getService(TimerService.class);
        this.scheduledExecutorService = container.getScheduledExecutor();
        this.identityService = container.getService(ManagerIdentityService.class);
        this.clientEventService = container.getService(ClientEventService.class);

        // Register the microservice REST resource
        container.getService(ManagerWebService.class).addApiSingleton(
                new MicroserviceResourceImpl(timerService, identityService, this));

        this.registry = new ConcurrentHashMap<>();

        // Add a subscription authorizer for microservice events
        clientEventService.addSubscriptionAuthorizer((realm, authContext, eventSubscription) -> {
            if (!eventSubscription.isEventType(MicroserviceEvent.class) || authContext == null) {
                return false;
            }

            @SuppressWarnings("unchecked")
            EventSubscription<MicroserviceEvent> subscription = (EventSubscription<MicroserviceEvent>) eventSubscription;

            // Add a custom filter to the subscription
            // Filters the events to only allow events for the authenticated realm or global
            // services
            subscription.setFilter(event -> {
                Microservice eventMicroservice = event.getMicroservice();
                boolean isGlobalService = eventMicroservice.getIsGlobal();
                boolean realmMatches = eventMicroservice.getRealm().equals(authContext.getAuthenticatedRealmName());

                return isGlobalService || realmMatches ? event : null;
            });

            // Super user is always allowed to subscribe
            if (authContext.isSuperUser()) {
                return true;
            }

            // Regular users must have the read services role
            return authContext.hasResourceRole(ClientRole.READ_SERVICES.getValue(),
                    Constants.KEYCLOAK_CLIENT_ID);
        });
    }

    @Override
    public void start(Container container) throws Exception {
        markExpiredInstancesAsUnavailableFuture = scheduledExecutorService.scheduleAtFixedRate(
                this::markExpiredInstancesAsUnavailable, 0,
                MARK_EXPIRED_INSTANCES_INTERVAL_MS, TimeUnit.MILLISECONDS);

        deregisterExpiredUnavailableInstancesFuture = scheduledExecutorService.scheduleAtFixedRate(
                this::deregisterLongExpiredInstances, 0,
                DEREGISTER_UNAVAILABLE_INTERVAL_MS, TimeUnit.MILLISECONDS);

        LOG.info("MicroserviceRegistryService started with lease duration: " + DEFAULT_LEASE_DURATION_MS
                + "ms and deregister interval: " + DEFAULT_DEREGISTER_UNAVAILABLE_MS + "ms");
    }

    @Override
    public void stop(Container container) throws Exception {
        if (markExpiredInstancesAsUnavailableFuture != null) {
            markExpiredInstancesAsUnavailableFuture.cancel(true);
        }
        if (deregisterExpiredUnavailableInstancesFuture != null) {
            deregisterExpiredUnavailableInstancesFuture.cancel(true);
        }
        registry.clear();
    }

    /**
     * Register a microservice instance
     *
     * @param microservice The microservice to register
     */
    public Microservice registerService(Microservice microservice) {
        return registerService(microservice, false);
    }

    /**
     * Register a microservice instance
     *
     * @param microservice    The microservice to register
     * @param isGlobalService Whether the service should be available to all realms
     *                        e.g. globally
     *                        should only be for services that use a super admin
     *                        service user e.g. has global access.
     */
    public Microservice registerService(Microservice microservice, boolean isGlobal) {
        LOG.info("Registering microservice: " + microservice.getServiceId() + ", instanceId: "
                + microservice.getInstanceId());

        List<Microservice> instances = registry.computeIfAbsent(microservice.getServiceId(),
                k -> new ArrayList<>());

        // Check if the given instance already exists in the registry
        Microservice existingEntry = instances.stream()
                .filter(e -> e.getInstanceId().equals(microservice.getInstanceId()))
                .findFirst()
                .orElse(null);

        if (existingEntry != null) {
            LOG.warning("Microservice instance already registered: " + microservice.getServiceId()
                    + ", instanceId: " + microservice.getInstanceId());
            throw new IllegalStateException("Microservice instance already registered");
        }

        // Set the lease info for the new instance
        long registrationTimestamp = timerService.getCurrentTimeMillis();
        long renewalTimestamp = timerService.getCurrentTimeMillis();
        long expirationTimestamp = registrationTimestamp + DEFAULT_LEASE_DURATION_MS;

        microservice.setLeaseInfo(new MicroserviceLeaseInfo(expirationTimestamp,
                registrationTimestamp, renewalTimestamp));

        // Set the global visibility state
        microservice.setIsGlobal(isGlobal);

        // Add the instance to the registry
        instances.add(microservice);

        // Publish the register event
        clientEventService.publishEvent(new MicroserviceEvent(microservice, MicroserviceEvent.Cause.REGISTER));

        LOG.info("Successfully registered microservice: " + microservice.getServiceId() + ", instanceId: "
                + microservice.getInstanceId() + ", isGlobal: " + microservice.getIsGlobal());

        return microservice;
    }

    /**
     * Update the active registration lease info for the specified microservice.
     * This is used to indicate that the microservice is still running and
     * available.
     * If the microservice is not found, a {@link NoSuchElementException} is
     * thrown.
     *
     * @param serviceId  The serviceId of the microservice to send the heartbeat to
     * @param instanceId The instanceId of the microservice to send the heartbeat to
     */
    public void heartbeat(String serviceId, String instanceId) {
        List<Microservice> instances = registry.get(serviceId);

        if (instances == null) {
            LOG.warning("Failed to refresh lease info for microservice: " + serviceId + ", instanceId: " + instanceId
                    + " - service not found");
            throw new NoSuchElementException("Specified service could not be found");
        }

        Microservice entry = instances.stream()
                .filter(e -> e.getInstanceId().equals(instanceId))
                .findFirst()
                .orElse(null);

        if (entry != null) {
            long renewalTimestamp = timerService.getCurrentTimeMillis();
            long expirationTimestamp = renewalTimestamp + DEFAULT_LEASE_DURATION_MS;

            // Update the lease info and set the instance status to available
            entry.getLeaseInfo().setRenewalTimestamp(renewalTimestamp);
            entry.getLeaseInfo().setExpirationTimestamp(expirationTimestamp);
            entry.setStatus(MicroserviceStatus.AVAILABLE);

            // Publish the update event as the lease has been refreshed
            clientEventService.publishEvent(new MicroserviceEvent(entry, MicroserviceEvent.Cause.UPDATE));

            LOG.fine(
                    "Successfully refreshed lease info for microservice: " + serviceId + ", instanceId: " + instanceId);

        } else {
            LOG.warning("Failed to refresh lease info for microservice: " + serviceId + ", instanceId: " + instanceId
                    + " - instance not found");
            throw new NoSuchElementException("Specified instance of service could not be found");
        }
    }

    /**
     * Deregister a microservice instance
     * 
     * If the microservice is not found, a {@link NoSuchElementException} is
     * thrown.
     *
     * @param serviceId  The serviceId of the microservice to deregister
     * @param instanceId The instanceId of the microservice to deregister
     */
    public void deregisterService(String serviceId, String instanceId) {
        List<Microservice> instances = registry.get(serviceId);
        if (instances == null) {
            throw new NoSuchElementException("The given serviceId does not exist in the registry");
        }

        Microservice entry = instances.stream()
                .filter(e -> e.getInstanceId().equals(instanceId))
                .findFirst()
                .orElse(null);

        if (entry == null) {
            throw new NoSuchElementException("The given instanceId does not exist in the registry");
        }

        instances.remove(entry);

        // Publish the deregister event
        clientEventService.publishEvent(new MicroserviceEvent(entry, MicroserviceEvent.Cause.DEREGISTER));

        // If no instances are left, remove the service from the registry
        if (instances.isEmpty()) {
            registry.remove(serviceId);
        }
    }

    /**
     * Get all registered services/microservices and their instances for the given
     * realm
     * 
     * @return An array of all registered microservices and their instances
     */
    public Microservice[] getServices(String realm) {
        return registry.values().stream()
                .flatMap(List::stream)
                .filter(entry -> entry.getRealm().equals(realm))
                .toArray(Microservice[]::new);
    }

    /**
     * Get all globally registered services/microservices and their instances
     * 
     * @return An array of all registered microservices and their instances
     */
    public Microservice[] getGlobalServices() {
        return registry.values().stream()
                .flatMap(List::stream)
                .filter(entry -> entry.getRealm().equals(MASTER_REALM) && entry.getIsGlobal())
                .toArray(Microservice[]::new);
    }

    /**
     * Get a specific microservice registration
     * 
     * @param serviceId  The serviceId of the microservice to get
     * @param instanceId The instanceId of the microservice to get
     * @return The microservice
     */
    public Microservice getService(String serviceId, String instanceId) {
        return registry.values().stream()
                .flatMap(List::stream)
                .filter(entry -> entry.getServiceId().equals(serviceId) && entry.getInstanceId().equals(instanceId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Specified instance of service could not be found: "
                        + serviceId + ", instanceId: " + instanceId));
    }

    /**
     * Check for expired registrations and mark them as unavailable if the
     * instance lease has expired.
     */
    protected void markExpiredInstancesAsUnavailable() {
        long currentTime = timerService.getCurrentTimeMillis();
        LOG.fine("Marking expired microservice instances as unavailable");

        registry.values().stream()
                .flatMap(List::stream)
                .filter(entry -> entry.getLeaseInfo().isExpired(currentTime)
                        && entry.getStatus() == MicroserviceStatus.AVAILABLE)
                .forEach(entry -> {
                    entry.setStatus(MicroserviceStatus.UNAVAILABLE);

                    // Publish the update event as the microservice has had its status updated
                    clientEventService.publishEvent(new MicroserviceEvent(entry, MicroserviceEvent.Cause.UPDATE));
                    LOG.fine("Marked microservice as unavailable: " + entry.getServiceId() + ", instanceId: "
                            + entry.getInstanceId());
                });
    }

    protected void deregisterLongExpiredInstances() {
        LOG.info("Deregistering long expired microservice registrations");

        long currentTime = timerService.getCurrentTimeMillis();
        long purgeThreshold = currentTime - DEFAULT_DEREGISTER_UNAVAILABLE_MS;

        // Collect all services that are unavailable and have an expiration timestamp
        // before the purge threshold
        List<Microservice> toRemove = registry.values().stream()
                .flatMap(List::stream)
                .filter(entry -> entry.getStatus() == MicroserviceStatus.UNAVAILABLE
                        && entry.getLeaseInfo().getExpirationTimestamp() < purgeThreshold)
                .toList();

        // Deregister long expired microservices0
        toRemove.forEach(entry -> deregisterService(entry.getServiceId(), entry.getInstanceId()));
        LOG.info("Deregistered " + toRemove.size() + " long expired microservice registrations");
    }

}
