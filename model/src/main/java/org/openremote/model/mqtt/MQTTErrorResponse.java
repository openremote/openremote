package org.openremote.model.mqtt;

import com.fasterxml.jackson.annotation.JsonCreator;

import javax.annotation.Nullable;

public class MQTTErrorResponse extends MQTTResponseMessage {

    public enum Error {
        MESSAGE_INVALID,
        NOT_FOUND,
        FORBIDDEN,
        UNAUTHORIZED,
        SERVER_ERROR,
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
