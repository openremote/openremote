package org.openremote.model.telematics.protocol;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Context information about an incoming message or connection.
 * <p>
 * This provides metadata that protocols may need for decoding:
 * <ul>
 *   <li>Device ID (if already known from prior identification)</li>
 *   <li>Transport type (TCP, UDP, MQTT)</li>
 *   <li>Remote address</li>
 *   <li>Protocol-specific state</li>
 * </ul>
 * <p>
 * MessageContext is mutable to allow protocols to update it during
 * the identification/handshake phase.
 */
public class MessageContext {

    /**
     * Transport types for telematics connections.
     */
    public enum Transport {
        TCP,
        UDP,
        MQTT,
        HTTP
    }

    private String deviceId;
    private Transport transport;
    private SocketAddress remoteAddress;
    private String realm;
    private String codecName;
    private int packetId;
    private final Map<String, Object> attributes = new HashMap<>();

    public MessageContext() {
    }

    public MessageContext(Transport transport) {
        this.transport = transport;
    }

    /**
     * The device identifier (IMEI), if known.
     */
    public Optional<String> getDeviceId() {
        return Optional.ofNullable(deviceId);
    }

    public MessageContext setDeviceId(String deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    /**
     * The transport layer used for this message.
     */
    public Transport getTransport() {
        return transport;
    }

    public MessageContext setTransport(Transport transport) {
        this.transport = transport;
        return this;
    }

    /**
     * Whether this is a connectionless (UDP) transport.
     */
    public boolean isConnectionless() {
        return transport == Transport.UDP;
    }

    /**
     * The remote address of the device.
     */
    public Optional<SocketAddress> getRemoteAddress() {
        return Optional.ofNullable(remoteAddress);
    }

    public MessageContext setRemoteAddress(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
        return this;
    }

    /**
     * The OpenRemote realm for this device.
     */
    public Optional<String> getRealm() {
        return Optional.ofNullable(realm);
    }

    public MessageContext setRealm(String realm) {
        this.realm = realm;
        return this;
    }

    /**
     * The codec name identified during parsing.
     */
    public Optional<String> getCodecName() {
        return Optional.ofNullable(codecName);
    }

    public MessageContext setCodecName(String codecName) {
        this.codecName = codecName;
        return this;
    }

    /**
     * Packet ID for protocols that require it in acknowledgments.
     */
    public int getPacketId() {
        return packetId;
    }

    public MessageContext setPacketId(int packetId) {
        this.packetId = packetId;
        return this;
    }

    /**
     * Get a custom attribute.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getAttribute(String key, Class<T> type) {
        Object value = attributes.get(key);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    /**
     * Set a custom attribute.
     */
    public MessageContext setAttribute(String key, Object value) {
        if (value == null) {
            attributes.remove(key);
        } else {
            attributes.put(key, value);
        }
        return this;
    }

    /**
     * Creates a copy of this context for a new message.
     * Preserves device ID and transport but clears per-message state.
     */
    public MessageContext copyForNewMessage() {
        MessageContext copy = new MessageContext();
        copy.deviceId = this.deviceId;
        copy.transport = this.transport;
        copy.remoteAddress = this.remoteAddress;
        copy.realm = this.realm;
        // Don't copy per-message state like packetId, codecName
        return copy;
    }

    @Override
    public String toString() {
        return "MessageContext{" +
                "deviceId='" + deviceId + '\'' +
                ", transport=" + transport +
                ", realm='" + realm + '\'' +
                '}';
    }
}
