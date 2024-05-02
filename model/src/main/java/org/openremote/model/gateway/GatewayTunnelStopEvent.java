package org.openremote.model.gateway;

import org.openremote.model.event.shared.SharedEvent;

public class GatewayTunnelStopEvent extends SharedEvent {

    protected GatewayTunnelInfo info;

    public GatewayTunnelStopEvent(GatewayTunnelInfo info) {
        this.info = info;
    }

    public GatewayTunnelInfo getInfo() {
        return info;
    }
}
