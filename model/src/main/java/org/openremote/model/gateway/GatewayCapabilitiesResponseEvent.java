package org.openremote.model.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openremote.model.event.shared.SharedEvent;

public class GatewayCapabilitiesResponseEvent extends SharedEvent {

    public static final String TYPE = "gateway-capabilities-response";
    protected final boolean tunnelingSupported;

    @JsonCreator
    public GatewayCapabilitiesResponseEvent(boolean tunnelingSupported) {
        this.tunnelingSupported = tunnelingSupported;
    }

    public boolean isTunnelingSupported() {
        return tunnelingSupported;
    }

    @Override
    public String toString() {
        return "GatewayCapabilitiesResponseEvent{" +
                "tunnelingSupported=" + tunnelingSupported +
                ", timestamp=" + timestamp +
                '}';
    }
}
