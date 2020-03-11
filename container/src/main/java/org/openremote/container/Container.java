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
package org.openremote.container;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.openremote.container.concurrent.ContainerThreads;
import org.openremote.model.ModelModule;
import org.openremote.container.util.LogUtil;

import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.stream.StreamSupport.stream;
import static org.openremote.container.util.MapAccess.getBoolean;

/**
 * A thread-safe registry of {@link ContainerService}s.
 * <p>
 * Create the container with {@link ContainerService}s, then let it
 * manage the life cycle of these services.
 * <p>
 * Access environment configuration through {@link #getConfig()} and the helper methods
 * in {@link org.openremote.container.util.MapAccess}. Consider using {@link #DEV_MODE}
 * to distinguish between development and production environments.
 * <p>
 * Read and write JSON with a sensible mapper configuration using {@link #JSON}.
 */
public class Container {

    public static final Logger LOG;

    static {
        LogUtil.configureLogging("logging.properties");
        LOG = Logger.getLogger(Container.class.getName());
    }

    public static final String DEV_MODE = "DEV_MODE";
    public static final boolean DEV_MODE_DEFAULT = true;

    @SuppressWarnings("deprecation")
    public static final ObjectMapper JSON = new ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false)
        .configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false)
        .configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY)
        .registerModule(new ModelModule())
        .registerModule(new Jdk8Module())
        .registerModule(new JavaTimeModule())
        .registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));

    protected final Map<String, String> config = new HashMap<>();
    protected final boolean devMode;

    protected Thread waitingThread;
    protected final Map<Class<? extends ContainerService>, ContainerService> services = new LinkedHashMap<>();

    /**
     * Discover {@link ContainerService}s using {@link ServiceLoader}; services are then ordered by
     * {@link ContainerService#getPriority}.
     */
    public Container() {
        this(stream(ServiceLoader.load(ContainerService.class).spliterator(), false)
                .sorted(Comparator.comparingInt(ContainerService::getPriority))
                .collect(Collectors.toList()));
    }

    public Container(ContainerService... services) {
        this(Arrays.asList(services));
    }

    public Container(Iterable<ContainerService> services) {
        this(System.getenv(), services);
    }

    public Container(Map<String, String> config, Iterable<ContainerService> services) {
        for (Map.Entry<String, String> entry : config.entrySet()) {
            this.config.put(entry.getKey(), entry.getValue());
        }

        this.devMode = getBoolean(this.config, DEV_MODE, DEV_MODE_DEFAULT);

        if (this.devMode) {
            JSON.enable(SerializationFeature.INDENT_OUTPUT);
        }

        // Any log handlers of the root logger that are container services must be registered
        for (Handler handler : Logger.getLogger("").getHandlers()) {
            if (handler instanceof ContainerService) {
                ContainerService containerServiceLogHandler = (ContainerService) handler;
                this.services.put(containerServiceLogHandler.getClass(), containerServiceLogHandler);
            }
        }

        if (services != null) {
            services.forEach(svc -> this.services.put(svc.getClass(), svc));
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public boolean isDevMode() {
        return devMode;
    }

    public boolean isRunning() {
        return waitingThread != null;
    }

    public void start() throws Exception {
        synchronized (services) {
            if (isRunning())
                return;
            LOG.info(">>> Starting runtime container...");
            try {
                for (ContainerService service : getServices()) {
                    LOG.fine("Initializing service: " + service);
                    service.init(Container.this);
                }
                for (ContainerService service : getServices()) {
                    LOG.fine("Starting service: " + service);
                    service.start(Container.this);
                }
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, ">>> Runtime container startup failed", ex);
                throw ex;
            }
            LOG.info(">>> Runtime container startup complete");
        }
    }

    public void stop() {
        synchronized (services) {
            if (!isRunning())
                return;
            LOG.info("<<< Stopping runtime container...");
            List<ContainerService> servicesToStop = Arrays.asList(getServices());
            Collections.reverse(servicesToStop);
            try {
                for (ContainerService service : servicesToStop) {
                    LOG.fine("Stopping service: " + service);
                    service.stop(this);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            } finally {
                waitingThread.interrupt();
                waitingThread = null;
            }
            LOG.info("<<< Runtime container stopped");
        }
    }

    /**
     * Starts the container and a non-daemon thread that waits forever.
     */
    public void startBackground() throws Exception {
        start();
        waitingThread = ContainerThreads.startWaitingThread();
    }

    public ContainerService[] getServices() {
        synchronized (services) {
            return services.values().toArray(new ContainerService[services.size()]);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends ContainerService> Collection<T> getServices(Class<T> type) {
        synchronized (services) {
            Set<T> result = new HashSet<>();
            for (ContainerService containerService : services.values()) {
                if (type.isAssignableFrom(containerService.getClass())) {
                    result.add((T) containerService);
                }
            }
            return result;
        }
    }

    public <T extends ContainerService> boolean hasService(Class<T> type) {
        return getServices(type).size() > 0;
    }

    /**
     * Get a service instance matching the specified type exactly, or if that yields
     * no result, try to get the first service instance that has a matching interface.
     */
    @SuppressWarnings("unchecked")
    public <T extends ContainerService> T getService(Class<T> type) {
        synchronized (services) {
            T service = (T) services.get(type);
            if (service == null) {
                for (ContainerService containerService : services.values()) {
                    if (type.isAssignableFrom(containerService.getClass())) {
                        service = (T) containerService;
                        break;
                    }
                }
            }
            if (service == null)
                throw new IllegalStateException("Missing required service: " + type);
            return service;
        }
    }

}
