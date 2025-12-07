package org.openremote.model.telematics.teltonika;

import org.openremote.model.telematics.AbstractPayload;
import org.openremote.model.telematics.DeviceParameter;
import org.openremote.model.telematics.Payload;

import java.util.Map;
import java.util.Objects;

public class TeltonikaResponsePayload extends AbstractPayload {

    public TeltonikaResponsePayload(String response) {
        this.put(TeltonikaResponseParameter.Instance, response);
    }

    @Override
    public Long getTimestamp() {
        return 0L;
    }
}
