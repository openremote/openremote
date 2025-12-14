package org.openremote.model.telematics.teltonika;

import org.openremote.model.telematics.AbstractPayload;

public class TeltonikaResponsePayload extends AbstractPayload {

    public TeltonikaResponsePayload(String response) {
        this.put(TeltonikaResponseParameter.Instance, response);
    }

    @Override
    public Long getTimestamp() {
        return 0L;
    }
}
