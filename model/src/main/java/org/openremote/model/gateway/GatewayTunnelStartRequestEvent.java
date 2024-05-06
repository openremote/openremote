package org.openremote.model.gateway;

import org.openremote.model.event.shared.SharedEvent;

public class GatewayTunnelStartRequestEvent extends SharedEvent {

    protected GatewayTunnelInfo info;

    public GatewayTunnelStartRequestEvent(GatewayTunnelInfo info) {
        this.info = info;
    }

    public GatewayTunnelInfo getInfo() {
        return info;
    }
}
