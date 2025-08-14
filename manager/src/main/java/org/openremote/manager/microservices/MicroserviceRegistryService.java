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
import org.openremote.model.event.shared.EventSubscription;
import org.openremote.model.microservices.Microservice;
import org.openremote.model.microservices.MicroserviceEvent;
import org.openremote.model.microservices.MicroserviceStatus;
import org.openremote.model.security.ClientRole;
import org.openremote.model.util.UniqueIdentifierGenerator;
import org.openremote.model.microservices.MicroserviceLeaseInfo;

/**
 * Service discovery and registration for microservices and external services.
 *
 * <p>
 * Provides centralized registry functionality including registration
 * management,
 * heartbeat management, and status tracking. Services are marked as unavailable
 * when their lease expires.
 * </p>
 *
 * <h3>Registry Structure</h3>
 * <p>
 * The registry supports multiple instances per service ID, allowing for
 * horizontal
 * scaling and high availability. Each service instance is uniquely identified
 * by
 * the combination of {@code serviceId} and {@code instanceId}.
 * </p>
 *
 * <h3>Service Types</h3>
 * <ul>
 * <li><strong>Global:</strong> Available to all realms, these services are
 * flagged as global (isGlobal=true) and are intended for multi-tenant use
 * cases/scenarios</li>
 * <li><strong>Realm-bound:</strong> Available to a singular specific realm,
 * these services are flagged as realm-bound (isGlobal=false) and are intended
 * for
 * single-tenant use cases/scenarios</li>
 * </ul>
 *
 * <h3>Notes</h3>
 * <ul>
 * <li>Multiple instances of the same service can be registered for load
 * balancing and redundancy</li>
 * <li>Global and realm-bound services can coexist with different service
 * IDs</li>
 * <li>Lease-based registration with automatic expiration (default:
 * {@link #DEFAULT_LEASE_DURATION_MS})</li>
 * <li>Heartbeat mechanism for lease renewal</li>
 * <li>Deregister unavailable instances after
 * {@link #DEFAULT_DEREGISTER_UNAVAILABLE_MS}</li>
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

    // serviceId: <instanceId: Microservice>
    protected ConcurrentHashMap<String, ConcurrentHashMap<String, Microservice>> registry;

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

        this.registry = new ConcurrentHashMap<>();

        // Register the microservice REST resource
        container.getService(ManagerWebService.class).addApiSingleton(
                new MicroserviceResourceImpl(timerService, identityService, this));

        addSubscriptionAuthorizer();

    }

    protected void addSubscriptionAuthorizer() {
        clientEventService.addSubscriptionAuthorizer((realm, authContext, eventSubscription) -> {
            if (!eventSubscription.isEventType(MicroserviceEvent.class) || authContext == null) {
                return false;
            }

            if (authContext.isSuperUser()) {
                return true;
            }

            @SuppressWarnings("unchecked")
            EventSubscription<MicroserviceEvent> subscription = (EventSubscription<MicroserviceEvent>) eventSubscription;
            subscription.setFilter(event -> {
                Microservice eventMicroservice = event.getMicroservice();
                boolean isGlobalService = eventMicroservice.getIsGlobal();
                boolean realmMatches = eventMicroservice.getRealm().equals(authContext.getAuthenticatedRealmName());

                // Filter events to only contain events for the authenticated realm or for
                // global services
                return isGlobalService || realmMatches ? event : null;
            });

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
    }

    @Override
    public void stop(Container container) throws Exception {
        // cancel any scheduled tasks
        if (markExpiredInstancesAsUnavailableFuture != null) {
            markExpiredInstancesAsUnavailableFuture.cancel(true);
        }
        if (deregisterExpiredUnavailableInstancesFuture != null) {
            deregisterExpiredUnavailableInstancesFuture.cancel(true);
        }

        // clear the registry
        registry.clear();
    }

    /**
     * Register a realm-bound microservice instance
     *
     * @param microservice The microservice to register
     */
    public Microservice registerService(Microservice microservice) {
        return registerService(microservice, false);
    }

    /**
     * Register a microservice instance
     *
     * @param microservice The microservice to register
     * @param isGlobal     Whether the service should be available to all realms.
     *                     This should only be used for services that use a super
     *                     admin
     *                     service user with global access.
     */
    public Microservice registerService(Microservice microservice, boolean isGlobal) {
        LOG.info("Registering microservice: " + microservice.getServiceId() + ", instanceId: "
                + microservice.getInstanceId());

        // Generate a instanceId if not provided
        if (microservice.getInstanceId() == null) {
            String instanceId = UniqueIdentifierGenerator.generateId();
            microservice.setInstanceId(instanceId);
        }

        // Check if the given microservice is already registered
        Microservice existingEntry = getService(microservice.getServiceId(), microservice.getInstanceId());

        if (existingEntry != null) {
            LOG.warning("Microservice already registered: " + microservice.getServiceId()
                    + ", instanceId: " + microservice.getInstanceId());
            throw new IllegalStateException("Microservice already registered");
        }

        // Set the lease info for the new instance
        long now = timerService.getCurrentTimeMillis();
        long expirationTimestamp = now + DEFAULT_LEASE_DURATION_MS;

        microservice.setLeaseInfo(new MicroserviceLeaseInfo(expirationTimestamp,
                now, now));

        // Set the global visibility state
        microservice.setIsGlobal(isGlobal);

        // Add the instance to the registry
        registry.computeIfAbsent(microservice.getServiceId(), k -> new ConcurrentHashMap<>())
                .put(microservice.getInstanceId(), microservice);

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
        Microservice entry = getService(serviceId, instanceId);

        if (entry != null) {

            long now = timerService.getCurrentTimeMillis();
            long renewalTimestamp = now;
            long expirationTimestamp = now + DEFAULT_LEASE_DURATION_MS;

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
            throw new NoSuchElementException(
                    "The given serviceId and instanceId combination does not exist in the registry");
        }
    }

    /**
     * Deregister a microservice instance.
     * 
     * If the microservice is not found, a {@link NoSuchElementException} is
     * thrown.
     *
     * @param serviceId  The serviceId of the microservice to deregister
     * @param instanceId The instanceId of the microservice to deregister
     */
    public void deregisterService(String serviceId, String instanceId) {
        LOG.info("De-registering microservice: " + serviceId + ", instanceId: " + instanceId);

        ConcurrentHashMap<String, Microservice> instanceMap = registry.get(serviceId);
        if (instanceMap == null || !instanceMap.containsKey(instanceId)) {
            throw new NoSuchElementException(
                    "The given serviceId and instanceId combination does not exist in the registry");
        }

        Microservice entry = instanceMap.remove(instanceId);

        // Publish the deregister event
        clientEventService.publishEvent(new MicroserviceEvent(entry, MicroserviceEvent.Cause.DEREGISTER));

        // Attempt to remove the outer map only if it's still empty
        registry.computeIfPresent(serviceId, (key, map) -> {
            if (map.isEmpty()) {
                LOG.info("No instances left for service: " + serviceId + ", removed from registry");
                return null;
            }
            return map;
        });

        LOG.info("Successfully de-registered microservice: " + serviceId + ", instanceId: " + instanceId);
    }

    /**
     * Get all registered services and microservices and their instances for the
     * given
     * realm.
     * 
     * @param realm The realm to filter services by
     * @return An array of all registered microservices and their instances
     */
    public Microservice[] getServices(String realm) {
        return registry.values().stream()
                .flatMap(instanceMap -> instanceMap.values().stream())
                .filter(entry -> entry.getRealm().equals(realm))
                .toArray(Microservice[]::new);
    }

    /**
     * Get all globally registered services and microservices and their instances.
     * A service is considered global if it is registered with the master realm and
     * is has the isGlobal flag set to true.
     * 
     * @return An array of all globally registered microservices and their instances
     */
    public Microservice[] getGlobalServices() {
        return registry.values().stream()
                .flatMap(instanceMap -> instanceMap.values().stream())
                .filter(entry -> entry.getRealm().equals(MASTER_REALM) && entry.getIsGlobal())
                .toArray(Microservice[]::new);
    }

    /**
     * Get a specific microservice registration.
     * 
     * @param serviceId  The serviceId of the microservice to get
     * @param instanceId The instanceId of the microservice to get
     * @return The microservice
     */
    public Microservice getService(String serviceId, String instanceId) {
        ConcurrentHashMap<String, Microservice> instanceMap = registry.get(serviceId);
        if (instanceMap != null) {
            return instanceMap.get(instanceId);
        }
        return null;
    }

    /**
     * Check for expired registrations and mark them as unavailable if the
     * instance lease has not been renewed within the configured
     * {@link #DEFAULT_LEASE_DURATION_MS} lease duration.
     */
    protected void markExpiredInstancesAsUnavailable() {
        long now = timerService.getCurrentTimeMillis();
        LOG.fine("Marking expired microservice instances as unavailable");

        registry.values().stream()
                .flatMap(instanceMap -> instanceMap.values().stream())
                .filter(entry -> entry.getLeaseInfo().isExpired(now)
                        && entry.getStatus() == MicroserviceStatus.AVAILABLE)
                .forEach(entry -> {
                    entry.setStatus(MicroserviceStatus.UNAVAILABLE);

                    // Publish the update event as the microservice has had its status updated
                    clientEventService.publishEvent(new MicroserviceEvent(entry, MicroserviceEvent.Cause.UPDATE));
                    LOG.fine("Marked microservice as unavailable: " + entry.getServiceId() + ", instanceId: "
                            + entry.getInstanceId());
                });
    }

    /**
     * Deregister long expired microservices. This is used to remove microservice
     * instances that have been unavailable for longer than the configured
     * {@link #DEFAULT_DEREGISTER_UNAVAILABLE_MS} threshold.
     */
    protected void deregisterLongExpiredInstances() {
        LOG.info("Deregistering long expired microservice registrations");

        long now = timerService.getCurrentTimeMillis();
        long purgeThreshold = now - DEFAULT_DEREGISTER_UNAVAILABLE_MS;

        List<Microservice> toDeregister = registry.values().stream()
                .flatMap(instanceMap -> instanceMap.values().stream())
                .filter(entry -> entry.getStatus() == MicroserviceStatus.UNAVAILABLE
                        && entry.getLeaseInfo().getExpirationTimestamp() < purgeThreshold)
                .toList();

        // Deregister long expired microservices
        toDeregister.forEach(entry -> {
            try {
                deregisterService(entry.getServiceId(), entry.getInstanceId());
            } catch (NoSuchElementException e) {
                LOG.warning("Service was already deregistered: " + entry.getServiceId() + ", instanceId: "
                        + entry.getInstanceId() + " - " + e.getMessage());
            }
        });
    }

}
