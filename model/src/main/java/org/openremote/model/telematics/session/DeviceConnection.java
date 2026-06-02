package org.openremote.model.telematics.session;

import org.openremote.model.telematics.protocol.MessageContext;

import java.time.Instant;
import java.util.Optional;

/**
 * Runtime connection state for one telematics device.
 */
public class DeviceConnection {

    private final String vendorId;
    private final String imei;
    private volatile String assetId;
    private final String realm;
    private volatile String protocolId;
    private volatile String codecId;
    private volatile MessageContext.Transport transport;
    private volatile boolean connected;
    private volatile Instant lastContact;
    private volatile long messageCount;
    private volatile long connectionCount;

    public DeviceConnection(String vendorId, String imei, String realm) {
        this.vendorId = vendorId;
        this.imei = imei;
        this.realm = realm;
        this.lastContact = Instant.now();
    }

    public String getRealm() {
        return realm;
    }

    public String getVendorId() {
        return vendorId;
    }

    public String getImei() {
        return imei;
    }

    public Optional<String> getAssetId() {
        return Optional.ofNullable(assetId);
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public Optional<String> getProtocolId() {
        return Optional.ofNullable(protocolId);
    }

    public void setProtocolId(String protocolId) {
        this.protocolId = protocolId;
    }

    public Optional<String> getCodecId() {
        return Optional.ofNullable(codecId);
    }

    public void setCodecId(String codecId) {
        this.codecId = codecId;
    }

    public Optional<MessageContext.Transport> getTransport() {
        return Optional.ofNullable(transport);
    }

    public void setTransport(MessageContext.Transport transport) {
        this.transport = transport;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public Instant getLastContact() {
        return lastContact;
    }

    public void touch() {
        this.lastContact = Instant.now();
    }

    public long getMessageCount() {
        return messageCount;
    }

    public void incrementMessageCount() {
        this.messageCount++;
    }

    public long getConnectionCount() {
        return connectionCount;
    }

    public void incrementConnectionCount() {
        this.connectionCount++;
    }
}
