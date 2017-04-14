/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.manager.server.setup;

import org.openremote.agent3.protocol.Protocol;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.model.asset.AttributeWrapperFilter;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This is for resolving objects that are loaded at startup
 * e.g. protocols and attribute wrappers. It uses the standard
 * Java {@link ServiceLoader} to load the list of types defined
 * here.
 */
public class RuntimeResolverService implements ContainerService {
    private static final Logger LOG = Logger.getLogger(RuntimeResolverService.class.getName());
    protected static final List<Class> types;
    protected static final Map<Class, Object> instances = new HashMap<>();

    static {
        types = Arrays.asList(
            Protocol.class,
            AttributeWrapperFilter.class
        );
    }

    @Override
    public void init(Container container) throws Exception {
        // Load each type
        for (Class type : types) {
            ServiceLoader
                .load(type)
                .forEach(instance -> {
                    instances.put(instance.getClass(), instance);
                });
        }

        for (Object instance : instances.values()) {
            // if it is a Container Service initialise it and pass it to the
            // container; the container will then call start
            if (instance instanceof ContainerService) {
                ContainerService service = (ContainerService)instance;
                container.addService(service);
                LOG.fine("Initializing service: " + service);
                service.init(container);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void start(Container container) throws Exception {
    }

    @Override
    public void allStarted(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {
    }

    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> clazz) {
        synchronized (instances) {
            return (T)instances.get(clazz);
        }
    }

    public <T> List<T> resolveAll(Class<T> clazz) {
        synchronized (instances) {
            return instances
                .values()
                .stream()
                .filter(instance -> clazz.isAssignableFrom(instance.getClass()))
                .map(instance -> (T)instance)
                .collect(Collectors.toList());

        }
    }
}
