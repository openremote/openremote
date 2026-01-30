package org.openremote.model.telematics.protocol;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a command to send to a telematics device.
 * <p>
 * Commands are protocol-agnostic - the {@link DeviceProtocol} implementation
 * is responsible for encoding them into the appropriate binary format.
 * <p>
 * Common command types:
 * <ul>
 *   <li>Configuration commands (set parameters)</li>
 *   <li>Output control (digital outputs, relays)</li>
 *   <li>Query commands (request status)</li>
 *   <li>Text/SMS commands</li>
 * </ul>
 */
public class DeviceCommand {

    /**
     * Standard command types that may be supported across protocols.
     */
    public enum Type {
        /**
         * Set a configuration parameter on the device.
         */
        CONFIGURATION,

        /**
         * Control a digital output.
         */
        OUTPUT_CONTROL,

        /**
         * Request device status or data.
         */
        QUERY,

        /**
         * Send a text/SMS message.
         */
        TEXT,

        /**
         * Raw command in protocol-specific format.
         */
        RAW,

        /**
         * Custom command type.
         */
        CUSTOM
    }

    private final Type type;
    private final String name;
    private final Map<String, Object> parameters;

    private DeviceCommand(Type type, String name, Map<String, Object> parameters) {
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.name = name;
        this.parameters = Collections.unmodifiableMap(new HashMap<>(parameters));
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, Class<T> type) {
        Object value = parameters.get(key);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return null;
    }

    public String getParameterAsString(String key) {
        Object value = parameters.get(key);
        return value != null ? value.toString() : null;
    }

    @Override
    public String toString() {
        return "DeviceCommand{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", parameters=" + parameters +
                '}';
    }

    /**
     * Create a builder for a command.
     */
    public static Builder builder(Type type) {
        return new Builder(type);
    }

    /**
     * Create a raw text command.
     */
    public static DeviceCommand text(String text) {
        return builder(Type.TEXT)
                .parameter("text", text)
                .build();
    }

    /**
     * Create a digital output control command.
     */
    public static DeviceCommand setOutput(int outputNumber, boolean state) {
        return builder(Type.OUTPUT_CONTROL)
                .name("setOutput")
                .parameter("output", outputNumber)
                .parameter("state", state)
                .build();
    }

    public static class Builder {
        private final Type type;
        private String name;
        private final Map<String, Object> parameters = new HashMap<>();

        private Builder(Type type) {
            this.type = type;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder parameter(String key, Object value) {
            this.parameters.put(key, value);
            return this;
        }

        public Builder parameters(Map<String, Object> params) {
            this.parameters.putAll(params);
            return this;
        }

        public DeviceCommand build() {
            return new DeviceCommand(type, name, parameters);
        }
    }
}
