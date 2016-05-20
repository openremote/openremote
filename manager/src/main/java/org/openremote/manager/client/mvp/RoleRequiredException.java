package org.openremote.manager.client.mvp;

public class RoleRequiredException extends Exception {

    public RoleRequiredException() {
    }

    public RoleRequiredException(String message) {
        super(message);
    }

    public RoleRequiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public RoleRequiredException(Throwable cause) {
        super(cause);
    }

    public RoleRequiredException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
