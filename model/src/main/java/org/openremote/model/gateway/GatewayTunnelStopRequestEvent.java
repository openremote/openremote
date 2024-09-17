package org.openremote.model.gateway;

import org.openremote.model.event.shared.SharedEvent;

public class GatewayTunnelStopRequestEvent extends SharedEvent {

    protected GatewayTunnelInfo info;

    protected GatewayTunnelStopRequestEvent() {

    }

    public GatewayTunnelStopRequestEvent(GatewayTunnelInfo info) {
        this.info = info;
    }

    public GatewayTunnelInfo getInfo() {
        return info;
    }
}
