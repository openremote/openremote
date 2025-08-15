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

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
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
 * Manages {@link org.openremote.model.microservices.Microservice} discovery and
 * registration with lease-based lifecycle management; supports both
 * global (multi-tenant) and realm-bound services, allowing multiple instances
 * per service ID for
 * horizontal scaling and high availability.
 * <p>
 * The service automatically manages microservice health through scheduled tasks
 * that mark expired
 * instances as unavailable and eventually deregister long-expired instances. It
 * registers a REST resource
 * ({@link MicroserviceResourceImpl}) for external access to the registry and
 * adds event subscription
 * authorization to the {@link ClientEventService} to filter microservice events
 * based on realm and user
 * roles, publishing microservice lifecycle events for external consumption.
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
    // Nested ConcurrentHashMaps for segmenting, segment locks and fast lookups
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

        addMicroserviceEventSubscriptionAuthorizer();

    }

    protected void addMicroserviceEventSubscriptionAuthorizer() {
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
                boolean realmMatches = Objects.equals(eventMicroservice.getRealm(),
                        authContext.getAuthenticatedRealmName());

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

        if (microservice.getInstanceId() == null) {
            microservice.setInstanceId(UniqueIdentifierGenerator.generateId());
        }

        long now = timerService.getCurrentTimeMillis();
        microservice.setLeaseInfo(new MicroserviceLeaseInfo(now + DEFAULT_LEASE_DURATION_MS, now, now));
        microservice.setIsGlobal(isGlobal);

        // merge the microservice into the registry if it doesn't already exist
        registry.merge(
                microservice.getServiceId(),
                new ConcurrentHashMap<>(Map.of(microservice.getInstanceId(), microservice)),
                (existingMap, newMap) -> {
                    Microservice existing = existingMap.putIfAbsent(microservice.getInstanceId(), microservice);
                    if (existing != null) {
                        throw new IllegalStateException("Microservice already registered: "
                                + microservice.getServiceId() + ", instanceId: " + microservice.getInstanceId());
                    }
                    return existingMap;
                });

        // publish the register event as a new microservice has been added to the
        // registry
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
        ConcurrentHashMap<String, Microservice> instanceMap = registry.get(serviceId);
        if (instanceMap == null) {
            throw new NoSuchElementException("Service not found: " + serviceId);
        }

        instanceMap.compute(instanceId, (id, entry) -> {
            if (entry == null) {
                throw new NoSuchElementException("Instance not found: " + serviceId + ", instanceId: " + instanceId);
            }
            // update the lease info and set the instance status to available
            long now = timerService.getCurrentTimeMillis();
            entry.getLeaseInfo().setRenewalTimestamp(now);
            entry.getLeaseInfo().setExpirationTimestamp(now + DEFAULT_LEASE_DURATION_MS);
            entry.setStatus(MicroserviceStatus.AVAILABLE);

            // publish the update event as the lease has been refreshed
            clientEventService.publishEvent(new MicroserviceEvent(entry, MicroserviceEvent.Cause.UPDATE));

            LOG.fine(
                    "Successfully refreshed lease info for microservice: " + serviceId + ", instanceId: " + instanceId);
            return entry;
        });
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
        registry.compute(serviceId, (unused, instanceMap) -> {
            if (instanceMap == null) {
                throw new NoSuchElementException(
                        "The given serviceId does not exist in the registry");
            }

            Microservice removed = instanceMap.remove(instanceId);
            if (removed == null) {
                throw new NoSuchElementException(
                        "The given serviceId and instanceId combination does not exist in the registry");
            }

            // Publish deregister event
            clientEventService.publishEvent(
                    new MicroserviceEvent(removed, MicroserviceEvent.Cause.DEREGISTER));

            LOG.info("Successfully de-registered microservice: " + serviceId + ", instanceId: " + instanceId);

            // If the map is empty after removal, we can just return null to remove the
            // serviceId entry
            if (instanceMap.isEmpty()) {
                LOG.info("No instances left for service: " + serviceId + ", removed from registry");
                return null;
            }

            return instanceMap;
        });
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
     * A service is considered global if it is registered with the master realm and,
     * it has the isGlobal flag set to true.
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
        return registry.values().stream()
                .flatMap(instanceMap -> instanceMap.values().stream())
                .filter(entry -> entry.getServiceId().equals(serviceId) && entry.getInstanceId().equals(instanceId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Check for expired registrations and mark them as unavailable if the
     * instance lease has not been renewed within the configured
     * {@link #DEFAULT_LEASE_DURATION_MS} lease duration.
     */
    protected void markExpiredInstancesAsUnavailable() {
        long now = timerService.getCurrentTimeMillis();
        LOG.fine("Marking expired microservice instances as unavailable");

        registry.entrySet().stream()
                .flatMap(serviceEntry -> serviceEntry.getValue().entrySet().stream())
                .map(Map.Entry::getValue)
                // filter by lease expired and current status is still available
                .filter(ms -> ms.getLeaseInfo().isExpired(now)
                        && ms.getStatus() == MicroserviceStatus.AVAILABLE)
                .forEach(ms -> {
                    ms.setStatus(MicroserviceStatus.UNAVAILABLE);
                    clientEventService.publishEvent(new MicroserviceEvent(ms, MicroserviceEvent.Cause.UPDATE));
                    LOG.fine("Marked microservice as unavailable: "
                            + ms.getServiceId() + ", instanceId: " + ms.getInstanceId());
                });
    }

    /**
     * Deregister long expired microservices. This is used to remove microservice
     * instances that have been unavailable for longer than the configured
     * {@link #DEFAULT_DEREGISTER_UNAVAILABLE_MS} threshold.
     */
    protected void deregisterLongExpiredInstances() {
        LOG.info("De-registering long expired microservice registrations");

        long now = timerService.getCurrentTimeMillis();
        long deregisterThreshold = now - DEFAULT_DEREGISTER_UNAVAILABLE_MS;

        registry.entrySet().stream()
                .flatMap(serviceEntry -> serviceEntry.getValue().entrySet().stream())
                .map(Map.Entry::getValue)
                // filter by unavailable + meets threshold
                .filter(ms -> ms.getStatus() == MicroserviceStatus.UNAVAILABLE
                        && ms.getLeaseInfo().getExpirationTimestamp() < deregisterThreshold)
                .forEach(ms -> {
                    try {
                        deregisterService(ms.getServiceId(), ms.getInstanceId());
                    } catch (NoSuchElementException e) {
                        LOG.warning("Service was already deregistered: " + ms.getServiceId()
                                + ", instanceId: " + ms.getInstanceId() + " - " + e.getMessage());
                    }
                });
    }

}
