package org.openremote.model.telematics.teltonika;

import org.openremote.model.telematics.AbstractPayload;
import org.openremote.model.telematics.DeviceParameter;

import java.util.Map;

public class TeltonikaDataPayload extends AbstractPayload {

    protected Long timestamp;

    public TeltonikaDataPayload(Map<String, Object> properties) {
        timestamp = properties.containsKey("ts") ? Long.parseLong(properties.get("ts").toString()) : 0;
        properties.forEach((key, value) -> {
            DeviceParameter parameter = new TeltonikaDataParameter(key);
            this.put(parameter, value);
        });
    }

    @Override
    public Long getTimestamp() {
        return timestamp;
    }
}
