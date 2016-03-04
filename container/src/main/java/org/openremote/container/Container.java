package org.openremote.container;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Container {

    private static final Logger LOG = Runtime.LOG;

    public final ObjectNode CONFIG;

    protected final Map<Class<? extends ContainerService>, ContainerService> services = new ConcurrentHashMap<>();

    public Container() {
        this(
            System.getenv(),
            StreamSupport.stream(ServiceLoader.load(ContainerService.class).spliterator(), false)
        );
    }

    public Container(ContainerService... services) {
        this(
            System.getenv(),
            Stream.concat(
                StreamSupport.stream(ServiceLoader.load(ContainerService.class).spliterator(), false),
                Stream.of(services)
            )
        );
    }

    @SafeVarargs
    public Container(Map<String, String> config, Stream<ContainerService>... serviceStreams) {
        CONFIG = Runtime.JSON.createObjectNode();
        for (Map.Entry<String, String> entry : config.entrySet()) {
            CONFIG.put(entry.getKey(), entry.getValue());
        }
        Runtime.configure(this);

        if (serviceStreams != null) {
            for (Stream<ContainerService> serviceStream : serviceStreams) {
                serviceStream.forEach(this::addService);
            }
        }

        Runtime.addShutdownHook(this::stop);
    }

    public String getConfig(String variable, String defaultValue) {
        return CONFIG.has(variable) ? CONFIG.get(variable).asText() : defaultValue;
    }

    public boolean getConfigBoolean(String variable, boolean defaultValue) {
        return CONFIG.has(variable) ? CONFIG.get(variable).asBoolean() : defaultValue;
    }

    public int getConfigInteger(String variable, int defaultValue) {
        return CONFIG.has(variable) ? CONFIG.get(variable).asInt() : defaultValue;
    }

    synchronized public void start() {
        // Don't block the main thread
        new Thread() {
            @Override
            public void run() {
                for (ContainerService service : getServices()) {
                    service.prepare(Container.this);
                }
                for (ContainerService service : getServices()) {
                    service.start(Container.this);
                }
                LOG.info("Runtime container startup complete");
            }
        }.start();
    }

    synchronized public void stop() {
        LOG.info("Stopping runtime container...");
        for (ContainerService service : getServices()) {
            service.stop(this);
        }
    }

    synchronized public void addService(ContainerService service) {
        services.put(service.getClass(), service);
    }

    synchronized public ContainerService[] getServices() {
        return services.values().toArray(new ContainerService[services.size()]);
    }

    synchronized public <T extends ContainerService> T getService(Class<T> type) {
        //noinspection unchecked
        T service = (T) services.get(type);
        if (service == null)
            throw new IllegalStateException("Missing required service: " + type);
        return service;
    }

}
