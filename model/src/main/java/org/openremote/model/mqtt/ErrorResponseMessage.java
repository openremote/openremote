package org.openremote.model.mqtt;

import com.fasterxml.jackson.annotation.JsonCreator;

public class ErrorResponseMessage {

    public enum Error {
        MESSAGE_EMPTY,
        MESSAGE_INVALID,
        NOT_FOUND,
        UNAUTHORIZED,
        FORBIDDEN,
        USER_DISABLED,
        SERVER_ERROR,
    }

    protected Error error;

    @JsonCreator
    public ErrorResponseMessage(Error error) {
        this.error = error;
    }

    public Error getError() {
        return error;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "error=" + error +
                '}';
    }
}
