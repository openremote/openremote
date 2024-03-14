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
    protected String message;

    @JsonCreator
    public ErrorResponseMessage(Error error) {
        this.error = error;
    }

    @JsonCreator
    public ErrorResponseMessage(Error error, String message) {
        this.error = error;
        this.message = message;
    }

    public Error getError() {
        return error;
    }
}
