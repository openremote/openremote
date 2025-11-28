package org.openremote.model.telematics;

import java.util.HashMap;
import java.util.Map;

public interface Payload {

    Object get(DeviceParameter parameter);

    <T> T get(DeviceParameter parameter, Class<T> type);

    Long getTimestamp();

    String getImei();

    boolean contains(DeviceParameter parameter);

    Map<DeviceParameter, Object> asMap(); // if you really need raw access

}
