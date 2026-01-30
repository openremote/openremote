package org.openremote.model.telematics.protocol;

/**
 * Exception thrown when command encoding fails.
 */
public class ProtocolEncodeException extends Exception {

    private final String protocolId;
    private final DeviceCommand.Type commandType;

    public ProtocolEncodeException(String message) {
        super(message);
        this.protocolId = null;
        this.commandType = null;
    }

    public ProtocolEncodeException(String protocolId, DeviceCommand.Type commandType, String message) {
        super("[" + protocolId + "] Failed to encode " + commandType + " command: " + message);
        this.protocolId = protocolId;
        this.commandType = commandType;
    }

    public ProtocolEncodeException(String message, Throwable cause) {
        super(message, cause);
        this.protocolId = null;
        this.commandType = null;
    }

    public String getProtocolId() {
        return protocolId;
    }

    public DeviceCommand.Type getCommandType() {
        return commandType;
    }
}
