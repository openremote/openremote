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
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.prometheus.client.CollectorRegistry;
import org.openremote.container.concurrent.ContainerScheduledExecutor;
import org.openremote.container.util.LogUtil;
import org.openremote.model.ContainerService;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.ValueUtil;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.System.Logger.Level.*;
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

    public static final System.Logger LOG = System.getLogger(Container.class.getName());
    public static ScheduledExecutorService EXECUTOR_SERVICE;
    public static final String OR_SCHEDULED_TASKS_THREADS_MAX = "OR_SCHEDULED_TASKS_THREADS_MAX";
    public static final int OR_SCHEDULED_TASKS_THREADS_MAX_DEFAULT = Math.max(Runtime.getRuntime().availableProcessors(), 2);
    protected final Map<String, String> config = new HashMap<>();
    protected final boolean devMode;
    protected MeterRegistry meterRegistry;

    protected Thread waitingThread;
    protected final Map<Class<? extends ContainerService>, ContainerService> services = new LinkedHashMap<>();

    static {
        LogUtil.initialiseJUL();
    }

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

        boolean metricsEnabled = getBoolean(getConfig(), OR_METRICS_ENABLED, OR_METRICS_ENABLED_DEFAULT);
        LOG.log(INFO, "Metrics enabled: " + metricsEnabled);

        if (metricsEnabled) {
            // TODO: Add a meter registry provider SPI to make this pluggable
            meterRegistry = new io.micrometer.prometheus.PrometheusMeterRegistry(PrometheusConfig.DEFAULT, io.prometheus.client.CollectorRegistry.defaultRegistry, Clock.SYSTEM);
        }

        int scheduledTasksThreadsMax = getInteger(
            getConfig(),
            OR_SCHEDULED_TASKS_THREADS_MAX,
            OR_SCHEDULED_TASKS_THREADS_MAX_DEFAULT);

        EXECUTOR_SERVICE = new ContainerScheduledExecutor("Scheduled task", scheduledTasksThreadsMax);

        if (meterRegistry != null) {
            EXECUTOR_SERVICE = ExecutorServiceMetrics.monitor(meterRegistry, EXECUTOR_SERVICE, "ContainerExecutorService");
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
        LOG.log(INFO, ">>> Starting runtime container...");
        try {
            for (ContainerService service : getServices()) {
                LOG.log(INFO, "Initializing service: " + service.getClass().getName());
                service.init(Container.this);
            }

            // Initialise the asset model
            ValueUtil.initialise(this);

            if (this.devMode) {
                ValueUtil.JSON.enable(SerializationFeature.INDENT_OUTPUT);
            }

            for (ContainerService service : getServices()) {
                LOG.log(INFO, "Starting service: " + service.getClass().getName());
                service.start(Container.this);
            }
        } catch (Exception ex) {
            LOG.log(ERROR, ">>> Runtime container startup failed", ex);
            throw ex;
        }
        LOG.log(INFO, ">>> Runtime container startup complete");
    }

    public synchronized void stop() {
        if (!isRunning())
            return;
        LOG.log(INFO, "<<< Stopping runtime container...");

        List<ContainerService> servicesToStop = Arrays.asList(getServices());
        Collections.reverse(servicesToStop);
        for (ContainerService service : servicesToStop) {
            LOG.log(INFO, "Stopping service: " + service.getClass().getName());
            try {
                service.stop(this);
            } catch (Exception e) {
                LOG.log(INFO, "Exception thrown whilst stopping service: " + service.getClass().getName(), e);
            }
        }

        try {
            LOG.log(INFO, "Cancelling scheduled tasks");
            EXECUTOR_SERVICE.shutdown();
        } catch (Exception e) {
            LOG.log(WARNING, "Exception thrown whilst trying to stop scheduled tasks", e);
        }

        Metrics.globalRegistry.remove(meterRegistry);
        CollectorRegistry.defaultRegistry.clear();
        meterRegistry = null;
        waitingThread.interrupt();
        waitingThread = null;
        LOG.log(INFO, "<<< Runtime container stopped");
    }

    /**
     * Starts the container and a non-daemon thread that waits forever.
     */
    public void startBackground() throws Exception {
        start();
        waitingThread = startWaitingThread();
    }

    static Thread startWaitingThread() {
        Thread thread = new Thread("Container Waiting") {
            @Override
            public void run() {
                try {
                    new CountDownLatch(1).await();
                } catch (InterruptedException ex) {
                    // Ignore, thrown on shutdown
                }
            }
        };
        thread.setDaemon(false);
        thread.start();
        return thread;
    }

    @Override
    public ContainerService[] getServices() {
        synchronized (services) {
            return services.values().toArray(new ContainerService[0]);
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
        return !getServices(type).isEmpty();
    }

    @Override
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
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
