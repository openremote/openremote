package org.openremote.model.gateway;

import org.openremote.model.event.shared.SharedEvent;

public class GatewayTunnelStartResponseEvent extends SharedEvent {

    protected String error;

    public GatewayTunnelStartResponseEvent() {
    }

    public GatewayTunnelStartResponseEvent(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }
}
