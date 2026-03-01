package org.openremote.model.telematics.protocol;

/**
 * Exception thrown when command encoding fails.
 */
public class ProtocolEncodeException extends Exception {

    private final String protocolId;
    private final String command;

    public ProtocolEncodeException(String message) {
        super(message);
        this.protocolId = null;
        this.command = null;
    }

    public ProtocolEncodeException(String protocolId, String command, String message) {
        super("[" + protocolId + "] Failed to encode command '" + command + "': " + message);
        this.protocolId = protocolId;
        this.command = command;
    }

    public ProtocolEncodeException(String message, Throwable cause) {
        super(message, cause);
        this.protocolId = null;
        this.command = null;
    }

    public String getProtocolId() {
        return protocolId;
    }

    public String getCommand() {
        return command;
    }
}
