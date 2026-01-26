package org.openremote.model.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.openremote.model.event.Event;
import org.openremote.model.event.RespondableEvent;
import org.openremote.model.event.shared.SharedEvent;

import java.util.Date;
import java.util.function.Consumer;

/**
 * Request the gateway to return a {@link GatewayCapabilitiesResponseEvent}
 */
public class GatewayCapabilitiesRequestEvent extends SharedEvent implements RespondableEvent {

    public static final String TYPE = "gateway-capabilities-request";
    @JsonIgnore
    protected Consumer<Event> responseConsumer;
    // The version of the gateway API that the central instance is using
    protected String version;
    // We provide active tunnels ASAP so the edge gateway can sync its current active SSH sessions (for reconnection purposes
    protected GatewayTunnelInfo[] activeTunnels;
    protected String tunnelHostname;
    protected Integer tunnelPort;

    @JsonCreator
    public GatewayCapabilitiesRequestEvent(Long timestamp, String version, String tunnelHostname, Integer tunnelPort) {
        super(timestamp != null ? timestamp : new Date().getTime());
        this.version = version;
        this.tunnelHostname = tunnelHostname;
        this.tunnelPort = tunnelPort;
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

    @Override
    public Consumer<Event> getResponseConsumer() {
        return responseConsumer;
    }

    @Override
    public void setResponseConsumer(Consumer<Event> responseConsumer) {
        this.responseConsumer = responseConsumer;
    }
}
