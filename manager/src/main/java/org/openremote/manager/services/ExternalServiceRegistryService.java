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
package org.openremote.manager.services;

import static org.openremote.model.Constants.MASTER_REALM;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.openremote.container.timer.TimerService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.event.shared.EventSubscription;
import org.openremote.model.services.ExternalService;
import org.openremote.model.services.ExternalServiceEvent;
import org.openremote.model.services.ExternalServiceStatus;


import org.openremote.model.security.ClientRole;
import org.openremote.model.services.ExternalServiceLeaseInfo;

/**
 * Manages {@link org.openremote.model.services.ExternalService} discovery and
 * registration with lease-based lifecycle management; supports both
 * global (multi-tenant) and realm-bound services, allowing multiple instances
 * per service ID for
 * horizontal scaling and high availability.
 * <p>
 * The service automatically manages external service health through scheduled tasks
 * that mark expired
 * instances as unavailable and eventually deregister long-expired instances. It
 * registers a REST resource
 * ({@link ExternalServiceResourceImpl}) for external access to the registry and
 * adds event subscription
 * authorization to the {@link ClientEventService} to filter external service events
 * based on realm and user
 * roles, publishing external service lifecycle events for external consumption.
 */
public class ExternalServiceRegistryService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(ExternalServiceRegistryService.class.getName());

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

    // serviceId: <instanceId: ExternalService>
    protected ConcurrentHashMap<String, HashMap<Integer, ExternalService>> registry;

    // Track next available instanceId for each serviceId
    protected ConcurrentHashMap<String, AtomicInteger> serviceIdCounters;

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
        this.serviceIdCounters = new ConcurrentHashMap<>();

        // Register the external service REST resource
        container.getService(ManagerWebService.class).addApiSingleton(
                new ExternalServiceResourceImpl(timerService, identityService, this));

        addExternalServiceEventSubscriptionAuthorizer();
    }

    protected void addExternalServiceEventSubscriptionAuthorizer() {
        clientEventService.addSubscriptionAuthorizer((realm, authContext, eventSubscription) -> {
            if (!eventSubscription.isEventType(ExternalServiceEvent.class) || authContext == null) {
                return false;
            }

            if (authContext.isSuperUser()) {
                return true;
            }

            @SuppressWarnings("unchecked")
            EventSubscription<ExternalServiceEvent> subscription = (EventSubscription<ExternalServiceEvent>) eventSubscription;
            subscription.setFilter(event -> {
                ExternalService eventExternalService = event.getService();
                boolean isGlobalService = eventExternalService.getIsGlobal();
                boolean realmMatches = Objects.equals(eventExternalService.getRealm(),
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
                this::markExpiredInstancesAsUnavailable, MARK_EXPIRED_INSTANCES_INTERVAL_MS,
                MARK_EXPIRED_INSTANCES_INTERVAL_MS, TimeUnit.MILLISECONDS);

        deregisterExpiredUnavailableInstancesFuture = scheduledExecutorService.scheduleAtFixedRate(
                this::deregisterLongExpiredInstances, DEREGISTER_UNAVAILABLE_INTERVAL_MS,
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

        // clear the registry and counters
        registry.clear();
        serviceIdCounters.clear();
    }


    /**
     * Register an external service instance
     *
     * @param registrarUserId The userId of the user who registered the service
     * @param externalService The external service to register
     */
    public void registerService(String registrarUserId, ExternalService externalService) {

        // Set the instanceId based on the incremental counter for this serviceId
        int nextInstanceId = serviceIdCounters.computeIfAbsent(externalService.getServiceId(),
                k -> new AtomicInteger(0)).incrementAndGet();
        externalService.setInstanceId(nextInstanceId);

        LOG.info("Registering external service: " + externalService.getServiceId() + ", instanceId: "
                + externalService.getInstanceId() + ", isGlobal: " + externalService.getIsGlobal() + ", registrarUserId: " + registrarUserId);

        long now = timerService.getCurrentTimeMillis();
        externalService.setLeaseInfo(new ExternalServiceLeaseInfo(registrarUserId, now + DEFAULT_LEASE_DURATION_MS, now, now));

        // merge the external service into the registry if it doesn't already exist
        registry.merge(
                externalService.getServiceId(),
                new HashMap<>(Map.of(externalService.getInstanceId(), externalService)),
                (existingMap, newMap) -> {
                    ExternalService existing = existingMap.putIfAbsent(externalService.getInstanceId(), externalService);
                    if (existing != null) {
                        throw new IllegalStateException("ExternalService already registered: "
                                + externalService.getServiceId() + ", instanceId: " + externalService.getInstanceId());
                    }
                    return existingMap;
                });

        // publish the register event as a new external service has been added to the
        // registry
        clientEventService.publishEvent(new ExternalServiceEvent(externalService, ExternalServiceEvent.Cause.REGISTER));

        LOG.info("Successfully registered external service: " + externalService.getServiceId() + ", instanceId: "
                + externalService.getInstanceId() + ", isGlobal: " + externalService.getIsGlobal() + ", registrarUserId: " + registrarUserId);
    }

    /**
     * Update the active registration lease info for the specified external service.
     * This is used to indicate that the external service is still running and
     * available.
     * If the external service is not found, a {@link NoSuchElementException} is
     * thrown.
     *
     * @param serviceId  The serviceId of the external service to send the heartbeat to
     * @param instanceId The instanceId of the external service to send the heartbeat to
     */
    public void heartbeat(String serviceId, int instanceId) {
        Map<Integer, ExternalService> instanceMap = registry.get(serviceId);
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
            entry.setStatus(ExternalServiceStatus.AVAILABLE);

            // publish the update event as the lease has been refreshed
            clientEventService.publishEvent(new ExternalServiceEvent(entry, ExternalServiceEvent.Cause.UPDATE));

            LOG.fine(
                    "Successfully refreshed lease info for external service: " + serviceId + ", instanceId: " + instanceId);
            return entry;
        });
    }

    /**
     * Deregister an external service instance.
     * <p>
     * If the external service is not found, a {@link NoSuchElementException} is
     * thrown.
     *
     * @param serviceId  The serviceId of the external service to deregister
     * @param instanceId The instanceId of the external service to deregister
     */
    public void deregisterService(String serviceId, int instanceId) {
        registry.compute(serviceId, (unused, instanceMap) -> {
            if (instanceMap == null) {
                throw new NoSuchElementException(
                        "The given serviceId does not exist in the registry");
            }

            ExternalService removed = instanceMap.remove(instanceId);
            if (removed == null) {
                throw new NoSuchElementException(
                        "The given serviceId and instanceId combination does not exist in the registry");
            }

            // Publish deregister event
            clientEventService.publishEvent(
                    new ExternalServiceEvent(removed, ExternalServiceEvent.Cause.DEREGISTER));

            LOG.info("Successfully de-registered external service: " + serviceId + ", instanceId: " + instanceId);

            // If the map is empty after removal, we can just return null to remove the
            // serviceId entry
            if (instanceMap.isEmpty()) {
                LOG.info("No instances left for service: " + serviceId + ", removed from registry");
                serviceIdCounters.remove(serviceId);
                return null;
            }

            return instanceMap;
        });
    }

    /**
     * Get all registered services and external services and their instances for the
     * given
     * realm.
     *
     * @param realm The realm to filter services by
     * @return An array of all registered external services and their instances
     */
    public ExternalService[] getServices(String realm) {
        return registry.entrySet().stream()
                .flatMap(serviceEntry -> serviceEntry.getValue().values().stream())
                .filter(entry -> entry.getRealm().equals(realm))
                .toArray(ExternalService[]::new);
    }

    /**
     * Get all globally registered services and external services and their instances.
     * A service is considered global if it is registered with the master realm and,
     * it has the isGlobal flag set to true.
     *
     * @return An array of all globally registered external services and their instances
     */
    public ExternalService[] getGlobalServices() {
        return registry.entrySet().stream()
                .flatMap(serviceEntry -> serviceEntry.getValue().values().stream())
                .filter(entry -> entry.getRealm().equals(MASTER_REALM) && entry.getIsGlobal())
                .toArray(ExternalService[]::new);
    }

    /**
     * Get a specific external service registration.
     *
     * @param serviceId  The serviceId of the external service to get
     * @param instanceId The instanceId of the external service to get
     * @return The external service
     */
    public ExternalService getService(String serviceId, int instanceId) {
        return Optional.ofNullable(registry.get(serviceId))
                .map(instanceMap -> instanceMap.get(instanceId))
                .orElse(null);
    }

    /**
     * Check for expired registrations and mark them as unavailable if the
     * instance lease has not been renewed within the configured
     * {@link #DEFAULT_LEASE_DURATION_MS} lease duration.
     */
    protected void markExpiredInstancesAsUnavailable() {
        long now = timerService.getCurrentTimeMillis();
        LOG.fine("Marking expired external service instances as unavailable");

        registry.entrySet().stream()
                .flatMap(serviceEntry -> serviceEntry.getValue().entrySet().stream())
                .map(Map.Entry::getValue)
                // filter by lease expired and current status is still available
                .filter(ms -> ms.getLeaseInfo().isExpired(now)
                        && ms.getStatus() == ExternalServiceStatus.AVAILABLE)
                .forEach(ms -> {
                    ms.setStatus(ExternalServiceStatus.UNAVAILABLE);
                    clientEventService.publishEvent(new ExternalServiceEvent(ms, ExternalServiceEvent.Cause.UPDATE));
                    LOG.fine("Marked external service as unavailable: "
                            + ms.getServiceId() + ", instanceId: " + ms.getInstanceId());
                });
    }

    /**
     * Deregister long expired external services. This is used to remove external service
     * instances that have been unavailable for longer than the configured
     * {@link #DEFAULT_DEREGISTER_UNAVAILABLE_MS} threshold.
     */
    protected void deregisterLongExpiredInstances() {
        LOG.info("De-registering long expired external service registrations");

        long now = timerService.getCurrentTimeMillis();
        long deregisterThreshold = now - DEFAULT_DEREGISTER_UNAVAILABLE_MS;

        // Collect services to deregister first
        var servicesToDeregister = registry.entrySet().stream()
                .flatMap(serviceEntry -> serviceEntry.getValue().entrySet().stream())
                .map(Map.Entry::getValue)
                // filter by unavailable + meets threshold
                .filter(ms -> ms.getStatus() == ExternalServiceStatus.UNAVAILABLE
                        && ms.getLeaseInfo().isExpired(deregisterThreshold))
                .toList();

        // Deregister the collected services
        servicesToDeregister.forEach(ms -> {
            try {
                deregisterService(ms.getServiceId(), ms.getInstanceId());
            } catch (NoSuchElementException e) {
                LOG.warning("Service was already deregistered: " + ms.getServiceId()
                        + ", instanceId: " + ms.getInstanceId() + " - " + e.getMessage());
            }
        });
    }

}
