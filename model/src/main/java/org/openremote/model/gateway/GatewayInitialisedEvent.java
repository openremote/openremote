package org.openremote.model.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openremote.model.event.shared.SharedEvent;

import java.util.Date;

/**
 * Indicates that the gateway has been fully initialised on the central instance.
 */
public class GatewayInitialisedEvent extends SharedEvent {
    protected GatewayTunnelInfo[] activeTunnels;
    public static final String TYPE = "gateway-initialised";

    @JsonCreator
    public GatewayInitialisedEvent(Date timestamp, GatewayTunnelInfo[] activeTunnels) {
        super(timestamp != null ? timestamp.getTime() : new Date().getTime());
        this.activeTunnels = activeTunnels;
    }

    public GatewayTunnelInfo[] getActiveTunnels() {
        return activeTunnels;
    }
}
