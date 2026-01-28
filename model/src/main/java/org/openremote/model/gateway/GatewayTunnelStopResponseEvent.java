package org.openremote.model.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openremote.model.event.shared.SharedEvent;

public class GatewayTunnelStopResponseEvent extends SharedEvent {

    protected String error;

    @JsonCreator
    public GatewayTunnelStopResponseEvent(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }
}
