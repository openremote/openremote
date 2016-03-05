package org.openremote.container;

import elemental.json.JsonValue;
import elemental.json.Json;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Container {

    public static final Logger LOG;
    public static final ObjectMapper JSON;

    static {
        if (System.getProperty("java.util.logging.config.file") == null) {
            try (InputStream is = Container.class.getClassLoader().getResourceAsStream("logging.properties")) {
                if (is != null) {
                    LogManager.getLogManager().readConfiguration(is);
                }
            } catch (IOException ignore) {
                // Ignore
            }
        }

        LOG = Logger.getLogger(Container.class.getName());
        LOG.info("Starting runtime container...");

        JSON = new ObjectMapper();
        JSON.setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false)
            .configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.NONE);

        // TODO: Server2 should use its' own object mapper maybe for elemental json
        SimpleModule elementalModule = new SimpleModule();
        elementalModule.addSerializer(JsonValue.class, new ElementalSerialiser());
        elementalModule.addDeserializer(JsonValue.class, new ElementalDeserialiser());
        JSON.registerModule(elementalModule);
    }

    public static class ElementalSerialiser extends JsonSerializer<JsonValue> {

        @Override
        public void serialize(JsonValue value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
            String str = value.toJson();
            gen.writeRaw(str);
        }
    }

    public static class ElementalDeserialiser extends JsonDeserializer<JsonValue> {

        @Override
        public JsonValue deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            String str = p.getText();
            return Json.parse(str);
        }
    }

    public static final String DEV_MODE = "DEV_MODE";
    public static final boolean DEV_MODE_DEFAULT = true;

    protected final ObjectNode config;
    protected final boolean devMode;

    protected final Map<Class<? extends ContainerService>, ContainerService> services = new ConcurrentHashMap<>();

    public static void configure(Container container) {
        if (container.getConfigBoolean(DEV_MODE, DEV_MODE_DEFAULT)) {
            JSON.enable(SerializationFeature.INDENT_OUTPUT);
        }
    }
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
        this.config = JSON.createObjectNode();
        for (Map.Entry<String, String> entry : config.entrySet()) {
            this.config.put(entry.getKey(), entry.getValue());
        }

        this.devMode = getConfigBoolean(DEV_MODE, DEV_MODE_DEFAULT);

        if (serviceStreams != null) {
            for (Stream<ContainerService> serviceStream : serviceStreams) {
                serviceStream.forEach(this::addService);
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public ObjectNode getConfig() {
        return config;
    }

    public String getConfig(String variable, String defaultValue) {
        return config.has(variable) ? config.get(variable).asText() : defaultValue;
    }

    public boolean getConfigBoolean(String variable, boolean defaultValue) {
        return config.has(variable) ? config.get(variable).asBoolean() : defaultValue;
    }

    public int getConfigInteger(String variable, int defaultValue) {
        return config.has(variable) ? config.get(variable).asInt() : defaultValue;
    }

    public boolean isDevMode() {
        return devMode;
    }

    synchronized public void start() {
        // Don't block the main thread
        new Thread() {
            @Override
            public void run() {
                for (ContainerService service : getServices()) {
                    LOG.fine("Preparing service: " + service);
                    service.prepare(Container.this);
                }
                for (ContainerService service : getServices()) {
                    LOG.fine("Starting service: " + service);
                    service.start(Container.this);
                }
                LOG.info("Runtime container startup complete");
            }
        }.start();
    }

    synchronized public void stop() {
        LOG.info("Stopping runtime container...");
        for (ContainerService service : getServices()) {
            LOG.fine("Stopping service: " + service);
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
