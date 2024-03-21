package org.openremote.model.mqtt;

import com.fasterxml.jackson.annotation.JsonCreator;

public class MQTTSuccessResponse {
    protected String realm;
    protected Object data;

    public enum Success {
        CREATED,
        ACCEPTED,
    }

    protected Success success;

    @JsonCreator
    public MQTTSuccessResponse(Success success, String realm, Object data) {
        this.success = success;
        this.realm = realm;
        this.data = data;
    }

    @JsonCreator
    public MQTTSuccessResponse(Success success, String realm) {
        this.success = success;
        this.realm = realm;
    }

    public String getRealm() {
        return realm;
    }

    public Object getData() {
        return data;
    }

}
