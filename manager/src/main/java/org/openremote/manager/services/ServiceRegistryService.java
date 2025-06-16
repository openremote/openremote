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
package org.openremote.manager.services;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.services.ServiceDescriptor;
import org.openremote.model.services.ServiceStatus;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalNotification;
import com.google.common.cache.RemovalListener;

/**
 * {@link ServiceRegistryService} is responsible for registering and
 * managing service registrations. It is used to store service registrations in
 * memory and to provide a way to get the registered services.
 * 
 * <li>The {@link ServiceRegistryService} will store the service registration in
 * memory, and will set the status to available.</li>
 * 
 * <li>The {@link ServiceRegistryService} will also handle the expiration of
 * service registrations, and set the status to unavailable. Registrations
 * should be updated within the TTL of the registration cache.</li>
 * 
 */
public class ServiceRegistryService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(ServiceRegistryService.class.getName());

    protected Cache<String, ServiceDescriptor> inMemoryServiceRegistry;

    /**
     * Removal listener -- Handle expired service registrations, set status to
     * unavailable and update the service registration in the cache
     */
    private final RemovalListener<String, ServiceDescriptor> cacheRemovalListener = (
            RemovalNotification<String, ServiceDescriptor> notification) -> {
        if (notification.getCause() == RemovalCause.EXPIRED) {
            LOG.info("Service registration expired: " + notification.getKey());

            // Get the key and service registration from the notification
            String key = notification.getKey();
            ServiceDescriptor serviceDescriptor = notification.getValue();

            // Set status to unavailable
            serviceDescriptor.setStatus(ServiceStatus.UNAVAILABLE);

            // Update the service registration in the cache
            inMemoryServiceRegistry.put(key, serviceDescriptor);
        }
    };

    @Override
    public void init(Container container) throws Exception {
    }

    @Override
    public void start(Container container) throws Exception {
        // Create the in memory service registry
        inMemoryServiceRegistry = CacheBuilder.newBuilder()
                .expireAfterWrite(60000, TimeUnit.MILLISECONDS)
                .removalListener(cacheRemovalListener)
                .build();

        LOG.info("Service registry service started");
    }

    @Override
    public void stop(Container container) throws Exception {
        // Clear the in memory service registry when the container stops
        inMemoryServiceRegistry.invalidateAll();

        LOG.info("Service registry service stopped");
    }

    private String getRegistrationKey(String serviceName, String clientAddress) {
        return serviceName + ":" + clientAddress;
    }

    public boolean registerService(String clientRemoteAddress, ServiceDescriptor serviceDescriptor) {
        // Update the service registration with the client remote address
        serviceDescriptor.setClientRemoteAddress(clientRemoteAddress);

        // Use a composite key of service name and client address
        String registrationKey = getRegistrationKey(serviceDescriptor.getName(), clientRemoteAddress);

        // Add or update the registration in the cache
        inMemoryServiceRegistry.put(registrationKey, serviceDescriptor);

        return true;
    }

    public ServiceDescriptor[] getRegisteredServices() {
        // Get all registrations
        return inMemoryServiceRegistry.asMap().values().toArray(new ServiceDescriptor[0]);
    }

}
