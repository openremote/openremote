package org.openremote.model.mqtt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.openremote.model.util.ValueUtil;

public class MQTTSuccessResponse extends MQTTResponseMessage {
    private final String realm;
    private final Object data;

    @JsonCreator
    public MQTTSuccessResponse(String realm, @JsonDeserialize(contentAs = Object.class) Object data) {
        this.realm = realm;
        this.data = data;
    }

    public String getRealm() {
        return realm;
    }

    public Object getData() {
        return data;
    }

    public <T> T getData(Class<T> clazz) {
        return ValueUtil.convert(data, clazz);
    }

}
