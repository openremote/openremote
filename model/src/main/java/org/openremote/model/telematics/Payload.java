package org.openremote.model.telematics;

import java.util.HashMap;
import java.util.Map;

public interface Payload extends Map<DeviceParameter, Object> {

    Long getTimestamp();

}
