package org.openremote.model.gateway;

import org.openremote.model.event.shared.SharedEvent;

public class GatewayTunnelStopResponseEvent extends SharedEvent {

    protected String error;

    public GatewayTunnelStopResponseEvent() {
    }

    public GatewayTunnelStopResponseEvent(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }
}
