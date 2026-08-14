package org.openremote.model.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openremote.model.event.shared.SharedEvent;

public class GatewayTunnelStartResponseEvent extends SharedEvent {

    protected String error;

    @JsonCreator
    public GatewayTunnelStartResponseEvent(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }

    @Override
    public String toString() {
        return "GatewayTunnelStartResponseEvent{" +
                "error='" + error + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
