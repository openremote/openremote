package org.openremote.model.telematics.core;

import org.openremote.model.telematics.protocol.MessageContext;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable queue payload for telematics message processing.
 */
public class TelematicsMessageEnvelope {

    private final String vendorId;
    private final String deviceId;
    private final String realm;
    private final String protocolId;
    private final MessageContext.Transport transport;
    private final Instant receivedAt;
    private final DeviceMessage message;

    public TelematicsMessageEnvelope(String vendorId,
                                     String deviceId,
                                     String realm,
                                     String protocolId,
                                     MessageContext.Transport transport,
                                     Instant receivedAt,
                                     DeviceMessage message) {
        this.vendorId = Objects.requireNonNull(vendorId, "vendorId cannot be null");
        this.deviceId = Objects.requireNonNull(deviceId, "deviceId cannot be null");
        this.realm = Objects.requireNonNull(realm, "realm cannot be null");
        this.protocolId = Objects.requireNonNull(protocolId, "protocolId cannot be null");
        this.transport = Objects.requireNonNull(transport, "transport cannot be null");
        this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt cannot be null");
        this.message = Objects.requireNonNull(message, "message cannot be null");
    }

    public String getVendorId() {
        return vendorId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getRealm() {
        return realm;
    }

    public String getProtocolId() {
        return protocolId;
    }

    public MessageContext.Transport getTransport() {
        return transport;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public DeviceMessage getMessage() {
        return message;
    }
}
