package org.openremote.model.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openremote.model.event.shared.SharedEvent;

public class GatewayCapabilitiesResponseEvent extends SharedEvent {

    public static final String TYPE = "gateway-capabilities-response";
    protected final boolean tunnelingSupported;
    protected final String version;

    @JsonCreator
    public GatewayCapabilitiesResponseEvent(String version, boolean tunnelingSupported) {
        this.version = version;
        this.tunnelingSupported = tunnelingSupported;
    }

    public boolean isTunnelingSupported() {
        return tunnelingSupported;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "GatewayCapabilitiesResponseEvent{" +
                "tunnelingSupported=" + tunnelingSupported +
                ", version=" + version +
                ", timestamp=" + timestamp +
                '}';
    }
}
