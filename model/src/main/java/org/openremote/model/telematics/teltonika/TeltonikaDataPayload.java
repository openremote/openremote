package org.openremote.model.telematics.teltonika;

import org.openremote.model.telematics.DeviceParameter;
import org.openremote.model.telematics.Payload;

import java.util.HashMap;
import java.util.Map;

public class TeltonikaDataPayload implements Payload {

    protected Long timestamp;
    protected Map<DeviceParameter, Object> payload = new HashMap<>();

    public TeltonikaDataPayload(Map<String, Object> properties) {
        timestamp = properties.containsKey("ts") ? Long.parseLong(properties.get("ts").toString()) : 0;
        properties.forEach((key, value) -> {
            DeviceParameter parameter = new TeltonikaDataParameter(key);
            payload.put(parameter, value);
        });
    }

    @Override
    public Object get(DeviceParameter parameter) {
        return null;
    }

    @Override
    public <T> T get(DeviceParameter parameter, Class<T> type) {
        return null;
    }

    @Override
    public Long getTimestamp() {
        return timestamp;
    }

    @Override
    public String getImei() {
        return "";
    }

    @Override
    public boolean contains(DeviceParameter parameter) {
        return false;
    }

    @Override
    public Map<DeviceParameter, Object> asMap() {
        return payload;
    }

}
