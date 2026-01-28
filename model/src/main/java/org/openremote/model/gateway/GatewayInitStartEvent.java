package org.openremote.model.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openremote.model.event.shared.SharedEvent;

/**
 * Indicates that the gateway has connected and the central manager wants to begin synchronisation
 */
public class GatewayInitStartEvent extends SharedEvent {
    public static final String TYPE = "gateway-init-start";
    protected GatewayTunnelInfo[] activeTunnels;
    // The version of the gateway API that the central instance is using
    protected String version;
    // The public hostname of the tunnel SSH server
    protected String tunnelHostname;
    // The public port of the tunnel SSH server
    protected Integer tunnelPort;

    @JsonCreator
    public GatewayInitStartEvent(GatewayTunnelInfo[] activeTunnels, String version, String tunnelHostname, Integer tunnelPort) {
        this.activeTunnels = activeTunnels;
        this.version = version;
        this.tunnelHostname = tunnelHostname;
        this.tunnelPort = tunnelPort;
    }

    public GatewayTunnelInfo[] getActiveTunnels() {
        return activeTunnels;
    }

    public String getVersion() {
        return version;
    }

    public String getTunnelHostname() {
        return tunnelHostname;
    }

    public Integer getTunnelPort() {
        return tunnelPort;
    }
}
