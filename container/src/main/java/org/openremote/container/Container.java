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

import com.fasterxml.jackson.databind.SerializationFeature;
import org.openremote.container.concurrent.ContainerScheduledExecutor;
import org.openremote.container.concurrent.ContainerThreads;
import org.openremote.container.util.LogUtil;
import org.openremote.model.ContainerService;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.ValueUtil;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.stream.StreamSupport.stream;
import static org.openremote.container.util.MapAccess.getBoolean;
import static org.openremote.container.util.MapAccess.getInteger;

/**
 * A thread-safe registry of {@link ContainerService}s.
 * <p>
 * Create the container with {@link ContainerService}s, then let it
 * manage the life cycle of these services.
 * <p>
 * Access environment configuration through {@link #getConfig()} and the helper methods
 * in {@link org.openremote.container.util.MapAccess}. Consider using {@link org.openremote.model.Container#OR_DEV_MODE}
 * to distinguish between development and production environments.
 */
public class Container implements org.openremote.model.Container {

    protected static class NoShutdownScheduledExecutorService extends ContainerScheduledExecutor {

        public NoShutdownScheduledExecutorService(String name, int corePoolSize) {
            super(name, corePoolSize);
        }

        @Override
        public void shutdown() {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public List<Runnable> shutdownNow() {
            throw new UnsupportedOperationException();
        }

        void doShutdownNow() {
            super.shutdownNow();
        }
    }

    public static final Logger LOG;
    public static ScheduledExecutorService EXECUTOR_SERVICE;
    public static final String OR_SCHEDULED_TASKS_THREADS_MAX = "OR_SCHEDULED_TASKS_THREADS_MAX";
    public static final int OR_SCHEDULED_TASKS_THREADS_MAX_DEFAULT = Math.max(Runtime.getRuntime().availableProcessors(), 2);

    static {
        LogUtil.configureLogging();
        LOG = Logger.getLogger(Container.class.getName());
    }

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
            if (!TextUtil.isNullOrEmpty(entry.getValue())) {
                this.config.put(entry.getKey(), entry.getValue());
            }
        }

        this.devMode = getBoolean(this.config, OR_DEV_MODE, OR_DEV_MODE_DEFAULT);

        if (this.devMode) {
            ValueUtil.JSON.enable(SerializationFeature.INDENT_OUTPUT);
        }

        int scheduledTasksThreadsMax = getInteger(
            getConfig(),
            OR_SCHEDULED_TASKS_THREADS_MAX,
            OR_SCHEDULED_TASKS_THREADS_MAX_DEFAULT);

        EXECUTOR_SERVICE = new NoShutdownScheduledExecutorService("Scheduled task", scheduledTasksThreadsMax);

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

    @Override
    public Map<String, String> getConfig() {
        return config;
    }

    public boolean isDevMode() {
        return devMode;
    }

    public boolean isRunning() {
        return waitingThread != null;
    }

    public synchronized void start() throws Exception {
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

    public synchronized void stop() {
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
        }

        try {
            LOG.info("Cancelling scheduled tasks");
            ((NoShutdownScheduledExecutorService) EXECUTOR_SERVICE).doShutdownNow();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Exception thrown whilst trying to stop scheduled tasks", e);
        }

        waitingThread.interrupt();
        waitingThread = null;
        LOG.info("<<< Runtime container stopped");
    }

    /**
     * Starts the container and a non-daemon thread that waits forever.
     */
    public void startBackground() throws Exception {
        start();
        waitingThread = ContainerThreads.startWaitingThread();
    }

    @Override
    public ContainerService[] getServices() {
        synchronized (services) {
            return services.values().toArray(new ContainerService[services.size()]);
        }
    }

    @Override
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

    @Override
    public <T extends ContainerService> boolean hasService(Class<T> type) {
        return getServices(type).size() > 0;
    }

    /**
     * Get a service instance matching the specified type exactly, or if that yields
     * no result, try to get the first service instance that has a matching interface.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ContainerService> T getService(Class<T> type) throws IllegalStateException {
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

    @Override
    public ScheduledExecutorService getExecutorService() {
        return EXECUTOR_SERVICE;
    }
}
