package org.openremote.model.telematics.core;

import org.openremote.model.attribute.Attribute;

import java.util.*;

/**
 * A collection of attributes parsed from a telematics device message.
 * <p>
 * This is simply a container for {@link Attribute} objects - the standard
 * OpenRemote data type. All device data (location, speed, IO elements, etc.)
 * is represented as Attributes, not as custom fields.
 * <p>
 * DeviceMessage adds only the minimal metadata needed for routing:
 * <ul>
 *   <li>Device identifier (IMEI) - to find/create the target asset</li>
 *   <li>Protocol name - for logging and metrics</li>
 * </ul>
 */
public class DeviceMessage {

    private final String deviceId;
    private final String protocolName;
    private final List<Attribute<?>> attributes;

    private DeviceMessage(Builder builder) {
        this.deviceId = Objects.requireNonNull(builder.deviceId, "deviceId cannot be null");
        this.protocolName = builder.protocolName;
        this.attributes = Collections.unmodifiableList(new ArrayList<>(builder.attributes));
    }

    /**
     * The unique device identifier (typically IMEI for GPS trackers).
     * Used to find or create the corresponding asset.
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * The name of the protocol that produced this message (e.g., "teltonika").
     * Used for logging and metrics.
     */
    public String getProtocolName() {
        return protocolName;
    }

    /**
     * All attributes parsed from this message.
     * These are ready to be applied to an asset.
     */
    public List<Attribute<?>> getAttributes() {
        return attributes;
    }

    /**
     * Get an attribute by name.
     */
    public Optional<Attribute<?>> getAttribute(String name) {
        return attributes.stream()
                .filter(a -> a.getName().equals(name))
                .findFirst();
    }

    /**
     * Get an attribute value by name, cast to the expected type.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getAttributeValue(String name, Class<T> type) {
        return getAttribute(name)
                .flatMap(Attribute::getValue)
                .filter(v -> type.isAssignableFrom(v.getClass()))
                .map(v -> (T) v);
    }

    /**
     * Number of attributes in this message.
     */
    public int size() {
        return attributes.size();
    }

    @Override
    public String toString() {
        return "DeviceMessage{" +
                "deviceId='" + deviceId + '\'' +
                ", protocol='" + protocolName + '\'' +
                ", attributes=" + attributes.size() +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String deviceId;
        private String protocolName;
        private final List<Attribute<?>> attributes = new ArrayList<>();

        public Builder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public Builder protocolName(String protocolName) {
            this.protocolName = protocolName;
            return this;
        }

        public Builder addAttribute(Attribute<?> attribute) {
            this.attributes.add(attribute);
            return this;
        }

        public Builder addAttributes(Collection<? extends Attribute<?>> attrs) {
            this.attributes.addAll(attrs);
            return this;
        }

        public DeviceMessage build() {
            return new DeviceMessage(this);
        }
    }
}
