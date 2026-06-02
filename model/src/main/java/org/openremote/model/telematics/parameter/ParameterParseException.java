package org.openremote.model.telematics.parameter;

/**
 * Exception thrown when parameter parsing fails.
 */
public class ParameterParseException extends RuntimeException {

    private final String parameterId;

    public ParameterParseException(String message) {
        super(message);
        this.parameterId = null;
    }

    public ParameterParseException(String parameterId, String message) {
        super("Failed to parse parameter " + parameterId + ": " + message);
        this.parameterId = parameterId;
    }

    public ParameterParseException(String parameterId, String message, Throwable cause) {
        super("Failed to parse parameter " + parameterId + ": " + message, cause);
        this.parameterId = parameterId;
    }

    public ParameterParseException(String message, Throwable cause) {
        super(message, cause);
        this.parameterId = null;
    }

    /**
     * Returns the parameter ID that failed to parse, if known.
     */
    public String getParameterId() {
        return parameterId;
    }
}
