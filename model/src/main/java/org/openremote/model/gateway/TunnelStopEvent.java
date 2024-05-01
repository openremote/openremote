package org.openremote.model.gateway;

import org.openremote.model.event.shared.SharedEvent;

public class TunnelStopEvent extends SharedEvent {

    protected TunnelInfo info;

    public TunnelStopEvent(TunnelInfo info) {
        this.info = info;
    }

    public TunnelInfo getInfo() {
        return info;
    }
}
