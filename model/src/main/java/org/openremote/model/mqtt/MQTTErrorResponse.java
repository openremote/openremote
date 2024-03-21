package org.openremote.model.mqtt;

import com.fasterxml.jackson.annotation.JsonCreator;

public class MQTTErrorResponse {

    public enum Error {
        MESSAGE_INVALID,
        NOT_FOUND,
        FORBIDDEN,
        SERVER_ERROR,
    }

    protected Error error;
    protected String message;

    @JsonCreator
    public MQTTErrorResponse(Error error) {
        this.error = error;
    }

    @JsonCreator
    public MQTTErrorResponse(Error error, String message) {
        this.error = error;
        this.message = message;
    }

    public Error getError() {
        return error;
    }
}
