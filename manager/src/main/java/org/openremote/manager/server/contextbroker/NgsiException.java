package org.openremote.manager.server.contextbroker;

public class NgsiException extends RuntimeException {

    public NgsiException() {
    }

    public NgsiException(String message) {
        super(message);
    }

    public NgsiException(String message, Throwable cause) {
        super(message, cause);
    }

    public NgsiException(Throwable cause) {
        super(cause);
    }

    public NgsiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
