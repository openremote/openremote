package org.openremote.model.gateway;

import org.openremote.model.event.shared.SharedEvent;

public class TunnelStartEvent extends SharedEvent {

    protected TunnelInfo info;

    public TunnelStartEvent(TunnelInfo info) {
        this.info = info;
    }

    public TunnelInfo getInfo() {
        return info;
    }
}
