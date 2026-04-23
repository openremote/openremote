package org.openremote.model.mqtt;

import com.fasterxml.jackson.annotation.JsonCreator;

import javax.annotation.Nullable;

public class MQTTErrorResponse extends MQTTResponseMessage {

    public enum Error {
        BAD_REQUEST,
        CONFLICT,
        NOT_FOUND,
        FORBIDDEN,
        UNAUTHORIZED,
        INTERNAL_SERVER_ERROR,
    }

    protected Error error;
    protected String message;

    @JsonCreator()
    public MQTTErrorResponse(Error error, @Nullable String message) {
        this.error = error;
        this.message = message;
    }

    public Error getError() {
        return error;
    }
}
