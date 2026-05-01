package org.openremote.model.telematics.protocol;

/**
 * Exception thrown when protocol decoding fails.
 */
public class ProtocolDecodeException extends Exception {

    private final String protocolId;
    private final String deviceId;

    public ProtocolDecodeException(String message) {
        super(message);
        this.protocolId = null;
        this.deviceId = null;
    }

    public ProtocolDecodeException(String protocolId, String message) {
        super("[" + protocolId + "] " + message);
        this.protocolId = protocolId;
        this.deviceId = null;
    }

    public ProtocolDecodeException(String protocolId, String deviceId, String message) {
        super("[" + protocolId + "/" + deviceId + "] " + message);
        this.protocolId = protocolId;
        this.deviceId = deviceId;
    }

    public ProtocolDecodeException(String message, Throwable cause) {
        super(message, cause);
        this.protocolId = null;
        this.deviceId = null;
    }

    public ProtocolDecodeException(String protocolId, String message, Throwable cause) {
        super("[" + protocolId + "] " + message, cause);
        this.protocolId = protocolId;
        this.deviceId = null;
    }

    public String getProtocolId() {
        return protocolId;
    }

    public String getDeviceId() {
        return deviceId;
    }
}
