package org.openremote.model.gateway;

import tools.jackson.annotation.JsonCreator;
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

    @Override
    public String toString() {
        return "GatewayTunnelStopResponseEvent{" +
                "error='" + error + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
