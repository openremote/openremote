package org.openremote.model.mqtt;

import com.fasterxml.jackson.annotation.JsonCreator;

import javax.annotation.Nullable;

public class MQTTSuccessResponse extends MQTTResponseMessage {
    protected String realm;
    protected Object data;

    @JsonCreator
    public MQTTSuccessResponse(String realm, @Nullable Object data) {
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
