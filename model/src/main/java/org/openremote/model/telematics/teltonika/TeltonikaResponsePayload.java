package org.openremote.model.telematics.teltonika;

import org.openremote.model.telematics.DeviceParameter;
import org.openremote.model.telematics.Payload;

import java.util.Map;
import java.util.Objects;

public class TeltonikaResponsePayload implements Payload {

    protected String response;

    public TeltonikaResponsePayload(String response) {
        this.response = response;
    }

    @Override
    public Object get(DeviceParameter parameter) {
        return response;
    }

    @Override
    public <T> T get(DeviceParameter parameter, Class<T> type) {
        return (T) response;
    }

    @Override
    public Long getTimestamp() {
        return 0L;
    }

    @Override
    public String getImei() {
        return "";
    }

    @Override
    public boolean contains(DeviceParameter parameter) {
        return Objects.equals(parameter.getDescriptor().getName(), "response");
    }

    @Override
    public Map<DeviceParameter, Object> asMap() {
        return Map.of(TeltonikaResponseParameter.Instance, response);
    }

    public String getResponse() {
        return response;
    }
}
