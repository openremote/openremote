package org.openremote.model.mqtt;

import com.fasterxml.jackson.annotation.JsonCreator;

public class SuccessResponseMessage {
    protected String realm;
    protected Object data;

    public enum Success {
        CREATED,
        UPDATED,
        DELETED,
        REQUESTED,
        PROVISIONED,
    }

    protected Success success;

    @JsonCreator
    public SuccessResponseMessage(Success success, String realm, Object data) {
        this.success = success;
        this.realm = realm;
        this.data = data;
    }

    public String getRealm() {
        return realm;
    }

    public Object getData() {
        return data;
    }

}
