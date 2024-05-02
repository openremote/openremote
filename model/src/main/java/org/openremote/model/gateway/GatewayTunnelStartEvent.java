package org.openremote.model.gateway;

import org.openremote.model.event.shared.SharedEvent;

public class GatewayTunnelStartEvent extends SharedEvent {

    protected GatewayTunnelInfo info;

    public GatewayTunnelStartEvent(GatewayTunnelInfo info) {
        this.info = info;
    }

    public GatewayTunnelInfo getInfo() {
        return info;
    }
}
